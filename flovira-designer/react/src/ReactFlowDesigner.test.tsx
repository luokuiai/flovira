// @vitest-environment jsdom

import { createRef, useState } from 'react'
import { act, cleanup, fireEvent, render, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, test } from 'vitest'
import { ReactFlowDesigner } from './ReactFlowDesigner'
import { createInitialDefinition, getApproverRule, insertNodeAfter, setApproverRule } from './model'
import type { DesignerInputProps, DesignerTooltipProps, ReactFlowDesignerRef } from './types'

afterEach(cleanup)

describe('ReactFlowDesigner', () => {
  test('shows multi approval only for multiple distinct specified people', () => {
    for (const ids of [[], ['a'], ['a', 'b']]) {
      const definition = createInitialDefinition()
      definition.nodeList = definition.nodeList.map((node) => node.nodeType === '1'
        ? setApproverRule(node, 'USER', ids.map((id) => ({ id, type: 'USER' }))) : node)
      const view = render(<ReactFlowDesigner defaultValue={definition} />)
      fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
      expect(Boolean(view.queryByRole('radiogroup', { name: '多人审批策略' }))).toBe(new Set(ids).size > 1)
      view.unmount()
    }
  })

  test('configures vote ratios and synchronizes runtime approval modes', () => {
    const ref = createRef<ReactFlowDesignerRef>()
    const definition = createInitialDefinition()
    definition.nodeList = definition.nodeList.map((node) => node.nodeType === '1' ? setApproverRule(node, 'USER', [{ id: 'a', type: 'USER' }, { id: 'b', type: 'USER' }]) : node)
    const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} onSave={async () => {}} />)
    fireEvent.click(view.getByRole('button', { name: '编辑节点：审批节点' }))
    fireEvent.click(view.getByRole('radio', { name: '票签' }))
    const ratio = view.getByLabelText('通过比例（%）') as HTMLInputElement
    const currentNode = () => ref.current!.getDefinition().nodeList.find((node) => node.nodeType === '1')!
    expect(ratio.value).toBe('60')
    fireEvent.change(ratio, { target: { value: '75' } })
    expect(JSON.parse(ref.current!.getFlowJson()).nodeList.find((node: { nodeType: string }) => node.nodeType === '1').nodeRatio).toBe('75')
    fireEvent.change(ratio, { target: { value: '' } })
    expect(ref.current!.validate().issues.some((issue) => issue.code === 'VOTE_RATIO_INVALID')).toBe(true)
    expect((view.getByRole('button', { name: '保存' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.change(ratio, { target: { value: '100' } })
    expect(view.getByRole('alert').textContent).toContain('小于 100')
    fireEvent.change(ratio, { target: { value: '60' } })
    expect(ref.current!.validate().issues.some((issue) => issue.code === 'VOTE_RATIO_INVALID')).toBe(false)
    fireEvent.click(view.getByRole('radio', { name: '会签' }))
    expect(currentNode().nodeRatio).toBe('100')
    expect(view.queryByLabelText('通过比例（%）')).toBeNull()
    fireEvent.click(view.getByRole('radio', { name: '或签' }))
    expect(currentNode().nodeRatio).toBe('0')
  })

  test('keeps subprocess providers isolated per designer instance', async () => {
    const initial = createInitialDefinition()
    const approval = initial.nodeList.find((node) => node.nodeType === '1')!
    const definition = insertNodeAfter(initial, approval.nodeCode, '6')
    const subprocess = definition.nodeList.find((node) => node.nodeType === '6')!

    const first = render(
      <ReactFlowDesigner
        defaultValue={definition}
        queryResources={async () => ({ items: [{ id: 'flow_a', code: 'flow_a', name: '租户 A 子流程', resourceType: 'SUBPROCESS' }], total: 1 })}
      />,
    )
    const second = render(
      <ReactFlowDesigner
        defaultValue={definition}
        queryResources={async () => ({ items: [{ id: 'flow_b', code: 'flow_b', name: '租户 B 子流程', resourceType: 'SUBPROCESS' }], total: 1 })}
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

  test('uses an injected UI adapter without changing designer behavior', () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const designerRef = createRef<ReactFlowDesignerRef>()
    const AdapterInput = ({ value, disabled, ariaLabel, onValueChange }: DesignerInputProps) => (
      <input
        data-adapter="custom"
        value={value}
        disabled={disabled}
        aria-label={ariaLabel}
        onChange={(event) => onValueChange(event.target.value)}
      />
    )
    const AdapterTooltip = ({ content, children }: DesignerTooltipProps) => (
      <span data-adapter-tooltip={String(content)}>{children}</span>
    )
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        ui={{ Input: AdapterInput, Tooltip: AdapterTooltip }}
      />,
    )
    const current = within(view.container)

    expect(current.getByLabelText('撤销').closest('[data-adapter-tooltip]')?.getAttribute('data-adapter-tooltip'))
      .toBe('撤销')
    fireEvent.click(current.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    const nameInput = current.getByLabelText('节点名称')
    expect(nameInput.getAttribute('data-adapter')).toBe('custom')
    fireEvent.change(nameInput, { target: { value: '适配器审批' } })

    expect(designerRef.current?.getDefinition().nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName)
      .toBe('适配器审批')
  })

  test('opens node settings in a closable drawer', () => {
    const definition = createInitialDefinition()
    definition.nodeList = definition.nodeList.map((node) => node.nodeType === '1' ? setApproverRule(node, 'USER', [{ id: 'a', type: 'USER' }, { id: 'b', type: 'USER' }]) : node)
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const view = render(<ReactFlowDesigner defaultValue={definition} />)

    expect(view.queryByRole('dialog', { name: '节点设置' })).toBeNull()
    fireEvent.click(view.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    expect(view.getByRole('dialog', { name: '节点设置' })).toBeTruthy()
    expect(view.getByRole('radio', { name: '或签' })).toBeTruthy()
    const countersign = view.getByRole('radio', { name: '会签' }) as HTMLInputElement
    fireEvent.click(countersign)
    expect(countersign.checked).toBe(true)
    expect(view.getByRole('radio', { name: '退回上一节点' })).toBeTruthy()
    expect(view.getByRole('radio', { name: '退回时指定节点' })).toBeTruthy()

    const closeButtons = view.getAllByRole('button', { name: '关闭节点设置' })
    fireEvent.click(closeButtons[closeButtons.length - 1])
    expect(view.queryByRole('dialog', { name: '节点设置' })).toBeNull()
  })

  test('deletes editable nodes from the card header instead of the drawer', () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const view = render(<ReactFlowDesigner defaultValue={definition} />)

    expect(view.queryByRole('button', { name: '删除节点：开始' })).toBeNull()
    expect(view.queryByRole('button', { name: '删除节点：结束' })).toBeNull()
    fireEvent.click(view.getByRole('button', { name: `删除节点：${approval.nodeName}` }))

    expect(view.queryByRole('button', { name: `编辑节点：${approval.nodeName}` })).toBeNull()
    expect(view.queryByRole('dialog', { name: '节点设置' })).toBeNull()
  })

  test('renders arbitrary approver strategies returned by the backend', async () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const designerRef = createRef<ReactFlowDesignerRef>()
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        capabilities={{
            schemaVersion: 1,
            nodeTypes: ['0', '1', '2', '3', '4', '5', '6', '7', '8'],
            approverStrategies: [{
              code: 'CUSTOM_MANAGER_CHAIN',
              name: '自定义负责人链',
              selectionType: 'RELATION',
              relationType: 'CUSTOM_MANAGER_CHAIN',
              multiple: false,
              editorType: 'NONE',
              resultCardinality: 'ZERO_OR_ONE',
              options: [{
                code: 'emptyPolicy',
                name: '无人审批策略',
                defaultValue: 'FAIL',
                choices: [
                  { value: 'FAIL', label: '阻止提交' },
                  { value: 'TO_ADMIN', label: '转交管理员' },
                ],
              }],
            }],
            approvalModes: ['OR'],
            returnPolicies: ['PREVIOUS'],
            timeoutNodeTypes: ['1'],
            operations: ['SAVE'],
            resourceTypes: [],
        }}
      />,
    )

    fireEvent.click(view.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))

    await waitFor(() => expect(view.getByRole('option', { name: '自定义负责人链' })).toBeTruthy())
    expect(view.queryByText('运行时由后端“自定义负责人链”人员解析器确定办理人')).toBeNull()
    expect(view.queryByRole('button', { name: '选择自定义负责人链' })).toBeNull()
    fireEvent.click(view.getByRole('radio', { name: '转交管理员' }))
    const current = designerRef.current?.getDefinition().nodeList
      .find((node) => node.nodeCode === approval.nodeCode)
    expect(current && getApproverRule(current).config).toEqual({ emptyPolicy: 'TO_ADMIN' })
  })

  test('renders an inline business editor and persists custom rule config', () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const designerRef = createRef<ReactFlowDesignerRef>()
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        capabilities={{
          schemaVersion: 1,
          nodeTypes: ['0', '1', '2'],
          approverStrategies: [{
            code: 'FORM_RULE',
            name: '表单规则',
            selectionType: 'RELATION',
            relationType: 'FORM_RULE',
            multiple: false,
            editorType: 'INLINE',
            editorKey: 'form-rule-editor',
          }],
          approvalModes: ['OR'],
          returnPolicies: ['PREVIOUS'],
          timeoutNodeTypes: [],
          operations: ['SAVE'],
          resourceTypes: [],
        }}
        renderApproverEditor={({ strategy, rule, onRuleChange }) => strategy.editorKey === 'form-rule-editor' ? (
          <button type="button" onClick={() => onRuleChange({ ...rule, config: { field: 'ownerId' } })}>
            配置表单规则
          </button>
        ) : null}
      />,
    )

    fireEvent.click(view.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    fireEvent.click(view.getByRole('button', { name: '配置表单规则' }))

    const current = designerRef.current?.getDefinition().nodeList
      .find((node) => node.nodeCode === approval.nodeCode)
    expect(current && getApproverRule(current)).toMatchObject({
      strategy: 'FORM_RULE',
      selectionType: 'RELATION',
      relationType: 'FORM_RULE',
      config: { field: 'ownerId' },
    })
    expect(view.queryByRole('dialog', { name: '人员选择' })).toBeNull()
  })

  test('shows strategy options only on their configured node types', () => {
    const initial = createInitialDefinition()
    initial.nodeList = initial.nodeList.map((node) => node.nodeType === '1' ? setApproverRule(node, 'USER', [{ id: 'a', type: 'USER' }, { id: 'b', type: 'USER' }]) : node)
    const approval = initial.nodeList.find((node) => node.nodeType === '1')!
    const definition = insertNodeAfter(initial, approval.nodeCode, '8')
    const carbonCopy = definition.nodeList.find((node) => node.nodeType === '8')!
    const view = render(<ReactFlowDesigner defaultValue={definition} />)

    fireEvent.click(view.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    expect(view.getByRole('radiogroup', { name: '多人审批策略' })).toBeTruthy()
    expect(view.getByRole('radiogroup', { name: '审批人与提交人为同一人时' })).toBeTruthy()
    const closeButtons = view.getAllByRole('button', { name: '关闭节点设置' })
    fireEvent.click(closeButtons[closeButtons.length - 1])
    fireEvent.click(view.getByRole('button', { name: `编辑节点：${carbonCopy.nodeName}` }))
    expect(view.queryByRole('radiogroup', { name: '多人审批策略' })).toBeNull()
    expect(view.queryByRole('radiogroup', { name: '审批人与提交人为同一人时' })).toBeNull()
  })

  test('hides multiple and empty policies for one concrete person', () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const policyOptions = [
      {
        code: 'approvalMode',
        name: '多人审批策略',
        condition: 'MULTIPLE' as const,
        choices: [{ value: 'OR', label: '任意一人通过' }],
      },
      {
        code: 'emptyPolicy',
        name: '无人审批策略',
        condition: 'EMPTY' as const,
        choices: [{ value: 'FAIL', label: '阻止提交' }],
      },
    ]
    const view = render(<ReactFlowDesigner defaultValue={definition} capabilities={{
      schemaVersion: 1,
      nodeTypes: ['0', '1', '2'],
      approverStrategies: [{
        code: 'USER',
        name: '指定人员',
        selectionType: 'RESOURCE',
        resourceType: 'USER',
        multiple: false,
        editorType: 'DIALOG',
        resultCardinality: 'EXACTLY_ONE',
        options: policyOptions,
      }],
      approvalModes: ['OR'],
      returnPolicies: ['PREVIOUS'],
      timeoutNodeTypes: [],
      operations: ['SAVE'],
      resourceTypes: ['USER'],
    }} />)

    fireEvent.click(view.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    expect(view.getByRole('button', { name: '选择指定人员' })).toBeTruthy()
    expect(view.queryByRole('radiogroup', { name: '多人审批策略' })).toBeNull()
    expect(view.queryByRole('radiogroup', { name: '无人审批策略' })).toBeNull()
  })

  test('delegates participant selection content to the host application', () => {
    const definition = createInitialDefinition()
    const approval = definition.nodeList.find((node) => node.nodeType === '1')!
    const designerRef = createRef<ReactFlowDesignerRef>()
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        renderApproverEditor={({ onChange }) => (
          <button
            type="button"
            onClick={() => onChange([{ id: 'u-100', type: 'USER', name: '组织架构用户' }])}
          >
            从组织架构选择
          </button>
        )}
      />,
    )

    fireEvent.click(view.getByRole('button', { name: `编辑节点：${approval.nodeName}` }))
    expect(view.queryByText('搜索用户')).toBeNull()
    fireEvent.click(view.getByRole('button', { name: '选择用户' }))
    expect(view.getByRole('dialog', { name: '人员选择' })).toBeTruthy()
    fireEvent.click(view.getByRole('button', { name: '从组织架构选择' }))
    fireEvent.click(view.getByRole('button', { name: '确定' }))

    const current = designerRef.current?.getDefinition().nodeList
      .find((node) => node.nodeCode === approval.nodeCode)
    expect(current && getApproverRule(current).subjects).toEqual([
      { id: 'u-100', type: 'USER', name: '组织架构用户' },
    ])
    expect(view.getAllByText('组织架构用户')).toHaveLength(2)
  })

  test('renders branches into one directional merge path', () => {
    const initial = createInitialDefinition()
    const start = initial.nodeList.find((node) => node.nodeType === '0')!
    const definition = insertNodeAfter(initial, start.nodeCode, '3')
    const view = render(<ReactFlowDesigner defaultValue={definition} />)

    expect(view.container.querySelectorAll('.frd-branch__name')).toHaveLength(0)
    expect(view.container.querySelectorAll('.frd-branch__merge-tail')).toHaveLength(2)
    expect(view.container.querySelectorAll('.frd-merge-flow')).toHaveLength(1)
    expect(view.container.querySelectorAll('.frd-connector--fork')).toHaveLength(1)
    expect(view.container.querySelectorAll('.flovira-react-branch-grid')).toHaveLength(1)
    expect(view.queryByText(/汇合至/)).toBeNull()
    expect(view.getAllByRole('button', { name: '编辑节点：审批节点' })).toHaveLength(1)
  })

  test.each(['1', '3', '4', '5'] as const)('inserts type %s inside a branch without duplicating its continuation', (type) => {
    const initial = createInitialDefinition()
    const start = initial.nodeList.find((node) => node.nodeType === '0')!
    const definition = insertNodeAfter(initial, start.nodeCode, '3')
    const branch = definition.nodeList.find((node) => node.nodeName === '分支一')!
    const sibling = definition.nodeList.find((node) => node.nodeName === '分支二')!
    const continuation = initial.nodeList.find((node) => node.nodeType === '1')!
    continuation.nodeName = '共同审批'
    definition.nodeList.find((node) => node.nodeCode === continuation.nodeCode)!.nodeName = continuation.nodeName
    const ref = createRef<ReactFlowDesignerRef>()
    const view = render(<ReactFlowDesigner ref={ref} defaultValue={definition} />)
    expect(view.container.querySelector('.frd-node__type')).toBeNull()
    fireEvent.click(view.getByRole('button', { name: '在 分支一 后添加节点' }))
    const labels = { '1': '审批节点', '3': '条件分支', '4': '并行分支', '5': '多选分支' }
    fireEvent.click(view.getByRole('menuitem', { name: `添加${labels[type]}` }))
    const updated = ref.current!.getDefinition()
    const insertedCode = updated.nodeList.find((node) => node.nodeCode === branch.nodeCode)!.skipList[0].targetNodeCode
    expect(updated.nodeList.find((node) => node.nodeCode === insertedCode)!.nodeType).toBe(type)
    expect(updated.nodeList.find((node) => node.nodeCode === sibling.nodeCode)!.skipList).toEqual(sibling.skipList)
    expect(view.getAllByRole('button', { name: '编辑节点：共同审批' })).toHaveLength(1)
    expect(view.getAllByRole('button', { name: '编辑节点：结束' })).toHaveLength(1)
    expect(view.container.querySelectorAll('.frd-branch .frd-insert-point').length).toBeGreaterThanOrEqual(2)
    act(() => ref.current!.undo())
    expect(ref.current!.getDefinition().nodeList).toHaveLength(definition.nodeList.length)
    act(() => ref.current!.redo())
    expect(ref.current!.getDefinition().nodeList).toHaveLength(updated.nodeList.length)
  })

  test('shows named and colored node options in the insert dropdown', () => {
    const view = render(<ReactFlowDesigner defaultValue={createInitialDefinition()} />)

    fireEvent.click(view.getAllByRole('button', { name: /后添加节点/ })[0])
    const approvalItem = view.getByRole('menuitem', { name: '添加审批节点' })
    expect(approvalItem.querySelector('.frd-dropdown-menu__icon')?.getAttribute('style'))
      .toContain('color')
    expect(view.getByRole('menuitem', { name: '添加条件分支' })).toBeTruthy()
    expect(view.getByRole('menuitem', { name: '添加等待节点' })).toBeTruthy()

    fireEvent.click(approvalItem)
    expect(view.getAllByRole('button', { name: '编辑节点：审批节点' })).toHaveLength(2)
    expect(view.queryByRole('menu')).toBeNull()
  })
})
