<template>
  <div>
    <p>修改后点击“应用条件”；同一明细组的条件必须由同一条记录满足。</p>
    <div v-for="(group, groupIndex) in groups" :key="groupIndex">
      <p v-if="groupIndex">或</p>
      <wf-form-item label="条件范围">
        <wf-select :model-value="group.collection?.code || ''" :disabled="disabled" @update:model-value="value => changeScope(groupIndex, value)">
          <wf-option value="" label="表单字段 / 汇总数量" />
          <wf-option v-for="scope in scopes" :key="scope.code" :value="scope.code" :label="scope.label" />
        </wf-select>
      </wf-form-item>
      <wf-form-item v-if="group.collection" label="明细匹配方式">
        <wf-select v-model="group.collection.quantifier" :disabled="disabled">
          <wf-option value="ANY" label="任一条满足以下全部条件" />
          <wf-option value="ALL" label="所有条满足以下全部条件" />
        </wf-select>
      </wf-form-item>
      <div v-for="(condition, index) in group.conditions" :key="index">
        <p v-if="index">且</p>
        <wf-form-item label="条件字段">
          <wf-select :model-value="condition.fieldCode" :disabled="disabled" @update:model-value="value => changeField(group, index, value)">
            <wf-option v-if="!available(group).some(field => field.code === condition.fieldCode)" :value="condition.fieldCode"
              :label="condition.fieldLabel + '（字段不可用）'" disabled />
            <wf-option v-for="field in available(group)" :key="field.code" :value="field.code" :label="field.label" />
          </wf-select>
        </wf-form-item>
        <wf-form-item label="比较方式">
          <wf-select v-model="condition.operator" :disabled="disabled">
            <wf-option v-for="item in operators(condition.fieldType)" :key="item.code" :value="item.code" :label="item.label" />
          </wf-select>
        </wf-form-item>
        <wf-form-item label="条件值">
          <wf-select v-if="condition.fieldType === 'BOOLEAN'" v-model="condition.value" :disabled="disabled">
            <wf-option value="true" label="是" /><wf-option value="false" label="否" />
          </wf-select>
          <wf-input v-else v-model="condition.value" :disabled="disabled" />
        </wf-form-item>
        <wf-button v-if="!disabled" @click="group.conditions.splice(index, 1)">删除条件</wf-button>
      </div>
      <wf-button v-if="!disabled" :disabled="!available(group).length" @click="group.conditions.push(newCondition(available(group)[0]))">添加条件</wf-button>
      <wf-button v-if="!disabled" @click="groups.splice(groupIndex, 1)">删除条件组</wf-button>
    </div>
    <wf-button v-if="!disabled" @click="addGroup">添加条件组</wf-button>
    <p v-if="error" role="alert">{{ error }}</p>
    <wf-button v-if="!disabled" type="primary" @click="apply">应用条件</wf-button>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, type Ref } from 'vue'
import { compileFormConditionGroup, fieldsForScope, getConditionScopes,
  type FormConditionField, type FormConditionGroup, type FormCondition } from '@/data/formDefinition'
const props = defineProps<{ rule?: any; disabled?: boolean }>()
const emit = defineEmits<{ (e: 'apply', rule: any): void }>()
const fields = inject<Ref<readonly FormConditionField[]>>('floviraConditionFields', ref([]))
const scopes = computed(() => getConditionScopes(fields.value))
const groups = ref<FormConditionGroup[]>(JSON.parse(JSON.stringify(props.rule?.groups || [])))
const error = ref('')
const available = (group: FormConditionGroup) => fieldsForScope(fields.value, group.collection?.code)
const newCondition = (field: FormConditionField): FormCondition => ({
  fieldCode: field.code, fieldLabel: field.label, fieldType: field.type, operator: 'EQ', value: field.type === 'BOOLEAN' ? 'true' : '',
})
const operators = (type: string) => [
  { code: 'EQ', label: '等于' }, { code: 'NE', label: '不等于' },
  ...(type === 'NUMBER' ? [{ code: 'GT', label: '大于' }, { code: 'GE', label: '大于等于' },
    { code: 'LT', label: '小于' }, { code: 'LE', label: '小于等于' }] : []),
]
function addGroup() {
  const list = fieldsForScope(fields.value)
  groups.value.push({ conditions: list.length ? [newCondition(list[0])] : [] })
}
function changeScope(index: number, code: string) {
  const scope = scopes.value.find(item => item.code === code)
  const list = fieldsForScope(fields.value, code)
  groups.value[index] = { ...(scope ? { collection: { ...scope, quantifier: 'ANY' } } : {}),
    conditions: list.length ? [newCondition(list[0])] : [] }
}
function changeField(group: FormConditionGroup, index: number, code: string) {
  const field = available(group).find(item => item.code === code)
  if (field) group.conditions[index] = newCondition(field)
}
function apply() {
  if (props.disabled) return
  try {
    if (!groups.value.length) throw new Error('请添加完整的条件规则')
    for (const group of groups.value) for (const condition of group.conditions) {
      if (!available(group).some(field => field.code === condition.fieldCode && field.type === condition.fieldType)) throw new Error('条件字段已不可用，请重新选择')
    }
    const expression = 'spel@@#{' + groups.value.map(group => '(' + compileFormConditionGroup(group) + ')').join(' or ') + '}'
    emit('apply', { mode: 'rules', groups: JSON.parse(JSON.stringify(groups.value)), expression })
    error.value = ''
  } catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
}
</script>
