import { describe, expect, test } from 'bun:test'
import { json2LogicFlowJson, logicFlowJsonToFlovira } from './tool'

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
      modelValue: 'CLASSICS',
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
      modelValue: 'CLASSICS',
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
})
