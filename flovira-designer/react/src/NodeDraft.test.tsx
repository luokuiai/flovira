// @vitest-environment jsdom
import { createRef } from 'react'
import { act, cleanup, fireEvent, render, within } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition, getNodeControlConfig, getTimeoutConfig } from './model'
import { DEMO_CAPABILITIES } from '../../examples/capabilities'
import { getNodeFormPermissions } from './formPermissions'
import type { ReactFlowDesignerRef } from './types'

afterEach(cleanup)

test('edits timeout duration and unit side by side and applies only on confirmation', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} capabilities={DEMO_CAPABILITIES} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('checkbox', { name: '启用超时处理' }))
  const row = view.container.querySelector('.frd-timeout-duration') as HTMLElement
  fireEvent.change(within(row).getByRole('spinbutton', { name: '超时时长' }), { target: { value: '3' } })
  fireEvent.change(within(row).getByRole('combobox', { name: '时间单位' }), { target: { value: 'DAYS' } })
  fireEvent.click(view.getByRole('radio', { name: '自动驳回' }))
  expect(getTimeoutConfig(ref.current!.getDefinition().nodeList[1]).enabled).not.toBe(true)
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(getTimeoutConfig(ref.current!.getDefinition().nodeList[1])).toMatchObject({ enabled: true, duration: 3, durationUnit: 'DAYS', action: 'AUTO_REJECT' })
})

function setup() {
  const ref = createRef<ReactFlowDesignerRef>()
  const onChange = vi.fn()
  const view = render(<ReactFlowDesigner ref={ref} onChange={onChange}
    formFields={[{ code: 'amount', label: '金额' }]} />)
  const original = ref.current!.getFlowJson()
  const open = () => fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  const rename = () => {
    fireEvent.click(view.getByRole('tab', { name: '基础信息' }))
    fireEvent.change(view.getByRole('textbox', { name: '节点名称' }), { target: { value: '财务审核' } })
  }
  return { ref, onChange, view, original, open, rename }
}

test('confirms all tabs as one history entry without exposing draft changes', async () => {
  const { ref, onChange, view, original, open, rename } = setup()
  open(); rename()
  fireEvent.click(view.getByRole('tab', { name: '审批配置' }))
  fireEvent.click(view.getByLabelText('允许转办'))
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  fireEvent.click(await view.findByRole('checkbox', { name: '金额（amount）可写' }))
  expect(ref.current!.getFlowJson()).toBe(original)
  expect(ref.current!.isDirty()).toBe(false)
  expect(onChange).not.toHaveBeenCalled()
  expect(view.getByRole('button', { name: '编辑节点：审批节点' })).toBeTruthy()
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  const node = ref.current!.getDefinition().nodeList[1]
  expect(node.nodeName).toBe('财务审核')
  expect(getNodeControlConfig(node).allowTransfer).toBe(true)
  expect(getNodeFormPermissions(node).fields).toEqual([{ code: 'amount', readable: true, writable: true }])
  expect(onChange).toHaveBeenCalledTimes(1)
  expect(view.queryByRole('dialog', { name: '节点设置' })).toBeNull()
  act(() => ref.current!.undo())
  expect(ref.current!.getFlowJson()).toBe(original)
  expect(view.getByRole('button', { name: '撤销' }).hasAttribute('disabled')).toBe(true)
  act(() => ref.current!.redo())
  expect(ref.current!.getDefinition().nodeList[1].nodeName).toBe('财务审核')
})

test.each(['cancel', 'close', 'mask', 'escape', 'switch'])('discards node draft on %s', action => {
  const { ref, onChange, view, original, open, rename } = setup()
  open(); rename()
  if (action === 'cancel') fireEvent.click(view.getByRole('button', { name: '取消' }))
  if (action === 'close') fireEvent.click(within(view.getByRole('dialog', { name: '节点设置' })).getByRole('button', { name: '关闭节点设置' }))
  if (action === 'mask') fireEvent.click(view.container.querySelector('.frd-drawer[data-open="true"] .frd-drawer__mask')!)
  if (action === 'escape') fireEvent.keyDown(document, { key: 'Escape' })
  if (action === 'switch') fireEvent.click(view.getByRole('button', { name: '编辑节点：开始' }))
  expect(ref.current!.getFlowJson()).toBe(original)
  expect(onChange).not.toHaveBeenCalled()
  open()
  fireEvent.click(view.getByRole('tab', { name: '基础信息' }))
  expect((view.getByRole('textbox', { name: '节点名称' }) as HTMLInputElement).value).toBe('审批节点')
})

test('no-op confirmation creates no change or history', () => {
  const { ref, onChange, view, open } = setup()
  open()
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(onChange).not.toHaveBeenCalled()
  expect(ref.current!.isDirty()).toBe(false)
  expect(view.getByRole('button', { name: '撤销' }).hasAttribute('disabled')).toBe(true)
})

test('external replacement invalidates drafts instead of overwriting new data', () => {
  const { ref, onChange, view, rename, open } = setup()
  open(); rename()
  const next = { ...createInitialDefinition(), flowName: '来自业务的新流程' }
  view.rerender(<ReactFlowDesigner ref={ref} onChange={onChange} value={next} />)
  expect(view.queryByRole('dialog', { name: '节点设置' })).toBeNull()
  expect(ref.current!.getDefinition().flowName).toBe(next.flowName)
  expect(ref.current!.getDefinition().nodeList[1].nodeName).toBe('审批节点')
  expect(onChange).not.toHaveBeenCalled()
})
