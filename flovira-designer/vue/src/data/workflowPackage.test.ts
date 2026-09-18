import { expect, test } from 'bun:test'
import fixture from '../../../../flovira-orm/src/contractTest/resources/workflow-package.json'
import { parseWorkflowPackage, getPackageDefinition, getPackageForm, parsePackageFormContent } from './workflowPackage'
import { json2LogicFlowJson } from '../components/design/common/js/tool'
import { readFileSync } from 'node:fs'

test('provides renderable canvas data without database access', () => {
  const bundle = parseWorkflowPackage(fixture)
  const graph = json2LogicFlowJson(getPackageDefinition(bundle))
  expect(graph.nodes).toHaveLength(3)
  expect(graph.edges).toHaveLength(2)
  expect(graph.nodes.find((node: { id: string }) => node.id === 'sub').type).toBe('subProcess')
})

test('does not expose a JSON download action in the built-in header', () => {
  const header = readFileSync(new URL('../components/design/FlowDesignerHeader.vue', import.meta.url), 'utf8')
  expect(header).not.toContain('download-json')
  expect(header).not.toContain('flowDesigner.downloadJson')
})

test('keeps persistence actions out of the default header', () => {
  const header = readFileSync(new URL('../components/design/FlowDesignerHeader.vue', import.meta.url), 'utf8')
  const designer = readFileSync(new URL('../components/design/FlowDesigner.vue', import.meta.url), 'utf8')
  expect(header).not.toContain('emitSave')
  expect(header).not.toContain('emitPublish')
  expect(header).not.toContain('common.save')
  expect(designer).toContain('name="toolbar" v-bind="toolbarContext"')
  expect(designer).not.toContain('saveJsonModel')
  expect(designer).not.toContain('props.onPublish')
})

test('parses the backend fixture for offline root, child and form display', () => {
  const bundle = parseWorkflowPackage(JSON.stringify(fixture))
  expect(getPackageDefinition(bundle).flowCode).toBe('package_parent')
  expect(getPackageDefinition(bundle, 'package_child').flowName).toBe('Child')
  expect(getPackageForm(bundle, 'source-form-1')?.version).toBe('4')
  expect(parsePackageFormContent(getPackageForm(bundle, 'source-form-1')!)).toEqual({
    schemaVersion: 1, fields: [{ key: 'amount', label: 'Amount', type: 'number' }],
  })
  getPackageDefinition(bundle).flowName = 'Changed'
  expect(fixture.definitions[1].flowName).toBe('Parent')
})

test('rejects unsupported versions, plain designs and incomplete dependencies', () => {
  expect(() => parseWorkflowPackage({ ...fixture, schemaVersion: 2 })).toThrow()
  expect(() => parseWorkflowPackage(fixture.definitions[1])).toThrow()
  expect(() => parseWorkflowPackage({ ...fixture, definitions: [fixture.definitions[1]] })).toThrow()
  expect(() => parseWorkflowPackage({ ...fixture, forms: [] })).toThrow()
  expect(() => parseWorkflowPackage({ ...fixture, forms: [...fixture.forms, fixture.forms[0]] })).toThrow()
})

test('keeps external form references explicit without falling back to another form', () => {
  const value = JSON.parse(JSON.stringify(fixture))
  value.definitions[1].formId = 'host:expense'
  value.externalFormIds.push('host:expense')
  const bundle = parseWorkflowPackage(value)
  expect(bundle.externalFormIds).toEqual(['host:expense'])
  expect(getPackageForm(bundle, 'host:expense')).toBeUndefined()
})

test('rejects malformed JSON and dangling edges', () => {
  expect(() => parseWorkflowPackage('{')).toThrow()
  const value = JSON.parse(JSON.stringify(fixture))
  value.definitions[1].nodeList[0].skipList[0].targetNodeCode = 'missing'
  expect(() => parseWorkflowPackage(value)).toThrow()
})

test('rejects cyclic subprocess dependencies', () => {
  const value = JSON.parse(JSON.stringify(fixture))
  value.definitions[1].nodeList[1].ext = JSON.stringify({ 'subprocessConfig': JSON.stringify({ schemaVersion: 1, fixedChildFlowCode: 'package_parent' }) })
  expect(() => parseWorkflowPackage(value)).toThrow()
})
