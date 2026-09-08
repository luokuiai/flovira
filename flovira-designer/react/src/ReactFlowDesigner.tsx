import { NodeControlEditor } from './NodeControlEditor'
import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type PointerEvent,
  type ReactNode,
} from 'react'
import {
  Download,
  LocateFixed,
  Network,
  Plus,
  Redo2,
  Save,
  Settings2,
  Timer,
  Undo2,
  Upload,
  X,
  ZoomIn,
  ZoomOut,
} from 'lucide-react'
import {
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
  findBranchMerge,
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
import { defaultDesignerUi } from './ui'
import { NODE_META } from './nodeMeta'
import { NodeHeader } from './NodeHeader'
import { BranchConditionEditor } from './BranchConditionEditor'
import { addCanvasBranch, branchSummary, insertCanvasNode, removeCanvasBranch, removeCanvasSplit, getBranchRule, setBranchRule } from './branchConditions'
import type {
  ApproverEditorType,
  ApproverRule,
  ApproverSubject,
  DesignerCapabilities,
  DesignerConditionField,
  DesignerUiAdapter,
  FloviraDefinition,
  FloviraNode,
  FloviraNodeType,
  ReactFlowDesignerProps,
  ReactFlowDesignerRef,
  DesignerResourcePage,
  DesignerResourceItem,
  SubprocessDefinition,
} from './types'


const approverEditorType = (strategy?: DesignerCapabilities['approverStrategies'][number]): ApproverEditorType => {
  if (strategy?.editorType) return strategy.editorType
  if (strategy?.selectionType === 'EXPRESSION') return 'INLINE'
  if (strategy?.selectionType === 'RESOURCE') return 'DIALOG'
  return 'NONE'
}

const approverOptionVisible = (
  option: NonNullable<DesignerCapabilities['approverStrategies'][number]['options']>[number],
  strategy: DesignerCapabilities['approverStrategies'][number],
  nodeType: FloviraNodeType,
  subjects: ApproverSubject[] = [],
): boolean => {
  if (option.nodeTypes?.length && !option.nodeTypes.includes(nodeType)) return false
  const condition = option.condition || 'ALWAYS'
  if ((condition === 'MULTIPLE' || option.code === 'approvalMode')
    && strategy.selectionType === 'RESOURCE' && strategy.resourceType === 'USER' && !strategy.relationType) {
    return strategy.multiple && new Set(subjects.map((subject) => subject.id)).size > 1
  }
  if (condition === 'ALWAYS') return true
  const cardinality = strategy.resultCardinality
    || (strategy.selectionType === 'RESOURCE' && strategy.resourceType === 'USER' && !strategy.relationType
      ? strategy.multiple ? 'ONE_OR_MORE' : 'EXACTLY_ONE'
      : 'ZERO_OR_MORE')
  if (condition === 'MULTIPLE') return cardinality === 'ONE_OR_MORE' || cardinality === 'ZERO_OR_MORE'
  return cardinality === 'ZERO_OR_ONE' || cardinality === 'ZERO_OR_MORE'
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
  if (rule.selectionType === 'EXPRESSION') return rule.expression || emptyLabel
  return rule.subjects.map((subject) => subject.name || subject.id).join('、') || emptyLabel
}

const ToolbarButton = ({
  Button,
  Tooltip,
  label,
  disabled,
  onPress,
  children,
}: {
  Button: DesignerUiAdapter['Button']
  Tooltip: NonNullable<DesignerUiAdapter['Tooltip']>
  label: string
  disabled?: boolean
  onPress: () => void
  children: ReactNode
}) => (
  <Tooltip content={label}>
    <Button
      size="icon"
      variant="text"
      ariaLabel={label}
      disabled={disabled}
      onPress={onPress}
    >
      {children}
    </Button>
  </Tooltip>
)

export const ReactFlowDesigner = forwardRef<ReactFlowDesignerRef, ReactFlowDesignerProps>(
  function ReactFlowDesigner({
    value,
    defaultValue,
    disabled = false,
    className = '',
    capabilities: configuredCapabilities,
    queryResources,
    conditionFields = [],
    queryConditionFields,
    compileBranchConditions,
    maxHistory = 50,
    onChange,
    onSave,
    renderNode,
    renderApproverEditor,
    ui,
  }, ref) {
    const components = useMemo(() => ({ ...defaultDesignerUi, ...ui }), [ui])
    const {
      Button: UiButton,
      Checkbox: UiCheckbox,
      Field: UiField,
      Input: UiInput,
      RadioGroup: UiRadioGroup,
      Select: UiSelect,
    } = components
    const UiTooltip = components.Tooltip
    const UiDropdownMenu = components.DropdownMenu
    const UiDrawer = components.Drawer
    const UiDialog = components.Dialog || defaultDesignerUi.Dialog!
    const initial = useMemo(
      () => normalizeDefinition(value ?? defaultValue ?? createInitialDefinition()),
      [],
    )
    const [definition, setDefinition] = useState<FloviraDefinition>(initial)
    const [past, setPast] = useState<FloviraDefinition[]>([])
    const [future, setFuture] = useState<FloviraDefinition[]>([])
    const [selectedCode, setSelectedCode] = useState('')
    const [loadedConditionFields, setLoadedConditionFields] = useState<readonly DesignerConditionField[]>([])
    const [conditionFieldState, setConditionFieldState] = useState<'loading' | 'ready' | 'error'>('loading')
    const [conditionFieldRetry, setConditionFieldRetry] = useState(0)
    const [selectedBranch, setSelectedBranch] = useState<{ nodeCode: string; index: number } | null>(null)
    const [zoom, setZoom] = useState(1)
    const [canvasDragging, setCanvasDragging] = useState(false)
    const canvasDragRef = useRef<{ pointerId: number; x: number; y: number; left: number; top: number } | null>(null)

    const handleCanvasPointerDown = (event: PointerEvent<HTMLDivElement>) => {
      if (event.button !== 0 || canvasDragRef.current
        || (event.target as Element).closest('button, a, input, select, textarea, [role="button"], [role="menu"], [contenteditable]')) return
      canvasDragRef.current = {
        pointerId: event.pointerId, x: event.clientX, y: event.clientY,
        left: event.currentTarget.scrollLeft, top: event.currentTarget.scrollTop,
      }
      event.currentTarget.setPointerCapture(event.pointerId)
      setCanvasDragging(true)
      event.preventDefault()
    }

    const handleCanvasPointerMove = (event: PointerEvent<HTMLDivElement>) => {
      const drag = canvasDragRef.current
      if (!drag || drag.pointerId !== event.pointerId) return
      event.currentTarget.scrollLeft = drag.left - (event.clientX - drag.x)
      event.currentTarget.scrollTop = drag.top - (event.clientY - drag.y)
    }

    const endCanvasDrag = (event: PointerEvent<HTMLDivElement>) => {
      if (canvasDragRef.current?.pointerId !== event.pointerId) return
      canvasDragRef.current = null
      setCanvasDragging(false)
      if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    }
    const [dirty, setDirty] = useState(false)
    const [subprocesses, setSubprocesses] = useState<SubprocessDefinition[]>([])
    const [subprocessState, setSubprocessState] = useState<'idle' | 'loading' | 'error'>('idle')
    const capabilities = configuredCapabilities || DEFAULT_DESIGNER_CAPABILITIES
    const [approverKeyword, setApproverKeyword] = useState('')
    const [approverPage, setApproverPage] = useState(1)
    const [approverResources, setApproverResources] = useState<DesignerResourceItem[]>([])
    const [approverTotal, setApproverTotal] = useState(0)
    const [approverResourceState, setApproverResourceState] = useState<'idle' | 'loading' | 'error'>('idle')
    const [participantPickerOpen, setParticipantPickerOpen] = useState(false)
    const [participantRuleDraft, setParticipantRuleDraft] = useState<ApproverRule | null>(null)
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
      if (!queryResources) {
        setSubprocessState('idle')
        return
      }
      let active = true
      setSubprocessState('loading')
      queryResources({ resourceType: 'SUBPROCESS', pageNum: 1, pageSize: 1000 })
        .then((response) => {
          if (!active) return
          setSubprocesses(extractSubprocesses(response))
          setSubprocessState('idle')
        })
        .catch(() => {
          if (active) setSubprocessState('error')
        })
      return () => { active = false }
    }, [queryResources])

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
      canvas.scrollTo({ top: 200, left: Math.max(0, (canvas.scrollWidth - canvas.clientWidth) / 2), behavior: 'smooth' })
    }, [])

    useEffect(() => {
      const canvas = canvasRef.current
      if (!canvas) return
      let positioned = false
      const positionCanvas = () => {
        if (positioned || !canvas.clientWidth) return
        canvas.scrollLeft = Math.max(0, (canvas.scrollWidth - canvas.clientWidth) / 2)
        canvas.scrollTop = 200
        positioned = true
      }
      positionCanvas()
      // 设计器可能先挂载在隐藏的 tab 内，等容器可见后再定位。
      if (typeof ResizeObserver === 'undefined') return
      const observer = new ResizeObserver(positionCanvas)
      observer.observe(canvas)
      return () => observer.disconnect()
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
      zoomOut: () => setZoom((current) => Math.max(0.5, Number((current - 0.1).toFixed(1)))),
      resetZoom: () => setZoom(1),
      locateStart,
    }), [commit, definition, dirty, locateStart, redo, undo])

    const branchNode = selectedBranch ? definition.nodeList.find((node) => node.nodeCode === selectedBranch.nodeCode) : undefined
    const activeBranch = branchNode && selectedBranch && branchNode.skipList[selectedBranch.index] ? selectedBranch : null
    useEffect(() => {
      if (!activeBranch || !branchNode || !queryConditionFields || branchNode.nodeType === '4') return
      let active = true
      setConditionFieldState('loading')
      setLoadedConditionFields([])
      Promise.resolve().then(() => queryConditionFields({ definition, node: branchNode, branch: branchNode.skipList[activeBranch.index] }))
        .then((fields) => {
          if (!active) return
          if (!Array.isArray(fields)) throw new Error('表单字段返回格式无效')
          setLoadedConditionFields(fields)
          setConditionFieldState('ready')
        }).catch(() => { if (active) setConditionFieldState('error') })
      return () => { active = false }
    }, [selectedBranch, definition, queryConditionFields, conditionFieldRetry])
    const openBranch = (node: FloviraNode, index: number) => {
      if (queryConditionFields) setConditionFieldState('loading')
      setSelectedCode('')
      setSelectedBranch({ nodeCode: node.nodeCode, index })
    }
    const selectedNode = definition.nodeList.find((node) => node.nodeCode === selectedCode)
    const selectedApproverRule = selectedNode?.nodeType === '8'
      ? getCarbonCopyRule(selectedNode)
      : selectedNode?.nodeType === '1' ? getApproverRule(selectedNode) : null
    const setSelectedParticipantRule = selectedNode?.nodeType === '8' ? setCarbonCopyRule : setApproverRule
    const selectedApproverStrategy = selectedApproverRule
      ? findApproverStrategy(capabilities, selectedApproverRule.strategy) || capabilities.approverStrategies[0]
      : undefined
    useEffect(() => {
      if (!selectedNode || !['1', '8'].includes(selectedNode.nodeType)
        || approverEditorType(selectedApproverStrategy) !== 'DIALOG'
        || selectedApproverStrategy?.selectionType !== 'RESOURCE'
        || !selectedApproverStrategy.resourceType
        || !participantPickerOpen
        || !queryResources) {
        setApproverResources([])
        setApproverTotal(0)
        setApproverResourceState('idle')
        return
      }
      let active = true
      setApproverResourceState('loading')
      queryResources({
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
    }, [approverKeyword, approverPage, participantPickerOpen, queryResources, selectedCode,
      selectedApproverStrategy?.code, selectedApproverStrategy?.resourceType, selectedApproverStrategy?.selectionType])
    const nodeMap = useMemo(
      () => new Map(definition.nodeList.map((node) => [node.nodeCode, node])),
      [definition],
    )
    const incomingCount = useMemo(() => {
      const counts = new Map<string, number>()
      definition.nodeList.forEach((node) => node.skipList.forEach((skip) =>
        counts.set(skip.targetNodeCode, (counts.get(skip.targetNodeCode) || 0) + 1)))
      return counts
    }, [definition])

    const changeSelected = (patch: Partial<FloviraNode>) => {
      if (!selectedNode) return
      commit(updateNode(definition, selectedNode.nodeCode, patch))
    }

    const openParticipantPicker = () => {
      if (!selectedApproverRule || approverEditorType(selectedApproverStrategy) !== 'DIALOG') return
      setApproverKeyword('')
      setApproverPage(1)
      setParticipantRuleDraft({
        ...selectedApproverRule,
        subjects: [...selectedApproverRule.subjects],
        config: selectedApproverRule.config ? { ...selectedApproverRule.config } : undefined,
      })
      setParticipantPickerOpen(true)
    }

    const confirmParticipantPicker = () => {
      if (!selectedNode || !selectedApproverStrategy || !participantRuleDraft) return
      const subjects = selectedApproverStrategy.multiple
        ? participantRuleDraft.subjects
        : participantRuleDraft.subjects.slice(0, 1)
      commit(updateNode(definition, selectedNode.nodeCode,
        setSelectedParticipantRule(selectedNode, selectedApproverStrategy.code, subjects,
          participantRuleDraft.expression || '', selectedApproverStrategy.relationType,
          selectedApproverStrategy.selectionType, participantRuleDraft.config)))
      setParticipantPickerOpen(false)
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
      const summary = summaryFor(node, subprocesses)
      const selected = selectedCode === node.nodeCode
      const deletable = !disabled && !['0', '2'].includes(node.nodeType)
      if (renderNode) {
        return <>{renderNode({ node, selected, summary })}</>
      }
      return (
        <div
          role="button"
          tabIndex={0}
          className={`frd-node frd-node--${meta.tone}`}
          onClick={() => { setSelectedBranch(null); setSelectedCode(node.nodeCode) }}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault()
              setSelectedBranch(null)
              setSelectedCode(node.nodeCode)
            }
          }}
          aria-label={`编辑节点：${node.nodeName}`}
        >
          <NodeHeader node={node}>
            {deletable && (
              <span className="frd-node__actions">
                {deletable && (
                  <button
                    type="button"
                    className="frd-node__delete"
                    aria-label={`删除节点：${node.nodeName}`}
                    onClick={(event) => {
                      event.stopPropagation()
                      commit(['3', '4', '5'].includes(node.nodeType) && findBranchMerge(definition, node.skipList.map((skip) => skip.targetNodeCode))
                        ? removeCanvasSplit(definition, node.nodeCode) : deleteNode(definition, node.nodeCode))
                      if (selected) setSelectedCode('')
                    }}
                  >
                    <X size={13} />
                  </button>
                )}
              </span>
            )}
          </NodeHeader>
          <span className="frd-node__summary">
            {summary}
          </span>
        </div>
      )
    }

    const InsertPoint = ({ after, branchIndex }: { after: FloviraNode; branchIndex?: number }) => {
      if (disabled || after.nodeType === '2') return <div className="frd-connector frd-connector--short" />
      const items = filterNodeTypes(INSERT_TYPES, capabilities).map((type) => {
        const meta = NODE_META[type]
        const MetaIcon = meta.icon
        return {
          value: type,
          label: `添加${nodeName(type)}`,
          icon: <MetaIcon size={17} aria-hidden="true" />,
          color: meta.color,
        }
      })
      return (
        <div className="frd-insert-point">
          <span className="frd-insert-point__line" />
          <UiDropdownMenu
            items={items}
            align="left"
            trigger={(
              <button
                type="button"
                className="frd-insert-point__trigger"
                aria-label={branchIndex === undefined ? `在 ${after.nodeName} 后添加节点` : `在分支 ${after.skipList[branchIndex].skipName || branchIndex + 1} 下添加节点`}
              >
                <Plus size={12} />
              </button>
            )}
            onSelect={(value) => {
              const next = insertCanvasNode(definition, after.nodeCode, value as FloviraNodeType, branchIndex)
              const added = next.nodeList.find((item) => !definition.nodeList.some((old) => old.nodeCode === item.nodeCode))
              commit(next)
              if (added) {
                if (['3', '4', '5'].includes(added.nodeType)) openBranch(added, 0)
                else { setSelectedBranch(null); setSelectedCode(added.nodeCode) }
              }
            }}
          />
        </div>
      )
    }

    const renderPath = (code: string, visited: Set<string>, stopBeforeCode?: string): ReactNode => {
      if (code === stopBeforeCode) return null
      const node = nodeMap.get(code)
      if (!node) return null
      if (visited.has(code)) {
        return (
          <div className="frd-path">
            <div className="frd-connector frd-connector--short" />
            <div className="frd-merge-label">
              汇合至 {node.nodeName}
            </div>
          </div>
        )
      }
      const nextVisited = new Set(visited)
      nextVisited.add(code)
      const outgoing = node.skipList.filter((skip) => skip.skipType !== 'REJECT' && nodeMap.has(skip.targetNodeCode))
      const branchMergeCode = outgoing.length > 1
        ? findBranchMerge(definition, outgoing.map((skip) => skip.targetNodeCode))
        : undefined
      return (
        <div className="frd-path" key={`${code}-${visited.size}`}>
          {!(outgoing.length > 1 && ['3', '4', '5'].includes(node.nodeType)) && <NodeCard node={node} />}
          {outgoing.length === 0 ? null : outgoing.length === 1 ? (
            <>
              <InsertPoint after={node} />
              {renderPath(outgoing[0].targetNodeCode, nextVisited, stopBeforeCode)}
            </>
          ) : (
            <>
              <div className="frd-connector frd-connector--fork" />
              <div className={`frd-branches${branchMergeCode ? ' frd-branches--merge' : ''}`}>
              <div className="frd-branch-add">
                {!disabled && <UiButton variant="text" size="compact" disabled={!branchMergeCode} onPress={() => {
                  const next = addCanvasBranch(definition, node.nodeCode)
                  commit(next)
                  const updated = next.nodeList.find((item) => item.nodeCode === node.nodeCode)!
                  openBranch(updated, updated.skipList.findIndex((skip) => !node.skipList.some((old) => old.id === skip.id)))
                }}><Plus size={13} />添加{node.nodeType === '4' ? '并行' : '条件'}分支</UiButton>}
              </div>
                <div className="flovira-react-branch-grid">
                  {outgoing.map((skip) => (
                    <div className="frd-branch" key={String(skip.id || node.skipList.indexOf(skip))}>
                      <span className="frd-branch__connector" aria-hidden="true" />
                      <div className="frd-condition-card-wrapper">
                      <button type="button" className={`frd-condition-card frd-node--${NODE_META[node.nodeType]?.tone || 'exclusive'}`} data-default={getBranchRule(node, node.skipList.indexOf(skip)).mode === 'default'}
                        aria-label={`配置分支：${skip.skipName || `分支 ${node.skipList.indexOf(skip) + 1}`}`}
                        onClick={() => openBranch(node, node.skipList.indexOf(skip))}>
                        <span className="frd-condition-card__header">
                          <span className="frd-condition-card__name-wrapper">
                            <UiTooltip content={skip.skipName || `分支 ${node.skipList.indexOf(skip) + 1}`}>
                              <span className="frd-condition-card__name">{skip.skipName || `分支 ${node.skipList.indexOf(skip) + 1}`}</span>
                            </UiTooltip>
                          </span>
                          {node.nodeType !== '4' && <span className="frd-condition-card__priority">{getBranchRule(node, node.skipList.indexOf(skip)).mode === 'default' ? '默认' : `优先级 ${node.skipList.indexOf(skip) + 1}`}</span>}
                        </span>
                        <span className="frd-condition-card__body">
                          <UiTooltip content={branchSummary(node, node.skipList.indexOf(skip))}>
                            <span className="frd-condition-card__summary">{branchSummary(node, node.skipList.indexOf(skip))}</span>
                          </UiTooltip>
                        </span>
                      </button>
                      {!disabled && <button type="button" className="frd-condition-delete"
                        aria-label={`删除分支：${skip.skipName || node.skipList.indexOf(skip) + 1}`}
                        onClick={() => { commit(removeCanvasBranch(definition, node.nodeCode, node.skipList.indexOf(skip))); setSelectedBranch(null) }}><X size={13} /></button>}
                      </div>
                      <InsertPoint after={node} branchIndex={node.skipList.indexOf(skip)} />
                      {renderPath(
                        skip.targetNodeCode,
                        nextVisited,
                        branchMergeCode ?? stopBeforeCode,
                      )}
                      {branchMergeCode && <span className="frd-branch__merge-tail" aria-hidden="true" />}
                    </div>
                  ))}
                </div>
              </div>
              {branchMergeCode && branchMergeCode !== stopBeforeCode && (
                <>
                  <div className="frd-merge-flow" aria-hidden="true" />
                  {renderPath(branchMergeCode, nextVisited, stopBeforeCode)}
                </>
              )}
            </>
          )}
        </div>
      )
    }

    const start = definition.nodeList.find((node) => node.nodeType === '0') || definition.nodeList[0]
    const validation = validateDefinition(definition)
    const updateInlineApproverRule = (rule: ApproverRule) => {
      if (!selectedNode || !selectedApproverStrategy) return
      const subjects = selectedApproverStrategy.multiple ? rule.subjects : rule.subjects.slice(0, 1)
      commit(updateNode(definition, selectedNode.nodeCode,
        setSelectedParticipantRule(selectedNode, selectedApproverStrategy.code, subjects,
          rule.expression || '', selectedApproverStrategy.relationType,
          selectedApproverStrategy.selectionType, rule.config)))
    }
    const editorRenderer = renderApproverEditor
    const editorContext = selectedNode && selectedApproverStrategy && selectedApproverRule
      ? {
          node: selectedNode,
          strategy: selectedApproverStrategy,
          rule: selectedApproverRule,
          selected: selectedApproverRule.subjects,
          multiple: selectedApproverStrategy.multiple,
          disabled,
          onChange: (subjects: ApproverSubject[]) => updateInlineApproverRule({
            ...selectedApproverRule,
            subjects: selectedApproverStrategy.multiple ? subjects : subjects.slice(0, 1),
          }),
          onRuleChange: updateInlineApproverRule,
        }
      : undefined
    const dialogEditorContext = editorContext && participantRuleDraft
      ? {
          ...editorContext,
          rule: participantRuleDraft,
          selected: participantRuleDraft.subjects,
          onChange: (subjects: ApproverSubject[]) => setParticipantRuleDraft({
            ...participantRuleDraft,
            subjects: selectedApproverStrategy?.multiple ? subjects : subjects.slice(0, 1),
          }),
          onRuleChange: (rule: ApproverRule) => setParticipantRuleDraft({
            ...rule,
            strategy: selectedApproverStrategy?.code || rule.strategy,
            selectionType: selectedApproverStrategy?.selectionType || rule.selectionType,
            relationType: selectedApproverStrategy?.relationType,
            subjects: selectedApproverStrategy?.multiple ? rule.subjects : rule.subjects.slice(0, 1),
          }),
        }
      : undefined
    const customDialogEditor = participantPickerOpen && dialogEditorContext
      ? editorRenderer?.(dialogEditorContext)
      : undefined
    const customInlineEditor = approverEditorType(selectedApproverStrategy) === 'INLINE' && editorContext
      ? editorRenderer?.(editorContext)
      : undefined

    return (
      <section className={`flovira-react-designer ${className}`}>
        <header className="frd-header">
          <div className="frd-heading">
            <div className="frd-heading__icon">
              <Network size={17} />
            </div>
            <div className="frd-heading__text">
              <h2>{definition.flowName || '流程设计'}</h2>
              <p>{definition.flowCode || '未设置流程编码'}</p>
            </div>
            {dirty && (
              <UiTooltip content="有未保存修改">
                <span className="frd-dirty-indicator" aria-label="有未保存修改" />
              </UiTooltip>
            )}
          </div>
          <div className="frd-toolbar">
            <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="撤销" disabled={!past.length || disabled} onPress={undo}><Undo2 size={16} /></ToolbarButton>
            <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="重做" disabled={!future.length || disabled} onPress={redo}><Redo2 size={16} /></ToolbarButton>
            <span className="frd-toolbar__divider" />
            <input ref={importRef} className="frd-visually-hidden" type="file" accept="application/json,.json" onChange={handleFile} />
            <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="导入 JSON" disabled={disabled} onPress={() => importRef.current?.click()}><Upload size={16} /></ToolbarButton>
            <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="导出 JSON" onPress={exportJson}><Download size={16} /></ToolbarButton>
            {onSave && (
              <UiButton
                variant="primary"
                disabled={disabled || validation.issues.some((issue) => ['BRANCH_CONDITION_REQUIRED', 'VOTE_RATIO_INVALID', 'REJECT_TARGET_INVALID'].includes(issue.code))}
                className="frd-save-button"
                onPress={async () => {
                  await onSave(definition, serializeDefinition(definition))
                  setDirty(false)
                }}
              >
                <Save size={14} />保存
              </UiButton>
            )}
          </div>
        </header>

        <div className="frd-workspace">
          <div className="frd-canvas-shell">
            <div ref={canvasRef} className="flovira-react-canvas" data-dragging={canvasDragging}
              onPointerDown={handleCanvasPointerDown} onPointerMove={handleCanvasPointerMove}
              onPointerUp={endCanvasDrag} onPointerCancel={endCanvasDrag} onLostPointerCapture={endCanvasDrag}>
              <div className="frd-canvas-workspace">
              <div
                className="frd-canvas-content"
                style={{ transform: `scale(${zoom})`, transformOrigin: 'top center' }}
              >
                {start ? renderPath(start.nodeCode, new Set()) : (
                  <div className="frd-error-text">流程缺少开始节点</div>
                )}
              </div>
              </div>
            </div>
            <div className="frd-zoom-controls">
              <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="缩小" onPress={() => setZoom((current) => Math.max(0.5, Number((current - 0.1).toFixed(1))))}><ZoomOut size={16} /></ToolbarButton>
              <UiButton variant="text" size="compact" ariaLabel="重置缩放" onPress={() => setZoom(1)}>{Math.round(zoom * 100)}%</UiButton>
              <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="放大" onPress={() => setZoom((current) => Math.min(1.5, current + 0.1))}><ZoomIn size={16} /></ToolbarButton>
              <ToolbarButton Button={UiButton} Tooltip={UiTooltip} label="定位开始节点" onPress={locateStart}><LocateFixed size={16} /></ToolbarButton>
            </div>
            {!validation.valid && (
              <div className="frd-validation-alert">
                {validation.issues.length} 项配置待完善：{validation.issues[0].message}
              </div>
            )}
          </div>

          <UiDrawer open={Boolean(activeBranch)} title={branchNode && activeBranch ? branchNode.skipList[activeBranch.index].skipName || '分支条件' : '分支条件'}
            width={560} ariaLabel="分支条件" onClose={() => setSelectedBranch(null)}>
            {branchNode && activeBranch && queryConditionFields && branchNode.nodeType !== '4' && conditionFieldState === 'loading'
              ? <p role="status">正在加载表单字段…</p>
              : branchNode && activeBranch && queryConditionFields && branchNode.nodeType !== '4' && conditionFieldState === 'error'
                ? <div><p role="alert">表单字段加载失败</p><UiButton onPress={() => setConditionFieldRetry((value) => value + 1)}>重新加载</UiButton></div>
                : branchNode && activeBranch && <BranchConditionEditor
              key={`${branchNode.nodeCode}-${activeBranch.index}-${JSON.stringify(branchNode)}`}
              node={branchNode} index={activeBranch.index} fields={queryConditionFields ? loadedConditionFields : conditionFields} ui={components} disabled={disabled}
              compile={compileBranchConditions} onSave={(name, rule) => {
                commit(updateNode(definition, branchNode.nodeCode, setBranchRule(branchNode, activeBranch.index, name, rule)))
                setSelectedBranch(null)
              }} />}
          </UiDrawer>
          <UiDrawer
            open={Boolean(selectedNode)}
            title={(
              <span className="frd-settings-panel__title">
                <Settings2 size={17} />
                <span className="frd-settings-panel__title-copy">
                  <strong>{selectedNode?.nodeName || '节点设置'}</strong>
                  <small>{selectedNode ? `${NODE_META[selectedNode.nodeType]?.label || '节点'}配置` : '节点配置'}</small>
                </span>
              </span>
            )}
            width={460}
            ariaLabel="节点设置"
            onClose={() => {
              setParticipantPickerOpen(false)
              setSelectedCode('')
            }}
          >
            {selectedNode && (
              <div className="frd-settings-panel">
              <div className="frd-settings-group">
                <h4>基础信息</h4>
                <UiField label="节点名称">
                  <UiInput value={selectedNode.nodeName} disabled={disabled} ariaLabel="节点名称" onValueChange={(value) => changeSelected({ nodeName: value })} />
                </UiField>
                <UiField label="节点编码">
                  <UiInput value={selectedNode.nodeCode} disabled onValueChange={() => undefined} />
                </UiField>
              </div>
              {['1', '8'].includes(selectedNode.nodeType) && (
                <div className="frd-settings-group">
                  <h4>{selectedNode.nodeType === '8' ? '抄送策略' : '审批策略'}</h4>
                  <UiField label={selectedNode.nodeType === '8' ? '抄送人类型' : '办理人类型'}>
                    <UiSelect
                      value={String(selectedApproverStrategy?.code || '')}
                      disabled={disabled}
                      options={approverStrategyOptions(capabilities).map((strategy) => ({
                        value: strategy.value,
                        label: strategy.label,
                      }))}
                      onValueChange={(value) => {
                        const strategy = findApproverStrategy(capabilities, value)
                        const config = strategy?.options
                          ?.filter((option) => approverOptionVisible(option, strategy, selectedNode.nodeType))
                          .reduce<Record<string, unknown>>((result, option) => {
                          const defaultValue = option.defaultValue ?? option.choices[0]?.value
                          if (defaultValue !== undefined) result[option.code] = defaultValue
                          return result
                        }, {})
                        setApproverKeyword('')
                        setApproverPage(1)
                        commit(updateNode(definition, selectedNode.nodeCode,
                          setSelectedParticipantRule(selectedNode, value, [], '', strategy?.relationType,
                            strategy?.selectionType || 'RESOURCE', config)))
                      }}
                    />
                  </UiField>
                  {selectedApproverStrategy && approverEditorType(selectedApproverStrategy) === 'INLINE' ? (
                    customInlineEditor ?? (selectedApproverStrategy?.selectionType === 'EXPRESSION' ? (
                    <UiField label={selectedNode.nodeType === '8' ? '抄送人表达式' : '办理人表达式'}>
                      <UiInput
                        value={String(selectedApproverRule?.expression || '')}
                        disabled={disabled}
                        placeholder="例如 ${approverIds}"
                        onValueChange={(value) => commit(updateNode(definition, selectedNode.nodeCode,
                          setSelectedParticipantRule(selectedNode, selectedApproverStrategy.code, [], value,
                            selectedApproverStrategy.relationType, selectedApproverStrategy.selectionType,
                            selectedApproverRule?.config)))}
                      />
                    </UiField>
                    ) : null)
                  ) : selectedApproverStrategy && approverEditorType(selectedApproverStrategy) === 'DIALOG' ? (
                    <div className="frd-participant-picker">
                      <UiField label={selectedApproverStrategy.name}>
                        <div className="frd-participant-launcher">
                          <UiTooltip content={`选择${selectedApproverStrategy.name}`}>
                            <UiButton
                              size="icon"
                              variant="default"
                              className="frd-participant-launcher__add"
                              disabled={disabled}
                              ariaLabel={`选择${selectedApproverStrategy.name}`}
                              onPress={openParticipantPicker}
                            >
                              <Plus size={14} />
                            </UiButton>
                          </UiTooltip>
                          {selectedApproverRule?.subjects.map((subject) => (
                            <span className="frd-participant-chip" key={`${subject.type}:${subject.id}`}>
                              <span>{subject.name || subject.id}</span>
                              <UiButton
                                size="icon"
                                variant="text"
                                className="frd-participant-chip__remove"
                                disabled={disabled}
                                ariaLabel={`移除${subject.name || subject.id}`}
                                onPress={() => commit(updateNode(definition, selectedNode.nodeCode,
                                  setSelectedParticipantRule(selectedNode, selectedApproverStrategy.code,
                                    selectedApproverRule.subjects.filter((item) => item.id !== subject.id), '',
                                    selectedApproverStrategy.relationType, selectedApproverStrategy.selectionType,
                                    selectedApproverRule.config)))}
                              >
                                <X size={12} />
                              </UiButton>
                            </span>
                          ))}
                        </div>
                      </UiField>
                    </div>
                  ) : null}
                  {selectedApproverStrategy?.options
                    ?.filter((option) => approverOptionVisible(option, selectedApproverStrategy, selectedNode.nodeType, selectedApproverRule?.subjects))
                    .map((option) => (
                    <UiField label={option.name} key={option.code}>
                      <UiRadioGroup
                        value={String(selectedApproverRule?.config?.[option.code]
                          ?? option.defaultValue
                          ?? option.choices[0]?.value
                          ?? '')}
                        disabled={disabled}
                        ariaLabel={option.name}
                        options={option.code === 'approvalMode'
                          ? option.choices.map((choice) => ({
                            ...choice,
                            label: ({ COUNTERSIGN: '会签', OR: '或签', VOTE: '票签' } as Record<string, string>)[choice.value] || choice.label,
                          })).sort((a, b) => {
                            const order = ['COUNTERSIGN', 'OR', 'VOTE']
                            const rank = (value: string) => order.includes(value) ? order.indexOf(value) : order.length
                            return rank(a.value) - rank(b.value)
                          })
                          : option.choices}
                        onValueChange={(value) => selectedApproverRule && updateInlineApproverRule({
                          ...selectedApproverRule,
                          config: { ...selectedApproverRule.config, [option.code]: value },
                        })}
                      />
                    </UiField>
                    ))}
                  {selectedNode.nodeType === '1' && (
                    selectedApproverRule?.config?.approvalMode === 'VOTE'
                    && selectedApproverStrategy?.options?.some((option) => option.code === 'approvalMode'
                      && approverOptionVisible(option, selectedApproverStrategy, selectedNode.nodeType, selectedApproverRule.subjects)) && (
                      <UiField label="通过比例（%）" hint="同意人数占比达到此比例即通过；必须大于 0、小于 100，全部同意请选会签。">
                        <UiInput ariaLabel="通过比例（%）" type="number"
                          value={Number.isFinite(Number(selectedNode.nodeRatio)) ? String(selectedNode.nodeRatio ?? '') : ''}
                          disabled={disabled} placeholder="例如 60"
                          onValueChange={(value) => changeSelected({ nodeRatio: value })} />
                        {validation.issues.some((issue) => issue.nodeCode === selectedNode.nodeCode && issue.code === 'VOTE_RATIO_INVALID')
                          && <p role="alert" className="frd-condition-error">请输入大于 0、小于 100 的通过比例</p>}
                        {!Number.isFinite(Number(selectedNode.nodeRatio)) && <p className="frd-condition-hint">当前票签规则：{selectedNode.nodeRatio}。填写比例后将替换此规则。</p>}
                      </UiField>
                    )
                  )}
                  {selectedNode.nodeType === '1' && <NodeControlEditor definition={definition} node={selectedNode}
                    disabled={disabled} ui={components}
                    onChange={(node) => commit(updateNode(definition, selectedNode.nodeCode, node))} />}

                </div>
              )}
              {selectedNode.nodeType === '6' && (
                <UiField label="固定子流程" hint={subprocessState === 'error' ? '子流程列表加载失败，原值仍会保留' : undefined}>
                  <UiSelect
                    value={String(getSubprocessConfig(selectedNode).fixedChildFlowCode || '')}
                    disabled={disabled || subprocessState === 'loading'}
                    options={[
                      { value: '', label: subprocessState === 'loading' ? '加载中...' : '请选择已发布流程' },
                      ...subprocesses.map((flow) => ({ value: flow.flowCode, label: flow.flowName })),
                    ]}
                    onValueChange={(value) => commit(updateNode(definition, selectedNode.nodeCode, setSubprocessConfig(selectedNode, value)))}
                  />
                </UiField>
              )}
              {selectedNode.nodeType === '7' && (
                <UiField label="等待标识" hint="业务系统使用该标识恢复等待任务">
                  <UiInput
                    value={String(getWaitConfig(selectedNode).waitKey || '')}
                    disabled={disabled}
                    placeholder="例如 ORDER_PAID"
                    onValueChange={(value) => commit(updateNode(
                      definition,
                      selectedNode.nodeCode,
                      setWaitConfig(selectedNode, value.trim()),
                    ))}
                  />
                </UiField>
              )}
              {capabilities.timeoutNodeTypes.includes(selectedNode.nodeType) && (() => {
                const timeout = getTimeoutConfig(selectedNode)
                const enabled = Boolean(timeout.enabled)
                return (
                  <div className="frd-settings-section">
                    <div className="frd-settings-section__header">
                      <span><Timer size={14} />节点超时</span>
                      <UiCheckbox
                        checked={enabled}
                        disabled={disabled}
                        ariaLabel="启用节点超时"
                        onCheckedChange={(checked) => commit(updateNode(
                          definition,
                          selectedNode.nodeCode,
                          setTimeoutConfig(selectedNode, { enabled: checked }),
                        ))}
                      />
                    </div>
                    {enabled && (
                      <>
                        <UiField label="超时时长">
                          <UiInput
                            type="number"
                            min={1}
                            value={Number(timeout.duration || 1)}
                            disabled={disabled}
                            onValueChange={(value) => commit(updateNode(definition, selectedNode.nodeCode,
                              setTimeoutConfig(selectedNode, { duration: Math.max(1, Number(value) || 1) })))}
                          />
                        </UiField>
                        <UiField label="时间单位">
                          <UiRadioGroup
                            value={String(timeout.durationUnit || 'HOURS')}
                            disabled={disabled}
                            ariaLabel="时间单位"
                            options={[
                              { value: 'MINUTES', label: '分钟' },
                              { value: 'HOURS', label: '小时' },
                              { value: 'DAYS', label: '天' },
                            ]}
                            onValueChange={(value) => commit(updateNode(definition, selectedNode.nodeCode,
                              setTimeoutConfig(selectedNode, { durationUnit: value })))}
                          />
                        </UiField>
                        <UiField label="超时动作">
                          <UiRadioGroup
                            value={String(timeout.action || (selectedNode.nodeType === '7' ? 'RESUME_WAIT' : 'AUTO_PASS'))}
                            disabled={disabled}
                            ariaLabel="超时动作"
                            options={selectedNode.nodeType === '7'
                              ? [{ value: 'RESUME_WAIT', label: '恢复等待并继续' }]
                              : [
                                  { value: 'AUTO_PASS', label: '自动通过' },
                                  { value: 'AUTO_REJECT', label: '自动退回' },
                                ]}
                            onValueChange={(value) => commit(updateNode(definition, selectedNode.nodeCode,
                              setTimeoutConfig(selectedNode, { action: value })))}
                          />
                        </UiField>
                      </>
                    )}
                  </div>
                )
              })()}
              {['3', '4', '5'].includes(selectedNode.nodeType) && (
                <div className="frd-settings-section">
                  <div className="frd-settings-section__header">
                    <span>分支</span>
                    <UiButton size="compact" variant="text" disabled={disabled || !findBranchMerge(definition, selectedNode.skipList.map((skip) => skip.targetNodeCode))} className="frd-add-branch" onPress={() => commit(addCanvasBranch(definition, selectedNode.nodeCode))}><Plus size={13} />增加分支</UiButton>
                  </div>
                  {selectedNode.skipList.map((skip, index) => (
                    <UiButton key={String(skip.id || index)} variant="text" onPress={() => openBranch(selectedNode, index)}>
                      {skip.skipName || `分支 ${index + 1}`}：{branchSummary(selectedNode, index)}
                    </UiButton>
                  ))}
                </div>
              )}
              {incomingCount.get(selectedNode.nodeCode) && incomingCount.get(selectedNode.nodeCode)! > 1 ? (
                <p className="frd-merge-note">该节点是 {incomingCount.get(selectedNode.nodeCode)} 条分支的汇合点。</p>
              ) : null}
              </div>
            )}
          </UiDrawer>
          {selectedNode && selectedApproverRule
            && selectedApproverStrategy
            && approverEditorType(selectedApproverStrategy) === 'DIALOG' && (
            <UiDialog
              open={participantPickerOpen}
              title={selectedApproverStrategy.resourceType === 'USER'
                ? selectedNode.nodeType === '8' ? '选择抄送人员' : '选择审批人员'
                : `选择${selectedApproverStrategy.name}`}
              width={600}
              ariaLabel="人员选择"
              onClose={() => setParticipantPickerOpen(false)}
              onConfirm={confirmParticipantPicker}
            >
              {participantPickerOpen && (customDialogEditor ?? (selectedApproverStrategy.selectionType === 'RESOURCE' ? (
                <div className="frd-default-participant-picker">
                  <UiInput
                    value={approverKeyword}
                    disabled={disabled || !queryResources}
                    ariaLabel={`搜索${selectedApproverStrategy.name}`}
                    placeholder={`搜索${selectedApproverStrategy.name}`}
                    onValueChange={(value) => {
                      setApproverKeyword(value)
                      setApproverPage(1)
                    }}
                  />
                  <div className="frd-resource-list">
                    {approverResourceState === 'loading' && <p className="frd-resource-list__state">加载中...</p>}
                    {approverResourceState === 'error' && <p className="frd-resource-list__state frd-error-text">人员数据加载失败</p>}
                    {approverResourceState === 'idle' && approverResources.length === 0 && (
                      <p className="frd-resource-list__state">{queryResources ? '暂无可选数据' : '未提供人员选择数据'}</p>
                    )}
                    {selectedApproverStrategy.multiple ? approverResources.map((item) => {
                      const selected = participantRuleDraft?.subjects.some((subject) => subject.id === item.id) || false
                      return (
                        <UiCheckbox
                          key={`${item.resourceType}:${item.id}`}
                          className="frd-resource-list__item"
                          checked={selected}
                          disabled={disabled || item.disabled}
                          onCheckedChange={() => participantRuleDraft && setParticipantRuleDraft({
                            ...participantRuleDraft,
                            subjects: selected
                              ? participantRuleDraft.subjects.filter((subject) => subject.id !== item.id)
                              : [...participantRuleDraft.subjects, { id: item.id, type: item.resourceType, name: item.name }],
                          })}
                        >
                          <span className="frd-resource-list__name">{item.name}</span>
                          {item.code && <span className="frd-resource-list__code">{item.code}</span>}
                        </UiCheckbox>
                      )
                    }) : (
                      <UiRadioGroup
                        className="frd-resource-list__radio-group"
                        direction="vertical"
                        value={participantRuleDraft?.subjects[0]?.id || ''}
                        disabled={disabled}
                        ariaLabel={`选择${selectedApproverStrategy.name}`}
                        options={approverResources.map((item) => ({
                          value: item.id,
                          disabled: item.disabled,
                          label: (
                            <span className="frd-resource-list__radio-content">
                              <span className="frd-resource-list__name">{item.name}</span>
                              {item.code && <span className="frd-resource-list__code">{item.code}</span>}
                            </span>
                          ),
                        }))}
                        onValueChange={(value) => {
                          const item = approverResources.find((resource) => resource.id === value)
                          if (item && participantRuleDraft) setParticipantRuleDraft({
                            ...participantRuleDraft,
                            subjects: [{ id: item.id, type: item.resourceType, name: item.name }],
                          })
                        }}
                      />
                    )}
                  </div>
                  {approverTotal > 20 && (
                    <div className="frd-pagination">
                      <UiButton size="compact" variant="text" disabled={approverPage <= 1} onPress={() => setApproverPage((page) => Math.max(1, page - 1))}>上一页</UiButton>
                      <span>{approverPage} / {Math.ceil(approverTotal / 20)}</span>
                      <UiButton size="compact" variant="text" disabled={approverPage * 20 >= approverTotal} onPress={() => setApproverPage((page) => page + 1)}>下一页</UiButton>
                    </div>
                  )}
                </div>
              ) : null))}
            </UiDialog>
          )}
        </div>
      </section>
    )
  },
)
