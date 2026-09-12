import { exampleApi } from '@flovira-example/common'
import type { DataProvider } from '@luokuiai/flovira-vue-designer'

const ok = <T>(data: T) => ({ code: 200, msg: 'success', data })
const definitionObject = (data: unknown): unknown => typeof data === 'string' ? JSON.parse(data) : data

export function createExampleProvider(currentUser: () => string): Partial<DataProvider> {
  return {
    async capabilities() { return ok(await exampleApi.capabilities()) as ReturnType<DataProvider['capabilities']> extends Promise<infer T> ? T : never },
    async queryResources(query) { return ok(await exampleApi.resources(query)) },
    async resolveRelationship(query) { return ok(await exampleApi.relationship(query)) },
    async saveJson(data, onlyNodeSkip) {
      if (onlyNodeSkip) {
        await exampleApi.designerSave(definitionObject(data), true, currentUser())
        return ok(null)
      }
      return ok(await exampleApi.saveDefinition(definitionObject(data), currentUser()))
    },
    async queryDef(id) { return ok(id ? await exampleApi.definition(Number(id)) : null) },
    async queryFlowChart(id) { return ok(await exampleApi.flowChart(id)) },
    async subprocessSummary(parentTaskId) { return ok(await exampleApi.subprocessSummary(parentTaskId)) },
    async subprocessChildren(runId, pageNum, pageSize) { return ok(await exampleApi.subprocessChildren(runId, pageNum, pageSize)) },
    async subprocessEvents(runId) { return ok(await exampleApi.subprocessEvents(runId)) },
    async subprocessHistory(runId, childId) { return ok(await exampleApi.subprocessHistory(runId, childId)) },
    async config() { return ok(await exampleApi.designerConfig()) },
  }
}
