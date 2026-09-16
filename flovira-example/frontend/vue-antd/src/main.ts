import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { FloviraDesigner, setDataProvider, setUiAdapter } from '@luokuiai/flovira-vue-designer'
import { antdvAdapter } from '@luokuiai/flovira-vue-designer/antdv'
import '@luokuiai/flovira-vue-designer/style'
import App from './App.vue'
import { createExampleProvider } from './provider'
import './style.css'

let selectedUser = 'alice'
setUiAdapter(antdvAdapter)
setDataProvider(createExampleProvider(() => selectedUser))

const app = createApp(App, {
  onUserChange: (user: string) => { selectedUser = user },
})
app.use(createPinia()).use(Antd).use(FloviraDesigner).mount('#app')
