import { createHttpProvider } from './httpProvider'
import type {
  ApiResponse,
  DesignerCapabilities,
  DesignerRelationshipQuery,
  DesignerResourcePage,
  DesignerResourceQuery,
  DesignerSubject,
} from './contracts'

/**
 * 设计器与后端交互的数据源契约（DataProvider）。
 *
 * 流程设计器内部所有后端调用都经由「当前 dataProvider」，默认实现为内置 axios（createHttpProvider），
 * 与默认后端 API 行为保持一致；通过 setDataProvider 注入自定义实现（业务方后端、mock 等），
 * 即可把数据层与具体后端解耦。这是「业务方注入数据源」与组件库 / npm 包形态的统一入口。
 *
 * 集成数据通过能力清单、通用资源查询和关系解析契约传输；
 * 流程定义与运行时接口保持各自的引擎模型。
 *
 * @author warm
 */
export interface DesignerDataProvider {
  // ===== 统一集成契约 =====
  capabilities(): Promise<ApiResponse<DesignerCapabilities>>
  queryResources(query: DesignerResourceQuery): Promise<ApiResponse<DesignerResourcePage>>
  resolveRelationship(query: DesignerRelationshipQuery): Promise<ApiResponse<DesignerSubject[]>>
}

export interface DataProvider extends DesignerDataProvider {

  // ===== 流程定义 =====
  saveJson(data: any, onlyNodeSkip?: boolean): Promise<any>
  queryDef(id?: string | number): Promise<any>
  queryFlowChart(id: string | number): Promise<any>
  subprocessSummary(parentTaskId: string | number): Promise<any>
  subprocessChildren(runId: string | number, pageNum?: number, pageSize?: number): Promise<any>
  subprocessEvents(runId: string | number): Promise<any>
  subprocessHistory(runId: string | number, childId?: string | number): Promise<any>

  // ===== 配置 =====
  config(): Promise<any>
}

let currentProvider: DataProvider = createHttpProvider()

/**
 * 注入自定义数据源。
 *
 * 传入对象会与默认 http 实现「合并」，因此业务方只需覆盖关心的方法，其余自动回退到内置 axios；
 * 传 null / 不传则恢复为纯默认 http 实现。
 *
 * @param provider 自定义数据源（部分或全部方法）
 */
export function setDataProvider(provider?: Partial<DataProvider> | null): void {
  currentProvider = Object.assign(createHttpProvider(), provider || {})
}

/**
 * 获取当前生效的数据源。api/* 内部统一通过它委托调用，调用方无需感知具体实现。
 */
export function getDataProvider(): DataProvider {
  return currentProvider
}
