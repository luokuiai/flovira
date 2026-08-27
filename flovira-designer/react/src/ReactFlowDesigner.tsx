import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type ReactNode,
} from 'react'
import {
  Box,
  Check,
  CircleDot,
  Download,
  GitBranch,
  GitFork,
  Hourglass,
  LocateFixed,
  Mail,
  Network,
  Plus,
  Redo2,
  Save,
  Settings2,
  Trash2,
  Timer,
  Undo2,
  Upload,
  UserRoundCheck,
  X,
  ZoomIn,
  ZoomOut,
} from 'lucide-react'
import {
  addGatewayBranch,
  approverStrategyOptions,
  createInitialDefinition,
  DEFAULT_DESIGNER_CAPABILITIES,
  deleteNode,
  filterNodeTypes,
  findApproverStrategy,
  getApproverRule,
  getCarbonCopyRule,
  getSubprocessConfig,
  getTimeoutConfig,
  getWaitConfig,
  insertNodeAfter,
  nodeName,
  normalizeDefinition,
  serializeDefinition,
  setSubprocessConfig,
  setApproverRule,
  setCarbonCopyRule,
  setTimeoutConfig,
  setWaitConfig,
  updateNode,
  validateDefinition,
} from './model'
import type {
  ApproverSubject,
  DesignerCapabilities,
  FloviraDefinition,
  FloviraNode,
  FloviraNodeType,
  ReactFlowDesignerProps,
  ReactFlowDesignerRef,
  DesignerResourcePage,
  DesignerResourceItem,
  SubprocessDefinition,
} from './types'

const NODE_META: Record<FloviraNodeType, {
  label: string
  icon: typeof CircleDot
  shell: string
  header: string
}> = {
  '0': { label: '开始', icon: CircleDot, shell: 'border-emerald-200', header: 'bg-emerald-600' },
  '1': { label: '审批', icon: UserRoundCheck, shell: 'border-blue-200', header: 'bg-blue-600' },
  '2': { label: '结束', icon: Check, shell: 'border-rose-200', header: 'bg-rose-600' },
  '3': { label: '互斥网关', icon: GitBranch, shell: 'border-amber-200', header: 'bg-amber-500' },
  '4': { label: '并行网关', icon: GitFork, shell: 'border-cyan-200', header: 'bg-cyan-600' },
  '5': { label: '包含网关', icon: Network, shell: 'border-violet-200', header: 'bg-violet-600' },
  '6': { label: '子流程', icon: Box, shell: 'border-purple-200', header: 'bg-purple-600' },
  '7': { label: '等待', icon: Hourglass, shell: 'border-emerald-200', header: 'bg-emerald-700' },
  '8': { label: '抄送', icon: Mail, shell: 'border-teal-200', header: 'bg-teal-600' },
}

const INSERT_TYPES: FloviraNodeType[] = ['1', '8', '7', '6', '3', '4', '5']

const extractSubprocesses = (
  value: DesignerResourcePage | { data?: DesignerResourcePage },
): SubprocessDefinition[] => {
  const envelope = value as { data?: DesignerResourcePage }
  const page = envelope.data || value as DesignerResourcePage
  return (page?.items || []).map((item) => ({
    ...item.metadata,
    flowCode: item.code || item.id,
    flowName: item.name,
  }))
}

const extractResourcePage = (
  value: DesignerResourcePage | { data?: DesignerResourcePage },
): DesignerResourcePage => {
  const envelope = value as { data?: DesignerResourcePage }
  return envelope.data || value as DesignerResourcePage || { items: [], total: 0 }
}

const extractCapabilities = (
  value: DesignerCapabilities | { data?: DesignerCapabilities },
): DesignerCapabilities => {
  const envelope = value as { data?: DesignerCapabilities }
  return envelope.data || value as DesignerCapabilities || DEFAULT_DESIGNER_CAPABILITIES
}

const summaryFor = (node: FloviraNode, subprocesses: SubprocessDefinition[]): string => {
  if (node.nodeType === '0') return '流程由此发起'
  if (node.nodeType === '2') return '流程在此完成'
  if (node.nodeType === '6') {
    const code = String(getSubprocessConfig(node).fixedChildFlowCode || '')
    return subprocesses.find((flow) => flow.flowCode === code)?.flowName || code || '未选择固定子流程'
  }
  if (node.nodeType === '7') return String(getWaitConfig(node).waitKey || '未配置等待标识')
  if (['3', '4', '5'].includes(node.nodeType)) return `${node.skipList.length} 条分支`
  const rule = node.nodeType === '8' ? getCarbonCopyRule(node) : getApproverRule(node)
  const emptyLabel = node.nodeType === '8' ? '未配置抄送人' : '未配置办理人'
  if (rule.strategy === 'EXPRESSION') return rule.expression || emptyLabel
  return rule.subjects.map((subject) => subject.name || subject.id).join('、') || emptyLabel
}

export const ReactFlowDesigner = forwardRef<ReactFlowDesignerRef, ReactFlowDesignerProps>(
  function ReactFlowDesigner({
    value,
    defaultValue,
    disabled = false,
    className = '',
    dataProvider,
    maxHistory = 50,
    onChange,
    onSave,
    renderNode,
  }, ref) {
    const initial = useMemo(
      () => normalizeDefinition(value ?? defaultValue ?? createInitialDefinition()),
      [],
    )
    const [definition, setDefinition] = useState<FloviraDefinition>(initial)
    const [past, setPast] = useState<FloviraDefinition[]>([])
    const [future, setFuture] = useState<FloviraDefinition[]>([])
    const [selectedCode, setSelectedCode] = useState('')
    const [insertAfter, setInsertAfter] = useState<string | null>(null)
    const [zoom, setZoom] = useState(1)
    const [dirty, setDirty] = useState(false)
    const [subprocesses, setSubprocesses] = useState<SubprocessDefinition[]>([])
    const [subprocessState, setSubprocessState] = useState<'idle' | 'loading' | 'error'>('idle')
    const [capabilities, setCapabilities] = useState<DesignerCapabilities>(DEFAULT_DESIGNER_CAPABILITIES)
    const [approverKeyword, setApproverKeyword] = useState('')
    const [approverPage, setApproverPage] = useState(1)
    const [approverResources, setApproverResources] = useState<DesignerResourceItem[]>([])
    const [approverTotal, setApproverTotal] = useState(0)
    const [approverResourceState, setApproverResourceState] = useState<'idle' | 'loading' | 'error'>('idle')
    const canvasRef = useRef<HTMLDivElement>(null)
    const importRef = useRef<HTMLInputElement>(null)
    const lastEmittedJsonRef = useRef<string | null>(null)

    useEffect(() => {
      if (value === undefined) return
      const incoming = normalizeDefinition(value)
      const incomingJson = serializeDefinition(incoming)
      if (incomingJson === lastEmittedJsonRef.current) {
        lastEmittedJsonRef.current = null
        return
      }
      setDefinition(incoming)
      setPast([])
      setFuture([])
      setDirty(false)
    }, [value])

    useEffect(() => {
      if (!dataProvider?.capabilities) {
        setCapabilities(DEFAULT_DESIGNER_CAPABILITIES)
        return
      }
      let active = true
      dataProvider.capabilities()
        .then((response) => {
          if (active) setCapabilities(extractCapabilities(response))
        })
        .catch(() => {
          if (active) setCapabilities(DEFAULT_DESIGNER_CAPABILITIES)
        })
      return () => { active = false }
    }, [dataProvider])

    useEffect(() => {
      if (!dataProvider?.queryResources) {
        setSubprocessState('idle')
        return
      }
      let active = true
      setSubprocessState('loading')
      dataProvider.queryResources({ resourceType: 'SUBPROCESS', pageNum: 1, pageSize: 1000 })
        .then((response) => {
          if (!active) return
          setSubprocesses(extractSubprocesses(response))
          setSubprocessState('idle')
        })
        .catch(() => {
          if (active) setSubprocessState('error')
        })
      return () => { active = false }
    }, [dataProvider])

    const emit = useCallback((next: FloviraDefinition, isDirty = true) => {
      const json = serializeDefinition(next)
      lastEmittedJsonRef.current = json
      setDefinition(next)
      setDirty(isDirty)
      onChange?.({ definition: next, json, dirty: isDirty })
    }, [onChange])

    const commit = useCallback((next: FloviraDefinition) => {
      setPast((items) => [...items.slice(-(Math.max(1, maxHistory) - 1)), definition])
      setFuture([])
      emit(next)
    }, [definition, emit, maxHistory])

    const undo = useCallback(() => {
      const previous = past[past.length - 1]
      if (!previous) return
      setPast((items) => items.slice(0, -1))
      setFuture((items) => [definition, ...items])
      emit(previous)
    }, [definition, emit, past])

    const redo = useCallback(() => {
      const next = future[0]
      if (!next) return
      setFuture((items) => items.slice(1))
      setPast((items) => [...items, definition])
      emit(next)
    }, [definition, emit, future])

    const locateStart = useCallback(() => {
      const canvas = canvasRef.current
      if (!canvas) return
      canvas.scrollTo({ top: 0, left: Math.max(0, (canvas.scrollWidth - canvas.clientWidth) / 2), behavior: 'smooth' })
    }, [])

    useImperativeHandle(ref, () => ({
      getDefinition: () => definition,
      getFlowJson: () => serializeDefinition(definition),
      importJson: (input) => {
        const next = normalizeDefinition(input)
        commit(next)
        setSelectedCode('')
      },
      validate: () => validateDefinition(definition),
      isDirty: () => dirty,
      resetDirty: () => setDirty(false),
      undo,
      redo,
      zoomIn: () => setZoom((current) => Math.min(1.5, current + 0.1)),
      zoomOut: () => setZoom((current) => Math.max(0.6, current - 0.1)),
      resetZoom: () => setZoom(1),
      locateStart,
    }), [commit, definition, dirty, locateStart, redo, undo])

    const selectedNode = definition.nodeList.find((node) => node.nodeCode === selectedCode)
    const selectedApproverRule = selectedNode?.nodeType === '8'
      ? getCarbonCopyRule(selectedNode)
      : selectedNode?.nodeType === '1' ? getApproverRule(selectedNode) : null
    const setSelectedParticipantRule = selectedNode?.nodeType === '8' ? setCarbonCopyRule : setApproverRule
    const selectedApproverStrategy = selectedApproverRule
      ? findApproverStrategy(capabilities, selectedApproverRule.strategy)
      : undefined
    useEffect(() => {
      if (!selectedNode || !['1', '8'].includes(selectedNode.nodeType)
        || selectedApproverStrategy?.selectionType !== 'RESOURCE'
        || !selectedApproverStrategy.resourceType
        || !dataProvider?.queryResources) {
        setApproverResources([])
        setApproverTotal(0)
        setApproverResourceState('idle')
        return
      }
      let active = true
      setApproverResourceState('loading')
      dataProvider.queryResources({
        resourceType: selectedApproverStrategy.resourceType,
        keyword: approverKeyword || undefined,
        pageNum: approverPage,
        pageSize: 20,
      }).then((response) => {
        if (!active) return
        const page = extractResourcePage(response)
        setApproverResources(page.items || [])
        setApproverTotal(page.total || 0)
        setApproverResourceState('idle')
      }).catch(() => {
        if (active) setApproverResourceState('error')
      })
      return () => { active = false }
    }, [approverKeyword, approverPage, dataProvider, selectedCode,
      selectedApproverStrategy?.code, selectedApproverStrategy?.resourceType, selectedApproverStrategy?.selectionType])
    const nodeMap = useMemo(
      () => new Map(definition.nodeList.map((node) => [node.nodeCode, node])),
      [definition],
    )
    const incomingCount = useMemo(() => {
      const counts = new Map<string, number>()
      definition.nodeList.forEach((node) => node.skipList.forEach((skip) =>
        counts.set(skip.nextNodeCode, (counts.get(skip.nextNodeCode) || 0) + 1)))
      return counts
    }, [definition])

    const changeSelected = (patch: Partial<FloviraNode>) => {
      if (!selectedNode) return
      commit(updateNode(definition, selectedNode.nodeCode, patch))
    }

    const handleFile = async (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0]
      if (!file) return
      try {
        const next = normalizeDefinition(await file.text())
        commit(next)
        setSelectedCode('')
      } finally {
        event.target.value = ''
      }
    }

    const exportJson = () => {
      const blob = new Blob([serializeDefinition(definition)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `${definition.flowCode || 'flovira-flow'}.json`
      anchor.click()
      URL.revokeObjectURL(url)
    }

    const NodeCard = ({ node }: { node: FloviraNode }) => {
      const meta = NODE_META[node.nodeType] || NODE_META['1']
      const Icon = meta.icon
      const summary = summaryFor(node, subprocesses)
      const selected = selectedCode === node.nodeCode
      if (renderNode) {
        return <>{renderNode({ node, selected, summary })}</>
      }
      return (
        <button
          type="button"
          className={`flovira-react-node overflow-hidden rounded-lg border bg-[var(--frd-surface)] text-left shadow-sm transition hover:-translate-y-px hover:shadow-md ${meta.shell} ${selected ? 'ring-2 ring-blue-500 ring-offset-2' : ''}`}
          onClick={() => setSelectedCode(node.nodeCode)}
          aria-label={`编辑节点：${node.nodeName}`}
        >
          <span className={`flex items-center gap-2 px-3 py-2 text-white ${meta.header}`}>
            <Icon size={16} aria-hidden="true" />
            <span className="min-w-0 flex-1 truncate text-sm font-semibold">{node.nodeName}</span>
            <span className="text-[11px] text-white/85">{meta.label}</span>
          </span>
          <span className="block truncate px-3 py-3 text-center text-xs text-[var(--frd-muted)]">
            {summary}
          </span>
        </button>
      )
    }

    const InsertPoint = ({ after }: { after: FloviraNode }) => {
      if (disabled || after.nodeType === '2') return <div className="h-8 w-px bg-slate-400" />
      return (
        <div className="relative flex h-12 flex-col items-center justify-center">
          <span className="absolute inset-y-0 w-px bg-slate-400" />
          <button
            type="button"
            className="relative z-10 flex h-6 w-6 items-center justify-center rounded-full border border-blue-400 bg-white text-blue-600 shadow-sm hover:bg-blue-50"
            aria-label={`在 ${after.nodeName} 后添加节点`}
            onClick={(event) => {
              event.stopPropagation()
              setInsertAfter(insertAfter === after.nodeCode ? null : after.nodeCode)
            }}
          >
            <Plus size={14} />
          </button>
          {insertAfter === after.nodeCode && (
            <div className="absolute left-1/2 top-9 z-50 flex -translate-x-1/2 gap-1 rounded-lg border border-[var(--frd-border)] bg-[var(--frd-surface)] p-1.5 shadow-xl">
              {filterNodeTypes(INSERT_TYPES, capabilities).map((type) => {
                const MetaIcon = NODE_META[type].icon
                return (
                  <button
                    key={type}
                    type="button"
                    title={`添加${nodeName(type)}`}
                    className="flex h-9 w-9 items-center justify-center rounded-md text-[var(--frd-muted)] hover:bg-blue-50 hover:text-blue-600"
                    onClick={() => {
                      const next = insertNodeAfter(definition, after.nodeCode, type)
                      const added = next.nodeList.find((item) => !definition.nodeList.some((old) => old.nodeCode === item.nodeCode))
                      commit(next)
                      if (added) setSelectedCode(added.nodeCode)
                      setInsertAfter(null)
                    }}
                  >
                    <MetaIcon size={17} />
                  </button>
                )
              })}
            </div>
          )}
        </div>
      )
    }

    const renderPath = (code: string, visited: Set<string>): ReactNode => {
      const node = nodeMap.get(code)
      if (!node) return null
      if (visited.has(code)) {
        return (
          <div className="flex flex-col items-center">
            <div className="h-8 w-px bg-slate-400" />
            <div className="rounded-md border border-dashed border-slate-300 bg-[var(--frd-surface)] px-3 py-1 text-xs text-[var(--frd-muted)]">
              汇合至 {node.nodeName}
            </div>
          </div>
        )
      }
      const nextVisited = new Set(visited)
      nextVisited.add(code)
      const outgoing = node.skipList.filter((skip) => nodeMap.has(skip.nextNodeCode))
      const branchChildren = outgoing.map((skip) => nodeMap.get(skip.nextNodeCode))
      const branchMergeCode = outgoing.length > 1
        && branchChildren.every((child) => child?.skipList.length === 1)
        && branchChildren.every((child) => child?.skipList[0].nextNodeCode === branchChildren[0]?.skipList[0].nextNodeCode)
        ? branchChildren[0]?.skipList[0].nextNodeCode
        : undefined
      return (
        <div className="flex flex-col items-center" key={`${code}-${visited.size}`}>
          <NodeCard node={node} />
          {outgoing.length === 0 ? null : outgoing.length === 1 ? (
            <>
              <InsertPoint after={node} />
              {renderPath(outgoing[0].nextNodeCode, nextVisited)}
            </>
          ) : (
            <>
              <div className="h-8 w-px bg-slate-400" />
              <div className="relative border-t border-slate-400 pt-6">
                <div className="flovira-react-branch-grid">
                  {outgoing.map((skip) => (
                    <div className="relative flex flex-col items-center" key={String(skip.id || skip.nextNodeCode)}>
                      <span className="absolute -top-6 h-6 w-px bg-slate-400" />
                      <input
                        value={skip.skipName || ''}
                        disabled={disabled}
                        aria-label={`分支名称：${node.nodeName}`}
                        className="mb-2 h-7 w-36 rounded-md border border-[var(--frd-border)] bg-[var(--frd-surface)] px-2 text-center text-xs text-[var(--frd-text)]"
                        placeholder="分支名称"
                        onChange={(event) => {
                          const skips = node.skipList.map((item) => item === skip
                            ? { ...item, skipName: event.target.value }
                            : item)
                          commit(updateNode(definition, node.nodeCode, { skipList: skips }))
                        }}
                      />
                      {renderPath(
                        skip.nextNodeCode,
                        branchMergeCode ? new Set([...nextVisited, branchMergeCode]) : nextVisited,
                      )}
                    </div>
                  ))}
                </div>
              </div>
              {branchMergeCode && (
                <>
                  <div className="h-8 w-px bg-slate-400" />
                  {renderPath(branchMergeCode, nextVisited)}
                </>
              )}
            </>
          )}
        </div>
      )
    }

    const start = definition.nodeList.find((node) => node.nodeType === '0') || definition.nodeList[0]
    const validation = validateDefinition(definition)

    return (
      <section className={`flovira-react-designer flex h-[min(820px,calc(100vh-32px))] min-h-[560px] w-full flex-col overflow-hidden rounded-lg border border-[var(--frd-border)] bg-[var(--frd-surface)] ${className}`}>
        <header className="flex min-h-14 shrink-0 flex-wrap items-center justify-between gap-2 border-b border-[var(--frd-border)] px-3 py-2 sm:px-4">
          <div className="flex min-w-0 items-center gap-2">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-blue-600 text-white">
              <Network size={17} />
            </div>
            <div className="min-w-0">
              <h2 className="truncate text-sm font-semibold">{definition.flowName || '流程设计'}</h2>
              <p className="truncate text-xs text-[var(--frd-muted)]">{definition.flowCode || '未设置流程编码'}</p>
            </div>
            {dirty && <span className="h-2 w-2 rounded-full bg-amber-500" title="有未保存修改" />}
          </div>
          <div className="flex items-center gap-1">
            <ToolbarButton label="撤销" disabled={!past.length || disabled} onClick={undo}><Undo2 size={16} /></ToolbarButton>
            <ToolbarButton label="重做" disabled={!future.length || disabled} onClick={redo}><Redo2 size={16} /></ToolbarButton>
            <span className="mx-1 h-5 w-px bg-[var(--frd-border)]" />
            <input ref={importRef} className="hidden" type="file" accept="application/json,.json" onChange={handleFile} />
            <ToolbarButton label="导入 JSON" disabled={disabled} onClick={() => importRef.current?.click()}><Upload size={16} /></ToolbarButton>
            <ToolbarButton label="导出 JSON" onClick={exportJson}><Download size={16} /></ToolbarButton>
            {onSave && (
              <button
                type="button"
                disabled={disabled}
                className="ml-1 inline-flex h-8 items-center gap-1.5 rounded-md border-0 bg-blue-600 px-3 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                onClick={async () => {
                  await onSave(definition, serializeDefinition(definition))
                  setDirty(false)
                }}
              >
                <Save size={14} />保存
              </button>
            )}
          </div>
        </header>

        <div className="flex min-h-0 flex-1">
          <div className="relative min-w-0 flex-1 overflow-hidden">
            <div ref={canvasRef} className="flovira-react-canvas h-full overflow-auto p-6 sm:p-10">
              <div
                className="mx-auto flex min-h-full min-w-max justify-center pb-28 pt-8 transition-transform duration-150"
                style={{ transform: `scale(${zoom})`, transformOrigin: 'top center' }}
                onClick={() => setInsertAfter(null)}
              >
                {start ? renderPath(start.nodeCode, new Set()) : (
                  <div className="text-sm text-rose-600">流程缺少开始节点</div>
                )}
              </div>
            </div>
            <div className="absolute bottom-4 left-4 flex items-center gap-1 rounded-md border border-[var(--frd-border)] bg-[var(--frd-surface)] p-1 shadow-sm">
              <ToolbarButton label="缩小" onClick={() => setZoom((current) => Math.max(0.6, current - 0.1))}><ZoomOut size={16} /></ToolbarButton>
              <button type="button" className="h-8 min-w-12 border-0 bg-transparent text-xs text-[var(--frd-muted)]" onClick={() => setZoom(1)}>{Math.round(zoom * 100)}%</button>
              <ToolbarButton label="放大" onClick={() => setZoom((current) => Math.min(1.5, current + 0.1))}><ZoomIn size={16} /></ToolbarButton>
              <ToolbarButton label="定位开始节点" onClick={locateStart}><LocateFixed size={16} /></ToolbarButton>
            </div>
            {!validation.valid && (
              <div className="absolute bottom-4 right-4 max-w-72 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800 shadow-sm">
                {validation.issues.length} 项配置待完善：{validation.issues[0].message}
              </div>
            )}
          </div>

          {selectedNode && (
            <aside className="absolute inset-y-14 right-0 z-40 w-[min(340px,calc(100%-24px))] overflow-y-auto border-l border-[var(--frd-border)] bg-[var(--frd-surface)] p-4 shadow-xl md:static md:inset-auto md:z-auto md:w-[328px] md:shrink-0 md:shadow-none">
              <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Settings2 size={17} className="text-blue-600" />
                  <h3 className="text-sm font-semibold">节点设置</h3>
                </div>
                <button type="button" className="flex h-8 w-8 items-center justify-center rounded-md hover:bg-slate-100 md:hidden" onClick={() => setSelectedCode('')} aria-label="关闭节点设置"><X size={16} /></button>
              </div>
              <Field label="节点名称">
                <input value={selectedNode.nodeName} disabled={disabled} onChange={(event) => changeSelected({ nodeName: event.target.value })} />
              </Field>
              <Field label="节点编码">
                <input value={selectedNode.nodeCode} disabled />
              </Field>
              {['1', '8'].includes(selectedNode.nodeType) && (
                <>
                  <Field label={selectedNode.nodeType === '8' ? '抄送人类型' : '办理人类型'}>
                    <select
                      value={String(selectedApproverRule?.strategy || 'USER')}
                      disabled={disabled}
                      onChange={(event) => {
                        const strategy = findApproverStrategy(capabilities, event.target.value)
                        setApproverKeyword('')
                        setApproverPage(1)
                        commit(updateNode(definition, selectedNode.nodeCode,
                          setSelectedParticipantRule(selectedNode, event.target.value, [], '', strategy?.relationType,
                            strategy?.selectionType || 'RESOURCE')))
                      }}
                    >
                      {approverStrategyOptions(capabilities).map((strategy) => (
                        <option key={strategy.value} value={strategy.value}>{strategy.label}</option>
                      ))}
                    </select>
                  </Field>
                  {selectedApproverStrategy?.selectionType === 'EXPRESSION' ? (
                    <Field label={selectedNode.nodeType === '8' ? '抄送人表达式' : '办理人表达式'}>
                      <input
                        value={String(selectedApproverRule?.expression || '')}
                        disabled={disabled}
                        placeholder="例如 ${approverIds}"
                        onChange={(event) => commit(updateNode(definition, selectedNode.nodeCode,
                          setSelectedParticipantRule(selectedNode, selectedApproverRule?.strategy || 'EXPRESSION', [], event.target.value,
                            selectedApproverStrategy.relationType, selectedApproverStrategy.selectionType)))}
                      />
                    </Field>
                  ) : selectedApproverStrategy?.selectionType === 'RESOURCE' ? (
                    <div className="mb-5">
                      <Field label={selectedNode.nodeType === '8' ? '搜索抄送人' : '搜索办理人'}>
                        <input
                          value={approverKeyword}
                          disabled={disabled || !dataProvider?.queryResources}
                          placeholder={`搜索${approverStrategyOptions(capabilities).find((item) => item.value === selectedApproverRule?.strategy)?.label || (selectedNode.nodeType === '8' ? '抄送人' : '办理人')}`}
                          onChange={(event) => {
                            setApproverKeyword(event.target.value)
                            setApproverPage(1)
                          }}
                        />
                      </Field>
                      <div className="max-h-56 overflow-y-auto border-y border-[var(--frd-border)]">
                        {approverResourceState === 'loading' && <p className="py-4 text-center text-xs text-[var(--frd-muted)]">加载中...</p>}
                        {approverResourceState === 'error' && <p className="py-4 text-center text-xs text-rose-600">办理人数据加载失败</p>}
                        {approverResourceState === 'idle' && approverResources.length === 0 && (
                          <p className="py-4 text-center text-xs text-[var(--frd-muted)]">暂无可选数据</p>
                        )}
                        {approverResources.map((item) => {
                          const selected = selectedApproverRule?.subjects.some((subject) => subject.id === item.id) || false
                          return (
                            <label key={`${item.resourceType}:${item.id}`} className="flex min-h-10 items-center gap-2 border-b border-[var(--frd-border)] px-1 text-xs last:border-b-0">
                              <input
                                type="checkbox"
                                checked={selected}
                                disabled={disabled || item.disabled}
                                onChange={() => {
                                  const current = selectedApproverRule?.subjects || []
                                  const subjects: ApproverSubject[] = selected
                                    ? current.filter((subject) => subject.id !== item.id)
                                    : selectedApproverStrategy.multiple
                                      ? [...current, { id: item.id, type: item.resourceType, name: item.name }]
                                      : [{ id: item.id, type: item.resourceType, name: item.name }]
                                  commit(updateNode(definition, selectedNode.nodeCode,
                                    setSelectedParticipantRule(selectedNode, String(selectedApproverRule?.strategy || 'USER'),
                                      subjects, '', selectedApproverStrategy.relationType,
                                      selectedApproverStrategy.selectionType)))
                                }}
                              />
                              <span className="min-w-0 flex-1 truncate">{item.name}</span>
                              {item.code && <span className="truncate text-[var(--frd-muted)]">{item.code}</span>}
                            </label>
                          )
                        })}
                      </div>
                      {approverTotal > 20 && (
                        <div className="mt-2 flex items-center justify-between text-xs text-[var(--frd-muted)]">
                          <button type="button" disabled={approverPage <= 1} onClick={() => setApproverPage((page) => Math.max(1, page - 1))}>上一页</button>
                          <span>{approverPage} / {Math.ceil(approverTotal / 20)}</span>
                          <button type="button" disabled={approverPage * 20 >= approverTotal} onClick={() => setApproverPage((page) => page + 1)}>下一页</button>
                        </div>
                      )}
                      {selectedApproverRule && selectedApproverRule.subjects.length > 0 && (
                        <p className="mt-2 text-xs text-[var(--frd-muted)]">已选择：{selectedApproverRule.subjects.map((subject) => subject.name || subject.id).join('、')}</p>
                      )}
                    </div>
                  ) : null}
                </>
              )}
              {selectedNode.nodeType === '6' && (
                <Field label="固定子流程" hint={subprocessState === 'error' ? '子流程列表加载失败，原值仍会保留' : undefined}>
                  <select
                    value={String(getSubprocessConfig(selectedNode).fixedChildFlowCode || '')}
                    disabled={disabled || subprocessState === 'loading'}
                    onChange={(event) => commit(updateNode(definition, selectedNode.nodeCode, setSubprocessConfig(selectedNode, event.target.value)))}
                  >
                    <option value="">{subprocessState === 'loading' ? '加载中...' : '请选择已发布流程'}</option>
                    {subprocesses.map((flow) => <option key={flow.flowCode} value={flow.flowCode}>{flow.flowName}</option>)}
                  </select>
                </Field>
              )}
              {selectedNode.nodeType === '7' && (
                <Field label="等待标识" hint="业务系统使用该标识恢复等待任务">
                  <input
                    value={String(getWaitConfig(selectedNode).waitKey || '')}
                    disabled={disabled}
                    placeholder="例如 ORDER_PAID"
                    onChange={(event) => commit(updateNode(
                      definition,
                      selectedNode.nodeCode,
                      setWaitConfig(selectedNode, event.target.value.trim()),
                    ))}
                  />
                </Field>
              )}
              {capabilities.timeoutNodeTypes.includes(selectedNode.nodeType) && (() => {
                const timeout = getTimeoutConfig(selectedNode)
                const enabled = Boolean(timeout.enabled)
                return (
                  <div className="mb-5 border-t border-[var(--frd-border)] pt-4">
                    <div className="mb-3 flex items-center justify-between">
                      <span className="inline-flex items-center gap-1.5 text-xs font-semibold"><Timer size={14} />节点超时</span>
                      <input
                        type="checkbox"
                        checked={enabled}
                        disabled={disabled}
                        aria-label="启用节点超时"
                        onChange={(event) => commit(updateNode(
                          definition,
                          selectedNode.nodeCode,
                          setTimeoutConfig(selectedNode, { enabled: event.target.checked }),
                        ))}
                      />
                    </div>
                    {enabled && (
                      <>
                        <Field label="超时时长">
                          <input
                            type="number"
                            min="1"
                            value={Number(timeout.duration || 1)}
                            disabled={disabled}
                            onChange={(event) => commit(updateNode(definition, selectedNode.nodeCode,
                              setTimeoutConfig(selectedNode, { duration: Math.max(1, Number(event.target.value) || 1) })))}
                          />
                        </Field>
                        <Field label="时间单位">
                          <select
                            value={String(timeout.durationUnit || 'HOURS')}
                            disabled={disabled}
                            onChange={(event) => commit(updateNode(definition, selectedNode.nodeCode,
                              setTimeoutConfig(selectedNode, { durationUnit: event.target.value })))}
                          >
                            <option value="MINUTES">分钟</option>
                            <option value="HOURS">小时</option>
                            <option value="DAYS">天</option>
                          </select>
                        </Field>
                        <Field label="超时动作">
                          <select
                            value={String(timeout.action || (selectedNode.nodeType === '7' ? 'RESUME_WAIT' : 'AUTO_PASS'))}
                            disabled={disabled}
                            onChange={(event) => commit(updateNode(definition, selectedNode.nodeCode,
                              setTimeoutConfig(selectedNode, { action: event.target.value })))}
                          >
                            {selectedNode.nodeType === '7' ? (
                              <option value="RESUME_WAIT">恢复等待并继续</option>
                            ) : (
                              <>
                                <option value="AUTO_PASS">自动通过</option>
                                <option value="AUTO_REJECT">自动退回</option>
                              </>
                            )}
                          </select>
                        </Field>
                      </>
                    )}
                  </div>
                )
              })()}
              {['3', '4', '5'].includes(selectedNode.nodeType) && (
                <div className="mb-5 border-t border-[var(--frd-border)] pt-4">
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-xs font-semibold">分支</span>
                    <button type="button" disabled={disabled} className="inline-flex items-center gap-1 border-0 bg-transparent text-xs text-blue-600 disabled:opacity-50" onClick={() => commit(addGatewayBranch(definition, selectedNode.nodeCode))}><Plus size={13} />增加分支</button>
                  </div>
                  {selectedNode.skipList.map((skip, index) => (
                    <div className="mb-2 rounded-md border border-[var(--frd-border)] p-2" key={String(skip.id || index)}>
                      <input
                        value={skip.skipName || ''}
                        disabled={disabled}
                        placeholder={`分支 ${index + 1}`}
                        onChange={(event) => changeSelected({ skipList: selectedNode.skipList.map((item, itemIndex) => itemIndex === index ? { ...item, skipName: event.target.value } : item) })}
                      />
                      {selectedNode.nodeType !== '4' && (
                        <input
                          className="mt-2"
                          value={skip.skipCondition || ''}
                          disabled={disabled}
                          placeholder="条件表达式"
                          onChange={(event) => changeSelected({ skipList: selectedNode.skipList.map((item, itemIndex) => itemIndex === index ? { ...item, skipCondition: event.target.value } : item) })}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
              {!['0', '2'].includes(selectedNode.nodeType) && (
                <button
                  type="button"
                  disabled={disabled}
                  className="inline-flex h-9 w-full items-center justify-center gap-2 rounded-md border border-rose-200 text-xs font-medium text-rose-600 hover:bg-rose-50 disabled:opacity-50"
                  onClick={() => {
                    commit(deleteNode(definition, selectedNode.nodeCode))
                    setSelectedCode('')
                  }}
                >
                  <Trash2 size={14} />删除节点
                </button>
              )}
              {incomingCount.get(selectedNode.nodeCode) && incomingCount.get(selectedNode.nodeCode)! > 1 ? (
                <p className="mt-3 text-xs text-[var(--frd-muted)]">该节点是 {incomingCount.get(selectedNode.nodeCode)} 条分支的汇合点。</p>
              ) : null}
            </aside>
          )}
        </div>
      </section>
    )
  },
)

const ToolbarButton = ({
  label,
  disabled,
  onClick,
  children,
}: {
  label: string
  disabled?: boolean
  onClick: () => void
  children: ReactNode
}) => (
  <button
    type="button"
    title={label}
    aria-label={label}
    disabled={disabled}
    onClick={onClick}
    className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md border-0 bg-transparent text-[var(--frd-muted)] hover:bg-slate-100 hover:text-blue-600 disabled:cursor-not-allowed disabled:opacity-35"
  >
    {children}
  </button>
)

const Field = ({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) => (
  <label className="mb-4 block text-xs font-medium text-[var(--frd-muted)]">
    <span className="mb-1.5 block">{label}</span>
    <span className="block [&_input]:h-9 [&_input]:w-full [&_input]:rounded-md [&_input]:border [&_input]:border-[var(--frd-border)] [&_input]:bg-[var(--frd-surface)] [&_input]:px-3 [&_input]:text-sm [&_input]:text-[var(--frd-text)] [&_select]:h-9 [&_select]:w-full [&_select]:rounded-md [&_select]:border [&_select]:border-[var(--frd-border)] [&_select]:bg-[var(--frd-surface)] [&_select]:px-3 [&_select]:text-sm [&_select]:text-[var(--frd-text)]">
      {children}
    </span>
    {hint && <span className="mt-1.5 block text-[11px] text-rose-600">{hint}</span>}
  </label>
)
