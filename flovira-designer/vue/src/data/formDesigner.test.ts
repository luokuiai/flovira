import { expect, test } from 'bun:test'
import { readFileSync } from 'node:fs'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as Vue from 'vue'
import * as model from './formDesignerModel'
import type { FormDefinition } from './formDefinition'

const controls = {
  wfComponents: Object.fromEntries(['input', 'select', 'option', 'button'].map(name => ['wf-' + name, Vue.defineComponent({
    props: ['modelValue', 'value', 'label', 'disabled'], emits: ['update:modelValue'],
    setup: (props, { emit, attrs, slots }) => () => Vue.h(name, {
      ...attrs, value: props.modelValue ?? props.value, disabled: props.disabled,
      onChange: (value: string) => emit('update:modelValue', value),
    }, props.label || slots.default?.()),
  })])),
}
function component(name: string): any {
  const source = readFileSync(new URL('../components/form/' + name + '.vue', import.meta.url), 'utf8')
  const script = compileScript(parse(source, { filename: name + '.vue' }).descriptor, { id: name, inlineTemplate: true }).content
  const js = new Bun.Transpiler({ loader: 'ts' }).transformSync(script)
    .replace(/import\s*\{([^}]+)\}\s*from\s*["']([^"']+)["'];?/g, (_, names, module) =>
      `const {${names.replace(/\s+as\s+/g, ':')}} = ${module === 'vue' ? 'Vue' : module.includes('ui/components') ? 'controls' : 'model'};`)
    .replace(/import FormFieldRow from [^;]+;/, 'const FormFieldRow = row;')
    .replace(/import\s*["'][^"']+\.css["'];?/, '')
    .replace('export default', 'return')
  return new Function('Vue', 'model', 'controls', 'row', js)(Vue, model, controls, name === 'FormDesigner' ? component('FormFieldRow') : null)
}
type Element = { type: string; props: Record<string, any>; children: Element[]; text?: string; parent?: Element }
const element = (type: string, text?: string): Element => ({ type, props: {}, children: [], text })
function harness(readOnly = false) {
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
  const original: FormDefinition = { schemaVersion: '1', renderer: { layout: 'host' }, fields: [
    { key: 'Order_ID', label: '订单编号', dataType: 'string' },
    { key: 'details', label: '明细', dataType: 'array', items: { dataType: 'object', fields: [{ key: 'AMOUNT', label: '金额', dataType: 'number' }] } },
  ] }
  const value = Vue.ref(original)
  const api = Vue.ref<model.FormDesignerInstance>()
  const appearance = Vue.ref<Record<string, unknown>>({})
  const editor = component('FormDesigner')
  const root = element('root')
  const app = renderer.createApp({ setup: () => () => Vue.h(editor, {
    ...appearance.value,
    ref: api, modelValue: value.value, readOnly, 'onUpdate:modelValue': (next: FormDefinition) => { value.value = next },
  }) })
  app.mount(root)
  const all = (node: Element = root): Element[] => [node, ...node.children.flatMap(child => all(child))]
  const text = (node: Element): string => node.type === 'comment' ? '' : (node.text || '') + node.children.map(text).join('')
  return { app, original, value, api, appearance, all,
    control: (label: string) => all().find(node => node.props['aria-label'] === label)!,
    button: (label: string) => all().find(node => node.type === 'button' && text(node) === label)! }
}
test('Vue customizes the container background without changing form data and restores the theme when removed', async () => {
  const view = harness()
  try {
    view.appearance.value = { appearance: 'embedded', background: '#123456' }
    await Vue.nextTick()
    const root = view.all().find(node => node.props.class === 'ffd-form')!
    expect(root.props.style.background).toBe('#123456')
    expect(view.api.value!.getDefinition()).toEqual(view.original)
    view.appearance.value = { background: '#123456', style: { background: 'transparent' } }
    await Vue.nextTick()
    expect(root.props.style.background).toBe('transparent')
    view.appearance.value = {}
    await Vue.nextTick()
    expect(root.props.style.background).toBeUndefined()
  } finally { view.app.unmount() }
})
test('Vue edits nested fields through v-model and exposes isolated snapshots', async () => {
  const view = harness()
  try {
    view.control('字段名称 1').props.onChange('业务单号')
    await Vue.nextTick()
    expect(view.original.fields[0].label).toBe('订单编号')
    expect(view.api.value!.getDefinition().fields[0].label).toBe('业务单号')
    expect(view.api.value!.validate().valid).toBe(true)
    expect(JSON.parse(view.api.value!.getJson()).renderer).toEqual({ layout: 'host' })
    view.button('＋ 添加子字段').props.onClick()
    await Vue.nextTick()
    expect(view.control('字段名称 2[].2')).toBeDefined()
    expect(view.api.value!.validate().valid).toBe(false)
    view.control('数据类型 2').props.onChange('string')
    await Vue.nextTick()
    expect(view.value.value.fields[1].dataType).toBe('array')
    view.button('确认切换').props.onClick()
    await Vue.nextTick()
    expect(view.value.value.fields[1].items).toBeUndefined()
    expect(view.api.value!.validate().valid).toBe(true)
    expect(view.all().some(node => 'tabindex' in node.props)).toBe(false)
  } finally { view.app.unmount() }
})
test('Vue read-only editor has disabled controls and no editing buttons', () => {
  const view = harness(true)
  try {
    expect(view.all().filter(node => ['input', 'select'].includes(node.type)).every(node => node.props.disabled)).toBe(true)
    expect(view.all().filter(node => node.type === 'button')).toHaveLength(0)
  } finally { view.app.unmount() }
})
test('Vue exposes direct move and delete icons with boundary states', async () => {
  const view = harness()
  try {
    expect(view.control('上移字段 1').props.disabled).toBe(true)
    expect(view.control('下移字段 2').props.disabled).toBe(true)
    view.control('上移字段 2').props.onClick()
    await Vue.nextTick()
    expect(view.value.value.fields[0].key).toBe('details')
    view.control('下移字段 1').props.onClick()
    await Vue.nextTick()
    expect(view.value.value.fields[0].key).toBe('Order_ID')
    view.control('删除字段 2[].1').props.onClick()
    await Vue.nextTick()
    expect(view.value.value.fields[1].items!.fields).toHaveLength(0)
    view.control('删除字段 2').props.onClick()
    await Vue.nextTick()
    expect(view.value.value.fields).toHaveLength(1)
  } finally { view.app.unmount() }
})
test('Vue restricts detail child types and rejects deeper imported definitions', () => {
  const view = harness()
  try {
    const child = view.control('数据类型 2[].1')
    expect(child.children.filter(node => node.type === 'option').map(node => node.props.value)).not.toContain('object')
    expect(child.children.filter(node => node.type === 'option').map(node => node.props.value)).not.toContain('array')
    expect(model.validateForm({ schemaVersion: '1', fields: [{
      key: 'a', label: '数组', dataType: 'array', items: { dataType: 'array', items: { dataType: 'string' } },
    }] }).message).toContain('不支持多层嵌套')
  } finally { view.app.unmount() }
})
