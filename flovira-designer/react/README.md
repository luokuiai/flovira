# @luokuiai/flovira-react-designer

An embeddable React approval-flow designer for Flovira. It reads and writes Flovira `nodeList/skipList` JSON without depending on routing, Vue, Tailwind, LogicFlow, or a fixed backend URL.

```bash
bun add @luokuiai/flovira-react-designer react react-dom
```

## Quick start

```tsx
import { useRef, useState, type ReactNode } from 'react'
import {
  ReactFlowDesigner,
  type DesignerCapabilities,
  type ApproverEditorRenderContext,
  type DesignerResourceLoader,
  type ReactFlowDesignerRef,
} from '@luokuiai/flovira-react-designer'
import '@luokuiai/flovira-react-designer/style.css'

export function ProcessEditor({
  capabilities,
  queryResources,
  renderApproverEditor,
}: {
  capabilities?: DesignerCapabilities
  queryResources?: DesignerResourceLoader
  renderApproverEditor?: (context: ApproverEditorRenderContext) => ReactNode
}) {
  const designer = useRef<ReactFlowDesignerRef>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit() {
    const editor = designer.current
    if (!editor || busy) return
    const result = editor.validate()
    if (!result.valid) {
      setError(result.issues.map(issue => issue.message).join('; '))
      return
    }
    const json = editor.getFlowJson()
    setBusy(true)
    setError('')
    try {
      const response = await fetch('/api/flows/leave', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: json,
      })
      if (!response.ok) throw new Error('Submission failed')
      if (editor.getFlowJson() === json) editor.resetDirty()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ height: 'calc(100dvh - 64px)' }}>
      <ReactFlowDesigner
        ref={designer}
        defaultValue={{ flowCode: 'leave', flowName: 'Leave approval', nodeList: [] }}
        capabilities={capabilities}
        queryResources={queryResources}
        renderApproverEditor={renderApproverEditor}
        renderToolbar={({ defaultToolbar, disabled }) => <div>
          {defaultToolbar}
          <button disabled={disabled || busy} onClick={submit}>Submit configuration</button>
          {error && <p role="alert">{error}</p>}
        </div>}
      />
    </div>
  )
}
```

The endpoint in this example belongs to the host application; replace it with your own persistence API.

## Designer API

- `value` / `defaultValue`: a Flovira definition object or JSON string for controlled / uncontrolled usage.
- `onChange`: receives the latest definition, JSON, and dirty state.
- `appearance`: `standalone` (default) displays a rounded card; `embedded` removes the outer border, radius, and shadow. Internal controls and toolbar are unchanged.
- `capabilities`: designer capabilities and approver strategies loaded by the host; omitting it leaves the approver strategy list empty.
- `queryResources`: host resource loader for default selectors and subprocesses. Custom selectors may load their own data.
- `renderApproverEditor`: custom approver configuration. Receives the current node, strategy, complete `rule`, read-only state, `onChange`, and `onRuleChange`; may render organization trees, tables, or other business controls.
- `renderNode`: custom node card renderer.
- `ui`: optional UI adapter for Button, Input, Select, Checkbox, RadioGroup, Field, Tooltip, DropdownMenu, Drawer, and Dialog. Unspecified controls use the defaults.
- Ref methods include `getDefinition`, `getFlowJson`, `importJson`, `validate`, `undo`, `redo`, zoom, and positioning commands.

### Host-owned actions

The designer has no save or publish buttons, methods, or callbacks. The host defines its actions and submission API. Set `toolbar={false}` to hide the top toolbar, or use `renderToolbar({ defaultToolbar, disabled, dirty })` to replace or compose it.

Call `validate()` and `getFlowJson()` through the ref. Handle requests, loading, errors, and navigation in the host. Call `resetDirty()` after success; if editing remains enabled during submission, first compare the current JSON with the submitted snapshot so subsequent edits remain dirty. Forms and workflow design can be validated and submitted together.

### Container height

The designer fills 100% of its parent, with no fixed or minimum height. Provide an explicit parent height. In flex layouts, use `flex: 1; min-height: 0` for the content area within a height-constrained ancestor. The canvas scrolls internally rather than growing the host as nodes are added.

## Resources and workflow packages

Subprocess candidates load when the selector opens, with keyword search and pages of 20 items. The host's `queryResources` must handle `keyword`, `pageNum`, and `pageSize`, and return the matching `total`. Selected workflows remain visible by code even when absent from the loaded page or after a failed request.

Complete workflow transfer uses backend workflow-package methods. The designer has no built-in JSON import/export buttons. Use `parseWorkflowPackage` to parse package JSON, then `getPackageDefinition` to extract the root workflow or a subprocess for `FlowPreview` / `ReactFlowDesigner`. `getPackageForm` and `parsePackageFormContent` expose form metadata and content for host renderers. See [workflow package integration](../../docs/workflow-packages.md).

## UI adapters and approver strategies

The default UI uses package-scoped CSS. Import `style.css`; Tailwind is not required. Adapter events use semantic names such as `onPress`, `onValueChange`, and `onCheckedChange`.

Official adapters:

- `@luokuiai/flovira-react-adapter-lumen`
- `@luokuiai/flovira-react-adapter-antd`

Pass a complete adapter or override individual controls; unspecified controls fall back to the defaults:

```tsx
import { antdDesignerUi } from '@luokuiai/flovira-react-adapter-antd'
import '@luokuiai/flovira-react-adapter-antd/style.css'

<ReactFlowDesigner ui={antdDesignerUi} />
```

The React package does not enumerate business approver types. It renders host-provided `approverStrategies`. `selectionType` describes runtime resolution semantics; `editorType` independently controls interaction: `NONE` hides configuration, `INLINE` renders inside the drawer, and `DIALOG` shows an add button that opens the adapter dialog.

`multiple` controls selector multiplicity; `editorKey` identifies a host editor. Custom editors may store structured resolver configuration in `ApproverRule.config`.

`resultCardinality` describes the number of resolved people, not selected resources: `EXACTLY_ONE`, `ONE_OR_MORE`, `ZERO_OR_ONE`, or `ZERO_OR_MORE`. Options may declare `condition: MULTIPLE | EMPTY | ALWAYS`. Direct single-person selection hides multiple-person and empty-result policies; direct multi-person selection shows the multiple-person policy; groups that may resolve to zero or more people show both.

Strategy descriptors come from registered backend resolvers. Standard abstract resolver classes expose the engine's empty-approver and same-as-initiator policies. Hosts implement personnel lookup, including a registered USER resolver for transfer targets. See [approver policies](../../docs/approver-policies.md) for configuration and system history identities.

Strategy `options` render as radio groups and write their values into `ApproverRule.config` under the option code:

```ts
{
  code: 'GROUP',
  name: 'Group',
  selectionType: 'RESOURCE',
  resourceType: 'GROUP',
  multiple: false,
  editorType: 'DIALOG',
  options: [{
    code: 'emptyPolicy',
    name: 'No approver policy',
    defaultValue: 'ERROR',
    nodeTypes: ['1'],
    condition: 'EMPTY',
    choices: [
      { value: 'ERROR', label: 'Block transition' },
      { value: 'SKIP', label: 'Skip automatically' },
      { value: 'TRANSFER_TO_USER', label: 'Transfer to selected users', selectionStrategy: 'USER', selectionConfigKey: 'emptyPolicySubjects' },
    ],
  }],
  resultCardinality: 'ZERO_OR_MORE',
}
```

The host backend configures business strategies such as submitter, specified users, and roles, together with their resolvers.

## Forms and branch conditions

Standard forms describe objects and arrays with `fields` / `items`. Use `getFormConditionFields` to produce readable condition fields. Detail groups match any row or all rows satisfying every condition in that group; array counts are also available. See [nested form fields and conditions](../../docs/form-field-conditions.md).

Each branch has a condition card. Click it to configure rules; the add button below it inserts approval, wait, subprocess, or nested branch nodes. A condition card does not create an approval task. Conditions within a group use AND; groups use OR. Conditional branches have a fallback, inclusive branches may be unconditional, and parallel branches do not need conditions.

Forms use opaque string `formId` references and may be Flovira-managed versioned forms or host-provided forms. The start node selects the workflow form; approval nodes may override it or inherit by leaving the reference empty. Supply `queryResources({ resourceType: 'FORM', ... })` for form options, or enter form identifiers directly when no resource callback is provided.

Load business fields through `queryConditionFields`, using `definition.formId` if needed. The designer does not bind to a form endpoint. Fields load when condition configuration opens, with loading feedback, retry on failure, and stale-response protection:

```tsx
<ReactFlowDesigner
  queryConditionFields={async ({ definition, node, branch }) => {
    const fields = await loadYourFormFields(definition)
    return fields.map(field => ({
      code: field.code,
      label: field.label,
      type: field.type, // STRING, NUMBER, or BOOLEAN
    }))
  }}
/>
```

Visual rules compile to `spel@@#{...}` by default and require the Spring SpEL condition strategy at runtime. Supply `compileBranchConditions(groups)` for another expression engine. Simple field codes support letters, digits, and underscores, but cannot start with a digit.

Rule metadata is stored in the parent branch node's `ext.branchConditions`; runtime expressions remain in connection `skipCondition`. No additional database fields are required. Existing expressions are not automatically converted or overwritten and remain editable in expression mode. Expression mode is also available when the field loader returns an empty list. Static `conditionFields` may be supplied directly; the callback takes precedence. Incomplete new conditional branches fail validation.

## Read-only preview

Use `FlowPreview` on detail pages or in dialogs. Nodes use compact, type-colored rounded squares with white icons; current nodes have a dot indicator. Hover or keyboard focus reveals full names and handlers. The preview supports parallel branches, joins, auto-fit, zoom, and panning on empty canvas space.

```tsx
import { FlowPreview, type FloviraDefinition } from '@luokuiai/flovira-react-designer'
import '@luokuiai/flovira-react-designer/style.css'

export function ProcessProgress({ definition }: { definition: FloviraDefinition }) {
  return (
    <FlowPreview
      value={definition}
      height={280}
      currentNodeCodes={['manager_review', 'finance_review']}
      completedNodeCodes={['start']}
      nodeHandlers={{ manager_review: ['Alex'], finance_review: ['Sam', 'Jordan'] }}
    />
  )
}
```

Replace node codes and names with actual instance data. The preview makes no backend requests; update props to change the workflow and progress.

| Prop | Description |
| --- | --- |
| `value` | Definition object or JSON string; missing or empty nodes show an empty state |
| `currentNodeCodes` | Current node codes; supports concurrent nodes and defaults to no highlights |
| `completedNodeCodes` | Enables three-state colors: light for pending, solid with a dot for current, solid for completed. Current takes precedence. Omit for type-only preview colors |
| `nodeHandlers` | Actual handler names keyed by node code; not inferred from design configuration |
| `height` | Canvas height, default `320`; accepts a number or CSS height, with a compact zoom bar below |
| `ui` | Optional Lumen / Ant Design adapter; preview uses its Tooltip only |
| `renderTooltip` | Custom content receiving `{ node, current, status, handlers }` |
| `renderNodeIcon` | Custom icon receiving the same context |
| `className` / `style` | Container styling; `--frp-*` variables customize colors |

State comes only from the supplied current and completed codes. The preview does not infer completed nodes or highlight historical paths. Nodes in neither list are pending. Rejection connections (`skipType: 'REJECT'`) and return paths are hidden. Compact automatic layout ignores stored designer coordinates. Forward cycles are unsupported; cycles, duplicate codes, invalid connections, and unparseable input produce explicit errors.

See `flovira-designer/examples/react-lumen` for a parallel-approval preview.

## Approval node control policies

Approval settings include rollback, transfer, add-sign, and remove-sign toggles. Rollback is enabled by default; the others are disabled. Disabling rollback hides its settings but preserves their values.

Rollback strategies include returning to the initiator, the previous node, a design-time predecessor approval node, or a node selected at runtime. Except for returning to the previous node, resubmission may restart sequential execution or resume at the node that initiated the rollback. A target cannot be the current node, a later node, or a parallel sibling. Deleted or invalid targets fail validation.

Configuration is stored in the existing `ext` array under `nodeControlConfig`:

```ts
const config = getNodeControlConfig(node)
// config.allowRollback / allowTransfer / allowAddSign / allowMinusSign
// config.rejectStrategy / rejectTargetNodeCode / resubmitStrategy
const updated = setNodeControlConfig(node, { allowTransfer: true })
```

These settings currently provide designer configuration and serialization only; the Flovira core does not automatically enforce them. Host runtime APIs must validate operation permissions, actual visited nodes, and rollback/resubmission paths. Runtime target choices must come from instance history. Frontend toggles do not replace backend authorization or change engine behavior on their own.

## Form designer background

`FormDesigner` accepts an optional `background` prop with any CSS background value, including colors, gradients, `transparent`, and CSS variables. Omit it to use the theme default. Both `standalone` and `embedded` retain a 12px corner radius; `embedded` removes only the outer border. This setting affects the container, not field controls or saved form metadata.

```tsx
<FormDesigner
  appearance="embedded"
  background="var(--business-form-background, #f8fafc)"
/>
```

The existing `style` prop takes precedence over `background` and can also customize `borderRadius`.

## 超时调度接入

**使用节点超时功能，业务系统必须自行接入调度。** 设计器仅保存超时规则；宿主调用 `FlowEngine.setTimeoutEnabled(true)` 只会开启全局超时能力，不会启动定时任务。宿主通过定时任务调用 `FlowEngine.timeoutService().executeDue(...)`，或通过延迟消息调用 `executeTimeout(taskId)`。多实例调度协调、重试与监控由宿主配置。参见[超时接入说明](../../docs/timeout-integration.md)。
