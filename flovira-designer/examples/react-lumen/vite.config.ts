import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { mockBackend } from './mockBackend'

export default defineConfig({
  plugins: [react(), mockBackend()],
  server: { port: 5184 },
})
