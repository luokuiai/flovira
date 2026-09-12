import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { antdDesignerUi } from '@luokuiai/flovira-react-adapter-antd'
import { ExampleApp } from '@flovira-example/react-common'
import 'antd/dist/reset.css'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-antd/style.css'
import '@flovira-example/react-common/style.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode><ExampleApp title="React + Ant Design" ui={antdDesignerUi} /></StrictMode>,
)
