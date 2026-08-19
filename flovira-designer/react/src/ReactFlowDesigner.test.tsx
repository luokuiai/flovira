// @vitest-environment jsdom

import { createRef, useState } from 'react'
import { act, cleanup, fireEvent, render, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, test } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition, insertNodeAfter } from './model'
import type { ReactFlowDesignerRef } from './types'

afterEach(cleanup)

describe('ReactFlowDesigner', () => {
  test('keeps subprocess providers isolated per designer instance', async () => {
    const initial = createInitialDefinition()
    const approval = initial.nodeList.find((node) => node.nodeType === '1')!
    const definition = insertNodeAfter(initial, approval.nodeCode, '6')
    const subprocess = definition.nodeList.find((node) => node.nodeType === '6')!

    const first = render(
      <ReactFlowDesigner
        defaultValue={definition}
        dataProvider={{ queryResources: async () => ({ items: [{ id: 'flow_a', code: 'flow_a', name: '租户 A 子流程', resourceType: 'SUBPROCESS' }], total: 1 }) }}
      />,
    )
    const second = render(
      <ReactFlowDesigner
        defaultValue={definition}
        dataProvider={{ queryResources: async () => ({ items: [{ id: 'flow_b', code: 'flow_b', name: '租户 B 子流程', resourceType: 'SUBPROCESS' }], total: 1 }) }}
      />,
    )

    fireEvent.click(within(first.container).getByRole('button', { name: `编辑节点：${subprocess.nodeName}` }))
    fireEvent.click(within(second.container).getByRole('button', { name: `编辑节点：${subprocess.nodeName}` }))

    await waitFor(() => {
      expect(within(first.container).getByRole('option', { name: '租户 A 子流程' })).toBeTruthy()
      expect(within(second.container).getByRole('option', { name: '租户 B 子流程' })).toBeTruthy()
    })
    expect(within(first.container).queryByRole('option', { name: '租户 B 子流程' })).toBeNull()
    expect(within(second.container).queryByRole('option', { name: '租户 A 子流程' })).toBeNull()
  })

  test('restores an edited definition through the imperative undo API', () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const designerRef = createRef<ReactFlowDesignerRef>()
    const view = render(<ReactFlowDesigner ref={designerRef} defaultValue={definition} />)

    const current = within(view.container)
    fireEvent.click(current.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    fireEvent.change(current.getByLabelText('节点名称'), {
      target: { value: '部门负责人审批' },
    })
    expect(designerRef.current?.getDefinition().nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName)
      .toBe('部门负责人审批')

    act(() => designerRef.current?.undo())
    expect(designerRef.current?.getDefinition().nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName)
      .toBe('审批节点')
  })

  test('keeps undo history when a controlled host echoes onChange', () => {
    const initial = createInitialDefinition()
    const approval = initial.nodeList.find((node) => node.nodeType === '1')!
    const designerRef = createRef<ReactFlowDesignerRef>()
    const Controlled = () => {
      const [value, setValue] = useState(initial)
      return (
        <ReactFlowDesigner
          ref={designerRef}
          value={value}
          onChange={({ definition }) => setValue(definition)}
        />
      )
    }
    const view = render(<Controlled />)
    const current = within(view.container)

    fireEvent.click(current.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    fireEvent.change(current.getByLabelText('节点名称'), { target: { value: '受控审批' } })
    act(() => designerRef.current?.undo())

    expect(designerRef.current?.getDefinition().nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName)
      .toBe('审批节点')
  })
})
