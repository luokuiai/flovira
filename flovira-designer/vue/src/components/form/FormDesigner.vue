<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FormDefinition, FormFieldDefinition } from '../../data/formDefinition'
import { copyForm, emptyForm, newFormField, validateForm, type FormDesignerInstance } from '../../data/formDesignerModel'
import { wfComponents } from '../../ui/components'
import FormFieldRow from './FormFieldRow.vue'
import './formDesigner.css'
const WfButton = wfComponents['wf-button']
const props = withDefaults(defineProps<{
  modelValue?: FormDefinition
  appearance?: 'standalone' | 'embedded'
  /** 容器背景，支持 CSS background 值；不传时使用主题默认背景。 */
  background?: string
  readOnly?: boolean
}>(), { appearance: 'standalone', readOnly: false })
const emit = defineEmits<{ (event: 'update:modelValue', value: FormDefinition): void }>()
const internal = ref<FormDefinition>(emptyForm())
const definition = computed(() => props.modelValue ?? internal.value)
const root = ref<HTMLDivElement>()
const error = ref('')
function update(fields: FormFieldDefinition[]) {
  if (props.readOnly) return
  const next = copyForm({ ...definition.value, fields })
  internal.value = next; error.value = ''
  emit('update:modelValue', copyForm(next))
}
function replace(index: number, field: FormFieldDefinition) { update(definition.value.fields.map((entry, i) => i === index ? field : entry)) }
function move(index: number, offset: number) {
  const fields = [...definition.value.fields]; const target = index + offset
  if (target < 0 || target >= fields.length) return
  ;[fields[index], fields[target]] = [fields[target], fields[index]]
  update(fields)
}
const api: FormDesignerInstance = {
  getDefinition: () => copyForm(definition.value),
  getJson: () => JSON.stringify(definition.value),
  validate: () => { const result = validateForm(definition.value); error.value = result.message; return result },
  focusFirstField: () => root.value?.querySelector<HTMLInputElement>('input:not(:disabled)')?.focus(),
}
defineExpose(api)
</script>
<template>
  <div ref="root" class="ffd-form" :data-appearance="appearance" :style="{ background }">
    <div class="ffd-heading"><span>表单字段</span><span class="ffd-count">{{ definition.fields.length }} 个字段</span></div>
    <div class="ffd-scroll">
      <div class="ffd-table">
        <div class="ffd-row ffd-columns" aria-hidden="true"><span>字段名称</span><span>字段键</span><span>数据类型</span><span /></div>
        <div v-if="!definition.fields.length" class="ffd-empty">暂无字段{{ !readOnly ? '，添加字段开始定义表单' : '' }}</div>
        <FormFieldRow v-for="(field, index) in definition.fields" :key="index" :field="field" :path="String(index + 1)" :depth="0" :read-only="readOnly"
          :first="index === 0" :last="index === definition.fields.length - 1"
          @change="replace(index, $event)" @remove="update(definition.fields.filter((_, i) => i !== index))" @move="move(index, $event)" />
        <WfButton v-if="!readOnly" text class="ffd-add" @click="update([...definition.fields, newFormField()])">＋ 添加字段</WfButton>
      </div>
    </div>
    <p v-if="error" class="ffd-error" role="alert">{{ error }}</p>
  </div>
</template>
