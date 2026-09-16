import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const target = process.env.EXAMPLE_BACKEND_URL || 'http://localhost:8081'

export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@flovira-example/common': fileURLToPath(new URL('../common/src/index.ts', import.meta.url)) } },
  server: { proxy: { '/api': { target }, '/flovira': { target } } },
})
