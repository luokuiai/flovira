import { describe, expect, test } from 'bun:test'
import { json2LogicFlowJson, logicFlowJsonToFlovira } from './tool'

test('preserves definition business type independently of category', () => {
  const graph = json2LogicFlowJson({ category: 'finance', businessType: 'PURCHASE_ORDER', nodeList: [] })
  const saved = JSON.parse(logicFlowJsonToFlovira(graph))
  expect(saved.category).toBe('finance')
  expect(saved.businessType).toBe('PURCHASE_ORDER')
})

test('preserves only the workflow form and ignores node overrides', () => {
  const logic = json2LogicFlowJson({ formId: 'expense:v2', nodeList: [{
    nodeType: '1', nodeCode: 'approval', nodeName: '审批', nodeRatio: '0', formId: 'finance:v1', skipList: [],
  }] })
  let saved = JSON.parse(logicFlowJsonToFlovira(logic))
  expect(saved.formId).toBe('expense:v2')
  expect(saved.nodeList[0].formId).toBeUndefined()
  expect(logic.nodes[0].properties.formId).toBeUndefined()
  expect(saved).not.toHaveProperty('formCustom')
  expect(saved.nodeList[0]).not.toHaveProperty('formPath')
  logic.nodes[0].properties.formId = 'another-form'
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
        ext: JSON.stringify({ 'waitConfig': waitConfig, 'timeoutConfig': timeoutConfig, 'futureConfig': 'a,b' }),
      }],
    })

    expect(logic.nodes[0].type).toBe('wait')
    expect(logic.nodes[0].properties.ext.waitConfig).toBe(waitConfig)
    expect(logic.nodes[0].properties.ext.timeoutConfig).toBe(timeoutConfig)
    expect(logic.nodes[0].properties.ext.futureConfig).toBe('a,b')

    const exported = JSON.parse(logicFlowJsonToFlovira(logic))
    const ext = JSON.parse(exported.nodeList[0].ext)
    expect(exported.nodeList[0].nodeType).toBe('7')
    expect(ext.waitConfig).toEqual(JSON.parse(waitConfig))
    expect(ext.timeoutConfig).toEqual(JSON.parse(timeoutConfig))
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
        ext: JSON.stringify({ 'approverRule': approverRule }),
      }],
    })

    expect(logic.nodes[0].properties.ext.approverRule).toBe(approverRule)
    const exported = JSON.parse(logicFlowJsonToFlovira(logic))
    const ext = JSON.parse(exported.nodeList[0].ext)
    expect(ext.approverRule).toEqual(JSON.parse(approverRule))
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
        ext: JSON.stringify({ 'carbonCopyRule': carbonCopyRule }),
      }],
    })

    expect(logic.nodes[0].type).toBe('carbonCopy')
    expect(logic.nodes[0].properties.ext.carbonCopyRule).toBe(carbonCopyRule)
    const exported = JSON.parse(logicFlowJsonToFlovira(logic))
    const ext = JSON.parse(exported.nodeList[0].ext)
    expect(exported.nodeList[0].nodeType).toBe('8')
    expect(ext.carbonCopyRule).toEqual(JSON.parse(carbonCopyRule))
  })
})


test('converts legacy definitions without a designer mode field', () => {
  const graph = json2LogicFlowJson({ flowCode: 'single', flowName: '统一流程', modelValue: 'CLASSICS', nodeList: [] })
  expect(graph).not.toHaveProperty('modelValue')
  const saved = JSON.parse(logicFlowJsonToFlovira(graph))
  expect(saved).not.toHaveProperty('modelValue')
})
