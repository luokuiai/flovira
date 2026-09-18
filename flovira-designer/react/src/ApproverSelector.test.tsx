// @vitest-environment jsdom
import { createRef } from 'react'
import { act, cleanup, fireEvent, render, waitFor } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition, getApproverRule, getCarbonCopyRule, insertNodeAfter, setApproverRule, setCarbonCopyRule } from './model'
import { DEMO_CAPABILITIES } from '../../examples/capabilities'
import type { ApproverSelectionResult, ApproverSelector, ReactFlowDesignerRef } from './types'

afterEach(cleanup)
const oldUser = { id: 'old', type: 'USER', name: '原审批人' }
const newUser = { id: 'new', type: 'USER', name: '新审批人' }

test('shows a configured maximum and rejects oversized host results for approvers and transfer targets', async () => {
  const { definition, node } = fixture()
  const capabilities = { ...DEMO_CAPABILITIES, approverStrategies: DEMO_CAPABILITIES.approverStrategies.map(strategy =>
    strategy.code === 'USER' ? { ...strategy, maxSubjects: 2 } : strategy) }
  const selector = vi.fn<ApproverSelector>().mockResolvedValue({ subjects: [oldUser, newUser, { id: 'third', type: 'USER' }] })
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} capabilities={capabilities} onSelectApprover={selector} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  expect(view.getByText('新增人员（最多 2 人）')).toBeTruthy()
  const title = view.container.querySelector('.frd-settings-panel__title-copy')!
  expect(title.textContent).toBe('审批配置')
  expect(title.querySelector('small')).toBeNull()
  fireEvent.click(view.getByRole('button', { name: '选择用户' }))
  expect(await view.findByRole('alert')).toHaveProperty('textContent', '最多选择 2 项，请重新选择')
  expect(selector.mock.calls[0][0].strategy.maxSubjects).toBe(2)
  fireEvent.click(view.getByRole('radio', { name: '转交给指定人员' }))
  fireEvent.click(view.getByRole('button', { name: '选择指定人员' }))
  await waitFor(() => expect(selector).toHaveBeenCalledTimes(2))
  await waitFor(() => expect(view.getByRole('button', { name: '确定' }).hasAttribute('disabled')).toBe(false))
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  const rule = getApproverRule(ref.current!.getDefinition().nodeList[1])
  expect(rule.subjects).toEqual([oldUser])
  expect(rule.config?.sameAsStarterSubjects).toBeUndefined()
})

test('defaults an unconfigured approval draft to the backend INITIATOR strategy and commits only on confirmation', () => {
  const definition = createInitialDefinition()
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} capabilities={{ ...DEMO_CAPABILITIES,
    approverStrategies: [...DEMO_CAPABILITIES.approverStrategies, {
      code: 'INITIATOR', name: '提交人', selectionType: 'RELATION', multiple: false, editorType: 'NONE', resultCardinality: 'EXACTLY_ONE',
    }],
  }} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  expect((view.getByLabelText('审批人') as HTMLSelectElement).value).toBe('INITIATOR')
  expect(view.queryByRole('option', { name: '请选择人员策略' })).toBeNull()
  expect(getApproverRule(ref.current!.getDefinition().nodeList[1]).strategy).toBe('')
  fireEvent.click(view.getByRole('button', { name: '取消' }))
  expect(getApproverRule(ref.current!.getDefinition().nodeList[1]).strategy).toBe('')
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(getApproverRule(ref.current!.getDefinition().nodeList[1])).toMatchObject({
    strategy: 'INITIATOR', strategyVersion: 1, selectionType: 'RELATION', relationType: undefined,
  })
})

test('transfer choice picks separate users, cancellation preserves config, and node confirmation commits', async () => {
  const { definition, node } = fixture()
  const ref = createRef<ReactFlowDesignerRef>()
  const selector = vi.fn<ApproverSelector>().mockResolvedValueOnce(null).mockResolvedValueOnce({ subjects: [newUser] })
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} capabilities={DEMO_CAPABILITIES} onSelectApprover={selector} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  fireEvent.click(view.getByRole('radio', { name: '转交给指定人员' }))
  expect(selector).not.toHaveBeenCalled()
  fireEvent.click(view.getByRole('button', { name: '选择指定人员' }))
  await waitFor(() => expect(selector).toHaveBeenCalledTimes(1))
  await waitFor(() => expect(view.getByRole('button', { name: '选择指定人员' }).hasAttribute('disabled')).toBe(false))
  expect((view.getByRole('radio', { name: '转交给指定人员' }) as HTMLInputElement).checked).toBe(true)
  fireEvent.click(view.getByRole('button', { name: '选择指定人员' }))
  expect(await view.findByText('新审批人')).toBeTruthy()
  expect(selector.mock.calls[1][0].strategy.code).toBe('USER')
  expect(selector.mock.calls[1][0].selected).toEqual([])
  const current = () => getApproverRule(ref.current!.getDefinition().nodeList.find(item => item.nodeCode === node.nodeCode)!)
  expect(current().config?.sameAsStarterAction).toBeUndefined()
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(current().subjects).toEqual([oldUser])
  expect(current().config?.sameAsStarterSubjects).toEqual([newUser])
  expect(current().config?.sameAsStarterAction).toBe('TRANSFER_TO_USER')
})
function fixture(carbonCopy = false) {
  let definition = createInitialDefinition()
  if (carbonCopy) definition = insertNodeAfter(definition, definition.nodeList[0].nodeCode, '8')
  const node = definition.nodeList.find(entry => entry.nodeType === (carbonCopy ? '8' : '1'))!
  const setter = carbonCopy ? setCarbonCopyRule : setApproverRule
  definition.nodeList = definition.nodeList.map(entry => entry.nodeCode === node.nodeCode
    ? setter(entry, 'USER', [oldUser], '', undefined, 'RESOURCE', { approvalMode: 'OR' }) : entry)
  return { definition, node }
}
function deferred() {
  let resolve!: (value: ApproverSelectionResult | null) => void
  const promise = new Promise<ApproverSelectionResult | null>(done => { resolve = done })
  return { promise, resolve }
}

test.each([false, true])('host owns picker, commits detached results and supports undo (carbon copy %s)', async carbonCopy => {
  const { definition, node } = fixture(carbonCopy)
  const ref = createRef<ReactFlowDesignerRef>()
  const request = deferred()
  const selector = vi.fn<ApproverSelector>(context => {
    context.selected.push(newUser)
    context.node.nodeName = 'host mutation'
    return request.promise
  })
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} capabilities={DEMO_CAPABILITIES} onSelectApprover={selector} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  fireEvent.click(view.getByRole('button', { name: '选择用户' }))
  expect(view.queryByRole('dialog', { name: '人员选择' })).toBeNull()
  expect(view.queryByText('等待选择结果…')).toBeNull()
  expect(view.getByRole('button', { name: '选择用户' }).hasAttribute('disabled')).toBe(true)
  const getRule = () => (carbonCopy ? getCarbonCopyRule : getApproverRule)(ref.current!.getDefinition().nodeList.find(entry => entry.nodeCode === node.nodeCode)!)
  expect(getRule().subjects).toEqual([oldUser])
  expect(ref.current!.isDirty()).toBe(false)
  const result = { subjects: [{ ...newUser }] }
  await act(async () => request.resolve(result))
  expect(getRule().subjects).toEqual([oldUser])
  expect(ref.current!.isDirty()).toBe(false)
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(getRule().subjects).toEqual([newUser])
  expect(getRule().config).toEqual({ approvalMode: 'OR' })
  result.subjects[0].name = 'mutation after return'
  expect(getRule().subjects[0].name).toBe('新审批人')
  expect(ref.current!.isDirty()).toBe(true)
  act(() => ref.current!.undo())
  expect(getRule().subjects).toEqual([oldUser])
})

test('cancel preserves history; failure is visible and retry can explicitly clear the selection', async () => {
  const { definition, node } = fixture()
  const ref = createRef<ReactFlowDesignerRef>()
  const selector = vi.fn<ApproverSelector>().mockResolvedValueOnce(null)
    .mockRejectedValueOnce(new Error('业务选择器不可用')).mockResolvedValueOnce({ subjects: [] })
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} capabilities={DEMO_CAPABILITIES} onSelectApprover={selector} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  const choose = () => view.getByRole('button', { name: '选择用户' })
  fireEvent.click(choose())
  await waitFor(() => expect(choose().hasAttribute('disabled')).toBe(false))
  expect(ref.current!.isDirty()).toBe(false)
  fireEvent.click(choose())
  expect(await view.findByText('业务选择器不可用')).toBeTruthy()
  expect(ref.current!.isDirty()).toBe(false)
  fireEvent.click(choose())
  await waitFor(() => expect(choose().hasAttribute('disabled')).toBe(false))
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(getApproverRule(ref.current!.getDefinition().nodeList.find(entry => entry.nodeCode === node.nodeCode)!).subjects).toEqual([])
})

test.each(['node', 'strategy', 'import', 'disabled', 'unmount', 'cancel', 'reopen'])('ignores selection after %s changes', async change => {
  const { definition, node } = fixture()
  const ref = createRef<ReactFlowDesignerRef>()
  const request = deferred()
  const selector = vi.fn<ApproverSelector>(() => request.promise)
  const onChange = vi.fn()
  const props = { defaultValue: definition, capabilities: DEMO_CAPABILITIES, onSelectApprover: selector, onChange }
  const view = render(<ReactFlowDesigner ref={ref} {...props} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  fireEvent.click(view.getByRole('button', { name: '选择用户' }))
  if (change === 'node') fireEvent.click(view.getByRole('button', { name: '编辑节点：开始' }))
  if (change === 'strategy') fireEvent.change(view.getByLabelText('审批人'), { target: { value: 'ROLE' } })
  if (change === 'import') act(() => ref.current!.importJson({ ...definition, flowName: '新流程' }))
  if (change === 'disabled') view.rerender(<ReactFlowDesigner ref={ref} {...props} disabled />)
  if (change === 'unmount') view.unmount()
  if (change === 'cancel' || change === 'reopen') fireEvent.click(view.getByRole('button', { name: '取消' }))
  if (change === 'reopen') fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  const calls = onChange.mock.calls.length
  await act(async () => request.resolve({ subjects: [newUser] }))
  expect(onChange).toHaveBeenCalledTimes(calls)
})

test('rejects malformed results and enforces single selection', async () => {
  const { definition, node } = fixture()
  const ref = createRef<ReactFlowDesignerRef>()
  const capabilities = { ...DEMO_CAPABILITIES, approverStrategies: DEMO_CAPABILITIES.approverStrategies.map(strategy => ({ ...strategy, multiple: false })) }
  const selector = vi.fn<ApproverSelector>().mockResolvedValueOnce({ subjects: [{ id: '', type: 'USER' }] })
    .mockResolvedValueOnce({ subjects: [newUser, oldUser] })
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} capabilities={capabilities} onSelectApprover={selector} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  fireEvent.click(view.getByRole('button', { name: '选择用户' }))
  expect(await view.findByText('人员选择结果格式无效')).toBeTruthy()
  expect(ref.current!.isDirty()).toBe(false)
  fireEvent.click(view.getByRole('button', { name: '选择用户' }))
  await waitFor(() => expect(view.getByRole('button', { name: '选择用户' }).hasAttribute('disabled')).toBe(false))
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(getApproverRule(ref.current!.getDefinition().nodeList.find(entry => entry.nodeCode === node.nodeCode)!).subjects).toEqual([newUser])
})
