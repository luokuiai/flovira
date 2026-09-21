// @vitest-environment jsdom
import { cleanup, fireEvent, render, waitFor, within } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { SubprocessField } from './SubprocessField'
import { defaultDesignerUi } from './ui'
import type { DesignerResourcePage } from './types'

afterEach(cleanup)
const item = (code: string) => ({ id: code, code, name: code, resourceType: 'SUBPROCESS' })

test('loads only on demand, paginates and resets search while preserving the selected code', async () => {
  const query = vi.fn(async ({ pageNum, keyword }) => ({
    items: [item(keyword || `flow_${pageNum}`)], total: keyword ? 1 : 21,
  }))
  const view = render(<SubprocessField value="saved" queryResources={query} ui={defaultDesignerUi} onChange={() => {}} />)
  expect(query).not.toHaveBeenCalled()
  expect(view.getByRole('button', { name: '子流程' }).textContent).toBe('saved')
  expect(view.queryByText('选择流程')).toBeNull()
  fireEvent.click(view.getByRole('button', { name: '子流程' }))
  await view.findByRole('option', { name: 'flow_1' })
  expect(query).toHaveBeenLastCalledWith({ resourceType: 'SUBPROCESS', keyword: '', pageNum: 1, pageSize: 20 })
  fireEvent.click(view.getByText('加载更多'))
  await view.findByRole('option', { name: 'flow_2' })
  expect(view.getByRole('button', { name: '子流程' }).getAttribute('aria-expanded')).toBe('true')
  expect(view.getByRole('option', { name: 'flow_1' })).toBeTruthy()
  expect(view.queryByText('加载更多')).toBeNull()
  fireEvent.change(view.getByLabelText('搜索子流程'), { target: { value: 'finance' } })
  await view.findByRole('option', { name: 'finance' })
  expect(query).toHaveBeenLastCalledWith({ resourceType: 'SUBPROCESS', keyword: 'finance', pageNum: 1, pageSize: 20 })
  expect(view.queryByRole('option', { name: 'flow_1' })).toBeNull()
  expect(view.getByRole('button', { name: '子流程' }).textContent).toBe('saved')
})

test('ignores stale requests and retries the failed page', async () => {
  let resolveOld!: (page: DesignerResourcePage) => void
  const query = vi.fn().mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
    .mockRejectedValueOnce(new Error('network')).mockResolvedValue({ items: [item('new')], total: 1 })
  const view = render(<SubprocessField value="saved" queryResources={query} ui={defaultDesignerUi} onChange={() => {}} />)
  fireEvent.click(view.getByRole('button', { name: '子流程' }))
  await waitFor(() => expect(query).toHaveBeenCalledTimes(1))
  fireEvent.change(view.getByLabelText('搜索子流程'), { target: { value: 'new' } })
  await view.findByRole('alert')
  resolveOld({ items: [item('stale')], total: 1 })
  fireEvent.click(view.getByText('重试'))
  await view.findByRole('option', { name: 'new' })
  expect(view.queryByRole('option', { name: 'stale' })).toBeNull()
  expect(query.mock.calls[2][0].pageNum).toBe(1)
})

test('read-only fields never fetch candidates', () => {
  const query = vi.fn()
  const view = render(<SubprocessField value="saved" disabled queryResources={query} ui={defaultDesignerUi} onChange={() => {}} />)
  fireEvent.click(view.getByRole('button', { name: '子流程' }))
  expect(query).not.toHaveBeenCalled()
})

test('keeps search and pagination inside the dropdown and closes after choosing', async () => {
  const onChange = vi.fn()
  const query = vi.fn(async () => ({ items: [item('expense')], total: 21 }))
  const view = render(<SubprocessField value="" queryResources={query} ui={defaultDesignerUi} onChange={onChange} />)
  expect(view.queryByLabelText('搜索子流程')).toBeNull()
  fireEvent.click(view.getByRole('button', { name: '子流程' }))
  await view.findByRole('option', { name: 'expense' })
  const dropdown = within(view.container.querySelector('.frd-dropdown-menu') as HTMLElement)
  expect(dropdown.getByLabelText('搜索子流程')).toBeTruthy()
  expect(dropdown.getByRole('button', { name: '加载更多' })).toBeTruthy()
  fireEvent.click(dropdown.getByRole('option', { name: 'expense' }))
  expect(onChange).toHaveBeenCalledWith('expense', 'expense')
  expect(view.queryByRole('listbox')).toBeNull()
})
