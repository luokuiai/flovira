import type { ComponentType, ReactElement, ReactNode } from 'react'

export type FloviraNodeType = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8'
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
  nodeTypes?: FloviraNodeType[]
  condition?: ApproverOptionCondition
}

export interface DesignerApproverStrategy {
  code: ApproverStrategy | string
  name: string
  selectionType: ApproverSelectionType
  resourceType?: string
  relationType?: string
  multiple: boolean
  /** How this strategy is configured in the node drawer. Defaults from selectionType. */
  editorType?: ApproverEditorType
  /** Stable key used by the host to select a business-specific editor. */
  editorKey?: string
  /** Runtime result range after the strategy resolves selected values to concrete people. */
  resultCardinality?: ApproverResultCardinality
  /** Additional strategy-specific choices persisted in ApproverRule.config. */
  options?: DesignerApproverOption[]
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
  sourceNodeCode: string
  sourceNodeType?: FloviraNodeType
  targetNodeCode: string
  targetNodeType?: FloviraNodeType
  coordinate?: string
}

export interface FloviraNode extends Record<string, unknown> {
  /** 外部业务表单标识；留空继承流程表单。 */
  formId?: string | null
  nodeType: FloviraNodeType
  nodeCode: string
  nodeName: string
  coordinate?: string
  nodeRatio?: string | number
  permissionFlag?: string | null
  ext?: string | Array<{ code: string; value: unknown }>
  skipList: FloviraSkip[]
}

/** 审批节点控制配置，保存在 ext.nodeControlConfig，由业务运行接口执行。 */
export interface NodeControlConfig {
  schemaVersion: 1
  allowRollback: boolean
  allowTransfer: boolean
  allowAddSign: boolean
  allowMinusSign: boolean
  rejectStrategy: 'TO_DRAFT' | 'TO_PREVIOUS' | 'TO_SPECIFIED_NODE' | 'TO_REJECTOR_SPECIFIED_NODE' | 'REJECT'
  rejectTargetNodeCode: string
  resubmitStrategy: 'RESTART_FROM_BEGINNING' | 'CONTINUE_FROM_REJECTED_NODE'
}

export interface FloviraDefinition extends Record<string, unknown> {
  /** 外部业务表单标识，由业务系统解析。 */
  formId?: string | null
  id?: string | number
  flowCode?: string
  flowName?: string
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

export type DesignerResourceLoader = (
  query: DesignerResourceQuery,
) => Promise<DesignerResourcePage | { data?: DesignerResourcePage }>

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

export type DesignerButtonVariant = 'default' | 'primary' | 'danger' | 'text'
export type DesignerControlSize = 'default' | 'compact' | 'icon'

export interface DesignerButtonProps {
  children: ReactNode
  variant?: DesignerButtonVariant
  size?: DesignerControlSize
  disabled?: boolean
  title?: string
  ariaLabel?: string
  className?: string
  onPress(): void
}

export interface DesignerInputProps {
  value: string | number
  type?: 'text' | 'number'
  disabled?: boolean
  placeholder?: string
  ariaLabel?: string
  min?: number
  className?: string
  onValueChange(value: string): void
}

export interface DesignerSelectOption {
  value: string
  label: ReactNode
  disabled?: boolean
}

export interface DesignerSelectProps {
  value: string
  options: DesignerSelectOption[]
  disabled?: boolean
  ariaLabel?: string
  className?: string
  onValueChange(value: string): void
}

export interface DesignerCheckboxProps {
  checked: boolean
  disabled?: boolean
  ariaLabel?: string
  className?: string
  children?: ReactNode
  onCheckedChange(checked: boolean): void
}

export interface DesignerRadioGroupProps {
  value: string
  options: DesignerSelectOption[]
  disabled?: boolean
  ariaLabel?: string
  className?: string
  direction?: 'horizontal' | 'vertical'
  onValueChange(value: string): void
}

export interface DesignerFieldProps {
  label: ReactNode
  hint?: ReactNode
  children: ReactNode
  className?: string
}

export interface DesignerTooltipProps {
  content: ReactNode
  children: ReactElement
  placement?: 'top' | 'bottom' | 'left' | 'right'
  disabled?: boolean
}

export interface DesignerDropdownMenuItem {
  value: string
  label: ReactNode
  icon?: ReactNode
  color?: string
  disabled?: boolean
}

export interface DesignerDropdownMenuProps {
  trigger: ReactElement
  items: DesignerDropdownMenuItem[]
  align?: 'left' | 'right'
  onSelect(value: string): void
}

export interface DesignerDrawerProps {
  open: boolean
  title: ReactNode
  children: ReactNode
  width?: number
  ariaLabel?: string
  onClose(): void
}

export interface DesignerDialogProps {
  open: boolean
  title: ReactNode
  children: ReactNode
  width?: number
  ariaLabel?: string
  confirmText?: ReactNode
  cancelText?: ReactNode
  confirmDisabled?: boolean
  onConfirm(): void
  onClose(): void
}

export interface ApproverEditorRenderContext {
  node: FloviraNode
  strategy: DesignerApproverStrategy
  rule: ApproverRule
  selected: ApproverSubject[]
  multiple: boolean
  disabled: boolean
  onChange(subjects: ApproverSubject[]): void
  onRuleChange(rule: ApproverRule): void
}

export interface DesignerUiAdapter {
  Button: ComponentType<DesignerButtonProps>
  Input: ComponentType<DesignerInputProps>
  Select: ComponentType<DesignerSelectProps>
  Checkbox: ComponentType<DesignerCheckboxProps>
  RadioGroup: ComponentType<DesignerRadioGroupProps>
  Field: ComponentType<DesignerFieldProps>
  Tooltip: ComponentType<DesignerTooltipProps>
  DropdownMenu: ComponentType<DesignerDropdownMenuProps>
  Drawer: ComponentType<DesignerDrawerProps>
  Dialog?: ComponentType<DesignerDialogProps>
}

export interface DesignerConditionField {
  code: string
  label: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN'
}

export interface DesignerBranchCondition {
  fieldCode: string
  fieldLabel: string
  fieldType: DesignerConditionField['type']
  operator: 'EQ' | 'NE' | 'GT' | 'GE' | 'LT' | 'LE'
  value: string
}

export interface DesignerConditionGroup {
  conditions: DesignerBranchCondition[]
}

export interface DesignerConditionFieldContext {
  definition: FloviraDefinition
  node: FloviraNode
  branch: FloviraSkip
}

export type DesignerConditionFieldLoader = (
  context: DesignerConditionFieldContext,
) => Promise<readonly DesignerConditionField[]>

export interface ReactFlowDesignerProps {
  value?: FloviraDefinition | string
  defaultValue?: FloviraDefinition | string
  disabled?: boolean
  className?: string
  capabilities?: DesignerCapabilities
  queryResources?: DesignerResourceLoader
  /** Business fields available to the visual branch condition editor. */
  conditionFields?: readonly DesignerConditionField[]
  /** Load fields from the host form when opening a branch editor. Takes precedence over conditionFields. */
  queryConditionFields?: DesignerConditionFieldLoader
  /** Defaults to SpEL; override when using another backend condition strategy. */
  compileBranchConditions?: (groups: DesignerConditionGroup[]) => string
  maxHistory?: number
  onChange?: (change: ReactFlowDesignerChange) => void
  onSave?: (definition: FloviraDefinition, json: string) => void | Promise<void>
  renderNode?: (context: NodeRendererContext) => ReactNode
  /** Render a strategy editor for INLINE or DIALOG strategies. */
  renderApproverEditor?: (context: ApproverEditorRenderContext) => ReactNode
  /** Override individual controls to integrate the designer with a host UI library. */
  ui?: Partial<DesignerUiAdapter>
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
