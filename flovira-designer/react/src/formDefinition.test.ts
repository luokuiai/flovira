import { expect, test } from 'vitest'
import fixture from '../../../flovira-core/src/test/resources/nested-form-conditions.json'
import { getFormConditionFields, fieldsForScope, compileFormCondition, compileFormConditionGroup, type FormDefinition, type FormConditionGroup } from './formDefinition'

test('expands named object and array metadata without treating containers as scalar fields', () => {
  const fields = getFormConditionFields(fixture.form as FormDefinition)
  expect(fields.find(field => field.code === 'address.city')?.label).toBe('地址 / 城市')
  expect(fields.find(field => field.code === 'details[].amount')?.label).toBe('报销明细 / 每一项 / 金额')
  expect(fieldsForScope(fields).map(field => field.code)).toEqual(['address.city', 'details.$count', 'tags.$count'])
  expect(fieldsForScope(fields, 'details').map(field => field.code)).toEqual(['details[].amount', 'details[].category'])
  expect(fieldsForScope(fields, 'tags').map(field => field.code)).toEqual(['tags[]'])
})

test('compiles the shared runtime fixture with one row scope for the whole group', () => {
  const group = fixture.group as FormConditionGroup
  expect('spel@@#{(' + compileFormConditionGroup(group) + ')}').toBe(fixture.anyExpression)
  expect('spel@@#{(' + compileFormConditionGroup({ ...group, collection: { ...group.collection!, quantifier: 'ALL' } }) + ')}').toBe(fixture.allExpression)
  expect(() => compileFormConditionGroup({ conditions: group.conditions })).toThrow()
  expect(() => compileFormConditionGroup({ ...group, conditions: [{ ...group.conditions[0], fieldCode: 'tags[]' }] })).toThrow('同一个明细')
})

test('rejects incomplete schemas and expression injection while preserving flat fields', () => {
  for (const field of [
    { key: 'bad', label: '无结构对象', dataType: 'object' },
    { key: 'bad', label: '无元素数组', dataType: 'array' },
    { key: 'bad', label: '', dataType: 'string' },
  ]) expect(() => getFormConditionFields({ schemaVersion: '1', fields: [field] } as FormDefinition)).toThrow()
  const duplicate = fixture.form.fields[0]
  expect(() => getFormConditionFields({ schemaVersion: '1', fields: [duplicate, duplicate] } as FormDefinition)).toThrow('重复')
  expect(() => compileFormCondition('address.getClass()', 'STRING', 'EQ', 'x')).toThrow()
  expect(() => compileFormCondition('details[].amount', 'NUMBER', 'GT', '1')).toThrow()
  expect(compileFormCondition('amount', 'NUMBER', 'GT', '1')).toBe('#amount > 1')
  expect(compileFormCondition('details.$count', 'NUMBER', 'GT', '0')).toContain('#details.size() > 0')
  expect(compileFormCondition('address.city', 'STRING', 'EQ', "O'Brien")).toContain("'O''Brien'")
  expect(() => compileFormCondition('root', 'STRING', 'EQ', 'x')).toThrow()
  expect(() => compileFormCondition('amount', 'NUMBER', 'constructor' as 'EQ', '1')).toThrow()
})

test('allows inner array counts without opening nested detail traversal', () => {
  const fields = getFormConditionFields({ schemaVersion: '1', fields: [
    { key: 'matrix', label: '分组', dataType: 'array', items: { dataType: 'array', items: { dataType: 'number' } } },
  ] })
  expect(fieldsForScope(fields, 'matrix').map(field => field.code)).toEqual(['matrix[].$count'])
  expect(compileFormConditionGroup({
    collection: { code: 'matrix', label: '分组', quantifier: 'ANY' },
    conditions: [{ fieldCode: 'matrix[].$count', fieldLabel: '分组 / 每一项 / 数量', fieldType: 'NUMBER', operator: 'GT', value: '2' }],
  })).toContain('#this.size() > 2')
})
