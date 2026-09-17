// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, within } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { createRef, useState } from 'react'
import { FormDesigner } from './FormDesigner'
import { getFormConditionFields, type FormDefinition } from './formDefinition'
import type { FormDesignerInstance } from './formDesignerModel'
afterEach(cleanup)
const initial: FormDefinition = { schemaVersion: '1', renderer: { custom: { layout: 'host' } }, fields: [
  { key: 'Order_ID', label: '订单编号', dataType: 'string' },
  { key: 'details', label: '明细', dataType: 'array', items: { dataType: 'object', fields: [
    { key: 'AMOUNT', label: '金额', dataType: 'number' },
  ] } },
] }
test('limits objects and detail rows to scalar children and rejects imported multi-level structures', () => {
  const ref = createRef<FormDesignerInstance>()
  const view = render(<FormDesigner ref={ref} value={initial} />)
  expect(within(view.getByLabelText('数据类型 2[]')).queryByRole('option', { name: '数组' })).toBeNull()
  expect(within(view.getByLabelText('数据类型 2[].1')).queryByRole('option', { name: '对象' })).toBeNull()
  expect(within(view.getByLabelText('数据类型 2[].1')).queryByRole('option', { name: '数组' })).toBeNull()
  const nested: FormDefinition = { schemaVersion: '1', fields: [{
    key: 'a', label: '对象', dataType: 'object', fields: [{ key: 'b', label: '子对象', dataType: 'object', fields: [{ key: 'c', label: '值', dataType: 'string' }] }],
  }] }
  view.rerender(<FormDesigner ref={ref} value={nested} />)
  act(() => expect(ref.current!.validate().message).toContain('不支持多层嵌套'))
  expect(ref.current!.getDefinition()).toEqual(nested)
})
test('edits controlled forms without mutating the host and preserves renderer metadata', () => {
  const ref = createRef<FormDesignerInstance>()
  function Host() {
    const [value, setValue] = useState(initial)
    return <FormDesigner ref={ref} value={value} onChange={setValue} appearance="embedded" />
  }
  const view = render(<Host />)
  fireEvent.change(view.getByLabelText('字段名称 1'), { target: { value: '业务单号' } })
  fireEvent.change(view.getByLabelText('字段键 1'), { target: { value: '_ORDER_ID' } })
  expect(initial.fields[0].label).toBe('订单编号')
  expect(ref.current!.getDefinition().renderer).toEqual(initial.renderer)
  expect(JSON.parse(ref.current!.getJson()).fields[0].key).toBe('_ORDER_ID')
  act(() => expect(ref.current!.validate().valid).toBe(true))
  expect(getFormConditionFields(ref.current!.getDefinition()).map(field => field.label)).toContain('明细 / 每一项 / 金额')
  const copy = ref.current!.getDefinition(); copy.fields[0].label = 'external mutation'
  expect(ref.current!.getDefinition().fields[0].label).toBe('业务单号')
  expect(view.container.querySelector('.ffd-form')!.hasAttribute('tabindex')).toBe(false)
  act(() => ref.current!.focusFirstField())
  expect(document.activeElement).toBe(view.getByLabelText('字段名称 1'))
  expect(view.getByLabelText('字段名称 1').getAttribute('placeholder')).toBe('请输入字段名称')
  expect(view.getByLabelText('字段键 1').getAttribute('placeholder')).toContain('请输入字段键')
  expect(view.queryByRole('button', { name: '保存' })).toBeNull()
})
test('adds, reorders, deletes and validates fields using the existing schema', () => {
  const ref = createRef<FormDesignerInstance>()
  const view = render(<FormDesigner ref={ref} defaultValue={initial} />)
  fireEvent.click(view.getByRole('button', { name: '添加字段' }))
  act(() => expect(ref.current!.validate().valid).toBe(false))
  expect(view.getByRole('alert')).toBeTruthy()
  fireEvent.change(view.getByLabelText('字段名称 3'), { target: { value: '日期' } })
  fireEvent.change(view.getByLabelText('字段键 3'), { target: { value: 'Order_ID' } })
  act(() => expect(ref.current!.validate().message).toContain('重复'))
  fireEvent.change(view.getByLabelText('字段键 3'), { target: { value: 'Created_At' } })
  fireEvent.change(view.getByLabelText('数据类型 3'), { target: { value: 'date' } })
  fireEvent.click(view.getByRole('button', { name: '字段操作 3' }))
  fireEvent.click(view.getByRole('menuitem', { name: '上移' }))
  expect(ref.current!.getDefinition().fields[1].key).toBe('Created_At')
  fireEvent.click(view.getByRole('button', { name: '字段操作 3' }))
  fireEvent.click(view.getByRole('menuitem', { name: '删除' }))
  act(() => expect(ref.current!.validate().valid).toBe(true))
  expect(ref.current!.getDefinition().fields).toHaveLength(2)
})
test('requires confirmation to discard nested fields and supports arrays of objects', () => {
  const ref = createRef<FormDesignerInstance>()
  const view = render(<FormDesigner ref={ref} defaultValue={initial} />)
  fireEvent.change(view.getByLabelText('数据类型 2'), { target: { value: 'number' } })
  expect(ref.current!.getDefinition().fields[1].dataType).toBe('array')
  fireEvent.click(view.getByRole('button', { name: '取消' }))
  expect(view.getByLabelText('字段名称 2[].1')).toBeTruthy()
  fireEvent.click(view.getByRole('button', { name: '添加子字段' }))
  expect(view.getByLabelText('字段名称 2[].2')).toBeTruthy()
  fireEvent.change(view.getByLabelText('数据类型 2'), { target: { value: 'number' } })
  fireEvent.click(view.getByRole('button', { name: '确认切换' }))
  expect(ref.current!.getDefinition().fields[1]).toEqual({ key: 'details', label: '明细', dataType: 'number' })
})
test('read-only mode exposes no editing actions and honors injected controls', () => {
  const input = vi.fn(({ value }: { value: string | number }) => <input value={value} disabled readOnly />)
  const view = render(<FormDesigner defaultValue={initial} readOnly ui={{ Input: input }} />)
  expect(input).toHaveBeenCalled()
  expect(view.queryByRole('button')).toBeNull()
  expect(view.getAllByRole('combobox').every(control => control.hasAttribute('disabled'))).toBe(true)
})
