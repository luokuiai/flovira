import { describe, expect, test } from 'vitest'
import { addCanvasBranch, compileBranchConditions, getBranchRule, insertCanvasNode, removeCanvasBranch, removeCanvasSplit, setBranchRule } from './branchConditions'
import { createInitialDefinition, normalizeDefinition, serializeDefinition, validateDefinition } from './model'
import type { DesignerConditionGroup } from './types'

const groups: DesignerConditionGroup[] = [
  { conditions: [
    { fieldCode: 'amount', fieldLabel: '金额', fieldType: 'NUMBER', operator: 'GE', value: '10000' },
    { fieldCode: 'department', fieldLabel: '部门', fieldType: 'STRING', operator: 'EQ', value: "O'Brien" },
  ] },
  { conditions: [{ fieldCode: 'urgent', fieldLabel: '紧急', fieldType: 'BOOLEAN', operator: 'EQ', value: 'true' }] },
]
const setup = () => {
  const initial = createInitialDefinition()
  const start = initial.nodeList.find((node) => node.nodeType === '0')!
  const definition = insertCanvasNode(initial, start.nodeCode, '3')
  const split = definition.nodeList.find((node) => node.nodeType === '3')!
  return { initial, definition, split }
}

describe('branch condition editing', () => {
  test('compiles typed conditions with AND groups, OR alternatives and escaped strings', () => {
    expect(compileBranchConditions(groups)).toBe("spel@@#{(#amount >= 10000 and #department == 'O''Brien') or (#urgent == true)}")
    expect(() => compileBranchConditions([])).toThrow('完整')
    expect(() => compileBranchConditions([{ conditions: [] }])).toThrow('完整')
    expect(() => compileBranchConditions([{ conditions: [{ ...groups[0].conditions[0], value: '1 or true' }] }])).toThrow('数字')
    expect(() => compileBranchConditions([{ conditions: [{ ...groups[0].conditions[0], fieldCode: 'amount.toString()' }] }])).toThrow('字段编码')
  })
  test('creates empty condition branches without approval placeholders and flags unconfigured rules', () => {
    const { initial, definition, split } = setup()
    expect(definition.nodeList.length).toBe(initial.nodeList.length + 1)
    expect(split.skipList[0].targetNodeCode).toBe(split.skipList[1].targetNodeCode)
    expect(getBranchRule(split, 0).mode).toBe('rules')
    expect(getBranchRule(split, 1).mode).toBe('default')
    expect(validateDefinition(definition).issues.some((issue) => issue.code === 'BRANCH_CONDITION_REQUIRED')).toBe(true)
  })
  test('inserts a nested split on one edge and preserves its condition and sibling', () => {
    const { definition, split } = setup()
    const rule = { mode: 'rules' as const, groups, expression: compileBranchConditions(groups) }
    const configured = setBranchRule(split, 0, '大额或紧急', rule)
    definition.nodeList = definition.nodeList.map((node) => node.nodeCode === split.nodeCode ? configured : node)
    const next = insertCanvasNode(definition, split.nodeCode, '4', 0)
    const updated = next.nodeList.find((node) => node.nodeCode === split.nodeCode)!
    expect(updated.skipList[0].skipCondition).toBe(rule.expression)
    expect(updated.skipList[1]).toEqual(configured.skipList[1])
    expect(getBranchRule(updated, 0)).toEqual(rule)
    const restored = normalizeDefinition(serializeDefinition(next))
    expect(getBranchRule(restored.nodeList.find((node) => node.nodeCode === split.nodeCode)!, 0)).toEqual(rule)
  })
  test('keeps legacy expressions editable without guessing their structure', () => {
    const { split } = setup()
    split.skipList[0].skipCondition = 'custom@@business-condition'
    expect(getBranchRule(split, 0)).toEqual({ mode: 'expression', groups: [], expression: 'custom@@business-condition' })
  })
  test('deletes an empty split without deleting the shared continuation', () => {
    const { initial, definition, split } = setup()
    const next = removeCanvasSplit(definition, split.nodeCode)
    expect(next.nodeList.map((node) => node.nodeCode)).toEqual(initial.nodeList.map((node) => node.nodeCode))
    expect(next.nodeList[0].skipList[0].targetNodeCode).toBe(initial.nodeList[0].skipList[0].targetNodeCode)
  })
  test('collapses the last two branches without losing the surviving business path', () => {
    const { initial, definition, split } = setup()
    const inserted = insertCanvasNode(definition, split.nodeCode, '1', 0)
    const survivingCode = inserted.nodeList.find((node) => node.nodeCode === split.nodeCode)!.skipList[0].targetNodeCode
    const next = removeCanvasBranch(inserted, split.nodeCode, 1)
    expect(next.nodeList.some((node) => node.nodeCode === split.nodeCode)).toBe(false)
    expect(next.nodeList.find((node) => node.nodeType === '0')!.skipList[0].targetNodeCode).toBe(survivingCode)
    expect(next.nodeList.some((node) => node.nodeCode === initial.nodeList[1].nodeCode)).toBe(true)
    expect(next.nodeList).toHaveLength(initial.nodeList.length + 1)
  })

  test('removes only branch-local nodes and preserves the common continuation', () => {
    const { initial, definition, split } = setup()
    const expanded = addCanvasBranch(definition, split.nodeCode)
    const expandedSplit = expanded.nodeList.find((node) => node.nodeCode === split.nodeCode)!
    expect(getBranchRule(expandedSplit, 2).mode).toBe('default')
    const inserted = insertCanvasNode(expanded, split.nodeCode, '1', 1)
    const next = removeCanvasBranch(inserted, split.nodeCode, 1)
    expect(next.nodeList).toHaveLength(definition.nodeList.length)
    expect(next.nodeList.some((node) => node.nodeCode === initial.nodeList[1].nodeCode)).toBe(true)
    expect(next.nodeList.find((node) => node.nodeCode === split.nodeCode)!.skipList).toHaveLength(2)
  })
})
