import type { DesignerCapabilities, DesignerApproverOption } from '../react/src/types'

// Host-owned mock capabilities for examples and tests; not engine defaults.
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
    { value: 'TRANSFER_TO_USER', label: '转交给指定人员', selectionStrategy: 'USER', selectionConfigKey: 'sameAsStarterSubjects' },
  ],
}

const EMPTY_APPROVER_OPTION: DesignerApproverOption = {
  code: 'emptyPolicy', name: '审批人为空时', defaultValue: 'ERROR', nodeTypes: ['1'], condition: 'EMPTY',
  choices: [
    { value: 'ERROR', label: '报错并阻止流转' },
    { value: 'SKIP', label: '跳过' },
    { value: 'TRANSFER_TO_USER', label: '转交给指定人员', selectionStrategy: 'USER', selectionConfigKey: 'emptyPolicySubjects' },
  ],
}

export const DEMO_CAPABILITIES: DesignerCapabilities = {
  schemaVersion: 1,
  nodeTypes: ['0', '1', '2', '3', '4', '5', '6', '7', '8'],
  approverStrategies: [
    { code: 'USER', name: '用户', selectionType: 'RESOURCE', resourceType: 'USER', multiple: true, editorType: 'DIALOG', resultCardinality: 'ONE_OR_MORE', options: [MULTI_APPROVER_OPTION, SAME_AS_STARTER_OPTION] },
    { code: 'ROLE', name: '角色', selectionType: 'RESOURCE', resourceType: 'ROLE', relationType: 'ROLE_MEMBERS', multiple: true, editorType: 'DIALOG', resultCardinality: 'ZERO_OR_MORE', options: [MULTI_APPROVER_OPTION, EMPTY_APPROVER_OPTION, SAME_AS_STARTER_OPTION] },
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
