import { expect, test } from 'bun:test'
import { readFileSync } from 'node:fs'
import { useFlowDesigner } from '../composables/useFlowDesigner'
import { useFlowJson } from '../composables/useFlowJson'

test('Vue shell appearance is independent of toolbar visibility and internal controls', () => {
  const source = readFileSync(new URL('../components/design/FlowDesigner.vue', import.meta.url), 'utf8')
  expect(source).toContain(':data-appearance="props.appearance"')
  expect(source).toContain("appearance: 'standalone'")
  const embedded = source.match(/\.wf-designer-shell--workbench\[data-appearance='embedded'\]\s*\{([^}]+)\}/)![1]
  expect(embedded).toContain('border: 0;')
  expect(embedded).toContain('border-radius: 0;')
  expect(embedded).toContain('box-shadow: none;')
  const card = source.match(/\.wf-designer-shell--workbench\[data-appearance\]\s*\{([^}]+)\}/)![1]
  expect(card).toContain('padding: 0;')
  expect(card).toContain('border-radius: var(--wf-radius, 8px);')
  expect(source).toContain('[data-appearance] > .wf-designer-body')
})

test('Vue exposes editing methods without persistence actions or callbacks', async () => {
  const source = readFileSync(new URL('../components/design/FlowDesigner.vue', import.meta.url), 'utf8')
  const types = readFileSync(new URL('../designer/types.ts', import.meta.url), 'utf8')
  const api = useFlowDesigner()
  expect(api).not.toHaveProperty('save')
  expect(api).not.toHaveProperty('publish')
  expect(types).not.toContain('onSave')
  expect(types).not.toContain('onPublish')
  expect(source).not.toContain("emit('saved'")
  expect(source).not.toContain("emit('before-save'")
  expect(source).toContain('v-if="props.toolbar !== false" name="toolbar"')
  let dirty = true
  api.designerRef.value = {
    getFlowJson: () => '{"flowName":"业务草稿"}',
    isDirty: () => dirty,
    resetDirty: () => { dirty = false },
    validate: async () => true,
    validateStructure: () => ({ valid: true, errors: [] }),
  } as any
  expect(await api.validate()).toBe(true)
  expect(api.validateStructure().valid).toBe(true)
  expect(JSON.parse(api.getFlowJson()).flowName).toBe('业务草稿')
  const data = useFlowJson(api.designerRef)
  data.sync()
  expect(data.dirty.value).toBe(true)
  expect(data.bind).not.toHaveProperty('onSaved')
  api.resetDirty()
  data.sync()
  expect(data.dirty.value).toBe(false)
})
