export interface PackageDefinition extends Record<string, any> {
  flowCode: string
  flowName: string
  businessType: string
  nodeList: Record<string, any>[]
}
const normalizePackageDefinition = (value: PackageDefinition): PackageDefinition => value

/** 流程包只读解析，不访问后端、不执行表单内容或监听器。 */
export interface PackagedForm {
  reference: string
  formCode: string
  formName: string
  version: string
  formContent: string
  ext?: string | null
}

export interface WorkflowPackage {
  schemaVersion: 1
  rootFlowCode: string
  definitions: PackageDefinition[]
  forms: PackagedForm[]
  externalFormIds: string[]
}

function check(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message)
}
const nonblank = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0

/** 接受后端包对象或 JSON 字符串，校验版本、依赖和表单清单后返回独立副本。 */
export function parseWorkflowPackage(input: unknown): WorkflowPackage {
  const value = typeof input === 'string' ? JSON.parse(input) : JSON.parse(JSON.stringify(input))
  check(value && value.schemaVersion === 1, '不支持的流程包版本')
  check(nonblank(value.rootFlowCode) && Array.isArray(value.definitions) && value.definitions.length > 0, '流程包缺少根流程')
  check(Array.isArray(value.forms) && Array.isArray(value.externalFormIds), '流程包缺少表单清单')
  const flows = new Map<string, PackageDefinition>()
  const references = new Set<string>()
  const dependencies = new Map<string, string[]>()
  const addReference = (reference: unknown) => {
    if (reference == null || reference === '') return
    check(nonblank(reference), '无效的表单引用')
    references.add(reference)
  }
  for (const flow of value.definitions) {
    check(flow && nonblank(flow.flowCode) && nonblank(flow.flowName) && nonblank(flow.businessType), '流程基本信息不完整')
    check(!flows.has(flow.flowCode), '重复的流程编码: ' + flow.flowCode)
    check(Array.isArray(flow.nodeList) && flow.nodeList.length > 0, '流程节点列表为空')
    const codes = new Set<string>()
    const children: string[] = []
    for (const node of flow.nodeList) {
      check(node && nonblank(node.nodeCode) && !codes.has(node.nodeCode), '无效或重复的节点编码')
      check(/^[0-8]$/.test(String(node.nodeType)) && Array.isArray(node.skipList), '无效的节点或连线列表')
      codes.add(node.nodeCode)
      delete node.formId
      if (String(node.nodeType) === '6') {
        const ext = typeof node.ext === 'string' ? JSON.parse(node.ext || '{}') : node.ext ?? {}
        check(ext && typeof ext === 'object' && !Array.isArray(ext), '扩展配置必须为 JSON 对象')
        const config = typeof ext.subprocessConfig === 'string' ? JSON.parse(ext.subprocessConfig) : ext.subprocessConfig
        check(config?.schemaVersion === 1 && nonblank(config.fixedChildFlowCode), '缺少有效的固定子流程配置')
        children.push(config.fixedChildFlowCode)
      }
    }
    check(flow.nodeList.filter((node: { nodeType: unknown }) => String(node.nodeType) === '0').length === 1, '流程必须有一个开始节点')
    for (const node of flow.nodeList) {
      for (const skip of node.skipList) {
        check(skip && skip.sourceNodeCode === node.nodeCode && codes.has(skip.targetNodeCode), '连线引用了不存在的节点')
      }
    }
    addReference(flow.formId)
    flows.set(flow.flowCode, normalizePackageDefinition(flow))
    dependencies.set(flow.flowCode, children)
  }
  const visited = new Set<string>()
  const path = new Set<string>()
  const visit = (code: string) => {
    check(flows.has(code), '流程包缺少根流程或子流程: ' + code)
    check(!path.has(code), '流程包存在循环子流程依赖: ' + code)
    if (visited.has(code)) return
    path.add(code)
    for (const child of dependencies.get(code)!) visit(child)
    path.delete(code)
    visited.add(code)
  }
  visit(value.rootFlowCode)
  check(visited.size === flows.size, '流程包包含根流程未引用的设计')
  const declared = new Set<string>()
  for (const form of value.forms) {
    check(form && nonblank(form.reference) && nonblank(form.formCode) && nonblank(form.formName)
      && nonblank(form.version) && nonblank(form.formContent), '无效的包内表单')
    check(!declared.has(form.reference), '重复的表单引用: ' + form.reference)
    declared.add(form.reference)
  }
  for (const reference of value.externalFormIds) {
    check(nonblank(reference) && !declared.has(reference), '重复或无效的外部表单引用')
    declared.add(reference)
  }
  check(declared.size === references.size && [...references].every((ref) => declared.has(ref)),
    '流程包表单清单与设计引用不一致')
  return { schemaVersion: 1, rootFlowCode: value.rootFlowCode, definitions: [...flows.values()],
    forms: value.forms, externalFormIds: value.externalFormIds }
}

/** 返回根流程或指定子流程，可直接传给设计器/预览组件。 */
export function getPackageDefinition(bundle: WorkflowPackage, flowCode = bundle.rootFlowCode): PackageDefinition {
  const definition = bundle.definitions.find((flow) => flow.flowCode === flowCode)
  check(definition, '流程包中不存在流程: ' + flowCode)
  return definition
}

/** 不回退到服务器同名 ID；外部表单返回 undefined，由宿主单独展示或解析。 */
export function getPackageForm(bundle: WorkflowPackage, reference: string): PackagedForm | undefined {
  return bundle.forms.find((form) => form.reference === reference)
}

/** 解析 JSON 表单内容供宿主渲染器使用；非 JSON 内容显式报错，不执行 HTML/脚本。 */
export function parsePackageFormContent(form: PackagedForm): unknown {
  return JSON.parse(form.formContent)
}
