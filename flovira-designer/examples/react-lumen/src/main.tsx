import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  ReactFlowDesigner,
  createInitialDefinition,
  type DesignerCapabilities,
  type FloviraDefinition,
} from '@luokuiai/flovira-react-designer'
import { lumenDesignerUi } from '@luokuiai/flovira-react-adapter-lumen'
import '@luokuiai/lumen-ui/styles.css'
import '@luokuiai/lumen-theme-clarity'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-lumen/style.css'
import { loadDesignerCapabilities, queryDesignerResources } from './api'
import { OrganizationParticipantPicker } from './OrganizationParticipantPicker'
import './styles.css'

const initial = createInitialDefinition()
initial.flowCode = 'expense_approval'
initial.flowName = '费用报销审批'

function App() {
  const [saved, setSaved] = useState<FloviraDefinition | null>(null)
  const [capabilities, setCapabilities] = useState<DesignerCapabilities>()

  useEffect(() => {
    loadDesignerCapabilities().then(setCapabilities)
  }, [])

  return (
    <main>
      <ReactFlowDesigner
        defaultValue={initial}
        ui={lumenDesignerUi}
        capabilities={capabilities}
        queryResources={queryDesignerResources}
        renderApproverEditor={(context) => context.strategy.editorKey === 'organization-user-picker'
          ? <OrganizationParticipantPicker {...context} />
          : null}
        onSave={(definition) => setSaved(definition)}
      />
      {saved && <div className="save-toast" role="status">已保存 {saved.flowName}</div>}
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
