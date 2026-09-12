export const API_PREFIX = '/api/example/v1'
export const DESIGNER_PREFIX = '/flovira'
export const DEMO_USER_HEADER = 'X-Demo-User'

export interface ApiEnvelope<T> {
  code: number
  msg: string
  data: T
}

export interface DemoIdentity {
  id: string
  name: string
  organizationId: string
  roleId: string
}

export interface DefinitionSummary {
  id: number
  flowCode: string
  flowName: string
  version: string
  publishStatus: number
}

export interface PurchaseRequest {
  id: string
  title: string
  applicantId: string
  amount: number
  department: string
  status: string
}

export interface TaskSummary {
  id: number
  instanceId: number
  definitionId: number
  businessId: string
  nodeCode: string
  nodeName: string
  flowStatus: string
}

export interface InstanceProgress {
  instance: Record<string, unknown>
  currentTasks: TaskSummary[]
  history: Array<Record<string, unknown>>
  definition: Record<string, unknown>
  business: PurchaseRequest | null
}

export interface DesignerResourceQuery {
  resourceType: string
  keyword?: string
  scopeId?: string
  pageNum?: number
  pageSize?: number
  parameters?: Record<string, unknown>
}

export interface DesignerResourcePage {
  items: Array<{ id: string; code?: string; name: string; parentId?: string; resourceType: string; metadata?: Record<string, unknown> }>
  total: number
}

export interface DesignerRelationshipQuery {
  relationType: string
  subjectId?: string
  organizationId?: string
  parameters?: Record<string, unknown>
}

async function request<T>(path: string, init: RequestInit = {}, user = 'alice'): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  headers.set(DEMO_USER_HEADER, user)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const response = await fetch(path, { ...init, headers })
  const body = await response.json().catch(() => null) as ApiEnvelope<T> | T | null
  if (!response.ok) {
    const message = body && typeof body === 'object' && 'msg' in body ? String(body.msg) : `${response.status} ${response.statusText}`
    throw new Error(message)
  }
  if (body && typeof body === 'object' && 'code' in body) {
    const envelope = body as ApiEnvelope<T>
    if (envelope.code !== 200) throw new Error(envelope.msg || 'Backend operation failed')
    return envelope.data
  }
  return body as T
}

const query = (values: Record<string, string | number | undefined>) => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  const text = params.toString()
  return text ? `?${text}` : ''
}

export const exampleApi = {
  health: () => request<{ application: string; database: string }>(`${API_PREFIX}/health`),
  identities: () => request<DemoIdentity[]>(`${API_PREFIX}/identities`),
  definitions: () => request<DefinitionSummary[]>(`${API_PREFIX}/definitions`),
  definition: (id: number) => request<Record<string, unknown>>(`${DESIGNER_PREFIX}/query-def/${id}`),
  saveDefinition: (definition: unknown, user: string) => request<DefinitionSummary>(`${API_PREFIX}/definitions`, {
    method: 'POST', body: JSON.stringify(definition),
  }, user),
  publishDefinition: (id: number, user: string) => request<DefinitionSummary>(`${API_PREFIX}/definitions/${id}/publish`, { method: 'POST' }, user),
  resources: (input: DesignerResourceQuery) => request<DesignerResourcePage>(`${DESIGNER_PREFIX}/integration/resources${query({
    resourceType: input.resourceType, keyword: input.keyword, scopeId: input.scopeId,
    pageNum: input.pageNum, pageSize: input.pageSize,
  })}`),
  capabilities: () => request<Record<string, unknown>>(`${DESIGNER_PREFIX}/integration/capabilities`),
  relationship: (input: DesignerRelationshipQuery) => request<Array<Record<string, unknown>>>(`${DESIGNER_PREFIX}/integration/relationships/resolve`, {
    method: 'POST', body: JSON.stringify(input),
  }),
  designerConfig: () => request<Record<string, unknown>>(`${DESIGNER_PREFIX}/config`),
  designerSave: (definition: unknown, onlyNodeSkip = false, user = 'alice') => request<void>(`${DESIGNER_PREFIX}/save-json`, {
    method: 'POST', headers: { onlyNodeSkip: String(onlyNodeSkip) }, body: JSON.stringify(definition),
  }, user),
  flowChart: (id: string | number) => request<Record<string, unknown>>(`${DESIGNER_PREFIX}/query-flow-chart/${id}`),
  subprocessSummary: (parentTaskId: string | number) => request<Record<string, unknown>>(`${DESIGNER_PREFIX}/subprocess/summary/${parentTaskId}`),
  subprocessChildren: (runId: string | number, pageNum = 1, pageSize = 20) => request<Record<string, unknown>>(`${DESIGNER_PREFIX}/subprocess/runs/${runId}/children${query({ pageNum, pageSize })}`),
  subprocessEvents: (runId: string | number) => request<Array<Record<string, unknown>>>(`${DESIGNER_PREFIX}/subprocess/runs/${runId}/events`),
  subprocessHistory: (runId: string | number, childId?: string | number) => request<Array<Record<string, unknown>>>(`${DESIGNER_PREFIX}/subprocess/runs/${runId}/history${query({ childId })}`),
  purchases: () => request<PurchaseRequest[]>(`${API_PREFIX}/purchases`),
  start: (purchaseId: string, definitionId: number, user: string) => request<Record<string, unknown>>(`${API_PREFIX}/purchases/${purchaseId}/start`, {
    method: 'POST', body: JSON.stringify({ definitionId }),
  }, user),
  tasks: (user: string) => request<TaskSummary[]>(`${API_PREFIX}/tasks`, {}, user),
  handle: (taskId: number, action: 'approve' | 'reject', message: string, user: string) => request<Record<string, unknown>>(`${API_PREFIX}/tasks/${taskId}/${action}`, {
    method: 'POST', body: JSON.stringify({ message }),
  }, user),
  progress: (instanceId: number, user: string) => request<InstanceProgress>(`${API_PREFIX}/instances/${instanceId}/progress`, {}, user),
}

export function backendTarget(environment: Record<string, string | undefined>): string {
  return environment.EXAMPLE_BACKEND_URL || 'http://localhost:8081'
}
