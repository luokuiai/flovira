import { useRef, useState } from 'react'
import { FormDesigner, ReactFlowDesigner, createInitialDefinition, getFormConditionFields, getFormPermissionFields, setApproverRule,
  type FormDefinition, type FormDesignerInstance, type ReactFlowDesignerRef } from '@luokuiai/flovira-react-designer'
import { DEMO_CAPABILITIES } from '../../capabilities'

const initialForm: FormDefinition = { schemaVersion: '1', fields: [
  { key: 'expense_title', label: '报销事由', dataType: 'string' },
  { key: 'Amount', label: '报销金额', dataType: 'number' },
  { key: 'details', label: '报销明细', dataType: 'array', items: { dataType: 'object', fields: [
    { key: 'item_name', label: '费用名称', dataType: 'string' },
    { key: 'amount', label: '费用金额', dataType: 'number' },
  ] } },
] }
export function FormExample() {
  const [flow] = useState(() => {
    const definition = createInitialDefinition()
    definition.flowName = '费用报销审批'
    definition.nodeList = definition.nodeList.map(node => node.nodeType === '1'
      ? setApproverRule(node, 'USER', [{ id: 'demo-reviewer', type: 'USER', name: '示例审批人' }]) : node)
    return definition
  })
  const [form, setForm] = useState(initialForm)
  const [tab, setTab] = useState('form')
  const [message, setMessage] = useState('')
  const formRef = useRef<FormDesignerInstance>(null)
  const flowRef = useRef<ReactFlowDesignerRef>(null)
  const [snapshot, setSnapshot] = useState('')
  function save() {
    if (!formRef.current!.validate().valid) { setTab('form'); return }
    const result = flowRef.current!.validate()
    if (!result.valid) { setMessage(result.issues.map(issue => issue.message).join('；')); setTab('flow'); return }
    setSnapshot(JSON.stringify({ form: formRef.current!.getDefinition(), definition: flowRef.current!.getDefinition() }, null, 2))
    setMessage('表单与流程已一起保存到示例状态')
  }
  return <main className="form-example">
    <header><h1>费用报销</h1><button onClick={save}>保存</button></header>
    <nav aria-label="编辑内容">
      <button aria-pressed={tab === 'form'} onClick={() => { setTab('form'); requestAnimationFrame(() => formRef.current?.focusFirstField()) }}>表单定义</button>
      <button aria-pressed={tab === 'flow'} onClick={() => setTab('flow')}>流程设计</button>
    </nav>
    <section hidden={tab !== 'form'} className="form-example-panel">
      <FormDesigner ref={formRef} value={form} onChange={setForm} appearance="embedded" />
    </section>
    <section hidden={tab !== 'flow'} className="form-example-flow">
      <ReactFlowDesigner ref={flowRef} defaultValue={flow} appearance="embedded" capabilities={DEMO_CAPABILITIES}
        queryConditionFields={async () => getFormConditionFields(form)} queryFormFields={async () => getFormPermissionFields(form)} />
    </section>
    {message && <p role="status">{message}</p>}
    {snapshot && <details><summary>已保存的组合数据</summary><pre>{snapshot}</pre></details>}
  </main>
}
