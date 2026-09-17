// @vitest-environment jsdom
import { cleanup, fireEvent, render } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { TimeoutFormField } from './TimeoutFormField'
import { createInitialDefinition } from './model'
import { defaultDesignerUi } from './ui'

afterEach(cleanup)
test('offers named date fields, excluding other types and array items', async () => {
  const definition = createInitialDefinition()
  const onChange = vi.fn()
  const view = render(<TimeoutFormField definition={definition} node={definition.nodeList[1]}
    fields={[
      { code: 'deadline', label: '审批截止时间', dataType: 'datetime' },
      { code: 'schedule.day', label: '计划 / 日期', dataType: 'date' },
      { code: 'amount', label: '金额', dataType: 'number' },
      { code: 'items[].day', label: '明细日期', dataType: 'date' },
    ]} value="" label="" disabled={false} ui={defaultDesignerUi} onChange={onChange} />)
  expect(await view.findByRole('option', { name: '审批截止时间' })).toBeTruthy()
  expect(view.getByRole('option', { name: '计划 / 日期' })).toBeTruthy()
  expect(view.queryByRole('option', { name: '金额' })).toBeNull()
  expect(view.queryByRole('option', { name: '明细日期' })).toBeNull()
  fireEvent.change(view.getByLabelText('表单日期时间字段'), { target: { value: 'deadline' } })
  expect(onChange).toHaveBeenCalledWith('deadline', '审批截止时间')
})

test('reports a failed provider and supports retry', async () => {
  const definition = createInitialDefinition()
  const queryFields = vi.fn().mockRejectedValueOnce(new Error('加载失败')).mockResolvedValue([])
  const view = render(<TimeoutFormField definition={definition} node={definition.nodeList[1]}
    fields={[]} queryFields={queryFields} value="" label="" disabled={false}
    ui={defaultDesignerUi} onChange={() => {}} />)
  expect(await view.findByRole('alert')).toHaveProperty('textContent', '加载失败')
  fireEvent.click(view.getByRole('button', { name: '重新加载' }))
  expect(await view.findByText('暂无日期时间字段，请先在表单定义中添加。')).toBeTruthy()
})
