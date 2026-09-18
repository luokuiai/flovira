<template>
  <div class="submitter-editor">
    <wf-form-item label="可提交人员">
      <wf-select :model-value="rule.strategy" :disabled="disabled" @update:model-value="changeStrategy">
        <wf-option v-if="!descriptor" :value="rule.strategy" label="不支持的提交范围配置" disabled />
        <wf-option v-for="item in strategies" :key="item.code" :value="item.code" :label="item.name" />
      </wf-select>
    </wf-form-item>
    <p v-if="error" role="alert">{{ error }}</p>
    <wf-button v-if="loadFailed" :disabled="disabled" @click="loadCapabilities">重新加载</wf-button>
    <template v-if="descriptor?.selectionType === 'RESOURCE'">
      <wf-form-item :label="descriptor.resourceType === 'USER' ? '指定人员' : descriptor.name">
        <div class="submitter-selection">
          <span v-for="subject in rule.subjects" :key="subject.type + ':' + subject.id">
            {{ subject.name || subject.id }}
            <wf-button :disabled="disabled" link @click="remove(subject.id)">移除</wf-button>
          </span>
          <wf-button :disabled="disabled" @click="pickerVisible = true">选择{{ descriptor.name }}</wf-button>
        </div>
      </wf-form-item>
    </template>
    <wf-form-item v-if="descriptor?.selectionType === 'EXPRESSION'" label="提交范围表达式">
      <wf-input :model-value="rule.expression || ''" :disabled="disabled" @update:model-value="update({ expression: $event })" />
    </wf-form-item>
    <wf-form-item v-for="option in descriptor?.options || []" :key="option.code" :label="option.name">
      <wf-select :model-value="rule.config?.[option.code] ?? option.defaultValue ?? option.choices[0]?.value"
        :disabled="disabled" @update:model-value="update({ config: { ...rule.config, [option.code]: $event } })">
        <wf-option v-for="choice in option.choices" :key="choice.value" :value="choice.value" :label="choice.label" />
      </wf-select>
    </wf-form-item>
    <wf-dialog v-if="pickerVisible" v-model="pickerVisible" :title="'选择' + descriptor?.name" width="80%" append-to-body>
      <selectUser :key="rule.strategy" v-model:userVisible="pickerVisible" :permissionRows="rows"
        :resource-type="descriptor?.resourceType" :multiple="descriptor?.multiple" @handleUserSelect="select" />
    </wf-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import selectUser from './selectUser.vue'
import { designerCapabilities } from '@/api/flow/definition'
import { unwrapData, type ApproverRule, type DesignerApproverStrategy } from '@/data/contracts'

const props = withDefaults(defineProps<{ modelValue: Record<string, any>; disabled?: boolean }>(), { disabled: false })
const all: DesignerApproverStrategy = { code: 'ALL', name: '全员', version: 1, selectionType: 'RELATION', editorType: 'NONE', multiple: false }
const strategies = ref<DesignerApproverStrategy[]>([all])
const error = ref('')
const loadFailed = ref(false)
const pickerVisible = ref(false)
const rule = computed<ApproverRule>(() => {
  const empty: ApproverRule = { schemaVersion: 1, strategyVersion: 1, strategy: '', selectionType: 'RELATION', subjects: [] }
  const raw = props.modelValue.ext?.submitterRule
  if (raw === undefined) return { ...empty, strategy: 'ALL' }
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed?.schemaVersion === 1 && Array.isArray(parsed.subjects) ? { ...empty, ...parsed } : empty
  } catch { return empty }
})
const descriptor = computed(() => strategies.value.find(item => item.code === rule.value.strategy))
const rows = computed(() => rule.value.subjects.map(subject => ({ storageId: subject.id, handlerName: subject.name || subject.id })))

function update(patch: Partial<ApproverRule>) {
  if (props.disabled) return
  error.value = ''
  props.modelValue.ext = { ...props.modelValue.ext, submitterRule: JSON.stringify({ ...rule.value, ...patch }) }
}
function changeStrategy(code: string) {
  const next = strategies.value.find(item => item.code === code)
  if (!next || props.disabled) return
  pickerVisible.value = false
  const config = Object.fromEntries((next.options || []).map(option => [option.code, option.defaultValue ?? option.choices[0]?.value]))
  update({ schemaVersion: 1, strategy: code, strategyVersion: next.version ?? 1, selectionType: next.selectionType,
    relationType: next.relationType, subjects: [], expression: undefined, config })
}
function select(items: any[]) {
  if (props.disabled || !descriptor.value) return
  if (descriptor.value.maxSubjects != null && items.length > descriptor.value.maxSubjects) {
    error.value = `最多选择 ${descriptor.value.maxSubjects} 项`
    return
  }
  const selected = descriptor.value.multiple ? items : items.slice(0, 1)
  update({ subjects: selected.map(item => ({ id: item.storageId, name: item.handlerName, type: descriptor.value!.resourceType! })) })
  pickerVisible.value = false
}
function remove(id: string) { update({ subjects: rule.value.subjects.filter(subject => subject.id !== id) }) }
async function loadCapabilities() {
  try {
    const capabilities = unwrapData(await designerCapabilities())
    if (!capabilities) throw new Error('missing capabilities')
    strategies.value = [all, ...(capabilities.submitterStrategies ?? capabilities.approverStrategies.filter(item => ['USER', 'ROLE'].includes(item.code)))
      .filter(item => item.code !== 'ALL').map(item => ({ ...item,
        name: item.code === 'USER' ? '指定人员' : item.code === 'ROLE' ? '指定角色' : item.name,
        options: item.options?.filter(option => option.nodeTypes?.includes('0')) }))]
    loadFailed.value = false
    error.value = ''
  } catch {
    loadFailed.value = true
    error.value = '提交范围策略加载失败，请重试'
  }
}
async function validate() {
  const current = rule.value
  const strategy = descriptor.value
  if (!strategy || current.strategyVersion !== (strategy.version ?? 1) || current.selectionType !== strategy.selectionType
    || (strategy.selectionType === 'RESOURCE' && (!current.subjects.length || current.subjects.some(subject =>
      !subject || typeof subject.id !== 'string' || !subject.id.trim() || subject.type !== strategy.resourceType)))
    || (strategy.selectionType === 'EXPRESSION' && !current.expression?.trim())
    || (strategy.maxSubjects != null && current.subjects.length > strategy.maxSubjects)
    || (!strategy.multiple && current.subjects.length > 1)
    || (current.strategy === 'ALL' && (current.subjects.length || current.expression || current.relationType || Object.keys(current.config || {}).length))) {
    error.value = '请选择有效的可提交人员范围'
    throw new Error(error.value)
  }
}
loadCapabilities()
defineExpose({ validate })
</script>

<style scoped>
.submitter-selection { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
[role='alert'] { color: var(--wf-danger, #f56c6c); }
</style>
