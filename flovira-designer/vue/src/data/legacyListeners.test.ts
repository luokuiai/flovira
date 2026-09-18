import { expect, test } from 'bun:test'
import { json2LogicFlowJson, logicFlowJsonToFlovira } from '../components/design/common/js/tool'

test('graph conversion preserves JSON objects and does not interpret lifecycle business keys', () => {
  const ext = JSON.stringify({ business: { tags: ['a,b', 'c'], count: 0, enabled: false }, lifecycle: { custom: true } })
  const definition = { ext, nodeList: [{ nodeType: 1, nodeCode: 'approval', nodeName: '审批', nodeRatio: '0', ext, skipList: [] }] }
  const graph = json2LogicFlowJson(definition)
  const saved = JSON.parse(logicFlowJsonToFlovira(graph))
  expect(saved.ext).toBe(ext)
  expect(JSON.parse(saved.nodeList[0].ext)).toEqual(JSON.parse(ext))
  expect(definition.nodeList[0].ext).toBe(ext)
})

test('graph import rejects array extensions instead of converting them', () => {
  expect(() => json2LogicFlowJson({ nodeList: [{ nodeType: 1, nodeCode: 'approval', nodeRatio: '0', ext: '[]' }] })).toThrow('JSON 对象')
})

test('old callbacks including formLoad are rejected', () => {
  expect(() => json2LogicFlowJson({ listenerType: 'finish', listenerPath: 'oldBean' })).toThrow('旧监听配置')
  const flow = { listenerType: 'formLoad', listenerPath: 'formLoader', nodeList: [] }
  expect(() => json2LogicFlowJson(flow)).toThrow('旧监听配置')
  expect(() => json2LogicFlowJson({ nodeList: [flow] })).toThrow('旧监听配置')
})
