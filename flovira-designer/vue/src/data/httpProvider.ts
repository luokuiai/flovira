import request from '@/utils/request'
import type { DataProvider } from './provider'

const urlPrefix = import.meta.env.VITE_URL_PREFIX

/**
 * 默认数据源实现：基于内置 axios 调用 flovira REST 接口。
 *
 * 行为与后端设计器 API 完全一致，作为 dataProvider 的默认回退实现，保证默认 HTTP
 * 集成方式零影响。业务方可通过 setDataProvider 覆盖其中任意方法，未覆盖的自动回退到此实现。
 *
 * @author warm
 */
export function createHttpProvider(apiPrefix = urlPrefix + 'flovira'): DataProvider {
  const baseUrl = apiPrefix.replace(/\/$/, '')
  return {
    capabilities() {
      return request({ url: baseUrl + '/integration/capabilities', method: 'get' })
    },
    queryResources(query) {
      return request({ url: baseUrl + '/integration/resources', method: 'get', params: query })
    },
    resolveRelationship(query) {
      return request({ url: baseUrl + '/integration/relationships/resolve', method: 'post', data: query })
    },
    // ===== 流程定义（原 api/flow/definition.js） =====
    // 保存json流程定义
    saveJson(data, onlyNodeSkip) {
      return request({
        url: baseUrl + '/save-json',
        method: 'post',
        data: data,
        headers: { onlyNodeSkip: onlyNodeSkip }
      })
    },
    // 获取流程定义
    queryDef(id) {
      const suffix = id ? '/' + id : ''
      return request({
        url: baseUrl + '/query-def' + suffix,
        method: 'get'
      })
    },
    // 获取流程图
    queryFlowChart(id) {
      return request({
        url: baseUrl + '/query-flow-chart/' + id,
        method: 'get'
      })
    },
    subprocessSummary(parentTaskId) {
      return request({ url: baseUrl + '/subprocess/summary/' + parentTaskId, method: 'get' })
    },
    subprocessChildren(runId, pageNum = 1, pageSize = 20) {
      return request({ url: baseUrl + '/subprocess/runs/' + runId + '/children', method: 'get', params: { pageNum, pageSize } })
    },
    subprocessEvents(runId) {
      return request({ url: baseUrl + '/subprocess/runs/' + runId + '/events', method: 'get' })
    },
    subprocessHistory(runId, childId) {
      return request({ url: baseUrl + '/subprocess/runs/' + runId + '/history', method: 'get', params: { childId } })
    },

    // ===== 匿名 / 配置（原 api/anony.js） =====
    // 获取设计器配置
    config() {
      return request({ url: baseUrl + '/config', method: 'get' })
    }
  }
}
