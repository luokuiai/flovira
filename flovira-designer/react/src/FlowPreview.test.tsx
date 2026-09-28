// @vitest-environment jsdom

import { cleanup, fireEvent, render } from '@testing-library/react'
import { afterEach, describe, expect, test } from 'vitest'
import { FlowPreview } from './FlowPreview'
import { parseWorkflowPackage, getPackageDefinition, getPackageForm, parsePackageFormContent } from './workflowPackage'
import packageFixture from '../../../flovira-orm/src/contractTest/resources/workflow-package.json'
import { layoutPreview, PREVIEW_NODE_HEIGHT } from './previewLayout'
import type { DesignerTooltipProps, FloviraDefinition, FloviraNode, FloviraNodeType } from './types'

afterEach(cleanup)

const node = (code: string, targets: string[] = [], type: FloviraNodeType = '1'): FloviraNode => ({
  nodeCode: code,
  nodeName: code,
  nodeType: type,
  skipList: targets.map((target) => ({ sourceNodeCode: code, targetNodeCode: target, skipType: 'PASS' })),
})

const parallel: FloviraDefinition = {
  flowName: '报销进度',
  nodeList: [
    node('开始', ['并行审批'], '0'),
    node('并行审批', ['主管审批', '财务审批'], '4'),
    node('主管审批', ['归档']),
    node('财务审批', ['发票复核']),
    node('发票复核', ['归档']),
    node('归档', [], '2'),
  ],
}

describe('FlowPreview', () => {
  test('displays root and child designs from a package before backend import', () => {
    const bundle = parseWorkflowPackage(packageFixture)
    const form = getPackageForm(bundle, 'source-form-1')!
    const view = render(<><FlowPreview value={getPackageDefinition(bundle)} />
      <pre>{JSON.stringify(parsePackageFormContent(form))}</pre></>)
    expect(view.container.querySelectorAll('[data-node-code]')).toHaveLength(3)
    expect(view.getByText(/Amount/)).toBeTruthy()
    view.rerender(<FlowPreview value={getPackageDefinition(bundle, 'package_child')} />)
    expect(view.container.querySelectorAll('[data-node-code]')).toHaveLength(2)
    expect(view.getAllByLabelText('child_start')).toHaveLength(1)
  })

  test('distinguishes pending, current and completed nodes using explicit instance state', () => {
    const view = render(<FlowPreview value={parallel} currentNodeCodes={['财务审批']}
      completedNodeCodes={['开始', '并行审批', '主管审批', '财务审批']} />)
    const status = (code: string) => view.container.querySelector(`[data-node-code="${code}"] .frp-node`)?.getAttribute('data-status')
    expect(status('开始')).toBe('completed')
    expect(status('财务审批')).toBe('current')
    expect(status('归档')).toBe('pending')
    expect(view.getByLabelText('财务审批').textContent).toContain('审批中')
    expect(view.getByLabelText('主管审批').textContent).toContain('已审批')
    expect(view.getByLabelText('发票复核').textContent).toContain('待审批')
    expect(view.queryByRole('tooltip')).toBeNull()
    view.rerender(<FlowPreview value={parallel} completedNodeCodes={[]} />)
    expect(status('开始')).toBe('pending')
    view.rerender(<FlowPreview value={parallel} />)
    expect(status('开始')).toBe('default')
  })

  test('marks branches outside the current route as skipped instead of pending', () => {
    const definition: FloviraDefinition = { nodeList: [
      node('start', ['gateway'], '0'), node('gateway', ['selected', 'unused'], '3'),
      node('selected', ['end']), node('unused', ['end']), node('end', [], '2'),
    ] }
    const view = render(<FlowPreview value={definition} currentNodeCodes={['selected']}
      completedNodeCodes={['start', 'gateway']} nodeHandlers={{ unused: ['不应显示'] }} />)
    const status = (code: string) => view.container.querySelector(`[data-node-code="${code}"] .frp-node`)?.getAttribute('data-status')
    expect(status('selected')).toBe('current')
    expect(status('unused')).toBe('skipped')
    expect(view.getByLabelText('unused').textContent).toContain('未经过')
    expect(view.getByLabelText('unused').querySelector('.frp-node-status')).toBeNull()
    expect(view.getByLabelText('unused').textContent).not.toContain('不应显示')
    expect(status('end')).toBe('pending')
  })

  test('renders designer cards with icons and all forward edges, merging unequal branches once', () => {
    const before = JSON.stringify(parallel)
    const view = render(<FlowPreview value={parallel} />)
    expect(view.container.querySelectorAll('[data-node-code]')).toHaveLength(6)
    expect(view.container.querySelectorAll('.frd-node__icon svg')).toHaveLength(6)
    expect(view.container.querySelectorAll('[data-edge-source]')).toHaveLength(6)
    expect(view.getAllByLabelText('归档')).toHaveLength(1)
    expect(view.getByLabelText('流程图画布').style.height).toBe(`${layoutPreview(parallel).height}px`)
    expect(view.queryByRole('button', { name: /保存|删除|添加|撤销|编辑/ })).toBeNull()
    expect(JSON.stringify(parallel)).toBe(before)
    const layout = layoutPreview(parallel)
    const merge = layout.nodes.find((item) => item.node.nodeCode === '归档')!
    const review = layout.nodes.find((item) => item.node.nodeCode === '发票复核')!
    const split = layout.nodes.find((item) => item.node.nodeCode === '并行审批')!
    const manager = layout.nodes.find((item) => item.node.nodeCode === '主管审批')!
    expect(manager.y - split.y - PREVIEW_NODE_HEIGHT).toBe(52)
    expect(layout.edges.find((edge) => edge.source === '并行审批')!.path)
      .toContain(`V ${split.y + PREVIEW_NODE_HEIGHT + 20}`)
    expect(merge.y).toBeGreaterThan(review.y + PREVIEW_NODE_HEIGHT)
  })

  test('reserves straight columns for short branches while expanding nested branches on both sides', () => {
    const definition: FloviraDefinition = { nodeList: [
      node('split', ['short', 'nested', 'long'], '3'),
      node('short', ['merge']), node('nested', ['a', 'b', 'c'], '3'),
      node('long', ['review']), node('a', ['merge']), node('b', ['merge']),
      node('c', ['merge']), node('review', ['merge']), node('merge', [], '2'),
    ] }
    const layout = layoutPreview(definition)
    const position = (code: string) => layout.nodes.find((item) => item.node.nodeCode === code)!
    expect(position('short').x + position('short').width).toBeLessThan(position('a').x)
    expect(position('long').x).toBe(position('review').x)
    expect(position('short').x).toBeLessThan(position('split').x)
    expect(position('long').x).toBeGreaterThan(position('split').x)
    expect(position('split').x).toBe((position('short').x + position('long').x) / 2)
    expect(layout.nodes).toHaveLength(definition.nodeList.length)
    const shortRoute = layout.edges.find((edge) => edge.source === 'short')!.path
    const firstHorizontal = Number(shortRoute.match(/ H ([\d.]+)/)![1])
    expect(firstHorizontal).toBe(position('short').x + position('short').width / 2)
  })

  test('updates parallel current nodes and actual handlers without inferring completed nodes', () => {
    const view = render(<FlowPreview value={parallel} currentNodeCodes={['主管审批', '财务审批']} nodeHandlers={{ 开始: ['王小明'], 主管审批: ['张三', '李四'] }} />)
    expect(view.container.querySelectorAll('[aria-current="step"]')).toHaveLength(2)
    expect(view.getByLabelText('开始').textContent).toContain('王小明')
    expect(view.getByLabelText('主管审批').textContent).toContain('张三、李四')
    expect(view.container.textContent).not.toMatch(/发起人|办理人|审批人/)
    expect(view.queryByRole('tooltip')).toBeNull()

    view.rerender(<FlowPreview value={parallel} currentNodeCodes={['发票复核']} />)
    expect(view.container.querySelectorAll('[aria-current="step"]')).toHaveLength(1)
    expect(view.container.querySelector('[aria-current="step"]')?.getAttribute('aria-label')).toBe('发票复核')
    expect(view.container.querySelector('[data-node-code="开始"] [aria-current]')).toBeNull()
  })

  test('enables the UI adapter tooltip only when handler names are truncated', () => {
    const AdapterTooltip = ({ content, children, disabled }: DesignerTooltipProps) => <span>{children}{!disabled && <span role="tooltip">{content}</span>}</span>
    const view = render(<FlowPreview value={parallel} currentNodeCodes={['主管审批']} nodeHandlers={{ 主管审批: ['<script>name</script>'] }}
      ui={{ Tooltip: AdapterTooltip }} />)
    const names = view.getByText('<script>name</script>')
    expect(view.queryByRole('tooltip')).toBeNull()
    Object.defineProperty(names, 'scrollWidth', { configurable: true, value: 200 })
    Object.defineProperty(names, 'clientWidth', { configurable: true, value: 100 })
    fireEvent(window, new Event('resize'))
    expect(view.getByRole('tooltip').textContent).toBe('<script>name</script>')
    expect(view.getByRole('tooltip').querySelector('script')).toBeNull()
  })

  test('does not mistake configured participants for actual handlers', () => {
    const definition = { nodeList: [{ ...node('审批'), permissionFlag: 'role:admin' }, node('子流程', [], '6')] }
    const view = render(<FlowPreview value={definition} currentNodeCodes={['审批']}
      nodeHandlers={{ 子流程: ['不应显示'] }} />)
    expect(view.getByLabelText('审批').textContent).toContain('暂未提供')
    expect(view.getByLabelText('审批').textContent).not.toContain('admin')
    expect(view.getByLabelText('子流程').textContent).not.toContain('不应显示')
  })

  test('accepts custom tooltip content, icons and the existing UI adapter contract', () => {
    const view = render(<FlowPreview value={{ nodeList: [node('审批')] }}
      ui={{ Tooltip: ({ content, children }) => <div>{children}<aside>{content}</aside></div> }}
      renderTooltip={({ node, current }) => <strong>{node.nodeName}：{current ? '办理中' : '未高亮'}</strong>}
      renderNodeIcon={() => <svg aria-label="业务图标" />} />)
    expect(view.getByText('审批：未高亮')).toBeTruthy()
    expect(view.getByLabelText('业务图标')).toBeTruthy()
  })

  test('shows an empty state and replaces JSON input without generating a sample flow', () => {
    const view = render(<FlowPreview />)
    expect(view.getByRole('status').textContent).toBe('暂无流程数据')
    view.rerender(<FlowPreview value={JSON.stringify(parallel)} />)
    expect(view.getByLabelText('主管审批')).toBeTruthy()
    view.rerender(<FlowPreview value={{ nodeList: [] }} />)
    expect(view.queryByLabelText('主管审批')).toBeNull()
    expect(view.getByRole('status')).toBeTruthy()
  })

  test('reports malformed data, missing targets, duplicate codes and forward cycles', () => {
    const view = render(<FlowPreview value="invalid json" />)
    expect(view.getByRole('alert').textContent).toContain('无法预览流程')
    view.rerender(<FlowPreview value='{"nodeList": "invalid"}' />)
    expect(view.getByRole('alert').textContent).toContain('nodeList 数组')
    view.rerender(<FlowPreview value={{ nodeList: [node('a', ['missing'])] }} />)
    expect(view.getByRole('alert').textContent).toContain('连线目标不存在')
    view.rerender(<FlowPreview value={{ nodeList: [node('a'), node('a')] }} />)
    expect(view.getByRole('alert').textContent).toContain('重复')
    view.rerender(<FlowPreview value={{ nodeList: [node('a', ['b']), node('b', ['a'])] }} />)
    expect(view.getByRole('alert').textContent).toContain('正向循环')
  })

  test('ignores return edges and keeps all nested fork and merge connections', () => {
    const definition: FloviraDefinition = {
      nodeList: [node('s', ['fork'], '0'), node('fork', ['a', 'b'], '4'),
        node('a', ['c', 'd'], '3'), node('c', ['m']), node('d', ['m']),
        node('m', ['end']), node('b', ['end']), node('end', [], '2')],
    }
    definition.nodeList[2].skipList.push({ sourceNodeCode: 'a', targetNodeCode: 's', skipType: 'REJECT' })
    const view = render(<FlowPreview value={definition} />)
    expect(view.container.querySelectorAll('[data-node-code]')).toHaveLength(8)
    expect(view.container.querySelectorAll('[data-edge-source]')).toHaveLength(9)
    expect(view.container.querySelector('[data-edge-source="a"][data-edge-target="s"]')).toBeNull()
  })

  test('uses independent arrow markers per preview and supports zoom/reset', () => {
    const view = render(<><FlowPreview value={parallel} /><FlowPreview value={parallel} /></>)
    const markers = [...view.container.querySelectorAll('marker')].map((marker) => marker.id)
    expect(new Set(markers).size).toBe(2)
    const initialHeight = view.getAllByLabelText('流程图画布')[0].style.height
    fireEvent.click(view.getAllByRole('button', { name: '放大流程图' })[0])
    expect(view.getByText('110%')).toBeTruthy()
    expect(view.getAllByLabelText('流程图画布')[0].style.height).toBe(initialHeight)
    fireEvent.click(view.getAllByRole('button', { name: '自适应流程图' })[0])
    expect(view.queryByText('110%')).toBeNull()
  })
})
