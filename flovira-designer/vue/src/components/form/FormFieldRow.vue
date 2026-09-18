<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { FormFieldDefinition } from '../../data/formDefinition'
import { changeFieldType, formFieldTypes, hasNestedFields, newFormField } from '../../data/formDesignerModel'
import { wfComponents } from '../../ui/components'
const WfInput = wfComponents['wf-input']
const WfSelect = wfComponents['wf-select']
const WfOption = wfComponents['wf-option']
const WfButton = wfComponents['wf-button']
const props = defineProps<{
  field: FormFieldDefinition; path: string; depth: number; item?: boolean; readOnly?: boolean; first?: boolean; last?: boolean
}>()
const emit = defineEmits<{
  (event: 'change', field: FormFieldDefinition): void
  (event: 'remove'): void
  (event: 'move', offset: number): void
}>()
const pendingType = ref<FormFieldDefinition['dataType'] | null>(null)
const allowedTypes = computed(() => formFieldTypes.filter(type => props.depth === 0 || !['object', 'array'].includes(type.value) || props.item && props.depth === 1 && type.value === 'object'))
const unsupported = computed(() => !allowedTypes.value.some(type => type.value === props.field.dataType))
function fieldAction(action: 'up' | 'down' | 'delete') {
  if (props.readOnly) return
  if (action === 'delete') emit('remove')
  else emit('move', action === 'up' ? -1 : 1)
}
watch(() => props.field, () => { pendingType.value = null })
function update(field: FormFieldDefinition) { if (!props.readOnly) emit('change', field) }
function selectType(type: FormFieldDefinition['dataType']) {
  if (type === props.field.dataType) return
  if (hasNestedFields(props.field)) pendingType.value = type
  else update(changeFieldType(props.field, type))
}
function replaceChild(index: number, field: FormFieldDefinition) {
  update({ ...props.field, fields: (props.field.fields || []).map((entry, i) => i === index ? field : entry) })
}
function removeChild(index: number) {
  update({ ...props.field, fields: (props.field.fields || []).filter((_, i) => i !== index) })
}
function moveChild(index: number, offset: number) {
  const fields = [...(props.field.fields || [])]; const target = index + offset
  if (target < 0 || target >= fields.length) return
  ;[fields[index], fields[target]] = [fields[target], fields[index]]
  update({ ...props.field, fields })
}
function confirmType() {
  if (pendingType.value) update(changeFieldType(props.field, pendingType.value))
  pendingType.value = null
}
</script>
<template>
  <div class="ffd-field">
    <div class="ffd-row">
      <span v-if="item" class="ffd-item-label">数组元素</span>
      <template v-else>
        <WfInput :model-value="field.label || ''" :label="'字段名称 ' + path" :aria-label="'字段名称 ' + path" placeholder="请输入字段名称" :disabled="readOnly" @update:model-value="update({ ...field, label: $event })" />
        <WfInput :model-value="field.key || ''" :label="'字段键 ' + path" :aria-label="'字段键 ' + path" placeholder="请输入字段键，如 order_id" :disabled="readOnly" @update:model-value="update({ ...field, key: $event })" />
      </template>
      <WfSelect :model-value="field.dataType" :aria-label="'数据类型 ' + path" placeholder="请选择数据类型" :disabled="readOnly" @update:model-value="selectType">
        <WfOption v-if="unsupported" :value="field.dataType" label="不支持多层嵌套" disabled />
        <WfOption v-for="option in allowedTypes" :key="option.value" :value="option.value" :label="option.label" />
      </WfSelect>
      <div class="ffd-actions">
        <template v-if="!readOnly && !item">
          <WfButton text class="ffd-action" :aria-label="'上移字段 ' + path" title="上移" :disabled="first" @click="fieldAction('up')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 7-7 7 7M12 19V5" /></svg>
          </WfButton>
          <WfButton text class="ffd-action" :aria-label="'下移字段 ' + path" title="下移" :disabled="last" @click="fieldAction('down')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 7 7 7-7M12 5v14" /></svg>
          </WfButton>
          <WfButton text class="ffd-action ffd-action--delete" :aria-label="'删除字段 ' + path" title="删除" @click="fieldAction('delete')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18M19 6l-1 14H6L5 6M9 6V3h6v3M10 10v6M14 10v6" /></svg>
          </WfButton>
        </template>
      </div>
    </div>
    <div v-if="pendingType" class="ffd-confirm" role="alert">
      <span>切换类型会清除子字段，是否继续？</span>
      <WfButton @click="pendingType = null">取消</WfButton>
      <WfButton type="primary" :disabled="readOnly" @click="confirmType">确认切换</WfButton>
    </div>
    <div v-if="!unsupported && field.dataType === 'object'" class="ffd-children">
      <FormFieldRow v-for="(child, index) in field.fields || []" :key="index" :field="child" :path="path + '.' + (index + 1)" :depth="depth + 1" :read-only="readOnly"
        :first="index === 0" :last="index === (field.fields || []).length - 1"
        @change="replaceChild(index, $event)" @remove="removeChild(index)" @move="moveChild(index, $event)" />
      <WfButton v-if="!readOnly" text class="ffd-add" @click="update({ ...field, fields: [...(field.fields || []), newFormField()] })">＋ 添加子字段</WfButton>
    </div>
    <div v-if="!unsupported && field.dataType === 'array'" class="ffd-children">
      <FormFieldRow :field="field.items || { dataType: 'string' }" :path="path + '[]'" :depth="depth + 1" item :read-only="readOnly" @change="update({ ...field, items: $event })" />
    </div>
    <p v-if="unsupported" role="alert" class="ffd-error">不支持多层嵌套，请将子字段调整为基础类型。</p>
  </div>
</template>
