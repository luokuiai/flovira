// @vitest-environment jsdom
import { createRef } from 'react'
import { cleanup, fireEvent, render } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition, getNodeControlConfig, getRejectTargetCandidates, insertNodeAfter, normalizeDefinition, setNodeControlConfig, validateDefinition } from './model'
import type { ReactFlowDesignerRef } from './types'
afterEach(cleanup)

test('configures node permissions and return/resubmit strategies with round trip persistence', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByLabelText('允许转办'))
  fireEvent.click(view.getByLabelText('允许加签'))
  fireEvent.click(view.getByLabelText('允许减签'))
  fireEvent.click(view.getByRole('radio', { name: '退回发起人' }))
  fireEvent.click(view.getByRole('radio', { name: '回到执行退回的节点继续' }))
  const restored = normalizeDefinition(ref.current!.getFlowJson()).nodeList.find((node) => node.nodeType === '1')!
  expect(getNodeControlConfig(restored)).toMatchObject({ allowRollback: true, allowTransfer: true, allowAddSign: true, allowMinusSign: true,
    rejectStrategy: 'TO_DRAFT', resubmitStrategy: 'CONTINUE_FROM_REJECTED_NODE' })
  fireEvent.click(view.getByLabelText('允许退回'))
  expect(view.queryByRole('radiogroup', { name: '退回策略' })).toBeNull()
  expect(view.queryByRole('radiogroup', { name: '退回后重新提交' })).toBeNull()
  fireEvent.click(view.getByLabelText('允许退回'))
  expect((view.getByRole('radio', { name: '退回发起人' }) as HTMLInputElement).checked).toBe(true)
  expect((view.getByRole('radio', { name: '回到执行退回的节点继续' }) as HTMLInputElement).checked).toBe(true)
})

test('restricts fixed return targets to upstream approval nodes and catches removed targets', () => {
  const initial = createInitialDefinition()
  const previous = initial.nodeList.find((node) => node.nodeType === '1')!
  const definition = insertNodeAfter(initial, previous.nodeCode, '1')
  const current = definition.nodeList.find((node) => !initial.nodeList.some((item) => item.nodeCode === node.nodeCode))!
  expect(getRejectTargetCandidates(definition, current.nodeCode).map((node) => node.nodeCode)).toEqual([previous.nodeCode])
  expect(getRejectTargetCandidates(definition, previous.nodeCode)).toEqual([])
  definition.nodeList = definition.nodeList.map((node) => node.nodeCode === current.nodeCode
    ? setNodeControlConfig(node, { rejectStrategy: 'TO_SPECIFIED_NODE', rejectTargetNodeCode: previous.nodeCode }) : node)
  expect(validateDefinition(definition).issues.some((issue) => issue.code === 'REJECT_TARGET_INVALID')).toBe(false)
  definition.nodeList = definition.nodeList.filter((node) => node.nodeCode !== previous.nodeCode)
  expect(validateDefinition(definition).issues.some((issue) => issue.code === 'REJECT_TARGET_INVALID')).toBe(true)
  definition.nodeList = definition.nodeList.map((node) => setNodeControlConfig(node, { allowRollback: false }))
  expect(validateDefinition(definition).issues.some((issue) => issue.code === 'REJECT_TARGET_INVALID')).toBe(false)
})

test('requires a fixed return target before saving and preserves old policies', () => {
  const view = render(<ReactFlowDesigner onSave={async () => {}} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('radio', { name: '退回指定节点' }))
  expect(view.getByRole('alert').textContent).toContain('没有可选')
  expect((view.getByRole('button', { name: '保存' }) as HTMLButtonElement).disabled).toBe(true)
  const node = createInitialDefinition().nodeList[1]
  expect(getNodeControlConfig({ ...node, returnPolicy: 'ANY' }).rejectStrategy).toBe('TO_REJECTOR_SPECIFIED_NODE')
  expect(getNodeControlConfig({ ...node, returnPolicy: 'REJECT' }).rejectStrategy).toBe('REJECT')
  const saved = setNodeControlConfig({ ...node, returnPolicy: 'ANY', ext: '[{"code":"custom","value":"kept"}]' }, { allowTransfer: true })
  expect(saved.returnPolicy).toBe('ANY')
  expect(String(saved.ext)).toContain('kept')
})
