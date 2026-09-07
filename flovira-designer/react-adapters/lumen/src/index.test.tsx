// @vitest-environment jsdom

import { cleanup, fireEvent, render, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { lumenDesignerUi } from './index'

afterEach(cleanup)

describe('lumenDesignerUi', () => {
  test('maps semantic input changes to Lumen Input', () => {
    const onValueChange = vi.fn()
    const AdapterInput = lumenDesignerUi.Input
    const view = render(<AdapterInput value="before" ariaLabel="节点名称" onValueChange={onValueChange} />)

    const input = view.getByLabelText('节点名称')
    expect(input.getAttribute('data-ui')).toBe('input')
    fireEvent.change(input, { target: { value: 'after' } })
    expect(onValueChange).toHaveBeenCalledWith('after')
  })

  test('maps semantic button and checkbox events', () => {
    const onPress = vi.fn()
    const onCheckedChange = vi.fn()
    const AdapterButton = lumenDesignerUi.Button
    const AdapterCheckbox = lumenDesignerUi.Checkbox
    const view = render(
      <>
        <AdapterButton variant="primary" onPress={onPress}>保存</AdapterButton>
        <AdapterCheckbox checked={false} ariaLabel="启用超时" onCheckedChange={onCheckedChange} />
      </>,
    )

    fireEvent.click(view.getByRole('button', { name: '保存' }))
    fireEvent.click(view.getByLabelText('启用超时'))
    expect(onPress).toHaveBeenCalledOnce()
    expect(onCheckedChange).toHaveBeenCalledWith(true)
  })

  test('maps a Lumen single-select value', () => {
    const onValueChange = vi.fn()
    const AdapterSelect = lumenDesignerUi.Select
    const view = render(
      <AdapterSelect
        value="USER"
        options={[
          { value: 'USER', label: '用户' },
          { value: 'ROLE', label: '角色' },
        ]}
        ariaLabel="办理人类型"
        onValueChange={onValueChange}
      />,
    )

    fireEvent.click(view.getByTestId('select-trigger'))
    fireEvent.click(document.querySelector('[data-ui="select-option"]:last-child')!)
    expect(onValueChange).toHaveBeenCalledWith('ROLE')
  })

  test('maps a Lumen radio-group value', () => {
    const onValueChange = vi.fn()
    const AdapterRadioGroup = lumenDesignerUi.RadioGroup
    const view = render(
      <AdapterRadioGroup
        value="OR"
        options={[
          { value: 'OR', label: '或签' },
          { value: 'COUNTERSIGN', label: '会签' },
        ]}
        ariaLabel="多人审批方式"
        onValueChange={onValueChange}
      />,
    )

    fireEvent.click(view.getByRole('radio', { name: '会签' }))
    expect(onValueChange).toHaveBeenCalledWith('COUNTERSIGN')
  })

  test('uses Lumen DropdownMenu for semantic menu items', async () => {
    const onSelect = vi.fn()
    const AdapterDropdown = lumenDesignerUi.DropdownMenu
    const view = render(
      <AdapterDropdown
        trigger={<button type="button">新增</button>}
        items={[{ value: '1', label: '添加审批节点', icon: <span>图</span>, color: '#ff943e' }]}
        onSelect={onSelect}
      />,
    )

    fireEvent.click(view.getByRole('button', { name: '新增' }))
    await waitFor(() => expect(document.querySelector('[role="menuitem"]')).not.toBeNull())
    const item = document.querySelector('[role="menuitem"]') as HTMLElement
    fireEvent.click(item)
    expect(onSelect).toHaveBeenCalledWith('1')
  })

  test('uses Lumen Drawer for the settings surface', () => {
    const onClose = vi.fn()
    const AdapterDrawer = lumenDesignerUi.Drawer
    const view = render(
      <AdapterDrawer open title="节点设置" ariaLabel="节点设置" onClose={onClose}>
        <span>设置内容</span>
      </AdapterDrawer>,
    )

    expect(document.querySelector('[data-drawer-state="open"]')).not.toBeNull()
    expect(view.getByRole('dialog', { name: '节点设置' })).toBeTruthy()
    fireEvent.click(view.getByRole('button', { name: '关闭节点设置' }))
    expect(onClose).toHaveBeenCalledOnce()
  })

  test('uses Lumen Modal for business picker content', () => {
    const onConfirm = vi.fn()
    const AdapterDialog = lumenDesignerUi.Dialog!
    const view = render(
      <AdapterDialog open title="选择审批人员" ariaLabel="人员选择" onClose={() => undefined} onConfirm={onConfirm}>
        <span>组织架构</span>
      </AdapterDialog>,
    )

    expect(view.getByRole('dialog', { name: '人员选择' })).toBeTruthy()
    expect(view.getByText('组织架构')).toBeTruthy()
    fireEvent.click(view.getByRole('button', { name: '确定' }))
    expect(onConfirm).toHaveBeenCalledOnce()
  })
})
