// @vitest-environment jsdom
import { createRef } from 'react'
import { act, cleanup, fireEvent, render, waitFor, within } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition, insertNodeAfter, serializeDefinition } from './model'
import { getFormPermissionFields, getNodeFormPermissions, setNodeFieldPermission } from './formPermissions'
import type { DesignerFormField } from './formPermissions'
import type { ReactFlowDesignerRef } from './types'
import { DEMO_CAPABILITIES } from '../../examples/capabilities'

afterEach(cleanup)
const fields = [{ code: 'amount', label: '报销金额' }, { code: 'details[].name', label: '报销明细 / 费用名称' }]
test('expands actual form fields, without synthetic array counts', () => {
  expect(getFormPermissionFields({ schemaVersion: '1', fields: [
    { key: 'details', label: '明细', dataType: 'array', items: { dataType: 'object', fields: [{ key: 'name', label: '名称', dataType: 'string' }] } },
  ] })).toEqual([{ code: 'details[].name', label: '明细 / 每一项 / 名称', dataType: 'string' }])
})

test('preserves unrelated extensions and round trips versioned permissions', () => {
  const node = createInitialDefinition().nodeList[1]
  node.ext = JSON.stringify([{ code: 'business', value: '{"keep":true}' }])
  const updated = setNodeFieldPermission(node, { code: 'amount', readable: false, writable: true })
  expect(getNodeFormPermissions(updated).fields).toEqual([{ code: 'amount', readable: true, writable: true }])
  expect(JSON.parse(updated.ext as string)[0]).toEqual(JSON.parse(node.ext)[0])
  expect(getNodeFormPermissions(node).fields).toEqual([])
  const flow = createInitialDefinition()
  flow.nodeList[1] = updated
  expect(getNodeFormPermissions(JSON.parse(serializeDefinition(flow)).nodeList[1])).toEqual(getNodeFormPermissions(updated))
  expect(() => getNodeFormPermissions({ ...node, ext: 'broken' })).toThrow()
})

test('drawer separates tabs, enforces read/write dependency, persists per node and supports undo', async () => {
  const definition = createInitialDefinition()
  const node = definition.nodeList[1]
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} formFields={fields} capabilities={DEMO_CAPABILITIES} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：' + node.nodeName }))
  expect(view.getByRole('tab', { name: '审批配置' }).getAttribute('aria-selected')).toBe('true')
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  const table = within(await view.findByRole('table', { name: '节点表单权限' }))
  expect(view.queryByRole('combobox', { name: '审批人' })).toBeNull()
  const read = () => table.getByRole('checkbox', { name: '报销金额（amount）可读' }) as HTMLInputElement
  const write = () => table.getByRole('checkbox', { name: '报销金额（amount）可写' }) as HTMLInputElement
  expect(read().checked).toBe(true); expect(write().checked).toBe(false)
  fireEvent.click(read())
  expect(read().checked).toBe(false)
  fireEvent.click(write())
  expect(read().checked).toBe(true); expect(write().checked).toBe(true)
  fireEvent.click(read())
  expect(read().checked).toBe(false); expect(write().checked).toBe(false)
  expect(getNodeFormPermissions(ref.current!.getDefinition().nodeList[1]).fields).toEqual([])
  expect(getNodeFormPermissions(ref.current!.getDefinition().nodeList[0]).fields).toEqual([])
  fireEvent.keyDown(view.getByRole('tab', { name: '表单权限' }), { key: 'Home' })
  expect(view.getByRole('textbox', { name: '节点名称' })).toBeTruthy()
  expect(view.queryByRole('combobox', { name: '审批人' })).toBeNull()
  fireEvent.keyDown(view.getByRole('tab', { name: '基础信息' }), { key: 'ArrowRight' })
  expect(view.getByRole('combobox', { name: '审批人' })).toBeTruthy()
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  expect(getNodeFormPermissions(ref.current!.getDefinition().nodeList[1]).fields).toEqual([{ code: 'amount', readable: false, writable: false }])
  act(() => ref.current!.undo())
  expect(getNodeFormPermissions(ref.current!.getDefinition().nodeList[1]).fields).toEqual([])
  fireEvent.click(view.getByRole('button', { name: '编辑节点：开始' }))
  expect(view.queryByRole('tab')).toBeNull()
  expect(view.queryByRole('table', { name: '节点表单权限' })).toBeNull()
})

test('loads fields from host with node context and can retry a failure', async () => {
  const definition = createInitialDefinition()
  const loader = vi.fn().mockRejectedValueOnce(new Error('表单接口不可用')).mockResolvedValue(fields)
  const view = render(<ReactFlowDesigner defaultValue={definition} queryFormFields={loader} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  expect(await view.findByText('表单接口不可用')).toBeTruthy()
  expect(loader.mock.calls[0][0].node.nodeCode).toBe(definition.nodeList[1].nodeCode)
  fireEvent.click(view.getByRole('button', { name: '重新加载' }))
  expect(await view.findByRole('table', { name: '节点表单权限' })).toBeTruthy()
})

test('ignores responses from the previously selected node and disables read-only permissions', async () => {
  let resolve!: (value: DesignerFormField[]) => void
  const loader = vi.fn().mockImplementationOnce(() => new Promise(done => { resolve = done }))
    .mockResolvedValue([{ code: 'current', label: '当前字段' }])
  const initial = createInitialDefinition()
  const definition = insertNodeAfter(initial, initial.nodeList[0].nodeCode, '1')
  definition.nodeList.find(node => node.nodeCode !== initial.nodeList[1].nodeCode && node.nodeType === '1')!.nodeName = '另一审批节点'
  const view = render(<ReactFlowDesigner defaultValue={definition} queryFormFields={loader} disabled />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：另一审批节点' }))
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  await waitFor(() => expect(loader).toHaveBeenCalledTimes(1))
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  await view.findByRole('table', { name: '节点表单权限' })
  await act(async () => resolve(fields))
  expect(view.queryByText('报销金额')).toBeNull()
  expect(view.getByRole('checkbox', { name: '当前字段（current）可读' }).hasAttribute('disabled')).toBe(true)
})

test('keeps unmatched saved fields visible and blocks malformed permission configuration', async () => {
  const definition = createInitialDefinition()
  definition.nodeList[1] = setNodeFieldPermission(definition.nodeList[1], { code: 'removed', readable: false, writable: false })
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} formFields={fields} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  expect(await view.findByText('未匹配字段')).toBeTruthy()
  expect(view.getByText('removed')).toBeTruthy()
  const bad = createInitialDefinition()
  bad.nodeList[1].ext = JSON.stringify([{ code: 'formPermissions', value: '{"schemaVersion":99,"fields":[]}' }])
  act(() => ref.current!.importJson(bad))
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('tab', { name: '表单权限' }))
  expect(await view.findByText('表单权限配置格式无效')).toBeTruthy()
  expect(view.queryByRole('table', { name: '节点表单权限' })).toBeNull()
})
