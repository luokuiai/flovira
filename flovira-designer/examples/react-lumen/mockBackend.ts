import type { Plugin } from 'vite'

const multiApproverOption = {
  code: 'approvalMode',
  name: '多人审批策略',
  defaultValue: 'OR',
  nodeTypes: ['1'],
  condition: 'MULTIPLE',
  choices: [
    { value: 'OR', label: '任意一人通过' },
    { value: 'COUNTERSIGN', label: '全部通过' },
    { value: 'VOTE', label: '按比例通过' },
  ],
}

const emptyApproverOption = {
  code: 'emptyPolicy',
  name: '无人审批策略',
  defaultValue: 'FAIL',
  nodeTypes: ['1'],
  condition: 'EMPTY',
  choices: [
    { value: 'FAIL', label: '阻止提交' },
    { value: 'TO_ADMIN', label: '转交管理员' },
  ],
}

const sameAsStarterOption = {
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

const capabilities = {
  schemaVersion: 1,
  nodeTypes: ['0', '1', '2', '3', '4', '5', '6', '7', '8'],
  approverStrategies: [
    { code: 'STARTER', name: '提交人', selectionType: 'RELATION', relationType: 'STARTER', multiple: false, editorType: 'NONE', resultCardinality: 'EXACTLY_ONE', options: [sameAsStarterOption] },
    { code: 'USER', name: '指定人员', selectionType: 'RESOURCE', resourceType: 'USER', multiple: true, editorType: 'DIALOG', editorKey: 'organization-user-picker', resultCardinality: 'ONE_OR_MORE', options: [multiApproverOption, emptyApproverOption, sameAsStarterOption] },
    { code: 'GROUP', name: '分组', selectionType: 'RESOURCE', resourceType: 'GROUP', relationType: 'GROUP_MEMBERS', multiple: false, editorType: 'DIALOG', resultCardinality: 'ZERO_OR_MORE', options: [multiApproverOption, emptyApproverOption, sameAsStarterOption] },
    { code: 'ROLE', name: '指定角色', selectionType: 'RESOURCE', resourceType: 'ROLE', relationType: 'ROLE_MEMBERS', multiple: true, editorType: 'DIALOG', resultCardinality: 'ZERO_OR_MORE', options: [multiApproverOption, emptyApproverOption, sameAsStarterOption] },
    { code: 'ORG_MANAGER', name: '部门负责人', selectionType: 'RELATION', relationType: 'ORG_MANAGER', multiple: false, editorType: 'NONE', resultCardinality: 'ZERO_OR_ONE', options: [emptyApproverOption, sameAsStarterOption] },
    { code: 'SUPERVISING_LEADER', name: '分管领导', selectionType: 'RELATION', relationType: 'SUPERVISING_LEADER', multiple: false, editorType: 'NONE', resultCardinality: 'ZERO_OR_ONE', options: [emptyApproverOption, sameAsStarterOption] },
    { code: 'ORG_MANAGER_CHAIN', name: '逐级部门负责人', selectionType: 'RELATION', relationType: 'ORG_MANAGER_CHAIN', multiple: false, editorType: 'NONE', resultCardinality: 'ZERO_OR_MORE', options: [multiApproverOption, emptyApproverOption, sameAsStarterOption] },
    { code: 'FORM_USER_FIELD', name: '表单字段', selectionType: 'RESOURCE', resourceType: 'FORM_FIELD', relationType: 'FORM_FIELD_USER', multiple: false, editorType: 'DIALOG', resultCardinality: 'ZERO_OR_MORE', options: [multiApproverOption, emptyApproverOption, sameAsStarterOption] },
    { code: 'EXPRESSION', name: '表达式', selectionType: 'EXPRESSION', multiple: false, editorType: 'INLINE', resultCardinality: 'ZERO_OR_MORE', options: [multiApproverOption, emptyApproverOption, sameAsStarterOption] },
  ],
  approvalModes: ['OR', 'COUNTERSIGN', 'VOTE'],
  returnPolicies: ['PREVIOUS', 'ANY', 'REJECT'],
  timeoutNodeTypes: ['1', '7'],
  operations: ['SAVE', 'PUBLISH', 'VALIDATE', 'IMPORT', 'EXPORT'],
  resourceTypes: ['USER', 'GROUP', 'ROLE', 'FORM_FIELD', 'SUBPROCESS'],
}

const resources = {
  USER: [
    { id: 'user_zhang', code: 'zhangsan', name: '张三', resourceType: 'USER' },
    { id: 'user_li', code: 'lisi', name: '李四', resourceType: 'USER' },
    { id: 'user_wang', code: 'wangwu', name: '王五', resourceType: 'USER' },
  ],
  GROUP: [
    { id: 'finance_reviewers', code: 'FINANCE_REVIEWERS', name: '财务审批组', resourceType: 'GROUP' },
    { id: 'regional_managers', code: 'REGIONAL_MANAGERS', name: '区域负责人组', resourceType: 'GROUP' },
  ],
  ROLE: [
    { id: 'finance_manager', code: 'FINANCE_MANAGER', name: '财务负责人', resourceType: 'ROLE' },
    { id: 'department_manager', code: 'DEPARTMENT_MANAGER', name: '部门负责人', resourceType: 'ROLE' },
  ],
  FORM_FIELD: [
    { id: 'applicant_id', code: 'applicantId', name: '申请人', resourceType: 'FORM_FIELD' },
    { id: 'department_owner_id', code: 'departmentOwnerId', name: '部门负责人字段', resourceType: 'FORM_FIELD' },
  ],
  SUBPROCESS: [
    { id: 'finance_review', code: 'finance_review', name: '财务复核流程', resourceType: 'SUBPROCESS', metadata: { version: 3 } },
    { id: 'manager_review', code: 'manager_review', name: '管理层审批流程', resourceType: 'SUBPROCESS', metadata: { version: 2 } },
  ],
}

const send = (response: import('node:http').ServerResponse, data: unknown) => {
  response.setHeader('Content-Type', 'application/json; charset=utf-8')
  response.end(JSON.stringify({ code: 200, message: 'success', data }))
}

/** Development-only stand-in for an integrating application's backend providers. */
export const mockBackend = (): Plugin => ({
  name: 'flovira-lumen-mock-backend',
  configureServer(server) {
    server.middlewares.use((request, response, next) => {
      const url = new URL(request.url || '/', 'http://localhost')
      if (url.pathname === '/flovira/integration/capabilities') {
        send(response, capabilities)
        return
      }
      if (url.pathname === '/flovira/integration/resources') {
        const resourceType = url.searchParams.get('resourceType') || ''
        const keyword = (url.searchParams.get('keyword') || '').trim().toLowerCase()
        const items = (resources[resourceType as keyof typeof resources] || []).filter((item) =>
          !keyword || item.name.toLowerCase().includes(keyword) || item.code.toLowerCase().includes(keyword))
        send(response, { items, total: items.length })
        return
      }
      if (url.pathname === '/flovira/integration/relationships/resolve') {
        send(response, [])
        return
      }
      next()
    })
  },
})
