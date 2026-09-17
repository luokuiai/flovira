import { StrictMode, useEffect, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  ReactFlowDesigner,
  FormDesigner,
  getFormConditionFields,
  getFormPermissionFields,
  createInitialDefinition,
  type DesignerCapabilities,
  type FormDefinition,
  type FormDesignerInstance,
  type FloviraDefinition,
  type ReactFlowDesignerRef,
} from '@luokuiai/flovira-react-designer'
import { lumenDesignerUi } from '@luokuiai/flovira-react-adapter-lumen'
import { Button, Tabs } from '@luokuiai/lumen-ui'
import '@luokuiai/lumen-ui/styles.css'
import '@luokuiai/lumen-theme-clarity'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-lumen/style.css'
import { loadDesignerCapabilities, queryDesignerResources } from './api'
import { useApproverPicker } from './useApproverPicker'
import { FlowPreviewExamples } from './FlowPreviewExamples'
import './styles.css'

const initial = createInitialDefinition()
initial.flowCode = 'expense_approval'
initial.flowName = '费用报销审批'

const initialForm: FormDefinition = { schemaVersion: '1', fields: [
  { key: 'expense_title', label: '报销事由', dataType: 'string' },
  { key: 'amount', label: '报销金额', dataType: 'number' },
  { key: 'approval_deadline', label: '审批截止时间', dataType: 'datetime' },
  { key: 'details', label: '报销明细', dataType: 'array', items: { dataType: 'object', fields: [
    { key: 'item_name', label: '费用名称', dataType: 'string' },
    { key: 'amount', label: '费用金额', dataType: 'number' },
  ] } },
] }

function App() {
  const { selectApprover, picker } = useApproverPicker()
  const formRef = useRef<FormDesignerInstance>(null)
  const [form, setForm] = useState(initialForm)
  const [savedForm, setSavedForm] = useState('')
  const saveForm = () => {
    if (formRef.current?.validate().valid) setSavedForm(formRef.current.getJson())
  }
  const designerRef = useRef<ReactFlowDesignerRef>(null)
  const [error, setError] = useState('')
  const saveDesign = () => {
    const designer = designerRef.current
    if (!designer) return
    const result = designer.validate()
    if (!result.valid) { setError(result.issues.map(issue => issue.message).join('；')); return }
    setSaved(JSON.parse(designer.getFlowJson()))
    designer.resetDirty()
    setError('')
  }
  const [tab, setTab] = useState<'form' | 'designer' | 'preview'>('form')
  const [saved, setSaved] = useState<FloviraDefinition | null>(null)
  const [capabilities, setCapabilities] = useState<DesignerCapabilities>()

  useEffect(() => {
    loadDesignerCapabilities().then(setCapabilities)
  }, [])

  return (
    <main className={tab === 'designer' ? 'demo-main--designer' : undefined}>
      <Tabs value={tab} idPrefix="demo" className="demo-tabs"
        options={[{ value: 'form', label: '表单定义' }, { value: 'designer', label: '流程设计' }, { value: 'preview', label: '流程预览' }]}
        onChange={value => setTab(value as 'form' | 'designer' | 'preview')} />
      <div id="demo-panel-form" role="tabpanel" aria-labelledby="demo-tab-form" hidden={tab !== 'form'}>
        <header className="demo-form-header">
          <h1>费用报销表单</h1>
          <Button variant="outline" onClick={saveForm}>保存</Button>
        </header>
        <FormDesigner ref={formRef} value={form} onChange={setForm} ui={lumenDesignerUi} />
        {savedForm && <details className="demo-form-result">
          <summary>已保存到示例状态，查看 JSON</summary><pre>{JSON.stringify(JSON.parse(savedForm), null, 2)}</pre>
        </details>}
      </div>
      <div id="demo-panel-preview" role="tabpanel" aria-labelledby="demo-tab-preview" hidden={tab !== 'preview'}>
        <FlowPreviewExamples />
      </div>
      <div id="demo-panel-designer" role="tabpanel" aria-labelledby="demo-tab-designer" hidden={tab !== 'designer'}>
      <ReactFlowDesigner
        defaultValue={initial}
        queryConditionFields={async () => getFormConditionFields(form)}
        queryFormFields={async () => getFormPermissionFields(form)}
        ui={lumenDesignerUi}
        capabilities={capabilities}
        queryResources={queryDesignerResources}
        onSelectApprover={selectApprover}
        ref={designerRef}
        renderToolbar={({ defaultToolbar, disabled }) => <div className="demo-designer-toolbar">
          {defaultToolbar}
          <Button size="sm" variant="outline" disabled={disabled} onClick={saveDesign}>保存</Button>
        </div>}
      />
      {error && <p role="alert">{error}</p>}
      {saved && <div className="save-toast" role="status">已保存 {saved.flowName}</div>}
      </div>
      {picker}
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
