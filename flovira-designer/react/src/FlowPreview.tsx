import { useEffect, useId, useMemo, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { CircleHelp, Maximize, ZoomIn, ZoomOut } from 'lucide-react'
import { normalizeDefinition } from './model'
import { NODE_META } from './nodeMeta'
import { layoutPreview, PREVIEW_NODE_HEIGHT } from './previewLayout'
import type { DesignerTooltipProps, DesignerUiAdapter, FloviraDefinition, FloviraNode } from './types'
import './preview.css'

export interface FlowPreviewNodeContext {
  node: FloviraNode
  current: boolean
  status: 'default' | 'pending' | 'current' | 'completed'
  handlers: readonly string[]
}

export interface FlowPreviewProps {
  /** 流程定义或 JSON；无数据时显示空状态，不自动生成流程。 */
  value?: FloviraDefinition | string | null
  /** 实例当前所在节点，允许并行节点；不推断历史状态。 */
  currentNodeCodes?: readonly string[]
  /** 已办理节点；提供后启用进度配色，其余非当前节点显示为未办理。 */
  completedNodeCodes?: readonly string[]
  /** 业务提供的实际办理人姓名，以节点编号为键。 */
  nodeHandlers?: Readonly<Record<string, readonly string[]>>
  height?: CSSProperties['height']
  className?: string
  style?: CSSProperties
  ui?: Pick<DesignerUiAdapter, 'Tooltip'>
  renderTooltip?: (context: FlowPreviewNodeContext) => ReactNode
  renderNodeIcon?: (context: FlowPreviewNodeContext) => ReactNode
}

/** 默认提示使用浮层，避免在小尺寸滚动画布内被裁剪。 */
function PreviewTooltip({ content, children }: DesignerTooltipProps) {
  const id = useId()
  const trigger = useRef<HTMLSpanElement>(null)
  const [position, setPosition] = useState<{ left: number; top: number; above: boolean } | null>(null)
  const open = () => {
    const rect = trigger.current?.getBoundingClientRect()
    if (!rect) return
    const above = rect.top > window.innerHeight / 2
    setPosition({
      left: Math.max(12, Math.min(rect.left, window.innerWidth - 272)),
      top: above ? rect.top - 8 : rect.bottom + 8,
      above,
    })
  }
  useEffect(() => {
    if (!position) return
    const close = () => setPosition(null)
    window.addEventListener('scroll', close, true)
    window.addEventListener('resize', close)
    return () => {
      window.removeEventListener('scroll', close, true)
      window.removeEventListener('resize', close)
    }
  }, [position])
  return (
    <span
      ref={trigger}
      className="frp-tooltip-trigger"
      onMouseEnter={open}
      onMouseLeave={() => setPosition(null)}
      onFocus={open}
      onBlur={() => setPosition(null)}
      onKeyDown={(event) => { if (event.key === 'Escape') setPosition(null) }}
    >
      <span tabIndex={0} aria-describedby={position ? id : undefined}>{children}</span>
      {position && createPortal(
        <span
          id={id}
          role="tooltip"
          className="frp-tooltip"
          style={{ left: position.left, top: position.top, transform: position.above ? 'translateY(-100%)' : undefined }}
        >{content}</span>,
        document.body,
      )}
    </span>
  )
}

/** 业务详情页使用的紧凑只读流程图。 */
export function FlowPreview({
  value,
  currentNodeCodes = [],
  completedNodeCodes,
  nodeHandlers = {},
  height = 320,
  className = '',
  style,
  ui,
  renderTooltip,
  renderNodeIcon,
}: FlowPreviewProps) {
  const markerId = `frp-arrow-${useId().replace(/:/g, '')}`
  const viewport = useRef<HTMLDivElement>(null)
  const drag = useRef<{ x: number; y: number; left: number; top: number } | null>(null)
  const [size, setSize] = useState({ width: 0, height: 0 })
  const [zoom, setZoom] = useState<number | null>(null)
  const result = useMemo(() => {
    if (!value) return { name: '', layout: null, error: '' }
    try {
      const parsed = typeof value === 'string' ? JSON.parse(value) : value
      if (!parsed || !Array.isArray(parsed.nodeList)) throw new Error('流程数据必须包含 nodeList 数组')
      const definition = normalizeDefinition(parsed)
      return { name: definition.flowName || '流程预览', layout: layoutPreview(definition), error: '' }
    } catch (error) {
      return { name: '', layout: null, error: error instanceof Error ? error.message : '流程数据无法解析' }
    }
  }, [value])
  useEffect(() => {
    const element = viewport.current
    if (!element) return
    const measure = () => setSize({ width: element.clientWidth, height: element.clientHeight })
    measure()
    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', measure)
      return () => window.removeEventListener('resize', measure)
    }
    const observer = new ResizeObserver(measure)
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  const layout = result.layout
  const fit = layout && size.width && size.height
    ? Math.max(0.25, Math.min(1, size.width / layout.width, size.height / layout.height))
    : 1
  const scale = zoom ?? fit
  const current = new Set(currentNodeCodes)
  const completed = new Set(completedNodeCodes)
  const Tooltip = ui?.Tooltip ?? PreviewTooltip
  const hasNodes = Boolean(layout?.nodes.length)

  return (
    <section className={`flovira-flow-preview ${className}`} style={style} aria-label={result.name || '流程预览'}>
      {layout && layout.nodes.some(({ node }) => current.has(node.nodeCode)) && (
        <div className="frp-current-summary" role="status">
          <span className="frp-summary-dot" aria-hidden="true" />
          <span className="frp-current-label">当前：</span>
          <span>{layout.nodes.filter(({ node }) => current.has(node.nodeCode)).map(({ node }) => node.nodeName).join('、')}</span>
        </div>
      )}
      <div
        ref={viewport}
        className="frp-viewport"
        style={{ height }}
        role="region"
        aria-label="流程图画布"
        tabIndex={0}
        onPointerDown={(event) => {
          if (event.button !== 0 || event.pointerType === 'touch'
            || (event.target as HTMLElement).closest('[data-node-code]')) return
          const element = event.currentTarget
          drag.current = { x: event.clientX, y: event.clientY, left: element.scrollLeft, top: element.scrollTop }
          element.setPointerCapture?.(event.pointerId)
        }}
        onPointerMove={(event) => {
          if (!drag.current) return
          event.currentTarget.scrollLeft = drag.current.left + drag.current.x - event.clientX
          event.currentTarget.scrollTop = drag.current.top + drag.current.y - event.clientY
        }}
        onPointerUp={() => { drag.current = null }}
        onLostPointerCapture={() => { drag.current = null }}
        onPointerCancel={() => { drag.current = null }}
      >
        {result.error ? <div className="frp-empty" role="alert">无法预览流程：{result.error}</div>
          : !hasNodes ? <div className="frp-empty" role="status">暂无流程数据</div>
            : layout && (
              <div className="frp-scroll-area" style={{ width: layout.width * scale, height: layout.height * scale }}>
                <div className="frp-graph" style={{ width: layout.width, height: layout.height, transform: `scale(${scale})` }}>
                  <svg className="frp-edges" width={layout.width} height={layout.height} aria-hidden="true">
                    <defs>
                      <marker id={markerId} viewBox="0 0 6 10" refX="6" refY="5" markerWidth="4" markerHeight="7" markerUnits="userSpaceOnUse" orient="auto">
                        <path d="M 0 0 L 6 5 L 0 10 z" fill="var(--frd-connector-arrow, currentColor)" />
                      </marker>
                    </defs>
                    {layout.edges.map((edge, index) => (
                      <path
                        key={`${edge.source}-${edge.target}-${index}`}
                        data-edge-source={edge.source}
                        data-edge-target={edge.target}
                        d={edge.path}
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinejoin="round"
                        markerEnd={`url(#${markerId})`}
                      />
                    ))}
                  </svg>
                  {layout.nodes.map(({ node, x, y, width }) => {
                    const meta = NODE_META[node.nodeType]
                    const Icon = meta?.icon ?? CircleHelp
                    const status: FlowPreviewNodeContext['status'] = current.has(node.nodeCode) ? 'current'
                      : completed.has(node.nodeCode) ? 'completed' : completedNodeCodes ? 'pending' : 'default'
                    const context = { node, current: status === 'current', status, handlers: nodeHandlers[node.nodeCode] ?? [] }
                    const content = renderTooltip ? renderTooltip(context) : (
                      <span className="frp-tooltip-content">
                        <span className="frp-tooltip-heading">
                          <span className="frp-tooltip-name">{node.nodeName}</span>
                        </span>
                        {(context.handlers.length > 0 || context.current) && (
                          <span className="frp-tooltip-detail">
                            <span className="frp-tooltip-label">办理人：</span>
                            <span>{context.handlers.length ? context.handlers.join('、') : '暂未提供'}</span>
                          </span>
                        )}
                      </span>
                    )
                    return (
                      <div
                        key={node.nodeCode}
                        className="frp-node-position"
                        data-node-code={node.nodeCode}
                        style={{ left: x, top: y, width, height: PREVIEW_NODE_HEIGHT } as CSSProperties}
                      >
                        <Tooltip content={content}>
                          <span className={`frd-node frp-node frd-node--${meta?.tone ?? 'approval'}`} data-current={context.current} data-status={status} aria-current={context.current ? 'step' : undefined} aria-label={node.nodeName}
                            tabIndex={ui?.Tooltip ? 0 : undefined}>
                            <span className="frp-node-tile">
                              <span className="frd-node__icon">{renderNodeIcon?.(context) ?? <Icon size={18} strokeWidth={2} aria-hidden="true" />}</span>
                              {context.current && <span className="frp-current-dot" aria-label="当前办理中" />}
                            </span>
                          </span>
                        </Tooltip>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}
      </div>
      {hasNodes && (
        <div className="frp-controls" role="group" aria-label="流程图缩放">
          <button type="button" aria-label="缩小流程图" disabled={scale <= 0.25} onClick={() => setZoom(Math.max(0.25, scale - 0.1))}><ZoomOut size={14} /></button>
          <span className="frp-scale">{Math.round(scale * 100)}%</span>
          <button type="button" aria-label="放大流程图" disabled={scale >= 2} onClick={() => setZoom(Math.min(2, scale + 0.1))}><ZoomIn size={14} /></button>
          <button type="button" aria-label="自适应流程图" onClick={() => {
            setZoom(null)
            if (viewport.current) { viewport.current.scrollLeft = 0; viewport.current.scrollTop = 0 }
          }}><Maximize size={14} /></button>
        </div>
      )}
    </section>
  )
}
