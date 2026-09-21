import { NodeControlEditor } from "./NodeControlEditor";
import { FormPermissionEditor } from "./FormPermissionEditor";
import { TimeoutFormField } from "./TimeoutFormField";
import type { DesignerFormField } from "./formPermissions";
import {
  Fragment,
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useId,
  useMemo,
  useRef,
  useState,
  type PointerEvent,
  type ReactNode,
} from "react";
import {
  CircleAlert,
  CircleHelp,
  LocateFixed,
  Network,
  Plus,
  Redo2,
  Settings2,
  Undo2,
  X,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import {
  approverStrategyOptions,
  createInitialDefinition,
  DEFAULT_DESIGNER_CAPABILITIES,
  deleteNode,
  filterNodeTypes,
  findApproverStrategy,
  getApproverRule,
  getCarbonCopyRule,
  getSubmitterRule,
  setSubmitterRule,
  submitterStrategies,
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
} from "./model";
import { SubprocessField } from "./SubprocessField";
import { defaultDesignerUi } from "./ui";
import { NODE_META } from "./nodeMeta";
import { NodeHeader } from "./NodeHeader";
import { BranchConditionEditor } from "./BranchConditionEditor";
import {
  addCanvasBranch,
  branchSummary,
  insertCanvasNode,
  insertAfterBranchMerge,
  removeCanvasBranch,
  removeCanvasSplit,
  getBranchRule,
  setBranchRule,
} from "./branchConditions";
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
} from "./types";

const EMPTY_FORM_FIELDS: readonly DesignerFormField[] = [];

const approverEditorType = (
  strategy?: DesignerCapabilities["approverStrategies"][number],
): ApproverEditorType => {
  if (strategy?.editorType) return strategy.editorType;
  if (strategy?.selectionType === "EXPRESSION") return "INLINE";
  if (strategy?.selectionType === "RESOURCE") return "DIALOG";
  return "NONE";
};

const approverOptionVisible = (
  option: NonNullable<
    DesignerCapabilities["approverStrategies"][number]["options"]
  >[number],
  strategy: DesignerCapabilities["approverStrategies"][number],
  nodeType: FloviraNodeType,
  subjects: ApproverSubject[] = [],
): boolean => {
  if (option.nodeTypes?.length && !option.nodeTypes.includes(nodeType))
    return false;
  const condition = option.condition || "ALWAYS";
  if (
    (condition === "MULTIPLE" || option.code === "approvalMode") &&
    strategy.selectionType === "RESOURCE" &&
    strategy.resourceType === "USER" &&
    !strategy.relationType
  ) {
    return (
      strategy.multiple &&
      new Set(subjects.map((subject) => subject.id)).size > 1
    );
  }
  if (condition === "ALWAYS") return true;
  const cardinality =
    strategy.resultCardinality ||
    (strategy.selectionType === "RESOURCE" &&
    strategy.resourceType === "USER" &&
    !strategy.relationType
      ? strategy.multiple
        ? "ONE_OR_MORE"
        : "EXACTLY_ONE"
      : "ZERO_OR_MORE");
  if (condition === "MULTIPLE")
    return cardinality === "ONE_OR_MORE" || cardinality === "ZERO_OR_MORE";
  return cardinality === "ZERO_OR_ONE" || cardinality === "ZERO_OR_MORE";
};

const INSERT_TYPES: FloviraNodeType[] = ["1", "8", "7", "3", "4", "5", "6"];

const selectionHint = (
  name: string,
  strategy?: DesignerCapabilities["approverStrategies"][number],
) => {
  const max = strategy?.multiple === false ? 1 : strategy?.maxSubjects;
  return max == null
    ? name
    : `${name}（最多 ${max} ${strategy?.resourceType === "USER" ? "人" : "项"}）`;
};

const exceedsSelectionLimit = (
  subjects: ApproverSubject[],
  strategy: DesignerCapabilities["approverStrategies"][number],
) =>
  strategy.maxSubjects != null &&
  (!Number.isInteger(strategy.maxSubjects) ||
    strategy.maxSubjects < 1 ||
    new Set(subjects.map((subject) => `${subject.type}:${subject.id}`)).size >
      strategy.maxSubjects);

const extractResourcePage = (
  value: DesignerResourcePage | { data?: DesignerResourcePage },
): DesignerResourcePage => {
  const envelope = value as { data?: DesignerResourcePage };
  return (
    envelope.data || (value as DesignerResourcePage) || { items: [], total: 0 }
  );
};

const summaryFor = (
  node: FloviraNode,
  capabilities: DesignerCapabilities,
): string => {
  if (node.nodeType === "0") {
    const rule = getSubmitterRule(node);
    if (rule.strategy === "ALL") return "全员";
    return (
      rule.subjects.map((subject) => subject.name || subject.id).join("、") ||
      rule.expression ||
      (rule.selectionType === "RELATION" &&
        submitterStrategies(capabilities).find(
          (strategy) => strategy.code === rule.strategy,
        )?.name) ||
      "未配置提交范围"
    );
  }
  if (node.nodeType === "2") return "系统";
  if (node.nodeType === "6") {
    const config = getSubprocessConfig(node);
    return String(config.fixedChildFlowName || config.fixedChildFlowCode || "未选择固定子流程");
  }
  if (node.nodeType === "7")
    return String(getWaitConfig(node).waitKey || "未配置等待标识");
  if (["3", "4", "5"].includes(node.nodeType))
    return `${node.skipList.length} 条分支`;
  const rule =
    node.nodeType === "8" ? getCarbonCopyRule(node) : getApproverRule(node);
  const emptyLabel = node.nodeType === "8" ? "未配置抄送人" : "未配置办理人";
  if (rule.selectionType === "EXPRESSION") return rule.expression || emptyLabel;
  return (
    rule.subjects.map((subject) => subject.name || subject.id).join("、") ||
    emptyLabel
  );
};

const ToolbarButton = ({
  Button,
  Tooltip,
  label,
  disabled,
  onPress,
  children,
}: {
  Button: DesignerUiAdapter["Button"];
  Tooltip: NonNullable<DesignerUiAdapter["Tooltip"]>;
  label: string;
  disabled?: boolean;
  onPress: () => void;
  children: ReactNode;
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
);

const FieldHelpLabel = ({
  label,
  help,
  Tooltip,
}: {
  label: string;
  help: string;
  Tooltip: NonNullable<DesignerUiAdapter["Tooltip"]>;
}) => (
  <span className="frd-field-help-label">
    {label}
    <Tooltip content={help} placement="top">
      <span
        className="frd-field-help-label__icon"
        tabIndex={0}
        aria-label={`${label}说明`}
      >
        <CircleHelp size={14} aria-hidden="true" />
      </span>
    </Tooltip>
  </span>
);

export const ReactFlowDesigner = forwardRef<
  ReactFlowDesignerRef,
  ReactFlowDesignerProps
>(function ReactFlowDesigner(
  {
    value,
    defaultValue,
    disabled = false,
    appearance = "standalone",
    className = "",
    capabilities: configuredCapabilities,
    queryResources,
    conditionFields = [],
    queryConditionFields,
    compileBranchConditions,
    maxHistory = 50,
    onChange,
    toolbar,
    renderToolbar,
    renderNode,
    renderApproverEditor,
    onSelectApprover,
    formFields,
    queryFormFields,
    ui,
  },
  ref,
) {
  const components = useMemo(() => ({ ...defaultDesignerUi, ...ui }), [ui]);
  const {
    Button: UiButton,
    Checkbox: UiCheckbox,
    Field: UiField,
    Input: UiInput,
    RadioGroup: UiRadioGroup,
    Select: UiSelect,
  } = components;
  const UiTooltip = components.Tooltip;
  const UiDropdownMenu = components.DropdownMenu;
  const UiDrawer = components.Drawer;
  const UiTabs = components.Tabs || defaultDesignerUi.Tabs!;
  const UiDialog = components.Dialog || defaultDesignerUi.Dialog!;
  const initial = useMemo(
    () =>
      normalizeDefinition(value ?? defaultValue ?? createInitialDefinition()),
    [],
  );
  const [definition, setDefinition] = useState<FloviraDefinition>(initial);
  const [past, setPast] = useState<FloviraDefinition[]>([]);
  const [future, setFuture] = useState<FloviraDefinition[]>([]);
  const [selectedCode, setSelectedCodeState] = useState("");
  const [nodeEdit, setNodeEdit] = useState<{
    base: FloviraDefinition;
    draft: FloviraDefinition;
  } | null>(null);
  const nodeDefinition =
    nodeEdit?.base === definition ? nodeEdit.draft : definition;
  const setSelectedCode = (code: string, source = definition) => {
    setSelectedCodeState(code);
    setNodeEdit(
      code ? { base: source, draft: JSON.parse(JSON.stringify(source)) } : null,
    );
    setParticipantPickerOpen(false);
  };
  const commitNode = (next: FloviraDefinition) => {
    if (disabled) return;
    setNodeEdit((current) =>
      current?.base === definition ? { ...current, draft: next } : current,
    );
  };
  useEffect(() => {
    if (!nodeEdit || (nodeEdit.base === definition && !disabled)) return;
    setSelectedCodeState("");
    setNodeEdit(null);
    setParticipantPickerOpen(false);
    setSelectedBranch((current) => (current?.fromNode ? null : current));
  }, [definition, disabled]);
  const [nodeTab, setNodeTab] = useState<"basic" | "config" | "form">("config");
  const nodeTabId = useId();
  useEffect(() => {
    const type = definition.nodeList.find(
      (node) => node.nodeCode === selectedCode,
    )?.nodeType;
    setNodeTab(type === "0" || type === "2" ? "basic" : "config");
  }, [selectedCode]);
  const [loadedConditionFields, setLoadedConditionFields] = useState<
    readonly DesignerConditionField[]
  >([]);
  const [conditionFieldState, setConditionFieldState] = useState<
    "loading" | "ready" | "error"
  >("loading");
  const [conditionFieldRetry, setConditionFieldRetry] = useState(0);
  const [selectedBranch, setSelectedBranch] = useState<{
    nodeCode: string;
    index: number;
    fromNode?: boolean;
  } | null>(null);
  const [zoom, setZoom] = useState(1);
  const [canvasDragging, setCanvasDragging] = useState(false);
  const canvasDragRef = useRef<{
    pointerId: number;
    x: number;
    y: number;
    left: number;
    top: number;
  } | null>(null);

  const handleCanvasPointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (
      event.button !== 0 ||
      canvasDragRef.current ||
      (event.target as Element).closest(
        'button, a, input, select, textarea, [role="button"], [role="menu"], [contenteditable]',
      )
    )
      return;
    canvasDragRef.current = {
      pointerId: event.pointerId,
      x: event.clientX,
      y: event.clientY,
      left: event.currentTarget.scrollLeft,
      top: event.currentTarget.scrollTop,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
    setCanvasDragging(true);
    event.preventDefault();
  };

  const handleCanvasPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const drag = canvasDragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) return;
    event.currentTarget.scrollLeft = drag.left - (event.clientX - drag.x);
    event.currentTarget.scrollTop = drag.top - (event.clientY - drag.y);
  };

  const endCanvasDrag = (event: PointerEvent<HTMLDivElement>) => {
    if (canvasDragRef.current?.pointerId !== event.pointerId) return;
    canvasDragRef.current = null;
    setCanvasDragging(false);
    if (event.currentTarget.hasPointerCapture(event.pointerId))
      event.currentTarget.releasePointerCapture(event.pointerId);
  };
  const [dirty, setDirty] = useState(false);
  const capabilities = configuredCapabilities || DEFAULT_DESIGNER_CAPABILITIES;
  const [approverKeyword, setApproverKeyword] = useState("");
  const [approverPage, setApproverPage] = useState(1);
  const [approverResources, setApproverResources] = useState<
    DesignerResourceItem[]
  >([]);
  const [approverTotal, setApproverTotal] = useState(0);
  const [approverResourceState, setApproverResourceState] = useState<
    "idle" | "loading" | "error"
  >("idle");
  const [participantPickerOpen, setParticipantPickerOpen] = useState(false);
  const [participantRuleDraft, setParticipantRuleDraft] =
    useState<ApproverRule | null>(null);
  const [hostPickerPending, setHostPickerPending] = useState(false);
  const [hostPickerError, setHostPickerError] = useState("");
  const hostPickerRequest = useRef(0);
  const hostPickerBusy = useRef(false);
  const canvasRef = useRef<HTMLDivElement>(null);
  const lastEmittedJsonRef = useRef<string | null>(null);

  useEffect(() => {
    if (value === undefined) return;
    const incoming = normalizeDefinition(value);
    const incomingJson = serializeDefinition(incoming);
    if (incomingJson === lastEmittedJsonRef.current) {
      lastEmittedJsonRef.current = null;
      return;
    }
    setDefinition(incoming);
    setPast([]);
    setFuture([]);
    setDirty(false);
  }, [value]);

  const emit = useCallback(
    (next: FloviraDefinition, isDirty = true) => {
      const json = serializeDefinition(next);
      lastEmittedJsonRef.current = json;
      setDefinition(next);
      setDirty(isDirty);
      onChange?.({ definition: next, json, dirty: isDirty });
    },
    [onChange],
  );

  const commit = useCallback(
    (next: FloviraDefinition) => {
      setPast((items) => [
        ...items.slice(-(Math.max(1, maxHistory) - 1)),
        definition,
      ]);
      setFuture([]);
      emit(next);
    },
    [definition, emit, maxHistory],
  );

  const undo = useCallback(() => {
    const previous = past[past.length - 1];
    if (!previous) return;
    setPast((items) => items.slice(0, -1));
    setFuture((items) => [definition, ...items]);
    emit(previous);
  }, [definition, emit, past]);

  const redo = useCallback(() => {
    const next = future[0];
    if (!next) return;
    setFuture((items) => items.slice(1));
    setPast((items) => [...items, definition]);
    emit(next);
  }, [definition, emit, future]);

  const locateStart = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.scrollTo({
      top: 200,
      left: Math.max(0, (canvas.scrollWidth - canvas.clientWidth) / 2),
      behavior: "smooth",
    });
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let positioned = false;
    const positionCanvas = () => {
      if (positioned || !canvas.clientWidth) return;
      canvas.scrollLeft = Math.max(
        0,
        (canvas.scrollWidth - canvas.clientWidth) / 2,
      );
      canvas.scrollTop = 200;
      positioned = true;
    };
    positionCanvas();
    // 设计器可能先挂载在隐藏的 tab 内，等容器可见后再定位。
    if (typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(positionCanvas);
    observer.observe(canvas);
    return () => observer.disconnect();
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      getDefinition: () => definition,
      getFlowJson: () => serializeDefinition(definition),
      importJson: (input) => {
        const next = normalizeDefinition(input);
        commit(next);
        setSelectedCode("");
      },
      validate: () => validateDefinition(definition, capabilities),
      isDirty: () => dirty,
      resetDirty: () => setDirty(false),
      undo,
      redo,
      zoomIn: () => setZoom((current) => Math.min(1.5, current + 0.1)),
      zoomOut: () =>
        setZoom((current) => Math.max(0.5, Number((current - 0.1).toFixed(1)))),
      resetZoom: () => setZoom(1),
      locateStart,
    }),
    [capabilities, commit, definition, dirty, locateStart, redo, undo],
  );

  const branchDefinition = selectedBranch?.fromNode
    ? nodeDefinition
    : definition;
  const branchNode = selectedBranch
    ? branchDefinition.nodeList.find(
        (node) => node.nodeCode === selectedBranch.nodeCode,
      )
    : undefined;
  const activeBranch =
    branchNode && selectedBranch && branchNode.skipList[selectedBranch.index]
      ? selectedBranch
      : null;
  useEffect(() => {
    if (
      !activeBranch ||
      !branchNode ||
      !queryConditionFields ||
      branchNode.nodeType === "4"
    )
      return;
    let active = true;
    setConditionFieldState("loading");
    setLoadedConditionFields([]);
    Promise.resolve()
      .then(() =>
        queryConditionFields({
          definition: branchDefinition,
          node: branchNode,
          branch: branchNode.skipList[activeBranch.index],
        }),
      )
      .then((fields) => {
        if (!active) return;
        if (!Array.isArray(fields)) throw new Error("表单字段返回格式无效");
        setLoadedConditionFields(fields);
        setConditionFieldState("ready");
      })
      .catch(() => {
        if (active) setConditionFieldState("error");
      });
    return () => {
      active = false;
    };
  }, [
    selectedBranch,
    branchDefinition,
    queryConditionFields,
    conditionFieldRetry,
  ]);
  const openBranch = (node: FloviraNode, index: number, fromNode = false) => {
    if (queryConditionFields) setConditionFieldState("loading");
    if (!fromNode) setSelectedCode("");
    setSelectedBranch({ nodeCode: node.nodeCode, index, fromNode });
  };
  const selectedNode =
    nodeEdit?.base === definition
      ? nodeDefinition.nodeList.find((node) => node.nodeCode === selectedCode)
      : undefined;
  const simpleNode =
    selectedNode?.nodeType === "0" || selectedNode?.nodeType === "2";
  const participantCapabilities = useMemo(
    () =>
      selectedNode?.nodeType === "0"
        ? {
            ...capabilities,
            approverStrategies: submitterStrategies(capabilities),
          }
        : capabilities,
    [capabilities, selectedNode?.nodeType],
  );
  const selectedApproverRule =
    selectedNode?.nodeType === "0"
      ? getSubmitterRule(selectedNode)
      : selectedNode?.nodeType === "8"
        ? getCarbonCopyRule(selectedNode)
        : selectedNode?.nodeType === "1"
          ? getApproverRule(selectedNode)
          : null;
  const setSelectedParticipantRule = (
    ...args: Parameters<typeof setApproverRule>
  ) => {
    const setter =
      selectedNode?.nodeType === "0"
        ? setSubmitterRule
        : selectedNode?.nodeType === "8"
          ? setCarbonCopyRule
          : setApproverRule;
    const previous = selectedApproverRule;
    args[7] =
      previous?.strategy === args[1]
        ? (previous.strategyVersion ?? 1)
        : (findApproverStrategy(participantCapabilities, args[1])?.version ??
          1);
    return setter(...args);
  };
  const selectedApproverStrategy = selectedApproverRule
    ? findApproverStrategy(
        participantCapabilities,
        selectedApproverRule.strategy,
      )
    : undefined;
  useEffect(() => {
    if (
      disabled ||
      !selectedNode ||
      !["1", "8"].includes(selectedNode.nodeType) ||
      selectedApproverRule?.strategy
    )
      return;
    const initiator = findApproverStrategy(capabilities, "INITIATOR");
    if (!initiator) return;
    const config = initiator.options
      ?.filter((option) =>
        approverOptionVisible(option, initiator, selectedNode.nodeType),
      )
      .reduce<Record<string, unknown>>((result, option) => {
        const value = option.defaultValue ?? option.choices[0]?.value;
        if (value !== undefined) result[option.code] = value;
        return result;
      }, {});
    commitNode(
      updateNode(
        nodeDefinition,
        selectedNode.nodeCode,
        (selectedNode.nodeType === "8"
          ? setCarbonCopyRule
          : setApproverRule)(
          selectedNode,
          initiator.code,
          [],
          "",
          initiator.relationType,
          initiator.selectionType,
          config,
          initiator.version ?? 1,
        ),
      ),
    );
  }, [selectedNode, capabilities, disabled]);
  const pickerState = useRef({
    definition: nodeDefinition,
    selectedCode,
    disabled,
    onSelectApprover,
    selectedApproverStrategy,
  });
  pickerState.current = {
    definition: nodeDefinition,
    selectedCode,
    disabled,
    onSelectApprover,
    selectedApproverStrategy,
  };
  useEffect(() => {
    hostPickerRequest.current += 1;
    hostPickerBusy.current = false;
    setHostPickerPending(false);
    setHostPickerError("");
    return () => {
      hostPickerRequest.current += 1;
    };
  }, [nodeDefinition, selectedCode, disabled, selectedApproverStrategy]);
  useEffect(() => {
    if (
      !selectedNode ||
      !["0", "1", "8"].includes(selectedNode.nodeType) ||
      approverEditorType(selectedApproverStrategy) !== "DIALOG" ||
      selectedApproverStrategy?.selectionType !== "RESOURCE" ||
      !selectedApproverStrategy.resourceType ||
      !participantPickerOpen ||
      !queryResources
    ) {
      setApproverResources([]);
      setApproverTotal(0);
      setApproverResourceState("idle");
      return;
    }
    let active = true;
    setApproverResourceState("loading");
    queryResources({
      resourceType: selectedApproverStrategy.resourceType,
      keyword: approverKeyword || undefined,
      pageNum: approverPage,
      pageSize: 20,
    })
      .then((response) => {
        if (!active) return;
        const page = extractResourcePage(response);
        setApproverResources(page.items || []);
        setApproverTotal(page.total || 0);
        setApproverResourceState("idle");
      })
      .catch(() => {
        if (active) setApproverResourceState("error");
      });
    return () => {
      active = false;
    };
  }, [
    approverKeyword,
    approverPage,
    participantPickerOpen,
    queryResources,
    selectedCode,
    selectedApproverStrategy?.code,
    selectedApproverStrategy?.resourceType,
    selectedApproverStrategy?.selectionType,
  ]);
  const nodeMap = useMemo(
    () => new Map(definition.nodeList.map((node) => [node.nodeCode, node])),
    [definition],
  );
  const incomingCount = useMemo(() => {
    const counts = new Map<string, number>();
    definition.nodeList.forEach((node) =>
      node.skipList.forEach((skip) =>
        counts.set(
          skip.targetNodeCode,
          (counts.get(skip.targetNodeCode) || 0) + 1,
        ),
      ),
    );
    return counts;
  }, [definition]);

  const changeSelected = (patch: Partial<FloviraNode>) => {
    if (!selectedNode) return;
    commitNode(updateNode(nodeDefinition, selectedNode.nodeCode, patch));
  };

  const openParticipantPicker = async (target?: {
    strategy: string;
    configKey: string;
    optionCode: string;
    value: string;
  }) => {
    if (
      disabled ||
      !selectedNode ||
      !selectedApproverRule ||
      !selectedApproverStrategy ||
      (!target && approverEditorType(selectedApproverStrategy) !== "DIALOG")
    )
      return;
    const pickerStrategy = target
      ? findApproverStrategy(capabilities, target.strategy)
      : selectedApproverStrategy;
    if (
      target &&
      (!onSelectApprover ||
        !pickerStrategy ||
        pickerStrategy.resourceType !== "USER")
    ) {
      setHostPickerError("请接入人员选择回调，并提供指定人员策略");
      return;
    }
    if (!pickerStrategy) return;
    if (onSelectApprover) {
      if (hostPickerBusy.current) return;
      const request = ++hostPickerRequest.current;
      const state = pickerState.current;
      const isCurrent = () =>
        request === hostPickerRequest.current &&
        pickerState.current.definition === state.definition &&
        pickerState.current.selectedCode === state.selectedCode &&
        !pickerState.current.disabled &&
        pickerState.current.selectedApproverStrategy ===
          state.selectedApproverStrategy;
      hostPickerBusy.current = true;
      setHostPickerPending(true);
      setHostPickerError("");
      try {
        const result = await onSelectApprover(
          JSON.parse(
            JSON.stringify({
              node: selectedNode,
              strategy: pickerStrategy,
              rule: target
                ? {
                    ...selectedApproverRule,
                    strategy: pickerStrategy.code,
                    selectionType: pickerStrategy.selectionType,
                    subjects:
                      selectedApproverRule.config?.[target.configKey] || [],
                    config: {},
                  }
                : selectedApproverRule,
              selected: target
                ? selectedApproverRule.config?.[target.configKey] || []
                : selectedApproverRule.subjects,
              multiple: pickerStrategy.multiple,
              disabled: false,
            }),
          ),
        );
        if (!isCurrent() || result == null) return;
        if (
          !Array.isArray(result.subjects) ||
          result.subjects.some(
            (subject) =>
              !subject ||
              typeof subject.id !== "string" ||
              !subject.id.trim() ||
              typeof subject.type !== "string" ||
              !subject.type.trim(),
          )
        ) {
          throw new Error("人员选择结果格式无效");
        }
        const selection = JSON.parse(JSON.stringify(result));
        const subjects = pickerStrategy.multiple
          ? selection.subjects
          : selection.subjects.slice(0, 1);
        if (exceedsSelectionLimit(subjects, pickerStrategy))
          throw new Error(
            `最多选择 ${pickerStrategy.maxSubjects} 项，请重新选择`,
          );
        if (target) {
          if (
            !subjects.length ||
            subjects.some((subject: ApproverSubject) => subject.type !== "USER")
          ) {
            throw new Error("请选择至少一名指定人员");
          }
          updateInlineApproverRule({
            ...selectedApproverRule,
            config: {
              ...selectedApproverRule.config,
              [target.optionCode]: target.value,
              [target.configKey]: subjects,
            },
          });
          return;
        }
        commitNode(
          updateNode(
            nodeDefinition,
            selectedNode.nodeCode,
            setSelectedParticipantRule(
              selectedNode,
              selectedApproverStrategy.code,
              subjects,
              selection.expression ?? selectedApproverRule.expression ?? "",
              selectedApproverStrategy.relationType,
              selectedApproverStrategy.selectionType,
              selection.config ?? selectedApproverRule.config,
            ),
          ),
        );
      } catch (error) {
        if (isCurrent())
          setHostPickerError(
            error instanceof Error ? error.message : "人员选择失败，请重试",
          );
      } finally {
        if (request === hostPickerRequest.current) {
          hostPickerBusy.current = false;
          setHostPickerPending(false);
        }
      }
      return;
    }
    setApproverKeyword("");
    setApproverPage(1);
    setParticipantRuleDraft({
      ...selectedApproverRule,
      subjects: [...selectedApproverRule.subjects],
      config: selectedApproverRule.config
        ? { ...selectedApproverRule.config }
        : undefined,
    });
    setParticipantPickerOpen(true);
  };

  const confirmParticipantPicker = () => {
    if (!selectedNode || !selectedApproverStrategy || !participantRuleDraft)
      return;
    if (
      exceedsSelectionLimit(
        participantRuleDraft.subjects,
        selectedApproverStrategy,
      )
    ) {
      setHostPickerError(
        `最多选择 ${selectedApproverStrategy.maxSubjects} 项，请重新选择`,
      );
      return;
    }
    const subjects = selectedApproverStrategy.multiple
      ? participantRuleDraft.subjects
      : participantRuleDraft.subjects.slice(0, 1);
    commitNode(
      updateNode(
        nodeDefinition,
        selectedNode.nodeCode,
        setSelectedParticipantRule(
          selectedNode,
          selectedApproverStrategy.code,
          subjects,
          participantRuleDraft.expression || "",
          selectedApproverStrategy.relationType,
          selectedApproverStrategy.selectionType,
          participantRuleDraft.config,
        ),
      ),
    );
    setParticipantPickerOpen(false);
  };

  const NodeCard = ({ node }: { node: FloviraNode }) => {
    const meta = NODE_META[node.nodeType] || NODE_META["1"];
    const summary = summaryFor(node, capabilities);
    const selected = selectedCode === node.nodeCode;
    const deletable = !disabled && !["0", "2"].includes(node.nodeType);
    const issues = validation.issues.filter(
      (issue) => issue.nodeCode === node.nodeCode,
    );
    if (renderNode) {
      return <>{renderNode({ node, selected, summary })}</>;
    }
    return (
      <div
        role="button"
        tabIndex={0}
        className={`frd-node frd-node--${meta.tone}`}
        onClick={() => {
          setSelectedBranch(null);
          setSelectedCode(node.nodeCode);
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            setSelectedBranch(null);
            setSelectedCode(node.nodeCode);
          }
        }}
        aria-label={`编辑节点：${node.nodeName}`}
      >
        {issues.length > 0 && (
          <UiTooltip content={issues.map((issue) => issue.message).join("；")}>
            <span
              className="frd-node__validation"
              role="img"
              aria-label={`${node.nodeName} 配置待完善：${issues.map((issue) => issue.message).join("；")}`}
            >
              <CircleAlert size={20} aria-hidden="true" />
            </span>
          </UiTooltip>
        )}
        <NodeHeader node={node}>
          {deletable && (
            <span className="frd-node__actions">
              {deletable && (
                <button
                  type="button"
                  className="frd-node__delete"
                  aria-label={`删除节点：${node.nodeName}`}
                  onClick={(event) => {
                    event.stopPropagation();
                    commit(
                      ["3", "4", "5"].includes(node.nodeType) &&
                        findBranchMerge(
                          definition,
                          node.skipList.map((skip) => skip.targetNodeCode),
                        )
                        ? removeCanvasSplit(definition, node.nodeCode)
                        : deleteNode(definition, node.nodeCode),
                    );
                    if (selected) setSelectedCode("");
                  }}
                >
                  <X size={13} />
                </button>
              )}
            </span>
          )}
        </NodeHeader>
        <span className="frd-node__summary">{summary}</span>
      </div>
    );
  };

  const InsertPoint = ({
    after,
    branchIndex,
    afterMerge = false,
    entersNode = false,
  }: {
    after: FloviraNode;
    branchIndex?: number;
    afterMerge?: boolean;
    entersNode?: boolean;
  }) => {
    if (disabled)
      return (
        <div
          className={`frd-connector frd-connector--short ${entersNode ? "frd-connector--readonly-arrow" : "frd-connector--readonly"}`}
        />
      );
    if (after.nodeType === "2")
      return <div className="frd-connector frd-connector--short" />;
    const items = filterNodeTypes(INSERT_TYPES, capabilities).map((type) => {
      const meta = NODE_META[type];
      const MetaIcon = meta.icon;
      return {
        value: type,
        label: `添加${nodeName(type)}`,
        icon: <MetaIcon size={17} aria-hidden="true" />,
        color: meta.color,
      };
    });
    return (
      <div
        className={`frd-insert-point${entersNode ? " frd-insert-point--enters-node" : ""}`}
      >
        <span className="frd-insert-point__line" />
        <UiDropdownMenu
          items={items}
          align="left"
          trigger={
            <button
              type="button"
              className="frd-insert-point__trigger"
              aria-label={
                afterMerge
                  ? `在 ${after.nodeName} 合流后添加节点`
                  : branchIndex === undefined
                    ? `在 ${after.nodeName} 后添加节点`
                    : `在分支 ${after.skipList[branchIndex].skipName || branchIndex + 1} 下添加节点`
              }
            >
              <Plus size={12} />
            </button>
          }
          onSelect={(value) => {
            const next = afterMerge
              ? insertAfterBranchMerge(
                  definition,
                  after.nodeCode,
                  value as FloviraNodeType,
                )
              : insertCanvasNode(
                  definition,
                  after.nodeCode,
                  value as FloviraNodeType,
                  branchIndex,
                );
            const added = next.nodeList.find(
              (item) =>
                !definition.nodeList.some(
                  (old) => old.nodeCode === item.nodeCode,
                ),
            );
            commit(next);
            if (added) {
              if (["3", "4", "5"].includes(added.nodeType))
                openBranch(added, 0);
              else {
                setSelectedBranch(null);
                setSelectedCode(added.nodeCode, next);
              }
            }
          }}
        />
      </div>
    );
  };

  const entersVisibleNode = (code: string, stopBeforeCode?: string) => {
    if (code === stopBeforeCode) return false;
    const target = nodeMap.get(code);
    if (!target) return false;
    const outgoing = target.skipList.filter(
      (skip) => skip.skipType !== "REJECT" && nodeMap.has(skip.targetNodeCode),
    );
    return !(outgoing.length > 1 && ["3", "4", "5"].includes(target.nodeType));
  };

  const renderPath = (
    code: string,
    visited: Set<string>,
    stopBeforeCode?: string,
  ): ReactNode => {
    if (code === stopBeforeCode) return null;
    const node = nodeMap.get(code);
    if (!node) return null;
    if (visited.has(code)) {
      return (
        <div className="frd-path">
          <div className="frd-connector frd-connector--short" />
          <div className="frd-merge-label">汇合至 {node.nodeName}</div>
        </div>
      );
    }
    const nextVisited = new Set(visited);
    nextVisited.add(code);
    const outgoing = node.skipList.filter(
      (skip) => skip.skipType !== "REJECT" && nodeMap.has(skip.targetNodeCode),
    );
    const branchMergeCode =
      outgoing.length > 1
        ? findBranchMerge(
            definition,
            outgoing.map((skip) => skip.targetNodeCode),
          )
        : undefined;
    return (
      <div className="frd-path" key={`${code}-${visited.size}`}>
        {!(outgoing.length > 1 && ["3", "4", "5"].includes(node.nodeType)) && (
          <NodeCard node={node} />
        )}
        {outgoing.length === 0 ? null : outgoing.length === 1 ? (
          <>
            <InsertPoint
              after={node}
              entersNode={entersVisibleNode(
                outgoing[0].targetNodeCode,
                stopBeforeCode,
              )}
            />
            {renderPath(
              outgoing[0].targetNodeCode,
              nextVisited,
              stopBeforeCode,
            )}
          </>
        ) : (
          <>
            <div className="frd-connector frd-connector--fork" />
            <div
              className={`frd-branches${branchMergeCode ? " frd-branches--merge" : ""}`}
            >
              <div className="frd-branch-add">
                {!disabled && (
                  <UiButton
                    variant="text"
                    size="compact"
                    disabled={!branchMergeCode}
                    onPress={() => {
                      const next = addCanvasBranch(definition, node.nodeCode);
                      commit(next);
                      const updated = next.nodeList.find(
                        (item) => item.nodeCode === node.nodeCode,
                      )!;
                      openBranch(
                        updated,
                        updated.skipList.findIndex(
                          (skip) =>
                            !node.skipList.some((old) => old.id === skip.id),
                        ),
                      );
                    }}
                  >
                    <Plus size={13} />
                    添加{node.nodeType === "4" ? "并行" : "条件"}分支
                  </UiButton>
                )}
              </div>
              <div className="flovira-react-branch-grid">
                {outgoing.map((skip) => (
                  <div
                    className="frd-branch"
                    key={String(skip.id || node.skipList.indexOf(skip))}
                  >
                    <span
                      className="frd-branch__connector"
                      aria-hidden="true"
                    />
                    <div className="frd-condition-card-wrapper">
                      {validation.issues
                        .filter(
                          (issue) =>
                            issue.nodeCode === node.nodeCode &&
                            issue.skipIndex === node.skipList.indexOf(skip),
                        )
                        .map((issue) => (
                          <UiTooltip key={issue.code} content={issue.message}>
                            <span
                              className="frd-node__validation"
                              role="img"
                              aria-label={`分支配置待完善：${issue.message}`}
                            >
                              <CircleAlert size={20} aria-hidden="true" />
                            </span>
                          </UiTooltip>
                        ))}
                      <button
                        type="button"
                        className={`frd-condition-card frd-node--${NODE_META[node.nodeType]?.tone || "exclusive"}`}
                        data-default={
                          getBranchRule(node, node.skipList.indexOf(skip))
                            .mode === "default"
                        }
                        aria-label={`配置分支：${skip.skipName || `分支 ${node.skipList.indexOf(skip) + 1}`}`}
                        onClick={() =>
                          openBranch(node, node.skipList.indexOf(skip))
                        }
                      >
                        <span className="frd-condition-card__header">
                          <span className="frd-condition-card__name-wrapper">
                            <UiTooltip
                              content={
                                skip.skipName ||
                                `分支 ${node.skipList.indexOf(skip) + 1}`
                              }
                            >
                              <span className="frd-condition-card__name">
                                {skip.skipName ||
                                  `分支 ${node.skipList.indexOf(skip) + 1}`}
                              </span>
                            </UiTooltip>
                          </span>
                          {node.nodeType !== "4" && (
                            <span className="frd-condition-card__priority">
                              {getBranchRule(node, node.skipList.indexOf(skip))
                                .mode === "default"
                                ? "默认"
                                : `优先级 ${node.skipList.indexOf(skip) + 1}`}
                            </span>
                          )}
                        </span>
                        <span className="frd-condition-card__body">
                          <UiTooltip
                            content={branchSummary(
                              node,
                              node.skipList.indexOf(skip),
                            )}
                          >
                            <span className="frd-condition-card__summary">
                              {branchSummary(node, node.skipList.indexOf(skip))}
                            </span>
                          </UiTooltip>
                        </span>
                      </button>
                      {!disabled && (
                        <button
                          type="button"
                          className="frd-condition-delete"
                          aria-label={`删除分支：${skip.skipName || node.skipList.indexOf(skip) + 1}`}
                          onClick={() => {
                            commit(
                              removeCanvasBranch(
                                definition,
                                node.nodeCode,
                                node.skipList.indexOf(skip),
                              ),
                            );
                            setSelectedBranch(null);
                          }}
                        >
                          <X size={13} />
                        </button>
                      )}
                    </div>
                    <InsertPoint
                      after={node}
                      branchIndex={node.skipList.indexOf(skip)}
                      entersNode={entersVisibleNode(
                        skip.targetNodeCode,
                        branchMergeCode ?? stopBeforeCode,
                      )}
                    />
                    {renderPath(
                      skip.targetNodeCode,
                      nextVisited,
                      branchMergeCode ?? stopBeforeCode,
                    )}
                    {branchMergeCode && (
                      <span
                        className="frd-branch__merge-tail"
                        aria-hidden="true"
                      />
                    )}
                  </div>
                ))}
              </div>
            </div>
            {branchMergeCode && (
              <>
                <div className="frd-merge-flow">
                  <InsertPoint
                    after={node}
                    afterMerge
                    entersNode={entersVisibleNode(
                      branchMergeCode,
                      stopBeforeCode,
                    )}
                  />
                </div>
                {branchMergeCode !== stopBeforeCode &&
                  renderPath(branchMergeCode, nextVisited, stopBeforeCode)}
              </>
            )}
          </>
        )}
      </div>
    );
  };

  const start =
    definition.nodeList.find((node) => node.nodeType === "0") ||
    definition.nodeList[0];
  const validation = validateDefinition(definition, capabilities);
  const updateInlineApproverRule = (rule: ApproverRule) => {
    if (!selectedNode || !selectedApproverStrategy) return;
    if (exceedsSelectionLimit(rule.subjects, selectedApproverStrategy)) {
      setHostPickerError(
        `最多选择 ${selectedApproverStrategy.maxSubjects} 项，请重新选择`,
      );
      return;
    }
    const subjects = selectedApproverStrategy.multiple
      ? rule.subjects
      : rule.subjects.slice(0, 1);
    commitNode(
      updateNode(
        nodeDefinition,
        selectedNode.nodeCode,
        setSelectedParticipantRule(
          selectedNode,
          selectedApproverStrategy.code,
          subjects,
          rule.expression || "",
          selectedApproverStrategy.relationType,
          selectedApproverStrategy.selectionType,
          rule.config,
        ),
      ),
    );
  };
  const editorRenderer = renderApproverEditor;
  const editorContext =
    selectedNode && selectedApproverStrategy && selectedApproverRule
      ? {
          node: selectedNode,
          strategy: selectedApproverStrategy,
          rule: selectedApproverRule,
          selected: selectedApproverRule.subjects,
          multiple: selectedApproverStrategy.multiple,
          disabled,
          onChange: (subjects: ApproverSubject[]) =>
            updateInlineApproverRule({
              ...selectedApproverRule,
              subjects: selectedApproverStrategy.multiple
                ? subjects
                : subjects.slice(0, 1),
            }),
          onRuleChange: updateInlineApproverRule,
        }
      : undefined;
  const dialogEditorContext =
    editorContext && participantRuleDraft
      ? {
          ...editorContext,
          rule: participantRuleDraft,
          selected: participantRuleDraft.subjects,
          onChange: (subjects: ApproverSubject[]) =>
            setParticipantRuleDraft({
              ...participantRuleDraft,
              subjects: selectedApproverStrategy?.multiple
                ? subjects
                : subjects.slice(0, 1),
            }),
          onRuleChange: (rule: ApproverRule) =>
            setParticipantRuleDraft({
              ...rule,
              strategy: selectedApproverStrategy?.code || rule.strategy,
              selectionType:
                selectedApproverStrategy?.selectionType || rule.selectionType,
              relationType: selectedApproverStrategy?.relationType,
              subjects: selectedApproverStrategy?.multiple
                ? rule.subjects
                : rule.subjects.slice(0, 1),
            }),
        }
      : undefined;
  const customDialogEditor =
    participantPickerOpen && dialogEditorContext
      ? editorRenderer?.(dialogEditorContext)
      : undefined;
  const customInlineEditor =
    approverEditorType(selectedApproverStrategy) === "INLINE" && editorContext
      ? editorRenderer?.(editorContext)
      : undefined;

  const historyControls = (
    <div className="frd-toolbar" role="group" aria-label="历史操作">
      <ToolbarButton
        Button={UiButton}
        Tooltip={UiTooltip}
        label="撤销"
        disabled={!past.length || disabled}
        onPress={undo}
      >
        <Undo2 size={16} />
      </ToolbarButton>
      <ToolbarButton
        Button={UiButton}
        Tooltip={UiTooltip}
        label="重做"
        disabled={!future.length || disabled}
        onPress={redo}
      >
        <Redo2 size={16} />
      </ToolbarButton>
    </div>
  );
  const defaultToolbar =
    appearance === "embedded" ? (
      historyControls
    ) : (
      <header className="frd-header">
        <div className="frd-heading">
          <div className="frd-heading__icon">
            <Network size={14} />
          </div>
          <div className="frd-heading__text">
            <h2>{definition.flowName || "流程设计"}</h2>
          </div>
          {dirty && (
            <UiTooltip content="有未保存修改">
              <span className="frd-dirty-indicator" aria-label="有未保存修改" />
            </UiTooltip>
          )}
        </div>
        {historyControls}
      </header>
    );
  const toolbarContent =
    toolbar !== false &&
    (renderToolbar
      ? renderToolbar({ defaultToolbar, disabled, dirty })
      : defaultToolbar);
  return (
    <section
      className={`flovira-react-designer ${className}`}
      data-appearance={appearance}
    >
      {appearance !== "embedded" && toolbarContent}
      <div className="frd-workspace">
        <div className="frd-canvas-shell">
          {appearance === "embedded" && toolbar !== false && (
            <div className="frd-history-controls">{toolbarContent}</div>
          )}
          <div
            ref={canvasRef}
            className="flovira-react-canvas"
            data-dragging={canvasDragging}
            onPointerDown={handleCanvasPointerDown}
            onPointerMove={handleCanvasPointerMove}
            onPointerUp={endCanvasDrag}
            onPointerCancel={endCanvasDrag}
            onLostPointerCapture={endCanvasDrag}
          >
            <div className="frd-canvas-workspace">
              <div
                className="frd-canvas-content"
                style={{
                  transform: `scale(${zoom})`,
                  transformOrigin: "top center",
                }}
              >
                {start ? (
                  renderPath(start.nodeCode, new Set())
                ) : (
                  <div className="frd-error-text">流程缺少开始节点</div>
                )}
              </div>
            </div>
          </div>
          <div className="frd-zoom-controls">
            <ToolbarButton
              Button={UiButton}
              Tooltip={UiTooltip}
              label="缩小"
              onPress={() =>
                setZoom((current) =>
                  Math.max(0.5, Number((current - 0.1).toFixed(1))),
                )
              }
            >
              <ZoomOut size={16} />
            </ToolbarButton>
            <UiButton
              variant="text"
              size="compact"
              ariaLabel="重置缩放"
              onPress={() => setZoom(1)}
            >
              {Math.round(zoom * 100)}%
            </UiButton>
            <ToolbarButton
              Button={UiButton}
              Tooltip={UiTooltip}
              label="放大"
              onPress={() => setZoom((current) => Math.min(1.5, current + 0.1))}
            >
              <ZoomIn size={16} />
            </ToolbarButton>
            <ToolbarButton
              Button={UiButton}
              Tooltip={UiTooltip}
              label="定位开始节点"
              onPress={locateStart}
            >
              <LocateFixed size={16} />
            </ToolbarButton>
          </div>
        </div>

        <UiDrawer
          open={Boolean(activeBranch)}
          title={
            branchNode && activeBranch
              ? branchNode.skipList[activeBranch.index].skipName || "分支条件"
              : "分支条件"
          }
          width={460}
          ariaLabel="分支条件"
          onClose={() => setSelectedBranch(null)}
        >
          {branchNode &&
          activeBranch &&
          queryConditionFields &&
          branchNode.nodeType !== "4" &&
          conditionFieldState === "loading" ? (
            <p role="status">正在加载表单字段…</p>
          ) : branchNode &&
            activeBranch &&
            queryConditionFields &&
            branchNode.nodeType !== "4" &&
            conditionFieldState === "error" ? (
            <div>
              <p role="alert">表单字段加载失败</p>
              <UiButton
                onPress={() => setConditionFieldRetry((value) => value + 1)}
              >
                重新加载
              </UiButton>
            </div>
          ) : (
            branchNode &&
            activeBranch && (
              <BranchConditionEditor
                key={`${branchNode.nodeCode}-${activeBranch.index}-${JSON.stringify(branchNode)}`}
                node={branchNode}
                index={activeBranch.index}
                fields={
                  queryConditionFields ? loadedConditionFields : conditionFields
                }
                ui={components}
                disabled={disabled}
                compile={compileBranchConditions}
                onSave={(name, rule) => {
                  const next = updateNode(
                    branchDefinition,
                    branchNode.nodeCode,
                    setBranchRule(branchNode, activeBranch.index, name, rule),
                  );
                  if (activeBranch.fromNode) commitNode(next);
                  else commit(next);
                  setSelectedBranch(null);
                }}
              />
            )
          )}
        </UiDrawer>
        <UiDrawer
          open={Boolean(selectedNode) && !selectedBranch?.fromNode}
          title={
            <span className="frd-settings-panel__title">
              <Settings2 size={17} />
              <span className="frd-settings-panel__title-copy">
                <strong>
                  {selectedNode?.nodeType === "0"
                    ? "开始节点配置"
                    : selectedNode?.nodeType === "2"
                      ? "结束节点配置"
                      : "审批配置"}
                </strong>
              </span>
            </span>
          }
          width={460}
          ariaLabel="节点设置"
          footer={
            <div className="frd-node-actions">
              <UiButton size="compact" onPress={() => setSelectedCode("")}>
                取消
              </UiButton>
              <UiButton
                size="compact"
                variant="primary"
                disabled={disabled || hostPickerPending}
                onPress={() => {
                  if (!selectedNode || nodeEdit?.base !== definition) return;
                  if (
                    serializeDefinition(nodeDefinition) !==
                    serializeDefinition(definition)
                  )
                    commit(nodeDefinition);
                  setSelectedCode("");
                }}
              >
                确定
              </UiButton>
            </div>
          }
          onClose={() => {
            setParticipantPickerOpen(false);
            setSelectedCode("");
          }}
        >
          {selectedNode && (
            <div className="frd-settings-panel">
              {!simpleNode && (
                <UiTabs
                  value={nodeTab}
                  idPrefix={nodeTabId}
                  ariaLabel="节点配置分类"
                  options={[
                    { value: "basic", label: "基础信息" },
                    {
                      value: "config",
                      label:
                        selectedNode.nodeType === "1" ? "审批配置" : "节点配置",
                    },
                    { value: "form", label: "表单权限" },
                  ]}
                  onValueChange={(value) =>
                    setNodeTab(value as "basic" | "config" | "form")
                  }
                />
              )}
              <div
                role={simpleNode ? undefined : "tabpanel"}
                id={simpleNode ? undefined : `${nodeTabId}-panel-basic`}
                aria-labelledby={
                  simpleNode ? undefined : `${nodeTabId}-tab-basic`
                }
                hidden={!simpleNode && nodeTab !== "basic"}
              >
                <div className="frd-settings-group">
                  <UiField label="节点名称">
                    <UiInput
                      value={selectedNode.nodeName}
                      disabled={disabled}
                      ariaLabel="节点名称"
                      onValueChange={(value) =>
                        changeSelected({ nodeName: value })
                      }
                    />
                  </UiField>
                  <UiField label="节点编码">
                    <UiInput
                      value={selectedNode.nodeCode}
                      disabled
                      onValueChange={() => undefined}
                    />
                  </UiField>
                  {!["0", "2"].includes(selectedNode.nodeType) && (
                    <UiField
                      label={
                        <FieldHelpLabel
                          label="节点标识"
                          help="供业务系统定位节点，同一流程内唯一（非必填）"
                          Tooltip={UiTooltip}
                        />
                      }
                    >
                      <UiInput
                        value={selectedNode.nodeKey || ""}
                        disabled={disabled}
                        ariaLabel="节点标识"
                        placeholder="请输入节点标识，如 FINANCE_REVIEW"
                        onValueChange={(value) =>
                          changeSelected({ nodeKey: value })
                        }
                      />
                    </UiField>
                  )}
                </div>
              </div>
              <div
                role={simpleNode ? undefined : "tabpanel"}
                id={simpleNode ? undefined : `${nodeTabId}-panel-config`}
                aria-labelledby={
                  simpleNode ? undefined : `${nodeTabId}-tab-config`
                }
                hidden={
                  selectedNode.nodeType === "2" ||
                  (!simpleNode && nodeTab !== "config")
                }
              >
                {["0", "1", "8"].includes(selectedNode.nodeType) && (
                  <div className="frd-settings-group">
                    {selectedNode.nodeType === "8" && <h4>抄送策略</h4>}
                    <UiField
                      label={
                        selectedNode.nodeType === "0"
                          ? "可提交人员"
                          : selectedNode.nodeType === "8"
                            ? "抄送人类型"
                            : "审批人"
                      }
                    >
                      <UiSelect
                        ariaLabel={
                          selectedNode.nodeType === "0"
                            ? "可提交人员"
                            : selectedNode.nodeType === "8"
                              ? "抄送人类型"
                              : "审批人"
                        }
                        value={String(selectedApproverRule?.strategy || "")}
                        disabled={disabled}
                        options={[
                          ...(selectedNode.nodeType === "0" &&
                          !selectedApproverRule?.strategy
                            ? [
                                {
                                  value: "",
                                  label: "无效的提交范围配置",
                                  disabled: true,
                                },
                              ]
                            : []),
                          ...(selectedApproverRule?.strategy &&
                          !selectedApproverStrategy
                            ? [
                                {
                                  value: selectedApproverRule.strategy,
                                  label: `不支持的策略：${selectedApproverRule.strategy}`,
                                  disabled: true,
                                },
                              ]
                            : []),
                          ...approverStrategyOptions(
                            participantCapabilities,
                          ).map((strategy) => ({
                            value: strategy.value,
                            label: strategy.label,
                          })),
                        ]}
                        onValueChange={(value) => {
                          const strategy = findApproverStrategy(
                            participantCapabilities,
                            value,
                          );
                          const config = strategy?.options
                            ?.filter((option) =>
                              approverOptionVisible(
                                option,
                                strategy,
                                selectedNode.nodeType,
                              ),
                            )
                            .reduce<Record<string, unknown>>(
                              (result, option) => {
                                const defaultValue =
                                  option.defaultValue ??
                                  option.choices[0]?.value;
                                if (defaultValue !== undefined)
                                  result[option.code] = defaultValue;
                                return result;
                              },
                              {},
                            );
                          setApproverKeyword("");
                          setApproverPage(1);
                          commitNode(
                            updateNode(
                              nodeDefinition,
                              selectedNode.nodeCode,
                              setSelectedParticipantRule(
                                selectedNode,
                                value,
                                [],
                                "",
                                strategy?.relationType,
                                strategy?.selectionType || "RESOURCE",
                                config,
                              ),
                            ),
                          );
                        }}
                      />
                    </UiField>
                    {selectedApproverStrategy &&
                    approverEditorType(selectedApproverStrategy) ===
                      "INLINE" ? (
                      (customInlineEditor ??
                      (selectedApproverStrategy?.selectionType ===
                      "EXPRESSION" ? (
                        <UiField
                          label={
                            selectedNode.nodeType === "0"
                              ? "提交范围表达式"
                              : selectedNode.nodeType === "8"
                                ? "抄送人表达式"
                                : "办理人表达式"
                          }
                        >
                          <UiInput
                            value={String(
                              selectedApproverRule?.expression || "",
                            )}
                            disabled={disabled}
                            placeholder="例如 ${approverIds}"
                            onValueChange={(value) =>
                              commitNode(
                                updateNode(
                                  nodeDefinition,
                                  selectedNode.nodeCode,
                                  setSelectedParticipantRule(
                                    selectedNode,
                                    selectedApproverStrategy.code,
                                    [],
                                    value,
                                    selectedApproverStrategy.relationType,
                                    selectedApproverStrategy.selectionType,
                                    selectedApproverRule?.config,
                                  ),
                                ),
                              )
                            }
                          />
                        </UiField>
                      ) : null))
                    ) : selectedApproverStrategy &&
                      approverEditorType(selectedApproverStrategy) ===
                        "DIALOG" ? (
                      <div className="frd-participant-picker">
                        <UiField
                          className="frd-participant-field"
                          label={selectionHint(
                            selectedApproverStrategy.resourceType === "USER"
                              ? "新增人员"
                              : `新增${selectedApproverStrategy.name}`,
                            selectedApproverStrategy,
                          )}
                        >
                          <div className="frd-participant-launcher">
                            <UiTooltip
                              content={`选择${selectedApproverStrategy.name}`}
                            >
                              <UiButton
                                size="icon"
                                variant="default"
                                className="frd-participant-launcher__add"
                                disabled={disabled || hostPickerPending}
                                ariaLabel={`选择${selectedApproverStrategy.name}`}
                                onPress={() => openParticipantPicker()}
                              >
                                <Plus size={14} />
                              </UiButton>
                            </UiTooltip>
                            {selectedApproverRule?.subjects.map((subject) => (
                              <span
                                className="frd-participant-chip"
                                key={`${subject.type}:${subject.id}`}
                              >
                                <span>{subject.name || subject.id}</span>
                                <UiButton
                                  size="icon"
                                  variant="text"
                                  className="frd-participant-chip__remove"
                                  disabled={disabled}
                                  ariaLabel={`移除${subject.name || subject.id}`}
                                  onPress={() =>
                                    commitNode(
                                      updateNode(
                                        nodeDefinition,
                                        selectedNode.nodeCode,
                                        setSelectedParticipantRule(
                                          selectedNode,
                                          selectedApproverStrategy.code,
                                          selectedApproverRule.subjects.filter(
                                            (item) => item.id !== subject.id,
                                          ),
                                          "",
                                          selectedApproverStrategy.relationType,
                                          selectedApproverStrategy.selectionType,
                                          selectedApproverRule.config,
                                        ),
                                      ),
                                    )
                                  }
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
                      ?.filter((option) =>
                        approverOptionVisible(
                          option,
                          selectedApproverStrategy,
                          selectedNode.nodeType,
                          selectedApproverRule?.subjects,
                        ),
                      )
                      .map((option) => {
                        const OptionControl =
                          option.code === "approvalMode"
                            ? UiSelect
                            : UiRadioGroup;
                        return (
                          <Fragment key={option.code}>
                            <UiField
                              label={option.name}
                              className="frd-approver-option"
                            >
                              <OptionControl
                                value={String(
                                  selectedApproverRule?.config?.[option.code] ??
                                    option.defaultValue ??
                                    option.choices[0]?.value ??
                                    "",
                                )}
                                disabled={disabled}
                                ariaLabel={option.name}
                                options={
                                  option.code === "approvalMode"
                                    ? option.choices
                                        .map((choice) => ({
                                          ...choice,
                                          label:
                                            (
                                              {
                                                COUNTERSIGN: "会签",
                                                OR: "或签",
                                                VOTE: "票签",
                                              } as Record<string, string>
                                            )[choice.value] || choice.label,
                                        }))
                                        .sort((a, b) => {
                                          const order = [
                                            "COUNTERSIGN",
                                            "OR",
                                            "VOTE",
                                          ];
                                          const rank = (value: string) =>
                                            order.includes(value)
                                              ? order.indexOf(value)
                                              : order.length;
                                          return rank(a.value) - rank(b.value);
                                        })
                                    : option.choices
                                }
                                onValueChange={(value) => {
                                  if (selectedApproverRule) {
                                    const config = {
                                      ...selectedApproverRule.config,
                                      [option.code]: value,
                                    };
                                    option.choices.forEach((item) => {
                                      if (
                                        item.selectionConfigKey &&
                                        item.value !== value
                                      )
                                        delete config[item.selectionConfigKey];
                                    });
                                    updateInlineApproverRule({
                                      ...selectedApproverRule,
                                      config,
                                    });
                                  }
                                }}
                              />
                              {option.choices
                                .filter(
                                  (choice) =>
                                    choice.selectionStrategy &&
                                    choice.selectionConfigKey &&
                                    selectedApproverRule?.config?.[
                                      option.code
                                    ] === choice.value,
                                )
                                .map((choice) => {
                                  const subjects = selectedApproverRule
                                    ?.config?.[choice.selectionConfigKey!] as
                                    | ApproverSubject[]
                                    | undefined;
                                  return (
                                    <div
                                      className="frd-option-subjects"
                                      key={choice.value}
                                    >
                                      <p className="frd-participant-hint">
                                        {selectionHint(
                                          "新增人员",
                                          findApproverStrategy(
                                            capabilities,
                                            choice.selectionStrategy!,
                                          ),
                                        )}
                                      </p>
                                      <div className="frd-participant-launcher">
                                        <UiTooltip content="选择指定人员">
                                          <UiButton
                                            size="icon"
                                            variant="default"
                                            className="frd-participant-launcher__add"
                                            ariaLabel="选择指定人员"
                                            disabled={
                                              disabled || hostPickerPending
                                            }
                                            onPress={() =>
                                              openParticipantPicker({
                                                strategy:
                                                  choice.selectionStrategy!,
                                                configKey:
                                                  choice.selectionConfigKey!,
                                                optionCode: option.code,
                                                value: choice.value,
                                              })
                                            }
                                          >
                                            <Plus size={14} />
                                          </UiButton>
                                        </UiTooltip>
                                        {Array.isArray(subjects) &&
                                          subjects.map((subject) => (
                                            <span
                                              className="frd-participant-chip"
                                              key={`${subject.type}:${subject.id}`}
                                            >
                                              <span>
                                                {subject.name || subject.id}
                                              </span>
                                            </span>
                                          ))}
                                      </div>
                                    </div>
                                  );
                                })}
                            </UiField>
                            {option.code === "approvalMode" &&
                              selectedNode.nodeType === "1" &&
                              selectedApproverRule?.config?.approvalMode ===
                                "VOTE" && (
                                <UiField label="通过比例（%）">
                                  <UiInput
                                    ariaLabel="通过比例（%）"
                                    type="number"
                                    min={1}
                                    value={
                                      Number.isFinite(
                                        Number(selectedNode.nodeRatio),
                                      )
                                        ? String(selectedNode.nodeRatio ?? "")
                                        : ""
                                    }
                                    disabled={disabled}
                                    placeholder="例如 60"
                                    onValueChange={(value) =>
                                      changeSelected({ nodeRatio: value })
                                    }
                                  />
                                  {validateDefinition(
                                    nodeDefinition,
                                    capabilities,
                                  ).issues.some(
                                    (issue) =>
                                      issue.nodeCode ===
                                        selectedNode.nodeCode &&
                                      issue.code === "VOTE_RATIO_INVALID",
                                  ) && (
                                    <p
                                      role="alert"
                                      className="frd-condition-error"
                                    >
                                      请输入大于等于 1、小于 100 的通过比例
                                    </p>
                                  )}
                                  {!Number.isFinite(
                                    Number(selectedNode.nodeRatio),
                                  ) && (
                                    <p className="frd-condition-hint">
                                      当前票签规则：{selectedNode.nodeRatio}
                                      。填写比例后将替换此规则。
                                    </p>
                                  )}
                                </UiField>
                              )}
                          </Fragment>
                        );
                      })}
                    {hostPickerError && (
                      <p role="alert" className="frd-error-text">
                        {hostPickerError}
                      </p>
                    )}
                    {selectedNode.nodeType === "1" && (
                      <NodeControlEditor
                        definition={nodeDefinition}
                        node={selectedNode}
                        disabled={disabled}
                        ui={components}
                        onChange={(node) =>
                          commitNode(
                            updateNode(
                              nodeDefinition,
                              selectedNode.nodeCode,
                              node,
                            ),
                          )
                        }
                      />
                    )}
                  </div>
                )}
                {selectedNode.nodeType === "6" && (
                  <SubprocessField
                    key={selectedNode.nodeCode}
                    value={String(
                      getSubprocessConfig(selectedNode).fixedChildFlowCode ||
                        "",
                    )}
                    label={String(
                      getSubprocessConfig(selectedNode).fixedChildFlowName ||
                        "",
                    )}
                    disabled={disabled}
                    queryResources={queryResources}
                    ui={components}
                    onChange={(value, label) =>
                      commitNode(
                        updateNode(
                          nodeDefinition,
                          selectedNode.nodeCode,
                          setSubprocessConfig(selectedNode, value, label),
                        ),
                      )
                    }
                  />
                )}
                {selectedNode.nodeType === "7" && (
                  <UiField
                    label={
                      <FieldHelpLabel
                        label="等待标识"
                        help="供业务系统恢复等待任务（必填）"
                        Tooltip={UiTooltip}
                      />
                    }
                  >
                    <UiInput
                      value={String(getWaitConfig(selectedNode).waitKey || "")}
                      disabled={disabled}
                      placeholder="请输入等待标识，如 ORDER_PAID"
                      onValueChange={(value) =>
                        commitNode(
                          updateNode(
                            nodeDefinition,
                            selectedNode.nodeCode,
                            setWaitConfig(selectedNode, value.trim()),
                          ),
                        )
                      }
                    />
                  </UiField>
                )}
                {capabilities.timeoutNodeTypes.includes(
                  selectedNode.nodeType,
                ) &&
                  (() => {
                    const timeout = getTimeoutConfig(selectedNode);
                    const enabled = Boolean(timeout.enabled);
                    return (
                      <div className="frd-settings-section frd-settings-section--timeout">
                        <div className="frd-settings-section__header">
                          <span>超时处理</span>
                          <UiCheckbox
                            checked={enabled}
                            disabled={disabled}
                            ariaLabel="启用超时处理"
                            onCheckedChange={(checked) =>
                              commitNode(
                                updateNode(
                                  nodeDefinition,
                                  selectedNode.nodeCode,
                                  setTimeoutConfig(selectedNode, {
                                    enabled: checked,
                                  }),
                                ),
                              )
                            }
                          >
                            启用超时处理
                          </UiCheckbox>
                        </div>
                        {enabled && (
                          <>
                            <UiField
                              label="超时来源"
                              className="frd-timeout-action"
                            >
                              <UiRadioGroup
                                ariaLabel="超时来源"
                                value={String(timeout.source || "DURATION")}
                                disabled={disabled}
                                options={[
                                  { value: "DURATION", label: "固定时长" },
                                  {
                                    value: "FORM_FIELD",
                                    label: "表单日期时间字段",
                                  },
                                ]}
                                onValueChange={(source) =>
                                  changeSelected(
                                    setTimeoutConfig(selectedNode, { source }),
                                  )
                                }
                              />
                            </UiField>
                            {timeout.source === "FORM_FIELD" ? (
                              <TimeoutFormField
                                definition={nodeDefinition}
                                node={selectedNode}
                                fields={formFields || EMPTY_FORM_FIELDS}
                                queryFields={queryFormFields}
                                value={String(timeout.fieldCode || "")}
                                label={String(timeout.fieldLabel || "")}
                                disabled={disabled}
                                ui={components}
                                onChange={(fieldCode, fieldLabel) =>
                                  changeSelected(
                                    setTimeoutConfig(selectedNode, {
                                      fieldCode,
                                      fieldLabel,
                                    }),
                                  )
                                }
                              />
                            ) : (
                              <div className="frd-timeout-duration">
                                <UiField label="超时时长">
                                  <UiInput
                                    type="number"
                                    min={1}
                                    ariaLabel="超时时长"
                                    placeholder="请输入超时时长"
                                    value={Number(timeout.duration || 1)}
                                    disabled={disabled}
                                    onValueChange={(value) =>
                                      commitNode(
                                        updateNode(
                                          nodeDefinition,
                                          selectedNode.nodeCode,
                                          setTimeoutConfig(selectedNode, {
                                            duration: Math.max(
                                              1,
                                              Number(value) || 1,
                                            ),
                                          }),
                                        ),
                                      )
                                    }
                                  />
                                </UiField>
                                <UiField label="时间单位">
                                  <UiSelect
                                    value={String(
                                      timeout.durationUnit || "HOURS",
                                    )}
                                    disabled={disabled}
                                    ariaLabel="时间单位"
                                    options={[
                                      { value: "MINUTES", label: "分钟" },
                                      { value: "HOURS", label: "小时" },
                                      { value: "DAYS", label: "天" },
                                    ]}
                                    onValueChange={(value) =>
                                      commitNode(
                                        updateNode(
                                          nodeDefinition,
                                          selectedNode.nodeCode,
                                          setTimeoutConfig(selectedNode, {
                                            durationUnit: value,
                                          }),
                                        ),
                                      )
                                    }
                                  />
                                </UiField>
                              </div>
                            )}
                            <UiField
                              label="超时动作"
                              className="frd-timeout-action"
                            >
                              <UiRadioGroup
                                value={String(
                                  timeout.action ||
                                    (selectedNode.nodeType === "7"
                                      ? "RESUME_WAIT"
                                      : "AUTO_PASS"),
                                )}
                                disabled={disabled}
                                ariaLabel="超时动作"
                                options={
                                  selectedNode.nodeType === "7"
                                    ? [
                                        {
                                          value: "RESUME_WAIT",
                                          label: "恢复等待并继续",
                                        },
                                      ]
                                    : [
                                        {
                                          value: "AUTO_PASS",
                                          label: "自动通过",
                                        },
                                        {
                                          value: "AUTO_REJECT",
                                          label: "自动驳回",
                                        },
                                      ]
                                }
                                onValueChange={(value) =>
                                  commitNode(
                                    updateNode(
                                      nodeDefinition,
                                      selectedNode.nodeCode,
                                      setTimeoutConfig(selectedNode, {
                                        action: value,
                                      }),
                                    ),
                                  )
                                }
                              />
                            </UiField>
                          </>
                        )}
                      </div>
                    );
                  })()}
                {["3", "4", "5"].includes(selectedNode.nodeType) && (
                  <div className="frd-settings-section">
                    <div className="frd-settings-section__header">
                      <span>分支</span>
                      <UiButton
                        size="compact"
                        variant="text"
                        disabled={
                          disabled ||
                          !findBranchMerge(
                            nodeDefinition,
                            selectedNode.skipList.map(
                              (skip) => skip.targetNodeCode,
                            ),
                          )
                        }
                        className="frd-add-branch"
                        onPress={() =>
                          commitNode(
                            addCanvasBranch(
                              nodeDefinition,
                              selectedNode.nodeCode,
                            ),
                          )
                        }
                      >
                        <Plus size={13} />
                        增加分支
                      </UiButton>
                    </div>
                    {selectedNode.skipList.map((skip, index) => (
                      <UiButton
                        key={String(skip.id || index)}
                        variant="text"
                        onPress={() => openBranch(selectedNode, index, true)}
                      >
                        {skip.skipName || `分支 ${index + 1}`}：
                        {branchSummary(selectedNode, index)}
                      </UiButton>
                    ))}
                  </div>
                )}
                {incomingCount.get(selectedNode.nodeCode) &&
                incomingCount.get(selectedNode.nodeCode)! > 1 ? (
                  <p className="frd-merge-note">
                    该节点是 {incomingCount.get(selectedNode.nodeCode)}{" "}
                    条分支的汇合点。
                  </p>
                ) : null}
              </div>
              {!simpleNode && nodeTab === "form" && (
                <div
                  role="tabpanel"
                  id={`${nodeTabId}-panel-form`}
                  aria-labelledby={`${nodeTabId}-tab-form`}
                >
                  <FormPermissionEditor
                    key={selectedNode.nodeCode}
                    definition={nodeDefinition}
                    node={selectedNode}
                    fields={formFields || EMPTY_FORM_FIELDS}
                    queryFields={queryFormFields}
                    ui={components}
                    disabled={disabled}
                    onChange={(node) =>
                      commitNode(
                        updateNode(nodeDefinition, selectedNode.nodeCode, node),
                      )
                    }
                  />
                </div>
              )}
            </div>
          )}
        </UiDrawer>
        {selectedNode &&
          selectedApproverRule &&
          selectedApproverStrategy &&
          !onSelectApprover &&
          approverEditorType(selectedApproverStrategy) === "DIALOG" && (
            <UiDialog
              open={participantPickerOpen}
              title={
                selectedApproverStrategy.resourceType === "USER"
                  ? selectedNode.nodeType === "0"
                    ? "选择可提交人员"
                    : selectedNode.nodeType === "8"
                      ? "选择抄送人员"
                      : "选择审批人员"
                  : `选择${selectedApproverStrategy.name}`
              }
              width={600}
              ariaLabel="人员选择"
              onClose={() => setParticipantPickerOpen(false)}
              onConfirm={confirmParticipantPicker}
            >
              {participantPickerOpen &&
                (customDialogEditor ??
                  (selectedApproverStrategy.selectionType === "RESOURCE" ? (
                    <div className="frd-default-participant-picker">
                      <UiInput
                        value={approverKeyword}
                        disabled={disabled || !queryResources}
                        ariaLabel={`搜索${selectedApproverStrategy.name}`}
                        placeholder={`搜索${selectedApproverStrategy.name}`}
                        onValueChange={(value) => {
                          setApproverKeyword(value);
                          setApproverPage(1);
                        }}
                      />
                      <div className="frd-resource-list">
                        {approverResourceState === "loading" && (
                          <p className="frd-resource-list__state">加载中...</p>
                        )}
                        {approverResourceState === "error" && (
                          <p className="frd-resource-list__state frd-error-text">
                            人员数据加载失败
                          </p>
                        )}
                        {approverResourceState === "idle" &&
                          approverResources.length === 0 && (
                            <p className="frd-resource-list__state">
                              {queryResources
                                ? "暂无可选数据"
                                : "未提供人员选择数据"}
                            </p>
                          )}
                        {selectedApproverStrategy.multiple ? (
                          approverResources.map((item) => {
                            const selected =
                              participantRuleDraft?.subjects.some(
                                (subject) => subject.id === item.id,
                              ) || false;
                            return (
                              <UiCheckbox
                                key={`${item.resourceType}:${item.id}`}
                                className="frd-resource-list__item"
                                checked={selected}
                                disabled={
                                  disabled ||
                                  item.disabled ||
                                  (!selected &&
                                    selectedApproverStrategy.maxSubjects !=
                                      null &&
                                    (participantRuleDraft?.subjects.length ||
                                      0) >=
                                      selectedApproverStrategy.maxSubjects)
                                }
                                onCheckedChange={() =>
                                  participantRuleDraft &&
                                  setParticipantRuleDraft({
                                    ...participantRuleDraft,
                                    subjects: selected
                                      ? participantRuleDraft.subjects.filter(
                                          (subject) => subject.id !== item.id,
                                        )
                                      : [
                                          ...participantRuleDraft.subjects,
                                          {
                                            id: item.id,
                                            type: item.resourceType,
                                            name: item.name,
                                          },
                                        ],
                                  })
                                }
                              >
                                <span className="frd-resource-list__name">
                                  {item.name}
                                </span>
                                {item.code && (
                                  <span className="frd-resource-list__code">
                                    {item.code}
                                  </span>
                                )}
                              </UiCheckbox>
                            );
                          })
                        ) : (
                          <UiRadioGroup
                            className="frd-resource-list__radio-group"
                            direction="vertical"
                            value={participantRuleDraft?.subjects[0]?.id || ""}
                            disabled={disabled}
                            ariaLabel={`选择${selectedApproverStrategy.name}`}
                            options={approverResources.map((item) => ({
                              value: item.id,
                              disabled: item.disabled,
                              label: (
                                <span className="frd-resource-list__radio-content">
                                  <span className="frd-resource-list__name">
                                    {item.name}
                                  </span>
                                  {item.code && (
                                    <span className="frd-resource-list__code">
                                      {item.code}
                                    </span>
                                  )}
                                </span>
                              ),
                            }))}
                            onValueChange={(value) => {
                              const item = approverResources.find(
                                (resource) => resource.id === value,
                              );
                              if (item && participantRuleDraft)
                                setParticipantRuleDraft({
                                  ...participantRuleDraft,
                                  subjects: [
                                    {
                                      id: item.id,
                                      type: item.resourceType,
                                      name: item.name,
                                    },
                                  ],
                                });
                            }}
                          />
                        )}
                      </div>
                      {approverTotal > 20 && (
                        <div className="frd-pagination">
                          <UiButton
                            size="compact"
                            variant="text"
                            disabled={approverPage <= 1}
                            onPress={() =>
                              setApproverPage((page) => Math.max(1, page - 1))
                            }
                          >
                            上一页
                          </UiButton>
                          <span>
                            {approverPage} / {Math.ceil(approverTotal / 20)}
                          </span>
                          <UiButton
                            size="compact"
                            variant="text"
                            disabled={approverPage * 20 >= approverTotal}
                            onPress={() => setApproverPage((page) => page + 1)}
                          >
                            下一页
                          </UiButton>
                        </div>
                      )}
                    </div>
                  ) : null))}
            </UiDialog>
          )}
      </div>
    </section>
  );
});
