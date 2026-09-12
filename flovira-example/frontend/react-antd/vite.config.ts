import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

const target = process.env.EXAMPLE_BACKEND_URL || 'http://localhost:8081'
export default defineConfig({
  plugins: [react()],
  resolve: { alias: {
    '@flovira-example/react-common/style.css': fileURLToPath(new URL('../react-common/src/style.css', import.meta.url)),
    '@flovira-example/react-common': fileURLToPath(new URL('../react-common/src/index.tsx', import.meta.url)),
    '@flovira-example/common': fileURLToPath(new URL('../common/src/index.ts', import.meta.url)),
  } },
  server: { proxy: { '/api': { target }, '/flovira': { target } } },
})
