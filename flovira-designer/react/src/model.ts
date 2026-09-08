import type {
  ApproverRule,
  ApproverStrategy,
  ApproverSubject,
  DesignerApproverOption,
  DesignerApproverStrategy,
  DesignerCapabilities,
  FloviraDefinition,
  FloviraNode,
  FloviraNodeType,
  FloviraSkip,
  FlowValidationResult,
  NodeControlConfig,
} from './types'

const MULTI_APPROVER_OPTION: DesignerApproverOption = {
  code: 'approvalMode',
  name: '多人审批策略',
  defaultValue: 'OR',
  nodeTypes: ['1'],
  condition: 'MULTIPLE',
  choices: [
    { value: 'COUNTERSIGN', label: '会签' },
    { value: 'OR', label: '或签' },
    { value: 'VOTE', label: '票签' },
  ],
}

const SAME_AS_STARTER_OPTION: DesignerApproverOption = {
  code: 'sameAsStarterAction',
  name: '审批人与提交人为同一人时',
  defaultValue: 'SELF_APPROVE',
  nodeTypes: ['1'],
  condition: 'ALWAYS',
  choices: [
    { value: 'SELF_APPROVE', label: '本人审批' },
    { value: 'AUTO_SKIP_OR_TRANSFER', label: '跳过或由其他人审批' },
    { value: 'TRANSFER_TO_ORG_MANAGER', label: '转交部门负责人' },
  ],
}

export const DEFAULT_DESIGNER_CAPABILITIES: DesignerCapabilities = {
  schemaVersion: 1,
  nodeTypes: ['0', '1', '2', '3', '4', '5', '6', '7', '8'],
  approverStrategies: [
    { code: 'USER', name: '用户', selectionType: 'RESOURCE', resourceType: 'USER', multiple: true, editorType: 'DIALOG', resultCardinality: 'ONE_OR_MORE', options: [MULTI_APPROVER_OPTION, SAME_AS_STARTER_OPTION] },
    { code: 'ROLE', name: '角色', selectionType: 'RESOURCE', resourceType: 'ROLE', relationType: 'ROLE_MEMBERS', multiple: true, editorType: 'DIALOG', resultCardinality: 'ZERO_OR_MORE', options: [MULTI_APPROVER_OPTION, SAME_AS_STARTER_OPTION] },
    { code: 'ORGANIZATION', name: '组织', selectionType: 'RESOURCE', resourceType: 'ORGANIZATION', relationType: 'ORGANIZATION_MEMBERS', multiple: true, editorType: 'DIALOG', resultCardinality: 'ZERO_OR_MORE', options: [MULTI_APPROVER_OPTION, SAME_AS_STARTER_OPTION] },
    { code: 'EXPRESSION', name: '表达式', selectionType: 'EXPRESSION', multiple: false, editorType: 'INLINE', resultCardinality: 'EXACTLY_ONE', options: [SAME_AS_STARTER_OPTION] },
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
  '3': '条件分支',
  '4': '并行分支',
  '5': '多选分支',
  '6': '子流程',
  '7': '等待节点',
  '8': '抄送节点',
}

const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value)) as T

export const createId = (_prefix?: string): string => {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }
  const bytes = new Uint8Array(16)
  if (typeof globalThis.crypto?.getRandomValues === 'function') {
    globalThis.crypto.getRandomValues(bytes)
  } else {
    for (let index = 0; index < bytes.length; index += 1) {
      bytes[index] = Math.floor(Math.random() * 256)
    }
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const value = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${value.slice(0, 8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.slice(20)}`
}

export const nodeName = (type: FloviraNodeType): string => NODE_NAMES[type]

export const createNode = (type: FloviraNodeType, name = NODE_NAMES[type]): FloviraNode => ({
  nodeType: type,
  nodeCode: createId(type === '6' ? 'subprocess' : type === '7' ? 'wait' : type === '8' ? 'carbonCopy' : 'node'),
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
  sourceNodeCode: source.nodeCode,
  sourceNodeType: source.nodeType,
  targetNodeCode: target.nodeCode,
  targetNodeType: target.nodeType,
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
    nodeList: [start, approval, end],
  }
}

export const normalizeDefinition = (
  value?: FloviraDefinition | string | null,
): FloviraDefinition => {
  if (!value) return createInitialDefinition()
  const parsed = typeof value === 'string' ? JSON.parse(value) as FloviraDefinition : clone(value)
  delete (parsed as FloviraDefinition & { modelValue?: unknown }).modelValue
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
        sourceNodeCode: String(skip.sourceNodeCode || node.nodeCode),
        targetNodeCode: String(skip.targetNodeCode),
      })) : [],
    })),
  }
}

export const serializeDefinition = (definition: FloviraDefinition): string => {
  const saved = clone(definition) as FloviraDefinition & { modelValue?: unknown }
  delete saved.modelValue
  return JSON.stringify(saved, null, 2)
}

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
      ? nextDefinition.nodeList.find((node) => node.nodeCode === continuation.targetNodeCode)
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
    sourceNodeCode: inserted.nodeCode,
    sourceNodeType: inserted.nodeType,
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
      .map((skip) => definition.nodeList.find((node) => node.nodeCode === skip.targetNodeCode))
      .filter((node): node is FloviraNode => Boolean(node))
    : []
  const branchTargets = branchNodes
    .map((node) => node.skipList[0]?.targetNodeCode)
    .filter((code): code is string => Boolean(code))
  const sharedTarget = branchTargets.length === branchNodes.length
    && branchTargets.every((code) => code === branchTargets[0])
    ? branchTargets[0]
    : undefined
  const outgoing = sharedTarget
    ? { ...target.skipList[0], targetNodeCode: sharedTarget }
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
        if (!removedCodes.has(skip.targetNodeCode)) return [skip]
        if (!outgoing) return []
        const nextNode = nextDefinition.nodeList.find((item) => item.nodeCode === outgoing.targetNodeCode)
        return [{
          ...skip,
          targetNodeCode: outgoing.targetNodeCode,
          targetNodeType: nextNode?.nodeType,
        }]
      }),
    }))
  return nextDefinition
}

/** 找到各分支最近的公共后继，支持分支长度不同及嵌套分支。 */
export const findBranchMerge = (definition: FloviraDefinition, branchCodes: string[]): string | undefined => {
  if (branchCodes.length < 2) return undefined
  const nodes = new Map(definition.nodeList.map((node) => [node.nodeCode, node]))
  const distances = branchCodes.map((code) => {
    const result = new Map<string, number>()
    const queue = [{ code, distance: 0 }]
    for (let index = 0; index < queue.length; index += 1) {
      const current = queue[index]
      if (result.has(current.code) || !nodes.has(current.code)) continue
      result.set(current.code, current.distance)
      nodes.get(current.code)!.skipList.filter((skip) => skip.skipType !== 'REJECT').forEach((skip) => {
        queue.push({ code: skip.targetNodeCode, distance: current.distance + 1 })
      })
    }
    return result
  })
  return [...distances[0].keys()]
    .filter((code) => distances.every((distance) => distance.has(code)))
    .sort((left, right) => Math.max(...distances.map((distance) => distance.get(left)!))
      - Math.max(...distances.map((distance) => distance.get(right)!)))[0]
}

export const addGatewayBranch = (
  definition: FloviraDefinition,
  gatewayCode: string,
): FloviraDefinition => {
  const nextDefinition = clone(definition)
  const gateway = nextDefinition.nodeList.find((node) => node.nodeCode === gatewayCode)
  if (!gateway || !['3', '4', '5'].includes(gateway.nodeType)) return nextDefinition
  const branch = createNode('1', `分支${gateway.skipList.length + 1}`)
  const continuationCode = findBranchMerge(nextDefinition,
    gateway.skipList.filter((skip) => skip.skipType !== 'REJECT').map((skip) => skip.targetNodeCode))
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

export const getNodeControlConfig = (node: FloviraNode): NodeControlConfig => ({
  schemaVersion: 1,
  allowRollback: true,
  allowTransfer: false,
  allowAddSign: false,
  allowMinusSign: false,
  rejectStrategy: node.returnPolicy === 'ANY' ? 'TO_REJECTOR_SPECIFIED_NODE'
    : node.returnPolicy === 'REJECT' ? 'REJECT' : 'TO_PREVIOUS',
  rejectTargetNodeCode: '',
  resubmitStrategy: 'RESTART_FROM_BEGINNING',
  ...getNodeExtConfig(node, 'nodeControlConfig'),
})

export const setNodeControlConfig = (node: FloviraNode, patch: Partial<NodeControlConfig>): FloviraNode =>
  setNodeExtConfig(node, 'nodeControlConfig', { ...getNodeControlConfig(node), ...patch, schemaVersion: 1 })

/** 只允许选择正向路径上的前置审批节点，排除自身、后置节点和并行兄弟分支。 */
export const getRejectTargetCandidates = (definition: FloviraDefinition, nodeCode: string): FloviraNode[] => {
  const previous = new Set<string>()
  const queue = [nodeCode]
  while (queue.length) {
    const code = queue.pop()!
    for (const node of definition.nodeList) {
      if (node.nodeCode !== nodeCode && !previous.has(node.nodeCode)
        && node.skipList.some((skip) => skip.skipType !== 'REJECT' && skip.targetNodeCode === code)) {
        previous.add(node.nodeCode)
        queue.push(node.nodeCode)
      }
    }
  }
  const following = new Set<string>([nodeCode])
  const next = [nodeCode]
  while (next.length) {
    const code = next.pop()!
    const node = definition.nodeList.find((item) => item.nodeCode === code)
    for (const skip of node?.skipList || []) {
      if (skip.skipType !== 'REJECT' && !following.has(skip.targetNodeCode)) {
        following.add(skip.targetNodeCode)
        next.push(skip.targetNodeCode)
      }
    }
  }
  return definition.nodeList.filter((node) => node.nodeType === '1' && previous.has(node.nodeCode) && !following.has(node.nodeCode))
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

const getParticipantRule = (node: FloviraNode, code: string): ApproverRule => {
  const config = getNodeExtConfig(node, code)
  return {
    schemaVersion: 1,
    strategy: String(config.strategy || 'USER'),
    selectionType: (config.selectionType || (config.strategy === 'EXPRESSION' ? 'EXPRESSION' : 'RESOURCE')) as ApproverRule['selectionType'],
    relationType: config.relationType ? String(config.relationType) : undefined,
    subjects: Array.isArray(config.subjects) ? config.subjects as ApproverSubject[] : [],
    expression: String(config.expression || ''),
    config: config.config && typeof config.config === 'object'
      ? config.config as Record<string, unknown>
      : undefined,
  }
}

export const getApproverRule = (node: FloviraNode): ApproverRule => {
  const rule = getParticipantRule(node, 'approverRule')
  if (rule.config?.approvalMode === undefined && (Number(node.nodeRatio) > 0 && Number(node.nodeRatio) <= 100 || /^(passCount|rejectCount|default|spel)/.test(String(node.nodeRatio)))) {
    rule.config = { ...rule.config, approvalMode: Number(node.nodeRatio) === 100 ? 'COUNTERSIGN' : 'VOTE' }
  }
  return rule
}

export const getCarbonCopyRule = (node: FloviraNode): ApproverRule =>
  getParticipantRule(node, 'carbonCopyRule')

export const setApproverRule = (
  node: FloviraNode,
  strategy: ApproverStrategy | string,
  subjects: ApproverSubject[] = [],
  expression = '',
  relationType?: string,
  selectionType: ApproverRule['selectionType'] = strategy === 'EXPRESSION' ? 'EXPRESSION' : 'RESOURCE',
  config?: Record<string, unknown>,
): FloviraNode => {
  const next = setNodeExtConfig(node, 'approverRule', {
    schemaVersion: 1,
    strategy,
    selectionType,
    relationType,
    subjects,
    expression: selectionType === 'EXPRESSION' ? expression : undefined,
    config,
  })
  if (node.nodeType === '1') {
    if (config?.approvalMode === 'OR') next.nodeRatio = '0'
    else if (config?.approvalMode === 'COUNTERSIGN') next.nodeRatio = '100'
    else if (config?.approvalMode === 'VOTE'
      && getApproverRule(node).config?.approvalMode !== 'VOTE') next.nodeRatio = '60'
  }
  return next
}

export const setCarbonCopyRule = (
  node: FloviraNode,
  strategy: ApproverStrategy | string,
  subjects: ApproverSubject[] = [],
  expression = '',
  relationType?: string,
  selectionType: ApproverRule['selectionType'] = strategy === 'EXPRESSION' ? 'EXPRESSION' : 'RESOURCE',
  config?: Record<string, unknown>,
): FloviraNode => setNodeExtConfig(node, 'carbonCopyRule', {
  schemaVersion: 1,
  strategy,
  selectionType,
  relationType,
  subjects,
  expression: selectionType === 'EXPRESSION' ? expression : undefined,
  config,
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
    incoming.set(skip.targetNodeCode, (incoming.get(skip.targetNodeCode) || 0) + 1)
    if (!codes.has(skip.targetNodeCode)) {
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
    if (['3', '5'].includes(node.nodeType)) {
      const rules = getNodeExtConfig(node, 'branchConditions').rules
      if (Array.isArray(rules)) {
        rules.forEach((rule, index) => {
          if (['rules', 'expression'].includes(rule?.mode) && !node.skipList[index]?.skipCondition?.trim()) {
            issues.push({ code: 'BRANCH_CONDITION_REQUIRED', nodeCode: node.nodeCode,
              message: `${node.skipList[index]?.skipName || node.nodeName} 未设置条件` })
          }
        })
      }
    }
    if (node.nodeType === '6' && !String(getSubprocessConfig(node).fixedChildFlowCode || '').trim()) {
      issues.push({ code: 'SUBPROCESS_REQUIRED', nodeCode: node.nodeCode, message: `${node.nodeName} 未选择固定子流程` })
    }
    if (node.nodeType === '1') {
      const control = getNodeControlConfig(node)
      if (control.allowRollback && control.rejectStrategy === 'TO_SPECIFIED_NODE'
        && !getRejectTargetCandidates(definition, node.nodeCode).some((candidate) => candidate.nodeCode === control.rejectTargetNodeCode)) {
        issues.push({ code: 'REJECT_TARGET_INVALID', nodeCode: node.nodeCode, message: `${node.nodeName} 未选择有效的退回目标节点` })
      }
      if (getApproverRule(node).config?.approvalMode === 'VOTE'
        && !/^(passCount|rejectCount|default|spel)/.test(String(node.nodeRatio || ''))
        && !(Number(node.nodeRatio) > 0 && Number(node.nodeRatio) < 100)) {
        issues.push({ code: 'VOTE_RATIO_INVALID', nodeCode: node.nodeCode,
          message: `${node.nodeName} 的票签通过比例必须大于 0% 且小于 100%` })
      }
      const configured = getNodeExtConfig(node, 'approverRule')
      if (Object.keys(configured).length > 0) {
      const approver = getApproverRule(node)
      const invalid = approver.selectionType === 'EXPRESSION'
        ? !String(approver.expression || '').trim()
        : approver.selectionType === 'RESOURCE' && approver.subjects.length === 0
        if (invalid) {
          issues.push({ code: 'APPROVER_REQUIRED', nodeCode: node.nodeCode, message: `${node.nodeName} 未配置办理人` })
        }
      }
    }
    if (node.nodeType === '8') {
      const carbonCopy = getCarbonCopyRule(node)
      const invalid = carbonCopy.selectionType === 'EXPRESSION'
        ? !String(carbonCopy.expression || '').trim()
        : carbonCopy.selectionType === 'RESOURCE' && carbonCopy.subjects.length === 0
      if (invalid) {
        issues.push({ code: 'CARBON_COPY_REQUIRED', nodeCode: node.nodeCode, message: `${node.nodeName} 未配置抄送人` })
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
