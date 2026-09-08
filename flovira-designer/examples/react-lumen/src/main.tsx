import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  ReactFlowDesigner,
  createInitialDefinition,
  type DesignerCapabilities,
  type DesignerConditionFieldLoader,
  type FloviraDefinition,
} from '@luokuiai/flovira-react-designer'
import { lumenDesignerUi } from '@luokuiai/flovira-react-adapter-lumen'
import '@luokuiai/lumen-ui/styles.css'
import '@luokuiai/lumen-theme-clarity'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-lumen/style.css'
import { loadDesignerCapabilities, queryDesignerResources } from './api'
import { OrganizationParticipantPicker } from './OrganizationParticipantPicker'
import { FlowPreviewExamples } from './FlowPreviewExamples'
import './styles.css'

const initial = createInitialDefinition()
initial.flowCode = 'expense_approval'
initial.flowName = '费用报销审批'

// 示例模拟业务表单字段；实际项目在此调用自己的表单接口。
const queryFormConditionFields: DesignerConditionFieldLoader = async () => [
  { code: 'amount', label: '采购金额', type: 'NUMBER' },
  { code: 'department', label: '申请部门', type: 'STRING' },
  { code: 'urgent', label: '是否紧急', type: 'BOOLEAN' },
]

function App() {
  const [tab, setTab] = useState<'designer' | 'preview'>('preview')
  const [saved, setSaved] = useState<FloviraDefinition | null>(null)
  const [capabilities, setCapabilities] = useState<DesignerCapabilities>()

  useEffect(() => {
    loadDesignerCapabilities().then(setCapabilities)
  }, [])

  return (
    <main>
      <div className="demo-tabs" role="tablist" aria-label="示例内容">
        {([{ id: 'designer', label: '流程设计' }, { id: 'preview', label: '流程预览' }] as const).map((item) => (
          <button key={item.id} id={`tab-${item.id}`} type="button" role="tab"
            aria-selected={tab === item.id} aria-controls={`panel-${item.id}`} tabIndex={tab === item.id ? 0 : -1}
            onClick={() => setTab(item.id)}
            onKeyDown={(event) => {
              if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
              event.preventDefault()
              const tabs = ['designer', 'preview'] as const
              const index = tabs.indexOf(tab)
              const next = event.key === 'Home' ? tabs[0] : event.key === 'End' ? tabs[1]
                : tabs[(index + (event.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length]
              setTab(next)
              document.getElementById(`tab-${next}`)?.focus()
            }}>{item.label}</button>
        ))}
      </div>
      <div id="panel-preview" role="tabpanel" aria-labelledby="tab-preview" hidden={tab !== 'preview'} tabIndex={0}>
        <FlowPreviewExamples />
      </div>
      <div id="panel-designer" role="tabpanel" aria-labelledby="tab-designer" hidden={tab !== 'designer'} tabIndex={0}>
      <ReactFlowDesigner
        defaultValue={initial}
        queryConditionFields={queryFormConditionFields}
        ui={lumenDesignerUi}
        capabilities={capabilities}
        queryResources={queryDesignerResources}
        renderApproverEditor={(context) => context.strategy.editorKey === 'organization-user-picker'
          ? <OrganizationParticipantPicker {...context} />
          : null}
        onSave={(definition) => setSaved(definition)}
      />
      {saved && <div className="save-toast" role="status">已保存 {saved.flowName}</div>}
      </div>
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
