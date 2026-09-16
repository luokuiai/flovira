/** 标准表单结构，与后端 FormDefinition / FormFieldDefinition 一致。 */
export interface FormFieldDefinition {
  key?: string
  label?: string
  dataType: 'string' | 'number' | 'boolean' | 'date' | 'datetime' | 'object' | 'array'
  fields?: FormFieldDefinition[]
  items?: FormFieldDefinition
}
export interface FormDefinition {
  schemaVersion: '1'
  fields: FormFieldDefinition[]
  renderer?: Record<string, unknown>
}
export interface FormConditionField {
  code: string
  label: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN'
}

/** 显式元数据展开；从不根据业务数据推测字段名称或结构。 */
export function getFormConditionFields(input: FormDefinition | string): FormConditionField[] {
  const definition = typeof input === 'string' ? JSON.parse(input) : input
  if (!definition || definition.schemaVersion !== '1') throw new Error('不支持的表单定义版本')
  const result: FormConditionField[] = []
  const fail = (message: string): never => { throw new Error(message) }
  const fields = (list: FormFieldDefinition[], path: string, label: string, depth: number) => {
    if (!Array.isArray(list)) fail('缺少字段定义')
    const keys = new Set<string>()
    for (const field of list) {
      if (!field || typeof field.key !== 'string' || !/^[A-Za-z_][A-Za-z0-9_]*$/.test(field.key)) fail('字段编码不合法')
      if (depth === 0 && ['this', 'root'].includes(field.key!)) fail('顶层字段编码不能使用 this 或 root')
      if (keys.has(field.key!)) fail('字段编码重复')
      keys.add(field.key!)
      if (typeof field.label !== 'string' || !field.label.trim()) fail('字段名称不能为空')
      visit(field, path ? path + '.' + field.key : field.key!, label ? label + ' / ' + field.label : field.label!, depth)
    }
  }
  const visit = (field: FormFieldDefinition, path: string, label: string, depth: number) => {
    if (!field || depth >= 32) fail('字段嵌套过深或存在循环引用')
    if (field.dataType === 'object') {
      if (!Array.isArray(field.fields) || !field.fields.length || field.items != null) fail(label + '对象必须定义子字段且不能定义 items')
      fields(field.fields!, path, label, depth + 1)
    } else if (field.dataType === 'array') {
      if (!field.items || (field.fields != null && (!Array.isArray(field.fields) || field.fields.length))) fail(label + '数组必须定义 items')
      result.push({ code: path + '.$count', label: label + ' / 数量', type: 'NUMBER' })
      visit(field.items!, path + '[]', label + ' / 每一项', depth + 1)
    } else {
      if (!['string', 'number', 'boolean', 'date', 'datetime'].includes(field.dataType)) fail(label + '字段类型不支持')
      if (field.items != null || (field.fields != null && (!Array.isArray(field.fields) || field.fields.length))) fail(label + '基础类型不能包含子字段')
      result.push({ code: path, label, type: field.dataType === 'number' ? 'NUMBER' : field.dataType === 'boolean' ? 'BOOLEAN' : 'STRING' })
    }
  }
  fields(definition.fields, '', '', 0)
  return result
}

export interface FormCondition {
  fieldCode: string
  fieldLabel: string
  fieldType: FormConditionField['type']
  operator: 'EQ' | 'NE' | 'GT' | 'GE' | 'LT' | 'LE'
  value: string
}
export interface FormConditionGroup {
  conditions: FormCondition[]
  /** 同组全部条件在同一条明细上求值。code 指向数组，不含 []。 */
  collection?: { code: string; label: string; quantifier: 'ANY' | 'ALL' }
}

export function getConditionScopes(fields: readonly FormConditionField[]): { code: string; label: string }[] {
  return Array.from(new Map(fields.filter(field => field.code.includes('[]')).map(field => [
    field.code.split('[]')[0], { code: field.code.split('[]')[0], label: field.label.split(' / 每一项')[0] },
  ])).values())
}

export function fieldsForScope(fields: readonly FormConditionField[], scope = ''): FormConditionField[] {
  return fields.filter(field => scope
    ? (field.code === scope + '[]' || field.code.startsWith(scope + '[].')) && !field.code.slice(scope.length + 2).includes('[]')
    : !field.code.includes('[]'))
}

const operators: Record<string, string> = { EQ: '==', NE: '!=', GT: '>', GE: '>=', LT: '<', LE: '<=' }
function access(code: string, root?: string): { value: string; guards: string[] } {
  const count = code === '$count' || code.endsWith('.$count')
  const path = code === '$count' ? '' : count ? code.slice(0, -7) : code
  if (path && !/^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*$/.test(path)) throw new Error('字段编码不合法')
  if (!path && !root) throw new Error('字段编码不能为空')
  const parts = path ? path.split('.') : []
  if (!root && ['this', 'root'].includes(parts[0])) throw new Error('顶层字段编码不能使用 this 或 root')
  if (parts.length > 32) throw new Error('字段路径过深')
  let value = root || '#' + parts.shift()
  const guards: string[] = []
  for (const part of parts) { guards.push(value + ' != null'); value += "['" + part + "']" }
  if (count) { guards.push(value + ' != null'); value += '.size()' }
  return { value, guards }
}
function comparison(condition: FormCondition, code: string, root?: string): string {
  const operator = operators[condition.operator]
  const type = condition.fieldType
  if (!Object.prototype.hasOwnProperty.call(operators, condition.operator) || !['STRING', 'NUMBER', 'BOOLEAN'].includes(type)
    || (type !== 'NUMBER' && !['EQ', 'NE'].includes(condition.operator))) throw new Error('比较方式与字段类型不匹配')
  if ((code === '$count' || code.endsWith('.$count')) && type !== 'NUMBER') throw new Error('数组数量必须按数值比较')
  const input = condition.value
  let value: string
  if (type === 'NUMBER') {
    if (!/^-?\d+(\.\d+)?$/.test(input) || !Number.isFinite(Number(input))) throw new Error('请输入有效数字')
    value = input
  } else if (type === 'BOOLEAN') {
    if (!['true', 'false'].includes(input)) throw new Error('请选择是或否')
    value = input
  } else value = "'" + input.replace(/'/g, "''") + "'"
  const reference = access(code, root)
  // 原有顶层字段保留原表达式；嵌套字段、集合元素缺失时一律不匹配。
  if (root || reference.guards.length) reference.guards.push(reference.value + ' != null')
  return [...reference.guards, reference.value + ' ' + operator + ' ' + value].join(' and ')
}
/** 单个普通字段比较，不允许在字段级别对数组进行量化。 */
export function compileFormCondition(code: string, type: FormConditionField['type'], operator: FormCondition['operator'], value: string): string {
  return comparison({ fieldCode: code, fieldLabel: '', fieldType: type, operator, value }, code)
}
/** 集合筛选只执行一次，整组条件在同一个 #this 上求值。 */
export function compileFormConditionGroup(group: FormConditionGroup): string {
  if (!group.conditions.length) throw new Error('请添加完整的条件规则')
  if (!group.collection) return group.conditions.map(condition => comparison(condition, condition.fieldCode)).join(' and ')
  const { code, quantifier } = group.collection
  if (!code || code.includes('[]') || code.includes('$')) throw new Error('明细组需选择单层数组；多层明细请分别建模')
  if (!['ANY', 'ALL'].includes(quantifier)) throw new Error('请选择任一条或所有条满足')
  const source = access(code)
  const predicate = group.conditions.map(condition => {
    const prefix = code + '[]'
    if (condition.fieldCode !== prefix && !condition.fieldCode.startsWith(prefix + '.')) throw new Error('同组条件必须属于同一个明细')
    const relative = condition.fieldCode.slice(prefix.length).replace(/^\./, '')
    return '(' + comparison(condition, relative, '#this') + ')'
  }).join(' and ')
  const matched = source.value + '.?[' + predicate + '].size()'
  return [...source.guards, source.value + ' != null', source.value + '.size() > 0',
    quantifier === 'ALL' ? matched + ' == ' + source.value + '.size()' : matched + ' > 0'].join(' and ')
}
