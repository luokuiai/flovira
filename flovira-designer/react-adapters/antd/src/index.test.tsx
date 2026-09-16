// @vitest-environment jsdom

import { cleanup, fireEvent, render, waitFor } from '@testing-library/react'
import { afterEach, beforeAll, describe, expect, test, vi } from 'vitest'
import { antdDesignerUi } from './index'

beforeAll(() => {
  const getComputedStyle = window.getComputedStyle
  window.getComputedStyle = (element) => getComputedStyle(element)
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(() => ({
      matches: false,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })
  globalThis.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
})

afterEach(cleanup)

describe('antdDesignerUi', () => {
  test('maps semantic input changes to Ant Design Input', () => {
    const onValueChange = vi.fn()
    const AdapterInput = antdDesignerUi.Input
    const view = render(<AdapterInput value="before" ariaLabel="节点名称" onValueChange={onValueChange} />)

    fireEvent.change(view.getByLabelText('节点名称'), { target: { value: 'after' } })
    expect(onValueChange).toHaveBeenCalledWith('after')
  })

  test('maps semantic button and checkbox events', () => {
    const onPress = vi.fn()
    const onCheckedChange = vi.fn()
    const AdapterButton = antdDesignerUi.Button
    const AdapterCheckbox = antdDesignerUi.Checkbox
    const view = render(
      <>
        <AdapterButton variant="primary" onPress={onPress}>保存</AdapterButton>
        <AdapterCheckbox checked={false} ariaLabel="启用超时" onCheckedChange={onCheckedChange} />
      </>,
    )

    fireEvent.click(view.getByRole('button'))
    fireEvent.click(view.getByLabelText('启用超时'))
    expect(onPress).toHaveBeenCalledOnce()
    expect(onCheckedChange).toHaveBeenCalledWith(true)
  })

  test('maps an Ant Design single-select value', () => {
    const onValueChange = vi.fn()
    const AdapterSelect = antdDesignerUi.Select
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

    fireEvent.mouseDown(view.getByRole('combobox'))
    fireEvent.click(document.querySelector('[title="角色"]')!)
    expect(onValueChange).toHaveBeenCalledWith('ROLE')
  })

  test('maps an Ant Design radio-group value', () => {
    const onValueChange = vi.fn()
    const AdapterRadioGroup = antdDesignerUi.RadioGroup
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

  test('uses Ant Design Dropdown for semantic menu items', async () => {
    const onSelect = vi.fn()
    const AdapterDropdown = antdDesignerUi.DropdownMenu
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

  test('uses Ant Design Drawer for the settings surface', () => {
    const onClose = vi.fn()
    const AdapterDrawer = antdDesignerUi.Drawer
    const view = render(
      <AdapterDrawer open title="节点设置" ariaLabel="节点设置" onClose={onClose}>
        <span>设置内容</span>
      </AdapterDrawer>,
    )

    expect(document.querySelector('.ant-drawer')).not.toBeNull()
    expect(view.getByText('设置内容')).toBeTruthy()
    fireEvent.click(document.querySelector('.ant-drawer-close')!)
    expect(onClose).toHaveBeenCalledOnce()
  })

  test('uses Ant Design Modal for business picker content', () => {
    const onConfirm = vi.fn()
    const AdapterDialog = antdDesignerUi.Dialog!
    const view = render(
      <AdapterDialog open title="选择审批人员" ariaLabel="人员选择" onClose={() => undefined} onConfirm={onConfirm}>
        <span>组织架构</span>
      </AdapterDialog>,
    )

    expect(view.getByRole('dialog', { name: '选择审批人员' })).toBeTruthy()
    expect(view.getByText('组织架构')).toBeTruthy()
    fireEvent.click(view.getByText('确 定').closest('button')!)
    expect(onConfirm).toHaveBeenCalledOnce()
  })
})
