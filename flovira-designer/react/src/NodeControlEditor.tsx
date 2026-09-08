import { getNodeControlConfig, getRejectTargetCandidates, setNodeControlConfig } from './model'
import type { DesignerUiAdapter, FloviraDefinition, FloviraNode, NodeControlConfig } from './types'

export function NodeControlEditor({ definition, node, ui, disabled, onChange }: {
  definition: FloviraDefinition
  node: FloviraNode
  ui: DesignerUiAdapter
  disabled: boolean
  onChange(node: FloviraNode): void
}) {
  const { Field, Checkbox, RadioGroup, Select } = ui
  const config = getNodeControlConfig(node)
  const candidates = getRejectTargetCandidates(definition, node.nodeCode)
  const change = (patch: Partial<NodeControlConfig>) => onChange(setNodeControlConfig(node, patch))
  return <div className="frd-node-controls">
    <Field label="节点控制策略">
      <div className="frd-node-controls__permissions">
        {([
          ['allowRollback', '允许退回'], ['allowTransfer', '允许转办'],
          ['allowAddSign', '允许加签'], ['allowMinusSign', '允许减签'],
        ] as const).map(([key, label]) => <Checkbox key={key} checked={config[key]} disabled={disabled}
          ariaLabel={label} onCheckedChange={(value) => change({ [key]: value })}>{label}</Checkbox>)}
      </div>
    </Field>
    {config.allowRollback && <>
      <Field label="退回策略">
        <RadioGroup ariaLabel="退回策略" value={config.rejectStrategy} disabled={disabled}
          options={[
            { value: 'TO_DRAFT', label: '退回发起人' },
            { value: 'TO_PREVIOUS', label: '退回上一节点' },
            { value: 'TO_SPECIFIED_NODE', label: '退回指定节点' },
            { value: 'TO_REJECTOR_SPECIFIED_NODE', label: '退回时指定节点' },
            ...(config.rejectStrategy === 'REJECT' ? [{ value: 'REJECT', label: '直接驳回（旧配置）' }] : []),
          ]} onValueChange={(value) => change({ rejectStrategy: value as NodeControlConfig['rejectStrategy'] })} />
      </Field>
      {config.rejectStrategy === 'TO_SPECIFIED_NODE' && <Field label="退回目标节点">
        <Select ariaLabel="退回目标节点" value={config.rejectTargetNodeCode} disabled={disabled}
          options={[{ value: '', label: '请选择前置审批节点' }, ...candidates.map((item) => ({ value: item.nodeCode, label: item.nodeName }))]}
          onValueChange={(value) => change({ rejectTargetNodeCode: value })} />
        {!candidates.some((item) => item.nodeCode === config.rejectTargetNodeCode) && <p role="alert" className="frd-condition-error">
          {config.rejectTargetNodeCode ? '原退回目标已失效，请重新选择' : candidates.length ? '请选择退回目标节点' : '当前没有可选的前置审批节点'}
        </p>}
      </Field>}
      {['TO_DRAFT', 'TO_SPECIFIED_NODE', 'TO_REJECTOR_SPECIFIED_NODE'].includes(config.rejectStrategy) && <Field label="退回后重新提交">
        <RadioGroup ariaLabel="退回后重新提交" value={config.resubmitStrategy} disabled={disabled}
          options={[
            { value: 'RESTART_FROM_BEGINNING', label: '重新顺序流转' },
            { value: 'CONTINUE_FROM_REJECTED_NODE', label: '回到执行退回的节点继续' },
          ]} onValueChange={(value) => change({ resubmitStrategy: value as NodeControlConfig['resubmitStrategy'] })} />
      </Field>}
    </>}
  </div>
}
