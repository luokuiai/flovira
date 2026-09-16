import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import dts from 'vite-plugin-dts'

export default defineConfig({
  plugins: [
    react(),
    dts({
      include: ['src'],
      exclude: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
      insertTypesEntry: true,
      afterDiagnostic: (diagnostics) => {
        if (diagnostics.length > 0) {
          throw new Error(`Type declaration generation failed with ${diagnostics.length} diagnostic(s)`)
        }
      },
    }),
  ],
  build: {
    lib: {
      entry: 'src/index.tsx',
      formats: ['es'],
      fileName: 'flovira-react-adapter-antd',
      cssFileName: 'flovira-react-adapter-antd',
    },
    rollupOptions: {
      external: [
        '@luokuiai/flovira-react-designer',
        'antd',
        'react',
        'react-dom',
        'react/jsx-runtime',
      ],
    },
  },
})
