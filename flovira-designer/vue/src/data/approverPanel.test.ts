import { expect, test } from 'bun:test'
import { readFileSync } from 'node:fs'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as Vue from 'vue'
import * as contracts from './contracts'
import * as options from './approverOptions'
import { DEMO_CAPABILITIES } from '../../../examples/capabilities'

// 编译实际审批面板，使用中立控件验证配置及选人交互，不依赖具体 UI 框架。
function component() {
  const source = readFileSync(new URL('../components/design/common/vue/between.vue', import.meta.url), 'utf8')
  const script = compileScript(parse(source).descriptor, { id: 'approver-panel-test', inlineTemplate: true }).content
  const js = new Bun.Transpiler({ loader: 'ts' }).transformSync(script)
    .replace(/import\s*\{([^}]+)\}\s*from\s*["']([^"']+)["'];?/g, (_, names, module) =>
      `const {${names.replace(/\s+as\s+/g, ':')}} = modules[${JSON.stringify(module)}];`)
    .replace(/import\s+(\w+)\s+from\s*["']([^"']+)["'];?/g, (_, name, module) =>
      `const ${name} = modules[${JSON.stringify(module)}];`)
    .replace('export default', 'return')
  const stub = Vue.defineComponent({ render: () => null })
  const picker = Vue.defineComponent({ props: ['permissionRows', 'resourceType'], emits: ['handleUserSelect'],
    setup: (props, { emit }) => () => Vue.h('picker', { resourceType: props.resourceType, rows: props.permissionRows,
      onSelect: (rows: any[]) => emit('handleUserSelect', rows) }) })
  return new Function('modules', js)({ vue: Vue, './selectUser.vue': picker, './nodeExtList.vue': stub,
    './nodeTimeout.vue': stub, '@/api/flow/definition': {
      designerCapabilities: async () => DEMO_CAPABILITIES, designerResourceItems: async () => [], designerSubjects: async () => [],
    }, '@/components/design/common/js/tool': { getPreviousNodes: () => [] },
    '@/utils/auth': { getFramework: () => 'test' }, '@/i18n': { useI18n: () => ({ t: (key: string) => key }) },
    '@/data/contracts': contracts, '@/data/approverOptions': options,
  })
}

type Element = { type: string; props: Record<string, any>; children: Element[]; text?: string; parent?: Element }
const element = (type: string, text?: string): Element => ({ type, props: {}, children: [], text })
async function harness(config: Record<string, unknown> = {}, disabled = false) {
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
  const form = Vue.reactive({ nodeCode: 'approval', nodeName: '审批', collaborativeWay: '1', listenerRows: [],
    ext: { approverRule: JSON.stringify({ schemaVersion: 1, strategyVersion: 1, strategy: 'ROLE', selectionType: 'RESOURCE',
      subjects: [{ id: 'finance', type: 'ROLE', name: '财务' }], config }) } })
  const app = renderer.createApp(component(), { modelValue: form, disabled })
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
  app.mount(root)
  await Vue.nextTick()
  const all = (node: Element = root): Element[] => [node, ...node.children.flatMap(child => all(child))]
  return { app, form, all, config: () => JSON.parse(form.ext.approverRule).config }
}

test('Vue edits both backend policies, selects transfer users, and reopens saved configuration', async () => {
  const view = await harness({ custom: 'keep' })
  try {
    const empty = view.all().find(node => node.type === 'select' && node.props.value === 'ERROR')!
    expect(empty).toBeTruthy()
    empty.props.onChange('TRANSFER_TO_USER')
    await Vue.nextTick()
    view.all().find(node => node.type === 'button' && node.children.some(child => child.text === '选择转交人员'))!.props.onClick()
    await Vue.nextTick()
    const picker = view.all().find(node => node.type === 'picker')!
    expect(picker.props.resourceType).toBe('USER')
    picker.props.onSelect([{ storageId: 'backup', handlerName: '备用审批人' }])
    await Vue.nextTick()
    expect(view.config()).toEqual({ custom: 'keep', emptyPolicy: 'TRANSFER_TO_USER',
      emptyPolicySubjects: [{ id: 'backup', type: 'USER', name: '备用审批人' }] })
    view.all().find(node => node.type === 'select' && node.props.value === 'SELF_APPROVE')!.props.onChange('AUTO_SKIP_OR_TRANSFER')
    await Vue.nextTick()
    const reopened = await harness(view.config(), true)
    try {
      expect(reopened.all().some(node => node.type === 'select' && node.props.value === 'AUTO_SKIP_OR_TRANSFER' && node.props.disabled)).toBe(true)
      expect(reopened.all().some(node => node.type === 'select' && node.props.value === 'TRANSFER_TO_USER')).toBe(true)
    } finally { reopened.app.unmount() }
  } finally { view.app.unmount() }
})
