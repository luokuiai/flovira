import { test } from 'node:test'
import assert from 'node:assert/strict'
import { migrateFormReferences } from './migrate-form-references.mjs'

test('maps definition and nested node references without changing business data', () => {
  const input = {
    formCustom: 'N', formPath: '/expense', formPathList: [],
    variables: { formPath: 'business-value', formCustom: 'business-value' },
    nodeList: [{ nodeCode: 'approval', formCustom: 'Y', formPath: '20', ext: '{"formPath":"business-value"}' }],
  }
  const result = migrateFormReferences(input, [
    { formCustom: 'N', formPath: '/expense', formId: 'expense:v2' },
    { formCustom: 'Y', formPath: '20', formId: 'finance:v1' },
  ])
  assert.equal(result.formId, 'expense:v2')
  assert.equal(result.nodeList[0].formId, 'finance:v1')
  assert.equal('formCustom' in result, false)
  assert.equal('formPath' in result.nodeList[0], false)
  assert.deepEqual(result.variables, input.variables)
  assert.equal(result.nodeList[0].ext, input.nodeList[0].ext)
  assert.equal(input.formPath, '/expense')
  assert.deepEqual(migrateFormReferences(result, []), result)
})

test('refuses missing, ambiguous, or conflicting mappings', () => {
  const input = { formCustom: 'Y', formPath: '20', nodeList: [] }
  const mapping = { formCustom: 'Y', formPath: '20', formId: 'expense:v2' }
  assert.throws(() => migrateFormReferences(input, []), /exactly one/)
  assert.throws(() => migrateFormReferences(input, [mapping, mapping]), /exactly one/)
  assert.throws(() => migrateFormReferences({ ...input, formId: 'other' }, [mapping]), /Conflicting/)
  assert.throws(() => migrateFormReferences(input, [{ ...mapping, formId: '' }]), /Invalid/)
})
