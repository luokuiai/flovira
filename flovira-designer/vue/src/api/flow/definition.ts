import { getDataProvider } from '@/data/provider'
import { unwrapData, type DesignerResourceItem, type DesignerResourceQuery } from '@/data/contracts'

// 说明：以下函数保持原有导出名与签名不变，内部统一委托给「当前数据源」(dataProvider)。
// 默认实现为内置 axios（见 src/data/httpProvider.ts），故所有调用方无需改动；
// 业务方可通过 setDataProvider 注入自定义后端 / mock，实现数据层与具体后端解耦。

// 保存json流程定义
export function saveJson(data: any, onlyNodeSkip?: boolean): Promise<any> {
  return getDataProvider().saveJson(data, onlyNodeSkip)
}

// 获取流程定义
export function queryDef(id?: string | number): Promise<any> {
  return getDataProvider().queryDef(id)
}

// 获取流程图
export function queryFlowChart(id: string | number): Promise<any> {
  return getDataProvider().queryFlowChart(id)
}

export function designerCapabilities(): Promise<any> {
  return getDataProvider().capabilities()
}

export function designerResources(query: DesignerResourceQuery): Promise<any> {
  return getDataProvider().queryResources(query)
}

export async function designerResourceItems(query: DesignerResourceQuery): Promise<DesignerResourceItem[]> {
  const page = unwrapData(await designerResources(query))
  return page?.items || []
}

export async function designerSubjects(storageIds: string[]): Promise<any[]> {
  if (!storageIds.length) return []
  const items = await designerResourceItems({
    resourceType: 'SUBJECT',
    pageNum: 1,
    pageSize: storageIds.length,
    parameters: { ids: storageIds },
  })
  const resources = new Map(items.map((item) => [item.id, item]))
  return storageIds.map((storageId) => ({
    storageId,
    handlerName: resources.get(storageId)?.name || storageId,
    resourceType: resources.get(storageId)?.resourceType,
  }))
}
