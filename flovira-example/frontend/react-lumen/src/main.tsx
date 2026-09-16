import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { lumenDesignerUi } from '@luokuiai/flovira-react-adapter-lumen'
import { ExampleApp } from '@flovira-example/react-common'
import '@luokuiai/lumen-ui/styles.css'
import '@luokuiai/lumen-theme-clarity'
import '@luokuiai/flovira-react-designer/style.css'
import '@luokuiai/flovira-react-adapter-lumen/style.css'
import '@flovira-example/react-common/style.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode><ExampleApp title="React + Lumen" ui={lumenDesignerUi} /></StrictMode>,
)
