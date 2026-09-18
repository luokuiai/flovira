// @vitest-environment jsdom
import { createRef } from 'react'
import { cleanup, fireEvent, render, within } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import type { ReactFlowDesignerRef } from './types'
import { lifecycleValue, parseLifecycle, validateLifecycle, lifecyclePoints, withLifecycle } from './lifecycle'
import { normalizeDefinition } from './model'
afterEach(cleanup)

test('saves node callbacks through the draft and restricts before hooks to transactions', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const view = render(<ReactFlowDesigner ref={ref} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  fireEvent.click(view.getByRole('tab', { name: '回调' }))
  const node = within(view.getByRole('region', { name: '节点回调' }))
  fireEvent.click(node.getByRole('button', { name: '添加回调' }))
  fireEvent.change(node.getByLabelText('监听器 Bean 名 1'), { target: { value: 'businessListener' } })
  expect(node.queryByRole('option', { name: '提交后' })).toBeNull()
  fireEvent.change(node.getByLabelText('回调时机 1'), { target: { value: 'NODE_LEFT' } })
  fireEvent.change(node.getByLabelText('执行阶段 1'), { target: { value: 'AFTER_COMMIT' } })
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  const saved = normalizeDefinition(ref.current!.getFlowJson()).nodeList.find(item => item.nodeType === '1')!
  expect(parseLifecycle(lifecycleValue(saved.ext)).subscriptions).toEqual([
    { code: 'businessListener', point: 'NODE_LEFT', phase: 'AFTER_COMMIT', order: 0 },
  ])
})

test('round trips opaque parameters and rejects invalid scopes, phases and legacy shape', () => {
  const config = parseLifecycle('{"schemaVersion":1,"subscriptions":[{"code":"bean","point":"NODE_ENTERED","phase":"IN_TRANSACTION","order":2,"parameters":"a,b"}]}')
  const ext = withLifecycle('[{"code":"custom","value":"kept"}]', config)
  expect(parseLifecycle(lifecycleValue(ext))).toEqual(config)
  expect(ext).toContain('kept')
  expect(lifecyclePoints('3')).toEqual([])
  expect(lifecyclePoints('7').some(([point]) => point === 'APPROVAL_ACTION_COMPLETED')).toBe(false)
  expect(() => validateLifecycle(config, '3')).toThrow()
  expect(() => parseLifecycle('{"schemaVersion":2,"subscriptions":[]}')).toThrow()
  config.subscriptions[0].point = 'BEFORE_OPERATION'
  config.subscriptions[0].phase = 'AFTER_COMMIT'
  expect(() => validateLifecycle(config)).toThrow()
})
