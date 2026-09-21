// @vitest-environment jsdom
import { createRef } from 'react'
import { cleanup, fireEvent, render, waitFor } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { BusinessFormField } from './BusinessFormField'
import { defaultDesignerUi } from './ui'
import { createInitialDefinition, normalizeDefinition, serializeDefinition } from './model'
import type { ReactFlowDesignerRef } from './types'

afterEach(cleanup)

test('imports and exports only the workflow-level form reference', () => {
  const definition = createInitialDefinition()
  definition.formId = 'workflow-form'
  definition.nodeList[1].formId = 'other-form'
  const normalized = normalizeDefinition(definition)
  expect(normalized.formId).toBe('workflow-form')
  expect(normalized.nodeList[1].formId).toBeUndefined()
  expect(JSON.parse(serializeDefinition(definition)).nodeList[1].formId).toBeUndefined()
  expect(definition.nodeList[1].formId).toBe('other-form')
})

test('keeps the workflow form reference out of node settings', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const definition = createInitialDefinition()
  definition.formId = 'finance:v1'
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：开始' }))
  expect(view.queryByLabelText('业务表单标识')).toBeNull()
  expect(view.queryByLabelText('业务表单')).toBeNull()
  expect(view.queryByText('节点表单')).toBeNull()
  expect(view.queryByText('继承流程表单')).toBeNull()
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(JSON.parse(ref.current!.getFlowJson()).formId).toBe('finance:v1')
  expect(JSON.parse(ref.current!.getFlowJson()).nodeList.every((node: { formId?: string }) => !node.formId)).toBe(true)
})

test('loads external form choices and retains a saved form missing from the current list', async () => {
  const queryResources = vi.fn(async () => ({ items: [{ id: 'expense:v2', name: '报销单', resourceType: 'FORM' }], total: 1 }))
  const onChange = vi.fn()
  const view = render(<BusinessFormField value="archived:v1" ui={defaultDesignerUi}
    queryResources={queryResources} onChange={onChange} />)
  await waitFor(() => expect(view.getByRole('option', { name: '报销单' })).toBeTruthy())
  expect(queryResources).toHaveBeenCalledWith({ resourceType: 'FORM', pageNum: 1, pageSize: 1000 })
  expect((view.getByLabelText('业务表单') as HTMLSelectElement).value).toBe('archived:v1')
  fireEvent.change(view.getByLabelText('业务表单'), { target: { value: 'expense:v2' } })
  expect(onChange).toHaveBeenCalledWith('expense:v2')
})

test('shows provider failures instead of an empty successful list', async () => {
  const view = render(<BusinessFormField value="expense:v2" ui={defaultDesignerUi}
    queryResources={async () => { throw new Error('offline') }} onChange={() => {}} />)
  await waitFor(() => expect(view.getByRole('alert').textContent).toContain('表单加载失败'))
  expect((view.getByLabelText('业务表单') as HTMLSelectElement).value).toBe('expense:v2')
})
