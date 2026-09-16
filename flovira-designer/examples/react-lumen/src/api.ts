import type {
  DesignerCapabilities,
  DesignerResourcePage,
  DesignerResourceQuery,
} from '@luokuiai/flovira-react-designer'

interface ApiResult<T> {
  data: T
}

const request = async <T,>(url: string): Promise<T> => {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`Request failed: ${response.status}`)
  return (await response.json() as ApiResult<T>).data
}

export const loadDesignerCapabilities = () =>
  request<DesignerCapabilities>('/flovira/integration/capabilities')

export const queryDesignerResources = (query: DesignerResourceQuery) => {
  const search = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined) search.set(key, typeof value === 'object' ? JSON.stringify(value) : String(value))
  })
  return request<DesignerResourcePage>(`/flovira/integration/resources?${search}`)
}
