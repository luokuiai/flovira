// @vitest-environment jsdom
import { createRef } from 'react'
import { cleanup, fireEvent, render } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import type { ReactFlowDesignerRef } from './types'
import { createInitialDefinition, normalizeDefinition, validateDefinition, setNodeControlConfig, getNodeControlConfig } from './model'
afterEach(cleanup)

test('node editing has no callback selection and preserves business JSON objects', () => {
  const ref = createRef<ReactFlowDesignerRef>()
  const definition = createInitialDefinition()
  definition.ext = JSON.stringify({ business: { tag: 'flow' } })
  definition.nodeList[1].ext = JSON.stringify({ business: { tags: ['a,b', 'c'], enabled: false } })
  const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} />)
  fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
  expect(view.queryByRole('tab', { name: '回调' })).toBeNull()
  expect(view.queryByLabelText(/监听器 Bean 名/)).toBeNull()
  fireEvent.click(view.getByRole('button', { name: '确定' }))
  const saved = normalizeDefinition(ref.current!.getFlowJson())
  expect(saved.ext).toBe(definition.ext)
  expect(JSON.parse(String(saved.nodeList[1].ext))).toEqual(JSON.parse(String(definition.nodeList[1].ext)))
})

test('configuration edits preserve nested host data without array conversion', () => {
  const node = createInitialDefinition().nodeList[1]
  node.ext = JSON.stringify({ business: { tags: ['a,b', 'c'], count: 0 } })
  const saved = setNodeControlConfig(node, { allowTransfer: true })
  expect(getNodeControlConfig(saved).allowTransfer).toBe(true)
  expect(JSON.parse(String(saved.ext)).business).toEqual({ tags: ['a,b', 'c'], count: 0 })
  expect(() => setNodeControlConfig({ ...node, ext: '[]' }, {})).toThrow('JSON 对象')
})

test('definition and node validation reject removed formLoad callbacks', () => {
  for (const scope of ['definition', 'node']) {
    const definition = createInitialDefinition()
    Object.assign(scope === 'definition' ? definition : definition.nodeList[1], {
      listenerType: 'formLoad', listenerPath: 'oldFormLoader',
    })
    expect(validateDefinition(definition).issues.some(issue => issue.code === 'LIFECYCLE_INVALID')).toBe(true)
  }
})
