import type { ComponentType, ReactElement, ReactNode } from 'react'

export type FloviraNodeType = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8'
export type ApproverStrategy = 'INITIATOR' | 'USER' | 'ROLE' | 'DEPARTMENT_LEADER' | 'SUPERVISING_LEADER'

export interface ApproverSubject {
  id: string
  type: string
  name?: string
}

export interface ApproverRule {
  schemaVersion: 1
  strategy: ApproverStrategy | string
  strategyVersion?: number
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
  /** Strategy used by the host picker for this choice's separate subjects. */
  selectionStrategy?: string
  /** Key in the owning rule's config where the selected subjects are stored. */
  selectionConfigKey?: string
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
  version?: number
  name: string
  selectionType: ApproverSelectionType
  resourceType?: string
  relationType?: string
  multiple: boolean
  /** Maximum explicitly selected subjects; omitted means no configured limit. */
  maxSubjects?: number
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
  rejectStrategy: 'TO_INITIATOR' | 'TO_PREVIOUS' | 'TO_SPECIFIED_NODE' | 'TO_REJECTOR_SPECIFIED_NODE' | 'REJECT'
  rejectTargetNodeCode: string
  resubmitStrategy: 'RESTART_FROM_BEGINNING' | 'CONTINUE_FROM_REJECTED_NODE'
}

export interface FloviraDefinition extends Record<string, unknown> {
  /** 流程定义配置的业务类型，启动时写入实例快照。 */
  businessType?: string | null
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
  lifecyclePoints?: string[]
  lifecyclePhases?: ('IN_TRANSACTION' | 'AFTER_COMMIT')[]
  listenerCodes?: string[]
  schemaVersion: 1
  nodeTypes: FloviraNodeType[]
  approverStrategies: DesignerApproverStrategy[]
  /** 开始节点提交范围；自定义策略由后端显式声明。 */
  submitterStrategies?: DesignerApproverStrategy[]
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
  /** 分支问题对应节点 skipList 中的位置。 */
  skipIndex?: number
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
  onOpenChange?(open: boolean): void
  renderContent?(context: { close(): void }): ReactNode
  onSelect(value: string): void
}

export interface DesignerDrawerProps {
  open: boolean
  title: ReactNode
  children: ReactNode
  footer?: ReactNode
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

/** Host owns the picker UI. null / undefined cancels without changing the rule. */
export type ApproverSelectionContext = Omit<ApproverEditorRenderContext, 'onChange' | 'onRuleChange'>
export type ApproverSelectionResult = Pick<ApproverRule, 'subjects' | 'expression' | 'config'>
export type ApproverSelector = (context: ApproverSelectionContext) =>
  ApproverSelectionResult | null | undefined | Promise<ApproverSelectionResult | null | undefined>

export interface DesignerUiAdapter {
  Tabs?: ComponentType<DesignerTabsProps>
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

export interface DesignerTabsProps {
  value: string
  options: { value: string; label: string }[]
  idPrefix: string
  ariaLabel?: string
  onValueChange(value: string): void
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
  collection?: { code: string; label: string; quantifier: 'ANY' | 'ALL' }
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
  /** standalone 为独立卡片；embedded 无外框和标题栏，历史操作悬浮于画布右上角。 */
  appearance?: 'standalone' | 'embedded'
  /** 是否显示顶栏或嵌入模式的悬浮历史操作栏；默认显示，不影响左下角缩放。 */
  toolbar?: boolean
  /** 自定义当前模式的操作栏：独立模式位于顶部，嵌入模式位于画布右上角。 */
  renderToolbar?: (context: DesignerToolbarContext) => ReactNode
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
  renderNode?: (context: NodeRendererContext) => ReactNode
  /** Render a strategy editor for INLINE or DIALOG strategies. */
  renderApproverEditor?: (context: ApproverEditorRenderContext) => ReactNode
  /** Takes precedence over the built-in dialog and renderApproverEditor for dialog strategies. */
  onSelectApprover?: ApproverSelector
  /** Actual form fields for per-node read/write metadata; not condition-derived fields. */
  formFields?: readonly import('./formPermissions').DesignerFormField[]
  queryFormFields?: import('./formPermissions').DesignerFormFieldLoader
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

export interface DesignerToolbarContext {
  defaultToolbar: ReactNode
  disabled: boolean
  dirty: boolean
}
