import { StrictMode, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  ReactFlowDesigner,
  createInitialDefinition,
  type FloviraDefinition,
} from '@luokuiai/flovira-react-designer'
import '@luokuiai/flovira-react-designer/style.css'
import './styles.css'

const initial = createInitialDefinition()
initial.flowCode = 'expense_approval'
initial.flowName = '费用报销审批'

function App() {
  const [saved, setSaved] = useState<FloviraDefinition | null>(null)

  return (
    <main>
      <ReactFlowDesigner
        defaultValue={initial}
        dataProvider={{
          queryResources: async () => ({
            items: [
              {
                id: 'finance_review',
                code: 'finance_review',
                name: '财务复核流程',
                resourceType: 'SUBPROCESS',
                metadata: { version: 3 },
              },
              {
                id: 'manager_review',
                code: 'manager_review',
                name: '管理层审批流程',
                resourceType: 'SUBPROCESS',
                metadata: { version: 2 },
              },
            ],
            total: 2,
          }),
        }}
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
