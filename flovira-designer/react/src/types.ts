import type { ReactNode } from 'react'

export type FloviraNodeType = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7'
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
}

export type ApproverSelectionType = 'RESOURCE' | 'RELATION' | 'EXPRESSION'

export interface DesignerApproverStrategy {
  code: ApproverStrategy | string
  name: string
  selectionType: ApproverSelectionType
  resourceType?: string
  relationType?: string
  multiple: boolean
}

export interface WaitConfig {
  schemaVersion: 1
  waitKey: string
}

export type TimeoutAction = 'AUTO_PASS' | 'AUTO_REJECT' | 'RESUME_WAIT'

export interface NodeTimeoutConfig {
  schemaVersion: 1
  enabled: boolean
  duration: number
  durationUnit: 'MINUTES' | 'HOURS' | 'DAYS'
  action: TimeoutAction
}

export interface FloviraSkip extends Record<string, unknown> {
  id?: string | number
  skipType?: string
  skipCondition?: string | null
  skipName?: string | null
  nowNodeCode: string
  nowNodeType?: FloviraNodeType
  nextNodeCode: string
  nextNodeType?: FloviraNodeType
  coordinate?: string
}

export interface FloviraNode extends Record<string, unknown> {
  nodeType: FloviraNodeType
  nodeCode: string
  nodeName: string
  coordinate?: string
  nodeRatio?: string | number
  permissionFlag?: string | null
  ext?: string | Array<{ code: string; value: unknown }>
  skipList: FloviraSkip[]
}

export interface FloviraDefinition extends Record<string, unknown> {
  id?: string | number
  flowCode?: string
  flowName?: string
  modelValue?: string
  version?: string | number
  nodeList: FloviraNode[]
}

export interface SubprocessDefinition {
  flowCode: string
  flowName: string
  version?: string | number
  [key: string]: unknown
}

export interface DesignerCapabilities {
  schemaVersion: 1
  nodeTypes: FloviraNodeType[]
  approverStrategies: DesignerApproverStrategy[]
  approvalModes: string[]
  returnPolicies: string[]
  timeoutNodeTypes: FloviraNodeType[]
  operations: string[]
  resourceTypes: string[]
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

export type ProviderResponse<T> = T | { data?: T }

export interface DesignerDataProvider {
  capabilities(): Promise<ProviderResponse<DesignerCapabilities>>
  queryResources(query: DesignerResourceQuery): Promise<ProviderResponse<DesignerResourcePage>>
  resolveRelationship(query: DesignerRelationshipQuery): Promise<ProviderResponse<DesignerSubject[]>>
}

export interface FlowValidationIssue {
  code: string
  message: string
  nodeCode?: string
}

export interface FlowValidationResult {
  valid: boolean
  issues: FlowValidationIssue[]
}

export interface ReactFlowDesignerChange {
  definition: FloviraDefinition
  json: string
  dirty: boolean
}

export interface NodeRendererContext {
  node: FloviraNode
  selected: boolean
  summary: string
}

export interface ReactFlowDesignerProps {
  value?: FloviraDefinition | string
  defaultValue?: FloviraDefinition | string
  disabled?: boolean
  className?: string
  dataProvider?: Partial<DesignerDataProvider>
  maxHistory?: number
  onChange?: (change: ReactFlowDesignerChange) => void
  onSave?: (definition: FloviraDefinition, json: string) => void | Promise<void>
  renderNode?: (context: NodeRendererContext) => ReactNode
}

export interface ReactFlowDesignerRef {
  getDefinition(): FloviraDefinition
  getFlowJson(): string
  importJson(value: FloviraDefinition | string): void
  validate(): FlowValidationResult
  isDirty(): boolean
  resetDirty(): void
  undo(): void
  redo(): void
  zoomIn(): void
  zoomOut(): void
  resetZoom(): void
  locateStart(): void
}
