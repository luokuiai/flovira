import { describe, expect, test } from 'bun:test'
import { json2LogicFlowJson, logicFlowJsonToFlovira } from './tool'

test('preserves external form IDs and supports clearing node overrides', () => {
  const logic = json2LogicFlowJson({ formId: 'expense:v2', nodeList: [{
    nodeType: '1', nodeCode: 'approval', nodeName: '审批', nodeRatio: '0', formId: 'finance:v1', skipList: [],
  }] })
  let saved = JSON.parse(logicFlowJsonToFlovira(logic))
  expect(saved.formId).toBe('expense:v2')
  expect(saved.nodeList[0].formId).toBe('finance:v1')
  expect(saved).not.toHaveProperty('formCustom')
  expect(saved.nodeList[0]).not.toHaveProperty('formPath')
  logic.nodes[0].properties.formId = ''
  saved = JSON.parse(logicFlowJsonToFlovira(logic))
  expect(saved.nodeList[0].formId).toBeUndefined()
  expect(saved.formId).toBe('expense:v2')
})

describe('wait and timeout definition conversion', () => {
  test('preserves versioned wait and timeout JSON during round trip', () => {
    const waitConfig = JSON.stringify({ schemaVersion: 1, waitKey: 'order.paid' })
    const timeoutConfig = JSON.stringify({
      schemaVersion: 1,
      enabled: true,
      duration: 30,
      durationUnit: 'MINUTES',
      action: 'RESUME_WAIT',
    })
    const logic = json2LogicFlowJson({
      flowCode: 'wait-flow',
      flowName: 'Wait flow',
      version: '1',
      nodeList: [{
        nodeType: 7,
        nodeCode: 'WAIT_PAYMENT',
        nodeName: 'Wait payment',
        nodeRatio: 0,
        skipList: [],
        ext: JSON.stringify([
          { code: 'waitConfig', value: waitConfig },
          { code: 'timeoutConfig', value: timeoutConfig },
          { code: 'futureConfig', value: 'a,b' },
        ]),
      }],
    })

    expect(logic.nodes[0].type).toBe('wait')
    expect(logic.nodes[0].properties.ext.waitConfig).toBe(waitConfig)
    expect(logic.nodes[0].properties.ext.timeoutConfig).toBe(timeoutConfig)
    expect(logic.nodes[0].properties.ext.futureConfig).toEqual(['a', 'b'])

    const exported = JSON.parse(logicFlowJsonToFlovira(logic))
    const ext = JSON.parse(exported.nodeList[0].ext)
    expect(exported.nodeList[0].nodeType).toBe('7')
    expect(ext.find((item) => item.code === 'waitConfig').value).toBe(waitConfig)
    expect(ext.find((item) => item.code === 'timeoutConfig').value).toBe(timeoutConfig)
  })
})

describe('approver rule definition conversion', () => {
  test('preserves the shared approver rule during round trip', () => {
    const approverRule = JSON.stringify({
      schemaVersion: 1,
      strategy: 'ROLE',
      selectionType: 'RESOURCE',
      relationType: 'ROLE_MEMBERS',
      subjects: [{ id: 'role:finance', type: 'ROLE', name: 'Finance' }],
    })
    const logic = json2LogicFlowJson({
      flowCode: 'approval-flow',
      flowName: 'Approval flow',
      version: '1',
      nodeList: [{
        nodeType: 1,
        nodeCode: 'APPROVE',
        nodeName: 'Approve',
        nodeRatio: 0,
        skipList: [],
        ext: JSON.stringify([{ code: 'approverRule', value: approverRule }]),
      }],
    })

    expect(logic.nodes[0].properties.ext.approverRule).toBe(approverRule)
    const exported = JSON.parse(logicFlowJsonToFlovira(logic))
    const ext = JSON.parse(exported.nodeList[0].ext)
    expect(ext.find((item) => item.code === 'approverRule').value).toBe(approverRule)
  })

  test('preserves carbon copy type and recipient rule during round trip', () => {
    const carbonCopyRule = JSON.stringify({
      schemaVersion: 1,
      strategy: 'USER',
      selectionType: 'RESOURCE',
      subjects: [{ id: 'user:auditor', type: 'USER', name: 'Auditor' }],
    })
    const logic = json2LogicFlowJson({
      flowCode: 'carbon-copy-flow',
      flowName: 'Carbon copy flow',
      version: '1',
      nodeList: [{
        nodeType: 8,
        nodeCode: 'CARBON_COPY',
        nodeName: 'Carbon copy',
        nodeRatio: 0,
        skipList: [],
        ext: JSON.stringify([{ code: 'carbonCopyRule', value: carbonCopyRule }]),
      }],
    })

    expect(logic.nodes[0].type).toBe('carbonCopy')
    expect(logic.nodes[0].properties.ext.carbonCopyRule).toBe(carbonCopyRule)
    const exported = JSON.parse(logicFlowJsonToFlovira(logic))
    const ext = JSON.parse(exported.nodeList[0].ext)
    expect(exported.nodeList[0].nodeType).toBe('8')
    expect(ext.find((item) => item.code === 'carbonCopyRule').value).toBe(carbonCopyRule)
  })
})


test('converts legacy definitions without a designer mode field', () => {
  const graph = json2LogicFlowJson({ flowCode: 'single', flowName: '统一流程', modelValue: 'CLASSICS', nodeList: [] })
  expect(graph).not.toHaveProperty('modelValue')
  const saved = JSON.parse(logicFlowJsonToFlovira(graph))
  expect(saved).not.toHaveProperty('modelValue')
})
