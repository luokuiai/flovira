import type { FloviraDefinition, FloviraNode, FloviraSkip } from './types'

export const PREVIEW_NODE_WIDTH = 40
export const PREVIEW_NODE_HEIGHT = 40
const COLUMN_GAP = 32
const ROW_GAP = 36
const PADDING = 24
const ARROW_NODE_GAP = 5

interface PreviewNode {
  node: FloviraNode
  x: number
  y: number
  width: number
}

interface PreviewEdge {
  source: string
  target: string
  skip: FloviraSkip
  path: string
}

/** 按正向连线分层，汇合节点只绘制一次；布局不修改流程定义。 */
export function layoutPreview(definition: FloviraDefinition) {
  const nodes = definition.nodeList
  const byCode = new Map(nodes.map((node) => [node.nodeCode, node]))
  if (byCode.size !== nodes.length) throw new Error('流程中存在重复的节点编号')
  const edges: PreviewEdge[] = []
  const incoming = new Map(nodes.map((node) => [node.nodeCode, 0]))
  const outgoing = new Map(nodes.map((node) => [node.nodeCode, [] as PreviewEdge[]]))
  nodes.forEach((node) => node.skipList.forEach((skip) => {
    if (skip.skipType === 'REJECT') return
    if (!byCode.has(skip.targetNodeCode)) throw new Error(`节点“${node.nodeName}”的连线目标不存在`)
    const edge = { source: node.nodeCode, target: skip.targetNodeCode, skip, path: '' }
    edges.push(edge)
    outgoing.get(node.nodeCode)!.push(edge)
    incoming.set(edge.target, incoming.get(edge.target)! + 1)
  }))

  const queue = nodes.filter((node) => incoming.get(node.nodeCode) === 0).map((node) => node.nodeCode)
  const ranks = new Map(nodes.map((node) => [node.nodeCode, 0]))
  for (let index = 0; index < queue.length; index += 1) {
    const code = queue[index]
    outgoing.get(code)!.forEach((edge) => {
      ranks.set(edge.target, Math.max(ranks.get(edge.target)!, ranks.get(code)! + 1))
      incoming.set(edge.target, incoming.get(edge.target)! - 1)
      if (incoming.get(edge.target) === 0) queue.push(edge.target)
    })
  }
  if (queue.length !== nodes.length) throw new Error('当前预览暂不支持包含正向循环的流程')

  // 跨层边占用虚拟节点的列，给短分支保留直通空间。
  interface Slot { node?: FloviraNode; x: number; y: number }
  const layers: Slot[][] = []
  const slots = new Map<string, Slot>()
  queue.forEach((code) => {
    const rank = ranks.get(code)!
    const slot = { node: byCode.get(code)!, x: 0, y: PADDING + rank * (PREVIEW_NODE_HEIGHT + ROW_GAP) }
    slots.set(code, slot)
    const layer = layers[rank] ||= []
    layer.push(slot)
  })
  const links: { from: Slot; to: Slot }[] = []
  const routes = edges.map((edge) => {
    const route = [slots.get(edge.source)!]
    for (let rank = ranks.get(edge.source)! + 1; rank < ranks.get(edge.target)!; rank += 1) {
      const slot: Slot = { x: 0, y: PADDING + rank * (PREVIEW_NODE_HEIGHT + ROW_GAP) }
      layers[rank].push(slot)
      route.push(slot)
    }
    route.push(slots.get(edge.target)!)
    route.slice(1).forEach((slot, index) => links.push({ from: route[index], to: slot }))
    return { edge, route }
  })
  const step = PREVIEW_NODE_WIDTH + COLUMN_GAP
  const maxColumns = Math.max(1, ...layers.map((layer) => layer.length))
  layers.forEach((layer) => {
    const parentCenter = (slot: Slot) => {
      const parents = links.filter((link) => link.to === slot).map((link) => link.from)
      return parents.length ? parents.reduce((sum, parent) => sum + parent.x, 0) / parents.length : 0
    }
    layer.sort((left, right) => parentCenter(left) - parentCenter(right))
    layer.forEach((slot, index) => { slot.x = PADDING + (maxColumns - layer.length) * step / 2 + index * step })
  })
  // 从下向上对齐独占的后继；汇合节点不把多个分支挤到同一列。
  for (let rank = layers.length - 2; rank >= 0; rank -= 1) {
    let right = -Infinity
    layers[rank].forEach((slot) => {
      const children = links.filter((link) => link.from === slot).map((link) => link.to)
      const exclusiveChildren = children.filter((child) => links.filter((link) => link.to === child).length === 1)
      const desired = exclusiveChildren.length
        ? (Math.min(...exclusiveChildren.map((child) => child.x)) + Math.max(...exclusiveChildren.map((child) => child.x))) / 2
        : slot.x
      slot.x = Math.max(desired, right)
      right = slot.x + step
    })
  }
  const allSlots = layers.flat()
  const offset = PADDING - Math.min(PADDING, ...allSlots.map((slot) => slot.x))
  allSlots.forEach((slot) => { slot.x += offset })
  routes.forEach(({ edge, route }) => {
    const first = route[0]
    let path = `M ${first.x + PREVIEW_NODE_WIDTH / 2} ${first.y + PREVIEW_NODE_HEIGHT}`
    route.slice(1).forEach((to, index) => {
      const from = route[index]
      const x = to.x + PREVIEW_NODE_WIDTH / 2
      const middle = from.y + PREVIEW_NODE_HEIGHT + 12
      // 虚拟节点整列直通，箭头仅落在真正的目标节点前。
      const end = to.node ? to.y - ARROW_NODE_GAP : to.y + PREVIEW_NODE_HEIGHT
      path += ` V ${middle} H ${x} V ${end}`
    })
    edge.path = path
  })
  return {
    nodes: allSlots.filter((slot) => slot.node).map((slot): PreviewNode => ({
      node: slot.node!, x: slot.x, y: slot.y, width: PREVIEW_NODE_WIDTH,
    })),
    edges,
    width: Math.max(PADDING, ...allSlots.map((slot) => slot.x + PREVIEW_NODE_WIDTH)) + PADDING,
    height: layers.length ? layers.length * (PREVIEW_NODE_HEIGHT + ROW_GAP) - ROW_GAP + PADDING * 2 : 0,
  }
}
