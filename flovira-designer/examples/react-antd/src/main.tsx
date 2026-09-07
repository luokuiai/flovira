import { StrictMode, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import {
  ReactFlowDesigner,
  createInitialDefinition,
  type FloviraDefinition,
} from '@luokuiai/flovira-react-designer'
import { antdDesignerUi } from '@luokuiai/flovira-react-adapter-antd'
import 'antd/dist/reset.css'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-antd/style.css'
import './styles.css'

const initial = createInitialDefinition()
initial.flowCode = 'expense_approval'
initial.flowName = '费用报销审批'

function App() {
  const [saved, setSaved] = useState<FloviraDefinition | null>(null)

  return (
    <ConfigProvider locale={zhCN} theme={{ cssVar: {} }} componentSize="small">
      <main>
        <ReactFlowDesigner
          defaultValue={initial}
          ui={antdDesignerUi}
          queryResources={async () => ({
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
            })}
          onSave={(definition) => setSaved(definition)}
        />
        {saved && <div className="save-toast" role="status">已保存 {saved.flowName}</div>}
      </main>
    </ConfigProvider>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
