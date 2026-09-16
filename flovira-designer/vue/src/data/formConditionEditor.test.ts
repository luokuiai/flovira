import { expect, test } from 'bun:test'
import { readFileSync } from 'node:fs'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as Vue from 'vue'
import * as helpers from './formDefinition'
import fixture from '../../../../flovira-core/src/test/resources/nested-form-conditions.json'

// Compile the real SFC and exercise its neutral controls with Vue's test renderer, without a browser dependency.
function component() {
  const source = readFileSync(new URL('../components/design/common/vue/FormConditionEditor.vue', import.meta.url), 'utf8')
  const script = compileScript(parse(source).descriptor, { id: 'form-condition-test', inlineTemplate: true }).content
  const js = new Bun.Transpiler({ loader: 'ts' }).transformSync(script)
    .replace(/import\s*\{([^}]+)\}\s*from\s*["']([^"']+)["'];?/g, (_, names, module) =>
      `const {${names.replace(/\s+as\s+/g, ':')}} = ${module === 'vue' ? 'Vue' : 'helpers'};`)
    .replace('export default', 'return')
  return new Function('Vue', 'helpers', js)(Vue, helpers)
}
type Element = { type: string; props: Record<string, any>; children: Element[]; text?: string; parent?: Element }
const element = (type: string, text?: string): Element => ({ type, props: {}, children: [], text })
function harness(rule?: any, disabled = false) {
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
    Vue.provide('floviraConditionFields', Vue.ref(helpers.getFormConditionFields(fixture.form as helpers.FormDefinition)))
    return () => Vue.h(editor, { rule, disabled, onApply: (value: any) => applied.push(value) })
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

test('Vue edits a same-row detail group and reopens it by business labels', async () => {
  const view = harness({ mode: 'rules', groups: [fixture.group], expression: fixture.anyExpression })
  try {
    expect(view.all().some(node => node.type === 'option' && view.text(node) === '报销明细 / 每一项 / 费用类型')).toBe(true)
    const quantifier = view.all().find(node => node.type === 'select' && node.props.value === 'ANY')!
    quantifier.props.onChange('ALL')
    await Vue.nextTick()
    view.button('应用条件').props.onClick()
    expect(view.applied[0].expression).toBe(fixture.allExpression)
    expect(view.applied[0].groups[0].conditions).toHaveLength(2)
  } finally { view.app.unmount() }
})

test('Vue rejects empty groups and does not expose apply in read-only mode', async () => {
  const view = harness()
  try {
    view.button('应用条件').props.onClick()
    await Vue.nextTick()
    expect(view.applied).toHaveLength(0)
    expect(view.all().some(node => node.props.role === 'alert')).toBe(true)
  } finally { view.app.unmount() }
  const readonly = harness({ mode: 'rules', groups: [fixture.group] }, true)
  try { expect(readonly.button('应用条件')).toBeUndefined() } finally { readonly.app.unmount() }
})
