import type { ReactNode } from 'react'
import { CircleHelp } from 'lucide-react'
import { NODE_META } from './nodeMeta'
import type { FloviraNode } from './types'

/** 设计器与只读预览共用标题内容，类型样式由外层节点提供。 */
export function NodeHeader({ node, icon, children }: {
  node: FloviraNode
  icon?: ReactNode
  children?: ReactNode
}) {
  const Icon = NODE_META[node.nodeType]?.icon ?? CircleHelp
  return (
    <span className="frd-node__header">
      <span className="frd-node__icon">{icon ?? <Icon size={13} aria-hidden="true" />}</span>
      <span className="frd-node__name">{node.nodeName}</span>
      {children}
    </span>
  )
}
