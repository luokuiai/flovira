// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, waitFor, within } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { createRef } from 'react'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition } from './model'
import type { DesignerConditionField, ReactFlowDesignerRef } from './types'

afterEach(cleanup)

test('configures a branch from business fields, then inserts business nodes independently', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const initial = createInitialDefinition()
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={initial} conditionFields={[
    { code: 'amount', label: '采购金额', type: 'NUMBER' },
    { code: 'urgent', label: '是否紧急', type: 'BOOLEAN' },
  ]} />)
  fireEvent.click(view.getByRole('button', { name: '在 开始 后添加节点' }))
  fireEvent.click(view.getByRole('menuitem', { name: '添加条件分支' }))
  const drawer = within(view.getByRole('dialog', { name: '分支条件' }))
  expect(view.queryByRole('button', { name: '编辑节点：分支一' })).toBeNull()
  fireEvent.click(drawer.getByRole('button', { name: '添加条件组' }))
  fireEvent.change(drawer.getByLabelText('比较方式 1-1'), { target: { value: 'GE' } })
  fireEvent.change(drawer.getByLabelText('条件值 1-1'), { target: { value: '10000' } })
  fireEvent.click(drawer.getByRole('button', { name: '添加条件组' }))
  fireEvent.change(drawer.getByLabelText('条件字段 2-1'), { target: { value: 'urgent' } })
  fireEvent.click(drawer.getByRole('button', { name: '保存条件' }))
  expect(view.queryByRole('dialog', { name: '分支条件' })).toBeNull()
  const split = ref.current!.getDefinition().nodeList.find((node) => node.nodeType === '3')!
  expect(split.skipList[0].skipCondition).toBe('spel@@#{(#amount >= 10000) or (#urgent == true)}')
  expect(view.getByRole('button', { name: '配置分支：分支一' }).textContent).toContain('采购金额 大于等于 10000 或 是否紧急 等于 是')
  fireEvent.click(view.getByRole('button', { name: '在分支 分支一 下添加节点' }))
  fireEvent.click(view.getByRole('menuitem', { name: '添加审批节点' }))
  expect(ref.current!.getDefinition().nodeList).toHaveLength(initial.nodeList.length + 2)
  expect(ref.current!.getDefinition().nodeList.find((node) => node.nodeCode === split.nodeCode)!.skipList[0].skipCondition).toBe(split.skipList[0].skipCondition)
})

test('keeps incomplete conditions open and permits a default branch without field configuration', () => {
  const view = render(<ReactFlowDesigner defaultValue={createInitialDefinition()} />)
  fireEvent.click(view.getByRole('button', { name: '在 开始 后添加节点' }))
  fireEvent.click(view.getByRole('menuitem', { name: '添加条件分支' }))
  let drawer = within(view.getByRole('dialog', { name: '分支条件' }))
  expect(drawer.getByRole('button', { name: '添加条件组' }).hasAttribute('disabled')).toBe(true)
  fireEvent.click(drawer.getByRole('button', { name: '保存条件' }))
  expect(drawer.getByRole('alert').textContent).toContain('条件')
  fireEvent.click(drawer.getByRole('button', { name: '关闭分支条件' }))
  fireEvent.click(view.getByRole('button', { name: '配置分支：其他条件' }))
  drawer = within(view.getByRole('dialog', { name: '分支条件' }))
  expect(drawer.getByText('其他分支条件都不满足时，进入此分支。')).toBeTruthy()
})


test('loads form fields through the host callback and retries failures with branch context', async () => {
  const loader = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue([
    { code: 'formAmount', label: '表单金额', type: 'NUMBER' },
  ])
  const view = render(<ReactFlowDesigner defaultValue={createInitialDefinition()} queryConditionFields={loader} />)
  fireEvent.click(view.getByRole('button', { name: '在 开始 后添加节点' }))
  fireEvent.click(view.getByRole('menuitem', { name: '添加条件分支' }))
  expect(await view.findByText('表单字段加载失败')).toBeTruthy()
  fireEvent.click(view.getByRole('button', { name: '重新加载' }))
  const addGroup = await view.findByRole('button', { name: '添加条件组' })
  fireEvent.click(addGroup)
  expect(view.getByRole('option', { name: '表单金额' })).toBeTruthy()
  expect(loader).toHaveBeenCalledTimes(2)
  expect(loader.mock.calls[0][0].node.nodeType).toBe('3')
  expect(loader.mock.calls[0][0].branch.skipName).toBe('分支一')
})

test('ignores a field response from a closed branch editor', async () => {
  let resolveOld!: (fields: DesignerConditionField[]) => void
  const loader = vi.fn().mockImplementationOnce(() => new Promise<DesignerConditionField[]>((resolve) => { resolveOld = resolve }))
    .mockResolvedValue([{ code: 'current', label: '当前表单字段', type: 'STRING' }])
  const view = render(<ReactFlowDesigner defaultValue={createInitialDefinition()} queryConditionFields={loader} />)
  fireEvent.click(view.getByRole('button', { name: '在 开始 后添加节点' }))
  fireEvent.click(view.getByRole('menuitem', { name: '添加条件分支' }))
  await waitFor(() => expect(loader).toHaveBeenCalledTimes(1))
  fireEvent.click(within(view.getByRole('dialog', { name: '分支条件' })).getByRole('button', { name: '关闭分支条件' }))
  fireEvent.click(view.getByRole('button', { name: '配置分支：分支一' }))
  const addGroup = await view.findByRole('button', { name: '添加条件组' })
  await act(async () => resolveOld([{ code: 'old', label: '过期字段', type: 'STRING' }]))
  fireEvent.click(addGroup)
  expect(view.getByRole('option', { name: '当前表单字段' })).toBeTruthy()
  expect(view.queryByRole('option', { name: '过期字段' })).toBeNull()
})
