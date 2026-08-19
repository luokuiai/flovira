import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

/**
 * Flovira 设计器 · Ant Design Vue 4 消费示例。
 *
 * 以「第三方业务工程」的姿势消费 npm 包 @luokuiai/flovira-vue-designer（本地 workspace:* 指向 ../../vue 的 dist-lib）。
 * 应用外壳与设计器全部组件均走 Ant Design Vue 4（main.ts 中 setUiAdapter(antdvAdapter)），不依赖 Element Plus。
 * 运行前确保库已 `bun build:lib`。
 *
 * @author warm
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    dedupe: ['vue', 'pinia']
  },
  optimizeDeps: {
    exclude: ['@luokuiai/flovira-vue-designer']
  },
  server: {
    port: 5181,
    open: false,
    fs: { allow: [path.resolve(__dirname, '../../')] }
  }
})
