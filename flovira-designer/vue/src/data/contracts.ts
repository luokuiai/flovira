export type DesignerNodeType = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8'
export type ApproverStrategy = 'USER' | 'ROLE' | 'ORGANIZATION' | 'EXPRESSION'

export interface ApproverSubject {
  id: string
  type: string
  name?: string
}

export interface ApproverRule {
  schemaVersion: 1
  strategy: ApproverStrategy | string
  selectionType: ApproverSelectionType
  relationType?: string
  subjects: ApproverSubject[]
  expression?: string
  config?: Record<string, unknown>
}

export type ApproverSelectionType = 'RESOURCE' | 'RELATION' | 'EXPRESSION'
export type ApproverEditorType = 'NONE' | 'INLINE' | 'DIALOG'
export type ApproverResultCardinality = 'EXACTLY_ONE' | 'ONE_OR_MORE' | 'ZERO_OR_ONE' | 'ZERO_OR_MORE'
export type ApproverOptionCondition = 'ALWAYS' | 'MULTIPLE' | 'EMPTY'

export interface DesignerApproverOptionChoice {
  value: string
  label: string
  disabled?: boolean
}

export interface DesignerApproverOption {
  code: string
  name: string
  choices: DesignerApproverOptionChoice[]
  defaultValue?: string
  nodeTypes?: DesignerNodeType[]
  condition?: ApproverOptionCondition
}

export interface DesignerApproverStrategy {
  code: ApproverStrategy | string
  name: string
  selectionType: ApproverSelectionType
  resourceType?: string
  relationType?: string
  multiple: boolean
  editorType?: ApproverEditorType
  editorKey?: string
  resultCardinality?: ApproverResultCardinality
  options?: DesignerApproverOption[]
}

export interface DesignerCapabilities {
  schemaVersion: 1
  nodeTypes: DesignerNodeType[]
  approverStrategies: DesignerApproverStrategy[]
  approvalModes: string[]
  returnPolicies: string[]
  timeoutNodeTypes: DesignerNodeType[]
  operations: string[]
  resourceTypes: string[]
}

const MULTI_APPROVER_OPTION: DesignerApproverOption = {
  code: 'approvalMode',
  name: '多人审批策略',
  defaultValue: 'OR',
  nodeTypes: ['1'],
  condition: 'MULTIPLE',
  choices: [
    { value: 'OR', label: '或签' },
    { value: 'VOTE', label: '票签' },
    { value: 'COUNTERSIGN', label: '会签' },
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

export interface DesignerResourceQuery {
  resourceType: string
  keyword?: string
  scopeId?: string
  pageNum?: number
  pageSize?: number
  parameters?: Record<string, unknown>
}

export interface DesignerResourceItem {
  id: string
  code?: string
  name: string
  parentId?: string
  resourceType: string
  disabled?: boolean
  metadata?: Record<string, unknown>
}

export interface DesignerResourcePage {
  items: DesignerResourceItem[]
  total: number
}

export interface DesignerRelationshipQuery {
  relationType: string
  subjectId?: string
  organizationId?: string
  context?: Record<string, unknown>
}

export interface DesignerSubject {
  id: string
  type: string
  name?: string
  metadata?: Record<string, unknown>
}

export const createApproverRule = (
  strategy: ApproverStrategy | string,
  subjects: ApproverSubject[] = [],
  expression?: string,
  relationType?: string,
  selectionType: ApproverSelectionType = strategy === 'EXPRESSION' ? 'EXPRESSION' : 'RESOURCE',
): ApproverRule => ({ schemaVersion: 1, strategy, selectionType, relationType, subjects, expression })

export type ApiResponse<T> = T | { data?: T }

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
    'USER', 'ROLE', 'ORGANIZATION', 'SUBJECT', 'CATEGORY', 'FORM',
    'FORM_FIELD', 'DICTIONARY', 'SUBPROCESS', 'NODE_EXTENSION', 'LISTENER',
  ],
}

export function unwrapData<T>(response: ApiResponse<T>): T | undefined {
  if (response && typeof response === 'object' && !Array.isArray(response) && 'data' in response) {
    return (response as { data?: T }).data
  }
  return response as T
}

const PALETTE_NODE_TYPES: Record<string, DesignerNodeType> = {
  start: '0',
  between: '1',
  end: '2',
  serial: '3',
  parallel: '4',
  inclusive: '5',
  subProcess: '6',
  wait: '7',
  carbonCopy: '8',
}

export function filterPaletteNodes<T extends { type: string }>(
  nodes: T[],
  capabilities: DesignerCapabilities,
): T[] {
  const available = new Set(capabilities.nodeTypes)
  return nodes.filter((node) => {
    const nodeType = PALETTE_NODE_TYPES[node.type]
    return !nodeType || available.has(nodeType)
  })
}

export interface DesignerResourceTreeNode {
  id: string
  name: string
  parentId?: string
  children: DesignerResourceTreeNode[]
}

export function resourcesToTree(items: DesignerResourceItem[]): DesignerResourceTreeNode[] {
  const nodes = new Map<string, DesignerResourceTreeNode>()
  items.forEach((item) => nodes.set(item.id, {
    id: item.id,
    name: item.name,
    parentId: item.parentId,
    children: [],
  }))
  const roots: DesignerResourceTreeNode[] = []
  nodes.forEach((node) => {
    const parent = node.parentId ? nodes.get(node.parentId) : undefined
    if (parent) parent.children.push(node)
    else roots.push(node)
  })
  return roots
}
