import { describe, expect, test } from 'vitest'
import {
  approverStrategyOptions,
  createInitialDefinition,
  createNode,
  DEFAULT_DESIGNER_CAPABILITIES,
  deleteNode,
  filterNodeTypes,
  getSubprocessConfig,
  getApproverRule,
  getCarbonCopyRule,
  getTimeoutConfig,
  getWaitConfig,
  insertNodeAfter,
  normalizeDefinition,
  serializeDefinition,
  setSubprocessConfig,
  setApproverRule,
  setCarbonCopyRule,
  setTimeoutConfig,
  setWaitConfig,
  updateNode,
  validateDefinition,
} from './model'

describe('Flovira definition model', () => {
  test('filters node and approver controls using host capabilities', () => {
    const capabilities = {
      ...DEFAULT_DESIGNER_CAPABILITIES,
      nodeTypes: ['0', '1', '2', '6'] as typeof DEFAULT_DESIGNER_CAPABILITIES.nodeTypes,
      approverStrategies: [
        { code: 'USER', name: '用户', selectionType: 'RESOURCE' as const, resourceType: 'USER', multiple: true },
        { code: 'EXPRESSION', name: '表达式', selectionType: 'EXPRESSION' as const, multiple: false },
        { code: 'PROJECT_OWNER', name: '项目负责人', selectionType: 'RELATION' as const, relationType: 'PROJECT_OWNER', multiple: false },
      ],
    }

    expect(filterNodeTypes(['1', '7', '6', '3'], capabilities)).toEqual(['1', '6'])
    expect(approverStrategyOptions(capabilities)).toEqual([
      { label: '用户', value: 'USER' },
      { label: '表达式', value: 'EXPRESSION' },
      { label: '项目负责人', value: 'PROJECT_OWNER' },
    ])
  })

  test('preserves unknown definition, node and skip fields', () => {
    const source = createInitialDefinition()
    source.futureDefinitionField = { enabled: true }
    source.nodeList[0].futureNodeField = 'kept'
    source.nodeList[0].skipList[0].futureSkipField = 42

    const result = normalizeDefinition(serializeDefinition(source))

    expect(result.futureDefinitionField).toEqual({ enabled: true })
    expect(result.nodeList[0].futureNodeField).toBe('kept')
    expect(result.nodeList[0].skipList[0].futureSkipField).toBe(42)
  })

  test('inserts and deletes a sequential node without breaking the path', () => {
    const source = createInitialDefinition()
    const start = source.nodeList.find((node) => node.nodeType === '0')!
    const inserted = insertNodeAfter(source, start.nodeCode, '6')
    const subprocess = inserted.nodeList.find((node) => node.nodeType === '6')!

    expect(start.nodeCode).not.toBe(subprocess.nodeCode)
    expect(inserted.nodeList.find((node) => node.nodeCode === start.nodeCode)?.skipList[0].nextNodeCode)
      .toBe(subprocess.nodeCode)

    const removed = deleteNode(inserted, subprocess.nodeCode)
    expect(removed.nodeList.some((node) => node.nodeCode === subprocess.nodeCode)).toBe(false)
    expect(validateDefinition(removed).valid).toBe(true)
  })

  test('creates gateway branches and keeps a structurally valid merge', () => {
    const source = createInitialDefinition()
    const approval = source.nodeList.find((node) => node.nodeType === '1')!
    const result = insertNodeAfter(source, approval.nodeCode, '3')
    const gateway = result.nodeList.find((node) => node.nodeType === '3')!

    expect(gateway.skipList).toHaveLength(2)
    expect(validateDefinition(result).valid).toBe(true)

    const removed = deleteNode(result, gateway.nodeCode)
    expect(removed.nodeList.some((node) => ['3', '4', '5'].includes(node.nodeType))).toBe(false)
    expect(removed.nodeList.some((node) => node.nodeName.startsWith('分支'))).toBe(false)
    expect(validateDefinition(removed).valid).toBe(true)
  })

  test('round trips fixed subprocess configuration', () => {
    const node = createNode('6')
    node.ext = JSON.stringify([{ code: 'future', value: 'kept' }])
    const configured = setSubprocessConfig(node, 'expense_child')

    expect(getSubprocessConfig(configured)).toMatchObject({
      fixedChildFlowCode: 'expense_child',
      selectionMode: 'FIXED',
      completionPolicy: 'ALL',
    })
    expect(JSON.parse(String(configured.ext))).toContainEqual({ code: 'future', value: 'kept' })
  })

  test('round trips the shared semantic approver rule', () => {
    const node = setApproverRule(createNode('1'), 'ROLE', [
      { id: 'role:finance', type: 'ROLE', name: '财务角色' },
    ])

    expect(getApproverRule(node)).toEqual({
      schemaVersion: 1,
      strategy: 'ROLE',
      selectionType: 'RESOURCE',
      relationType: undefined,
      subjects: [{ id: 'role:finance', type: 'ROLE', name: '财务角色' }],
      expression: '',
    })
    expect(JSON.parse(String(node.ext))).toContainEqual(expect.objectContaining({ code: 'approverRule' }))
  })

  test('round trips and validates carbon copy recipients', () => {
    const source = createInitialDefinition()
    const approval = source.nodeList.find((node) => node.nodeType === '1')!
    let definition = insertNodeAfter(source, approval.nodeCode, '8')
    const carbonCopy = definition.nodeList.find((node) => node.nodeType === '8')!

    expect(validateDefinition(definition).issues).toContainEqual(expect.objectContaining({
      code: 'CARBON_COPY_REQUIRED',
    }))

    const configured = setCarbonCopyRule(carbonCopy, 'USER', [
      { id: 'user:auditor', type: 'USER', name: '审计员' },
    ])
    definition = updateNode(definition, carbonCopy.nodeCode, configured)

    expect(getCarbonCopyRule(configured).subjects).toEqual([
      { id: 'user:auditor', type: 'USER', name: '审计员' },
    ])
    expect(validateDefinition(definition).issues.some((issue) => issue.code === 'CARBON_COPY_REQUIRED')).toBe(false)
  })

  test('validates a 128 business-node definition without truncation', () => {
    let definition = createInitialDefinition()
    for (let index = 0; index < 127; index += 1) {
      const approval = definition.nodeList.find((node) => node.nodeType === '1')!
      definition = insertNodeAfter(definition, approval.nodeCode, '1')
    }

    const parsed = normalizeDefinition(serializeDefinition(definition))
    expect(parsed.nodeList.filter((node) => node.nodeType === '1')).toHaveLength(128)
    expect(validateDefinition(parsed).valid).toBe(true)
  })

  test('reports a subprocess without a fixed child flow', () => {
    const source = createInitialDefinition()
    const approval = source.nodeList.find((node) => node.nodeType === '1')!
    const result = insertNodeAfter(source, approval.nodeCode, '6')

    expect(validateDefinition(result).issues).toContainEqual(expect.objectContaining({
      code: 'SUBPROCESS_REQUIRED',
    }))
  })

  test('round trips a wait node and timeout configuration', () => {
    let node = createNode('7')
    node = setWaitConfig(node, 'ORDER_PAID')
    node = setTimeoutConfig(node, {
      enabled: true,
      duration: 30,
      durationUnit: 'MINUTES',
      action: 'RESUME_WAIT',
    })

    expect(getWaitConfig(node)).toMatchObject({ schemaVersion: 1, waitKey: 'ORDER_PAID' })
    expect(getTimeoutConfig(node)).toMatchObject({
      schemaVersion: 1,
      enabled: true,
      duration: 30,
      durationUnit: 'MINUTES',
      action: 'RESUME_WAIT',
    })
  })

  test('rejects missing wait keys and incompatible timeout actions', () => {
    const source = createInitialDefinition()
    const approval = source.nodeList.find((node) => node.nodeType === '1')!
    let result = insertNodeAfter(source, approval.nodeCode, '7')
    const wait = result.nodeList.find((node) => node.nodeType === '7')!
    result = updateNode(result, wait.nodeCode, setTimeoutConfig(wait, {
      enabled: true,
      duration: 1,
      durationUnit: 'HOURS',
      action: 'AUTO_PASS',
    }))

    expect(validateDefinition(result).issues.map((issue) => issue.code))
      .toEqual(expect.arrayContaining(['WAIT_KEY_REQUIRED', 'TIMEOUT_INVALID']))
  })

  test('updates only the selected node', () => {
    const source = createInitialDefinition()
    const approval = source.nodeList.find((node) => node.nodeType === '1')!
    const result = updateNode(source, approval.nodeCode, { nodeName: '部门负责人审批' })

    expect(result.nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName)
      .toBe('部门负责人审批')
    expect(source.nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName)
      .toBe('审批节点')
  })
})
