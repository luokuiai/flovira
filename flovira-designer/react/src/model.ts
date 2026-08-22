import type {
  ApproverRule,
  ApproverStrategy,
  ApproverSubject,
  DesignerApproverStrategy,
  DesignerCapabilities,
  FloviraDefinition,
  FloviraNode,
  FloviraNodeType,
  FloviraSkip,
  FlowValidationResult,
} from './types'

export const DEFAULT_DESIGNER_CAPABILITIES: DesignerCapabilities = {
  schemaVersion: 1,
  nodeTypes: ['0', '1', '2', '3', '4', '5', '6', '7'],
  approverStrategies: [
    { code: 'USER', name: '用户', selectionType: 'RESOURCE', resourceType: 'USER', multiple: true },
    { code: 'ROLE', name: '角色', selectionType: 'RESOURCE', resourceType: 'ROLE', relationType: 'ROLE_MEMBERS', multiple: true },
    { code: 'ORGANIZATION', name: '组织', selectionType: 'RESOURCE', resourceType: 'ORGANIZATION', relationType: 'ORGANIZATION_MEMBERS', multiple: true },
    { code: 'EXPRESSION', name: '表达式', selectionType: 'EXPRESSION', multiple: false },
  ],
  approvalModes: ['OR', 'VOTE', 'COUNTERSIGN'],
  returnPolicies: ['PREVIOUS', 'ANY', 'REJECT'],
  timeoutNodeTypes: ['1', '7'],
  operations: ['SAVE', 'PUBLISH', 'VALIDATE', 'IMPORT', 'EXPORT'],
  resourceTypes: [
    'USER', 'ROLE', 'ORGANIZATION', 'SUBJECT', 'CATEGORY', 'FORM_PATH',
    'FORM_FIELD', 'DICTIONARY', 'SUBPROCESS', 'NODE_EXTENSION', 'LISTENER',
  ],
}

export const filterNodeTypes = (
  nodeTypes: FloviraNodeType[],
  capabilities: DesignerCapabilities,
): FloviraNodeType[] => nodeTypes.filter((type) => capabilities.nodeTypes.includes(type))

export const approverStrategyOptions = (capabilities: DesignerCapabilities) =>
  capabilities.approverStrategies.map((strategy) => ({ label: strategy.name, value: strategy.code }))

const NODE_NAMES: Record<FloviraNodeType, string> = {
  '0': '开始',
  '1': '审批节点',
  '2': '结束',
  '3': '互斥网关',
  '4': '并行网关',
  '5': '包含网关',
  '6': '子流程',
  '7': '等待节点',
}

let sequence = 0

const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value)) as T

export const createId = (prefix: string): string => {
  sequence += 1
  return `${prefix}_${Date.now().toString(36)}_${sequence.toString(36)}`
}

export const nodeName = (type: FloviraNodeType): string => NODE_NAMES[type]

export const createNode = (type: FloviraNodeType, name = NODE_NAMES[type]): FloviraNode => ({
  nodeType: type,
  nodeCode: createId(type === '6' ? 'subprocess' : type === '7' ? 'wait' : 'node'),
  nodeName: name,
  nodeRatio: '0',
  ext: '[]',
  skipList: [],
})

const createSkip = (source: FloviraNode, target: FloviraNode): FloviraSkip => ({
  id: createId('skip'),
  skipType: 'PASS',
  skipCondition: null,
  skipName: null,
  nowNodeCode: source.nodeCode,
  nowNodeType: source.nodeType,
  nextNodeCode: target.nodeCode,
  nextNodeType: target.nodeType,
})

export const createInitialDefinition = (): FloviraDefinition => {
  const start = createNode('0')
  const approval = createNode('1')
  const end = createNode('2')
  start.skipList = [createSkip(start, approval)]
  approval.skipList = [createSkip(approval, end)]
  return {
    flowCode: 'new_flow',
    flowName: '未命名流程',
    modelValue: 'MIMIC',
    nodeList: [start, approval, end],
  }
}

export const normalizeDefinition = (
  value?: FloviraDefinition | string | null,
): FloviraDefinition => {
  if (!value) return createInitialDefinition()
  const parsed = typeof value === 'string' ? JSON.parse(value) as FloviraDefinition : clone(value)
  const nodes = Array.isArray(parsed.nodeList) ? parsed.nodeList : []
  return {
    ...parsed,
    nodeList: nodes.map((node) => ({
      ...node,
      nodeType: String(node.nodeType) as FloviraNodeType,
      nodeCode: String(node.nodeCode),
      nodeName: node.nodeName || NODE_NAMES[String(node.nodeType) as FloviraNodeType] || '流程节点',
      skipList: Array.isArray(node.skipList) ? node.skipList.map((skip) => ({
        ...skip,
        nowNodeCode: String(skip.nowNodeCode || node.nodeCode),
        nextNodeCode: String(skip.nextNodeCode),
      })) : [],
    })),
  }
}

export const serializeDefinition = (definition: FloviraDefinition): string =>
  JSON.stringify(definition, null, 2)

export const updateNode = (
  definition: FloviraDefinition,
  nodeCode: string,
  patch: Partial<FloviraNode>,
): FloviraDefinition => ({
  ...clone(definition),
  nodeList: definition.nodeList.map((node) =>
    node.nodeCode === nodeCode ? { ...clone(node), ...clone(patch) } : clone(node)),
})

export const insertNodeAfter = (
  definition: FloviraDefinition,
  afterNodeCode: string,
  type: FloviraNodeType,
): FloviraDefinition => {
  const nextDefinition = clone(definition)
  const source = nextDefinition.nodeList.find((node) => node.nodeCode === afterNodeCode)
  if (!source || source.nodeType === '2') return nextDefinition
  const existingSkips = source.skipList.slice()
  const inserted = createNode(type)

  if (type === '3' || type === '4' || type === '5') {
    const continuation = existingSkips[0]
    const continuationNode = continuation
      ? nextDefinition.nodeList.find((node) => node.nodeCode === continuation.nextNodeCode)
      : undefined
    const left = createNode('1', '分支一')
    const right = createNode('1', '分支二')
    source.skipList = [createSkip(source, inserted)]
    inserted.skipList = [
      { ...createSkip(inserted, left), skipName: '分支一' },
      { ...createSkip(inserted, right), skipName: '分支二' },
    ]
    if (continuationNode) {
      left.skipList = [createSkip(left, continuationNode)]
      right.skipList = [createSkip(right, continuationNode)]
    }
    nextDefinition.nodeList.push(inserted, left, right)
    return nextDefinition
  }

  source.skipList = [createSkip(source, inserted)]
  inserted.skipList = existingSkips.map((skip) => ({
    ...skip,
    id: createId('skip'),
    nowNodeCode: inserted.nodeCode,
    nowNodeType: inserted.nodeType,
  }))
  nextDefinition.nodeList.push(inserted)
  return nextDefinition
}

export const deleteNode = (
  definition: FloviraDefinition,
  nodeCode: string,
): FloviraDefinition => {
  const target = definition.nodeList.find((node) => node.nodeCode === nodeCode)
  if (!target || target.nodeType === '0' || target.nodeType === '2') return clone(definition)
  const branchNodes = ['3', '4', '5'].includes(target.nodeType)
    ? target.skipList
      .map((skip) => definition.nodeList.find((node) => node.nodeCode === skip.nextNodeCode))
      .filter((node): node is FloviraNode => Boolean(node))
    : []
  const branchTargets = branchNodes
    .map((node) => node.skipList[0]?.nextNodeCode)
    .filter((code): code is string => Boolean(code))
  const sharedTarget = branchTargets.length === branchNodes.length
    && branchTargets.every((code) => code === branchTargets[0])
    ? branchTargets[0]
    : undefined
  const outgoing = sharedTarget
    ? { ...target.skipList[0], nextNodeCode: sharedTarget }
    : target.skipList[0]
  const removedCodes = new Set([
    nodeCode,
    ...(sharedTarget ? branchNodes.map((node) => node.nodeCode) : []),
  ])
  const nextDefinition = clone(definition)
  nextDefinition.nodeList = nextDefinition.nodeList
    .filter((node) => !removedCodes.has(node.nodeCode))
    .map((node) => ({
      ...node,
      skipList: node.skipList.flatMap((skip) => {
        if (!removedCodes.has(skip.nextNodeCode)) return [skip]
        if (!outgoing) return []
        const nextNode = nextDefinition.nodeList.find((item) => item.nodeCode === outgoing.nextNodeCode)
        return [{
          ...skip,
          nextNodeCode: outgoing.nextNodeCode,
          nextNodeType: nextNode?.nodeType,
        }]
      }),
    }))
  return nextDefinition
}

export const addGatewayBranch = (
  definition: FloviraDefinition,
  gatewayCode: string,
): FloviraDefinition => {
  const nextDefinition = clone(definition)
  const gateway = nextDefinition.nodeList.find((node) => node.nodeCode === gatewayCode)
  if (!gateway || !['3', '4', '5'].includes(gateway.nodeType)) return nextDefinition
  const branch = createNode('1', `分支${gateway.skipList.length + 1}`)
  const continuationCode = gateway.skipList
    .map((skip) => nextDefinition.nodeList.find((node) => node.nodeCode === skip.nextNodeCode))
    .find((node) => node?.skipList[0])?.skipList[0]?.nextNodeCode
  gateway.skipList.push({ ...createSkip(gateway, branch), skipName: branch.nodeName })
  if (continuationCode) {
    const continuation = nextDefinition.nodeList.find((node) => node.nodeCode === continuationCode)
    if (continuation) branch.skipList = [createSkip(branch, continuation)]
  }
  nextDefinition.nodeList.push(branch)
  return nextDefinition
}

export const getSubprocessConfig = (node: FloviraNode): Record<string, unknown> => {
  return getNodeExtConfig(node, 'subprocessConfig')
}

export const getNodeExtConfig = (node: FloviraNode, code: string): Record<string, unknown> => {
  try {
    const ext = typeof node.ext === 'string' ? JSON.parse(node.ext) : node.ext
    if (!Array.isArray(ext)) return {}
    const item = ext.find((entry) => entry?.code === code)
    if (!item) return {}
    return typeof item.value === 'string' ? JSON.parse(item.value) : clone(item.value || {})
  } catch {
    return {}
  }
}

export const setSubprocessConfig = (
  node: FloviraNode,
  fixedChildFlowCode: string,
): FloviraNode => {
  return setNodeExtConfig(node, 'subprocessConfig', {
    schemaVersion: 1,
    fixedChildFlowCode,
    selectionMode: 'FIXED',
    completionPolicy: 'ALL',
  })
}

export const setNodeExtConfig = (
  node: FloviraNode,
  code: string,
  config: Record<string, unknown>,
): FloviraNode => {
  let ext: Array<{ code: string; value: unknown }> = []
  try {
    const parsed = typeof node.ext === 'string' ? JSON.parse(node.ext) : node.ext
    if (Array.isArray(parsed)) ext = clone(parsed)
  } catch {
    ext = []
  }
  const value = JSON.stringify(config)
  const index = ext.findIndex((item) => item.code === code)
  if (index >= 0) ext[index] = { ...ext[index], value }
  else ext.push({ code, value })
  return { ...node, ext: JSON.stringify(ext) }
}

export const getApproverRule = (node: FloviraNode): ApproverRule => {
  const config = getNodeExtConfig(node, 'approverRule')
  return {
    schemaVersion: 1,
    strategy: String(config.strategy || 'USER'),
    selectionType: (config.selectionType || (config.strategy === 'EXPRESSION' ? 'EXPRESSION' : 'RESOURCE')) as ApproverRule['selectionType'],
    relationType: config.relationType ? String(config.relationType) : undefined,
    subjects: Array.isArray(config.subjects) ? config.subjects as ApproverSubject[] : [],
    expression: String(config.expression || ''),
  }
}

export const setApproverRule = (
  node: FloviraNode,
  strategy: ApproverStrategy | string,
  subjects: ApproverSubject[] = [],
  expression = '',
  relationType?: string,
  selectionType: ApproverRule['selectionType'] = strategy === 'EXPRESSION' ? 'EXPRESSION' : 'RESOURCE',
): FloviraNode => setNodeExtConfig(node, 'approverRule', {
  schemaVersion: 1,
  strategy,
  selectionType,
  relationType,
  subjects,
  expression: strategy === 'EXPRESSION' ? expression : undefined,
})

export const findApproverStrategy = (
  capabilities: DesignerCapabilities,
  code: string,
): DesignerApproverStrategy | undefined => capabilities.approverStrategies.find((strategy) => strategy.code === code)

export const getWaitConfig = (node: FloviraNode): Record<string, unknown> =>
  getNodeExtConfig(node, 'waitConfig')

export const setWaitConfig = (node: FloviraNode, waitKey: string): FloviraNode =>
  setNodeExtConfig(node, 'waitConfig', { schemaVersion: 1, waitKey })

export const getTimeoutConfig = (node: FloviraNode): Record<string, unknown> =>
  getNodeExtConfig(node, 'timeoutConfig')

export const setTimeoutConfig = (
  node: FloviraNode,
  patch: Record<string, unknown>,
): FloviraNode => {
  const current = getTimeoutConfig(node)
  const defaultAction = node.nodeType === '7' ? 'RESUME_WAIT' : 'AUTO_PASS'
  return setNodeExtConfig(node, 'timeoutConfig', {
    schemaVersion: 1,
    enabled: false,
    duration: 1,
    durationUnit: 'HOURS',
    action: defaultAction,
    ...current,
    ...patch,
  })
}

export const validateDefinition = (definition: FloviraDefinition): FlowValidationResult => {
  const issues: FlowValidationResult['issues'] = []
  const nodes = definition.nodeList
  const codes = new Set(nodes.map((node) => node.nodeCode))
  const incoming = new Map<string, number>()
  nodes.forEach((node) => node.skipList.forEach((skip) => {
    incoming.set(skip.nextNodeCode, (incoming.get(skip.nextNodeCode) || 0) + 1)
    if (!codes.has(skip.nextNodeCode)) {
      issues.push({ code: 'UNKNOWN_TARGET', nodeCode: node.nodeCode, message: `节点 ${node.nodeName} 指向不存在的节点` })
    }
  }))
  if (nodes.filter((node) => node.nodeType === '0').length !== 1) {
    issues.push({ code: 'START_COUNT', message: '流程必须且只能包含一个开始节点' })
  }
  if (nodes.filter((node) => node.nodeType === '2').length < 1) {
    issues.push({ code: 'END_MISSING', message: '流程至少需要一个结束节点' })
  }
  nodes.forEach((node) => {
    if (node.nodeType !== '0' && !incoming.has(node.nodeCode)) {
      issues.push({ code: 'NO_INCOMING', nodeCode: node.nodeCode, message: `${node.nodeName} 没有入口连接` })
    }
    if (node.nodeType !== '2' && node.skipList.length === 0) {
      issues.push({ code: 'NO_OUTGOING', nodeCode: node.nodeCode, message: `${node.nodeName} 没有出口连接` })
    }
    if (node.nodeType === '6' && !String(getSubprocessConfig(node).fixedChildFlowCode || '').trim()) {
      issues.push({ code: 'SUBPROCESS_REQUIRED', nodeCode: node.nodeCode, message: `${node.nodeName} 未选择固定子流程` })
    }
    if (node.nodeType === '1') {
      const configured = getNodeExtConfig(node, 'approverRule')
      if (Object.keys(configured).length > 0) {
      const approver = getApproverRule(node)
      const invalid = approver.strategy === 'EXPRESSION'
        ? !String(approver.expression || '').trim()
        : approver.selectionType === 'RESOURCE' && approver.subjects.length === 0
        if (invalid) {
          issues.push({ code: 'APPROVER_REQUIRED', nodeCode: node.nodeCode, message: `${node.nodeName} 未配置办理人` })
        }
      }
    }
    if (node.nodeType === '7' && !/^[A-Za-z][A-Za-z0-9_.:-]{0,127}$/.test(String(getWaitConfig(node).waitKey || ''))) {
      issues.push({ code: 'WAIT_KEY_REQUIRED', nodeCode: node.nodeCode, message: `${node.nodeName} 的等待标识无效` })
    }
    const timeout = getTimeoutConfig(node)
    if (timeout.enabled) {
      const duration = Number(timeout.duration)
      const validUnit = ['MINUTES', 'HOURS', 'DAYS'].includes(String(timeout.durationUnit))
      const validAction = node.nodeType === '7'
        ? timeout.action === 'RESUME_WAIT'
        : node.nodeType === '1' && ['AUTO_PASS', 'AUTO_REJECT'].includes(String(timeout.action))
      if (!Number.isFinite(duration) || duration < 1 || !validUnit || !validAction) {
        issues.push({ code: 'TIMEOUT_INVALID', nodeCode: node.nodeCode, message: `${node.nodeName} 的超时配置无效` })
      }
    }
  })
  return { valid: issues.length === 0, issues }
}
