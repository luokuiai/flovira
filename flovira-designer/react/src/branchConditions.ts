import { createId, createNode, findBranchMerge, getNodeExtConfig, insertNodeAfter, setNodeExtConfig } from './model'
import type { DesignerConditionGroup, FloviraDefinition, FloviraNode, FloviraNodeType, FloviraSkip } from './types'

export interface BranchRule {
  mode: 'rules' | 'expression' | 'default' | 'always'
  groups: DesignerConditionGroup[]
  expression: string
}

const operators = { EQ: '==', NE: '!=', GT: '>', GE: '>=', LT: '<', LE: '<=' }
export const operatorLabels = { EQ: '等于', NE: '不等于', GT: '大于', GE: '大于等于', LT: '小于', LE: '小于等于' }

/** Uses the existing SpEL strategy; field names and values never become executable fragments. */
export function compileBranchConditions(groups: DesignerConditionGroup[]): string {
  if (!groups.length || groups.some((group) => !group.conditions.length)) throw new Error('请添加完整的条件规则')
  const expression = groups.map((group) => `(${group.conditions.map((condition) => {
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(condition.fieldCode)) throw new Error('字段编码需为字母、数字或下划线，且不能以数字开头')
    const operator = operators[condition.operator]
    if (!operator || (condition.fieldType !== 'NUMBER' && !['EQ', 'NE'].includes(condition.operator))) throw new Error('比较方式与字段类型不匹配')
    let value: string
    if (condition.fieldType === 'NUMBER') {
      if (!/^-?\d+(\.\d+)?$/.test(condition.value) || !Number.isFinite(Number(condition.value))) throw new Error('请输入有效数字')
      value = condition.value
    } else if (condition.fieldType === 'BOOLEAN') {
      if (!['true', 'false'].includes(condition.value)) throw new Error('请选择是或否')
      value = condition.value
    } else {
      value = `'${condition.value.replace(/'/g, "''")}'`
    }
    return `#${condition.fieldCode} ${operator} ${value}`
  }).join(' and ')})`).join(' or ')
  return `spel@@#{${expression}}`
}

export function getBranchRule(node: FloviraNode, index: number): BranchRule {
  const expression = node.skipList[index]?.skipCondition || ''
  const saved = (getNodeExtConfig(node, 'branchConditions').rules as BranchRule[] | undefined)?.[index]
  if (saved && Array.isArray(saved.groups) && saved.expression === expression) return saved
  return { mode: expression ? 'expression' : node.nodeType === '4' || node.nodeType === '5' ? 'always' : 'default', groups: [], expression }
}

export function setBranchRule(node: FloviraNode, index: number, name: string, rule: BranchRule): FloviraNode {
  const rules = node.skipList.map((_, itemIndex) => itemIndex === index ? rule : getBranchRule(node, itemIndex))
  return setNodeExtConfig({ ...node, skipList: node.skipList.map((skip, itemIndex) => itemIndex === index
    ? { ...skip, skipName: name, skipCondition: rule.expression || null } : skip) }, 'branchConditions', { schemaVersion: 1, rules })
}

export function branchSummary(node: FloviraNode, index: number): string {
  const rule = getBranchRule(node, index)
  if (node.nodeType === '4') return '与其他分支同时执行'
  if (rule.mode === 'default') return '其他条件都不满足时进入'
  if (rule.mode === 'always') return '每次都进入此分支'
  if (rule.mode === 'expression') return rule.expression || '请设置条件'
  return rule.groups.map((group) => group.conditions.map((condition) =>
    `${condition.fieldLabel || condition.fieldCode} ${operatorLabels[condition.operator]} ${condition.fieldType === 'BOOLEAN' ? condition.value === 'true' ? '是' : '否' : condition.value}`
  ).join(' 且 ')).join(' 或 ') || '请设置条件'
}

const edge = (source: FloviraNode, target: FloviraNode): FloviraSkip => ({
  id: createId(), skipType: 'PASS', sourceNodeCode: source.nodeCode, sourceNodeType: source.nodeType,
  targetNodeCode: target.nodeCode, targetNodeType: target.nodeType,
})

/** New canvas branches are edges; no approval tasks are created just to represent a condition. */
export function insertCanvasNode(definition: FloviraDefinition, sourceCode: string, type: FloviraNodeType, branchIndex?: number): FloviraDefinition {
  if (branchIndex === undefined && !['3', '4', '5'].includes(type)) return insertNodeAfter(definition, sourceCode, type)
  const next: FloviraDefinition = JSON.parse(JSON.stringify(definition))
  const source = next.nodeList.find((node) => node.nodeCode === sourceCode)
  if (!source || source.nodeType === '2') return next
  const selected = branchIndex === undefined ? source.skipList[0] : source.skipList[branchIndex]
  const target = next.nodeList.find((node) => node.nodeCode === selected?.targetNodeCode)
  if (!target) throw new Error('添加节点前需要有效的后续连接')
  let added = createNode(type)
  if (['3', '4', '5'].includes(type)) {
    added.skipList = [edge(added, target), edge(added, target)]
    added = setBranchRule(added, 0, '分支一', { mode: type === '4' ? 'always' : 'rules', groups: [], expression: '' })
    added = setBranchRule(added, 1, type === '3' ? '其他条件' : '分支二', { mode: type === '3' ? 'default' : type === '4' ? 'always' : 'rules', groups: [], expression: '' })
  } else added.skipList = [edge(added, target)]
  if (branchIndex === undefined) source.skipList = [edge(source, added)]
  else source.skipList[branchIndex] = { ...selected, targetNodeCode: added.nodeCode, targetNodeType: added.nodeType }
  next.nodeList.push(added)
  return next
}

export function addCanvasBranch(definition: FloviraDefinition, code: string): FloviraDefinition {
  const next: FloviraDefinition = JSON.parse(JSON.stringify(definition))
  const node = next.nodeList.find((item) => item.nodeCode === code)!
  const merge = findBranchMerge(next, node.skipList.map((skip) => skip.targetNodeCode))
  const target = next.nodeList.find((item) => item.nodeCode === merge)
  if (!target) throw new Error('当前分支没有公共后续节点，无法自动添加分支')
  const index = node.skipList.length
  node.skipList.push(edge(node, target))
  let updated = setBranchRule(node, index, `分支${index + 1}`, { mode: node.nodeType === '4' ? 'always' : 'rules', groups: [], expression: '' })
  const defaultIndex = node.nodeType === '3' ? node.skipList.findIndex((_, i) => getBranchRule(updated, i).mode === 'default') : -1
  if (defaultIndex >= 0) {
    const rules = updated.skipList.map((_, i) => getBranchRule(updated, i))
    const addedSkip = updated.skipList.pop()!
    const addedRule = rules.pop()!
    updated.skipList.splice(defaultIndex, 0, addedSkip)
    rules.splice(defaultIndex, 0, addedRule)
    updated = setNodeExtConfig(updated, 'branchConditions', { schemaVersion: 1, rules })
  }
  next.nodeList = next.nodeList.map((item) => item.nodeCode === code ? updated : item)
  return next
}

/** Remove only nodes that lose every incoming connection; shared continuations stay intact. */
function pruneDetached(definition: FloviraDefinition, candidates: Set<string>): FloviraDefinition {
  let changed = true
  while (changed) {
    changed = false
    const incoming = new Set(definition.nodeList.flatMap((node) => node.skipList.map((skip) => skip.targetNodeCode)))
    definition.nodeList = definition.nodeList.filter((node) => {
      if (candidates.has(node.nodeCode) && !incoming.has(node.nodeCode) && !['0', '2'].includes(node.nodeType)) {
        changed = true
        return false
      }
      return true
    })
  }
  return definition
}

function descendants(definition: FloviraDefinition, codes: string[], stop?: string): Set<string> {
  const result = new Set<string>()
  const nodes = new Map(definition.nodeList.map((node) => [node.nodeCode, node]))
  for (let index = 0; index < codes.length; index += 1) {
    const code = codes[index]
    if (code === stop || result.has(code)) continue
    result.add(code)
    nodes.get(code)?.skipList.filter((skip) => skip.skipType !== 'REJECT').forEach((skip) => codes.push(skip.targetNodeCode))
  }
  return result
}

export function removeCanvasBranch(definition: FloviraDefinition, code: string, index: number): FloviraDefinition {
  const next: FloviraDefinition = JSON.parse(JSON.stringify(definition))
  const node = next.nodeList.find((item) => item.nodeCode === code)!
  if (node.skipList.length < 2) return next
  const merge = findBranchMerge(next, node.skipList.map((skip) => skip.targetNodeCode))
  const candidates = descendants(next, [node.skipList[index].targetNodeCode], merge)
  const rules = node.skipList.map((_, i) => getBranchRule(node, i)).filter((_, i) => i !== index)
  node.skipList.splice(index, 1)
  if (node.skipList.length === 1) {
    const continuation = node.skipList[0]
    next.nodeList = next.nodeList.filter((item) => item.nodeCode !== code).map((item) => ({ ...item,
      skipList: item.skipList.map((skip) => skip.targetNodeCode === code
        ? { ...skip, targetNodeCode: continuation.targetNodeCode, targetNodeType: continuation.targetNodeType } : skip),
    }))
  } else next.nodeList = next.nodeList.map((item) => item.nodeCode === code
    ? setNodeExtConfig(node, 'branchConditions', { schemaVersion: 1, rules }) : item)
  return pruneDetached(next, candidates)
}

export function removeCanvasSplit(definition: FloviraDefinition, code: string): FloviraDefinition {
  const next: FloviraDefinition = JSON.parse(JSON.stringify(definition))
  const node = next.nodeList.find((item) => item.nodeCode === code)!
  const merge = findBranchMerge(next, node.skipList.map((skip) => skip.targetNodeCode))
  const target = next.nodeList.find((item) => item.nodeCode === merge)
  if (!target) throw new Error('分支没有公共后续节点，无法整体删除')
  const candidates = descendants(next, node.skipList.map((skip) => skip.targetNodeCode), merge)
  next.nodeList = next.nodeList.filter((item) => item.nodeCode !== code).map((item) => ({ ...item,
    skipList: item.skipList.map((skip) => skip.targetNodeCode === code
      ? { ...skip, targetNodeCode: target.nodeCode, targetNodeType: target.nodeType } : skip),
  }))
  return pruneDetached(next, candidates)
}
