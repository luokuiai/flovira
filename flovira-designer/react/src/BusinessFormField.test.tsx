// @vitest-environment jsdom
import { createRef } from 'react'
import { cleanup, fireEvent, render, waitFor } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { BusinessFormField } from './BusinessFormField'
import { defaultDesignerUi } from './ui'
import type { ReactFlowDesignerRef } from './types'

afterEach(cleanup)

test('persists and clears node overrides independently from the workflow form', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.change(view.getByLabelText('业务表单标识'), { target: { value: 'finance:v1' } })
  expect(JSON.parse(ref.current!.getFlowJson()).nodeList.find((node: { nodeType: string }) => node.nodeType === '1').formId).toBe('finance:v1')
  fireEvent.change(view.getByLabelText('业务表单标识'), { target: { value: '' } })
  expect(JSON.parse(ref.current!.getFlowJson()).nodeList.find((node: { nodeType: string }) => node.nodeType === '1').formId).toBe('')
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
