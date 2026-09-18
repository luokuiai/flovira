import { expect, test } from 'bun:test'
import { readFileSync } from 'node:fs'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as Vue from 'vue'
import * as helpers from './lifecycle'
import { json2LogicFlowJson, logicFlowJsonToFlovira } from '../components/design/common/js/tool'

// Compile the real SFC and exercise its neutral controls with Vue's test renderer, without a browser dependency.
function component() {
  const source = readFileSync(new URL('../components/design/common/vue/LifecycleEditor.vue', import.meta.url), 'utf8')
  const script = compileScript(parse(source).descriptor, { id: 'form-condition-test', inlineTemplate: true }).content
  const js = new Bun.Transpiler({ loader: 'ts' }).transformSync(script)
    .replace(/import\s*\{([^}]+)\}\s*from\s*["']([^"']+)["'];?/g, (_, names, module) =>
      `const {${names.replace(/\s+as\s+/g, ':')}} = ${module === 'vue' ? 'Vue' : 'helpers'};`)
    .replace('export default', 'return')
  return new Function('Vue', 'helpers', js)(Vue, helpers)
}
type Element = { type: string; props: Record<string, any>; children: Element[]; text?: string; parent?: Element }
const element = (type: string, text?: string): Element => ({ type, props: {}, children: [], text })
function harness(value?: string, disabled = false, nodeType = '1') {
  const renderer = Vue.createRenderer<Element, Element>({
    createElement: tag => element(tag), createText: text => element('text', text), createComment: text => element('comment', text),
    setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text; node.children = [] },
    parentNode: node => node.parent!, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null,
    patchProp: (node, key, _, value) => { node.props[key] = value },
    insert: (node, parent, anchor) => {
      if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
      node.parent = parent
      const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node)
    },
    remove: node => { if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1) },
  })
  const applied: any[] = []
  const editor = component()
  const root = element('root')
  const app = renderer.createApp(Vue.defineComponent({ setup() {
    const model = Vue.ref(value)
    return () => Vue.h(editor, { modelValue: model.value, disabled, nodeType, 'onUpdate:modelValue': (value: string) => { model.value = value; applied.push(value) } })
  } }))
  for (const [name, tag] of [['wf-select', 'select'], ['wf-input', 'input']]) app.component(name, Vue.defineComponent({
    props: ['modelValue', 'disabled'], emits: ['update:modelValue'],
    setup(props, { emit, slots }) { return () => Vue.h(tag, { value: props.modelValue, disabled: props.disabled,
      onChange: (value: string) => emit('update:modelValue', value) }, slots.default?.()) },
  }))
  app.component('wf-option', Vue.defineComponent({ props: ['value', 'label', 'disabled'],
    setup: props => () => Vue.h('option', { value: props.value, disabled: props.disabled }, props.label) }))
  app.component('wf-button', Vue.defineComponent({ setup: (_, { attrs, slots }) => () => Vue.h('button', attrs, slots.default?.()) }))
  app.component('wf-form-item', Vue.defineComponent({ props: ['label'], setup: (props, { slots }) => () => Vue.h('label', {}, [props.label, slots.default?.()]) }))
  app.mount(root)
  const all = (node: Element = root): Element[] => [node, ...node.children.flatMap(child => all(child))]
  const text = (node: Element): string => node.type === 'comment' ? '' : (node.text || '') + node.children.map(text).join('')
  return { app, applied, all, text, button: (label: string) => all().find(node => node.type === 'button' && text(node) === label)! }
}


test('Vue edits lifecycle subscriptions, restricts phases and preserves JSON through graph conversion', async () => {
  const view = harness()
  try {
    view.button('添加回调').props.onClick()
    await Vue.nextTick()
    view.all().find(node => node.type === 'input')!.props.onChange('businessListener')
    await Vue.nextTick()
    expect(view.all().some(node => node.type === 'option' && node.props.value === 'AFTER_COMMIT')).toBe(false)
    view.all().find(node => node.type === 'select' && node.props.value === 'BEFORE_OPERATION')!.props.onChange('NODE_ENTERED')
    await Vue.nextTick()
    view.all().find(node => node.type === 'select' && node.props.value === 'IN_TRANSACTION')!.props.onChange('AFTER_COMMIT')
    await Vue.nextTick()
    const config = helpers.parseLifecycle(view.applied.at(-1))
    expect(config.subscriptions[0]).toMatchObject({ code: 'businessListener', point: 'NODE_ENTERED', phase: 'AFTER_COMMIT' })
    const ext = helpers.withLifecycle('[{"code":"custom","value":"kept"}]', config)
    const flow = { flowCode: 'example', ext, nodeList: [{ nodeCode: 'a', nodeType: 1, nodeRatio: '0', nodeName: '审批', ext, skipList: [] }] }
    const saved = JSON.parse(logicFlowJsonToFlovira(json2LogicFlowJson(flow)))
    expect(helpers.parseLifecycle(helpers.lifecycleValue(saved.ext))).toEqual(config)
    expect(helpers.parseLifecycle(helpers.lifecycleValue(saved.nodeList[0].ext))).toEqual(config)
    expect(saved.nodeList[0].ext).toContain('kept')
  } finally { view.app.unmount() }
})

test('Vue rejects gateway callbacks and keeps read-only controls disabled', () => {
  const config = helpers.parseLifecycle('{"schemaVersion":1,"subscriptions":[{"code":"bean","point":"NODE_LEFT","phase":"IN_TRANSACTION","order":0}]}')
  expect(() => helpers.validateLifecycle(config, '3')).toThrow()
  const view = harness(JSON.stringify(config), true)
  try { expect(view.all().filter(node => node.type === 'input' || node.type === 'select').every(node => node.props.disabled)).toBe(true) }
  finally { view.app.unmount() }
})

test('Vue import rejects legacy callbacks and malformed lifecycle without losing formLoad', () => {
  expect(() => json2LogicFlowJson({ listenerType: 'finish', listenerPath: 'oldBean' })).toThrow('旧监听配置')
  expect(() => json2LogicFlowJson({ ext: '[{"code":"lifecycle","value":"bad-json"}]' })).toThrow()
  const flow = { listenerType: 'formLoad', listenerPath: 'formLoader', nodeList: [] }
  expect(JSON.parse(logicFlowJsonToFlovira(json2LogicFlowJson(flow)))).toMatchObject(flow)
})
