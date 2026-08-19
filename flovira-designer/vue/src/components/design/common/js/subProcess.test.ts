import { describe, expect, test } from 'bun:test'
import { json2LogicFlowJson, logicFlowJsonToFlovira } from './tool'

const config = JSON.stringify({
  schemaVersion: 1,
  fixedChildFlowCode: 'expense-review',
  completionPolicy: 'ALL',
  allowEmpty: false,
})

function definition(modelValue: string) {
  return {
    flowCode: 'parent',
    flowName: 'Parent',
    modelValue,
    nodeList: [{
      nodeType: 6,
      nodeCode: 'subprocess',
      nodeName: 'Expense subprocess',
      nodeRatio: '0',
      coordinate: '100,100',
      ext: JSON.stringify([{ code: 'subprocessConfig', value: config }]),
      skipList: [],
    }],
  }
}

describe('subprocess designer conversion', () => {
  for (const modelValue of ['CLASSICS', 'MIMIC']) {
    test(`round trips fixed subprocess config in ${modelValue}`, () => {
      const graph = json2LogicFlowJson(definition(modelValue))
      expect(graph.nodes[0].type).toBe('subProcess')
      expect(graph.nodes[0].properties.ext.subprocessConfig).toBe(config)

      const saved = JSON.parse(logicFlowJsonToFlovira(graph))
      expect(saved.nodeList[0].nodeType).toBe('6')
      const ext = JSON.parse(saved.nodeList[0].ext)
      expect(ext).toEqual([{ code: 'subprocessConfig', value: config }])
    })
  }
})
