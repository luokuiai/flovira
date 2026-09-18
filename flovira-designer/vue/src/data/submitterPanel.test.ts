import { expect, test } from 'bun:test'
import { readFileSync } from 'node:fs'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as Vue from 'vue'
import * as contracts from './contracts'
import { DEMO_CAPABILITIES } from '../../../examples/capabilities'

// 编译实际提交范围面板，验证配置及选人交互，不依赖具体 UI 框架。
function component(capabilities = DEMO_CAPABILITIES) {
  const source = readFileSync(new URL('../components/design/common/vue/SubmitterEditor.vue', import.meta.url), 'utf8')
  const script = compileScript(parse(source).descriptor, { id: 'submitter-panel-test', inlineTemplate: true }).content
  const js = new Bun.Transpiler({ loader: 'ts' }).transformSync(script)
    .replace(/import\s*\{([^}]+)\}\s*from\s*["']([^"']+)["'];?/g, (_, names, module) =>
      `const {${names.replace(/\s+as\s+/g, ':')}} = modules[${JSON.stringify(module)}];`)
    .replace(/import\s+(\w+)\s+from\s*["']([^"']+)["'];?/g, (_, name, module) =>
      `const ${name} = modules[${JSON.stringify(module)}];`)
    .replace('export default', 'return')
  const picker = Vue.defineComponent({ props: ['permissionRows', 'resourceType'], emits: ['handleUserSelect'],
    setup: (props, { emit }) => () => Vue.h('picker', { resourceType: props.resourceType, rows: props.permissionRows,
      onSelect: (rows: any[]) => emit('handleUserSelect', rows) }) })
  return new Function('modules', js)({ vue: Vue, './selectUser.vue': picker,
    '@/api/flow/definition': { designerCapabilities: async () => capabilities },
    '@/data/contracts': contracts,
  })
}

type Element = { type: string; props: Record<string, any>; children: Element[]; text?: string; parent?: Element }
const element = (type: string, text?: string): Element => ({ type, props: {}, children: [], text })
async function harness(saved?: Record<string, unknown>, disabled = false, capabilities = DEMO_CAPABILITIES) {
  // v-show 在自定义 renderer 中无需写浏览器样式。
  const renderer = Vue.createRenderer<Element, Element>({
    createElement: tag => Object.assign(element(tag), { style: {} }), createText: text => element('text', text),
    createComment: text => element('comment', text), setText: (node, text) => { node.text = text },
    setElementText: (node, text) => { node.text = text; node.children = [] }, parentNode: node => node.parent!,
    nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null,
    patchProp: (node, key, _, value) => { node.props[key] = value },
    insert: (node, parent) => { node.parent = parent; parent.children.push(node) },
    remove: node => { if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1) },
  })
  const form = Vue.reactive({ nodeCode: 'start', nodeName: '开始', ext: saved ? { submitterRule: JSON.stringify(saved) } : {} } as Record<string, any>)
  const app = renderer.createApp(component(capabilities), { modelValue: form, disabled })
  for (const name of ['wf-form', 'wf-form-item', 'wf-tooltip', 'wf-icon', 'wf-table', 'wf-dialog']) {
    app.component(name, Vue.defineComponent({ props: ['label'], setup: (props, { slots }) => () => Vue.h(name, {}, [props.label, slots.default?.()]) }))
  }
  for (const name of ['wf-input', 'wf-input-number', 'wf-table-column', 'svg-icon']) app.component(name, { render: () => null })
  app.component('wf-select', Vue.defineComponent({ props: ['modelValue', 'disabled'], emits: ['update:modelValue', 'change'],
    setup: (props, { emit, slots }) => () => Vue.h('select', { value: props.modelValue, disabled: props.disabled,
      onChange: (value: string) => { emit('update:modelValue', value); emit('change', value) } }, slots.default?.()) }))
  app.component('wf-option', Vue.defineComponent({ props: ['value', 'label'], setup: props => () => Vue.h('option', { value: props.value }, props.label) }))
  app.component('wf-button', Vue.defineComponent({ setup: (_, { attrs, slots }) => () => Vue.h('button', attrs, slots.default?.()) }))
  const root = element('root')
  const instance = app.mount(root) as unknown as { validate(): Promise<void> }
  await Vue.nextTick()
  const all = (node: Element = root): Element[] => [node, ...node.children.flatMap(child => all(child))]
  return { app, form, all, validate: instance.validate, rule: () => form.ext.submitterRule ? JSON.parse(form.ext.submitterRule) : undefined }
}


test('Vue defaults to all, selects roles using existing picker, preserves version and reopens read-only', async () => {
  const view = await harness()
  try {
    const select = () => view.all().find(node => node.type === 'select')!
    expect(select().props.value).toBe('ALL')
    expect(view.rule()).toBeUndefined()
    select().props.onChange('ROLE')
    await Vue.nextTick()
    expect(view.rule().subjects).toEqual([])
    await expect(view.validate()).rejects.toThrow('请选择有效的可提交人员范围')
    view.all().find(node => node.type === 'button' && node.children.some(child => child.text === '选择指定角色'))!.props.onClick()
    await Vue.nextTick()
    const picker = view.all().find(node => node.type === 'picker')!
    expect(picker.props.resourceType).toBe('ROLE')
    picker.props.onSelect([{ storageId: 'finance', handlerName: '财务' }])
    await Vue.nextTick()
    expect(view.rule()).toMatchObject({ strategy: 'ROLE', strategyVersion: 1, subjects: [{ id: 'finance', type: 'ROLE', name: '财务' }] })
    expect(view.rule().config).toEqual({})
    await view.validate()
    const reopened = await harness(view.rule(), true)
    try { expect(reopened.all().find(node => node.type === 'select')!.props).toMatchObject({ value: 'ROLE', disabled: true }) }
    finally { reopened.app.unmount() }
    select().props.onChange('ALL')
    await Vue.nextTick()
    expect(view.rule().subjects).toEqual([])
    expect(view.rule().strategy).toBe('ALL')
  } finally { view.app.unmount() }
})

test('Vue preserves unsupported saved submission rules instead of silently allowing all', async () => {
  const view = await harness({ schemaVersion: 99, strategy: 'USER', subjects: [] })
  try {
    expect(view.all().find(node => node.type === 'select')!.props.value).toBe('')
    expect(view.rule().schemaVersion).toBe(99)
    await expect(view.validate()).rejects.toThrow()
  } finally { view.app.unmount() }
})
