<template>
  <wf-form ref="formRef" :model="form" :rules="rules" label-width="110px" :disabled="disabled">
    <wf-form-item :label="t('node.codeLabel')">
      <wf-input v-model="form.nodeCode" disabled />
    </wf-form-item>
    <wf-form-item>
      <template #label><FieldHelpLabel :label="t('node.keyLabel')" :content="t('node.keyHelp')" /></template>
      <wf-input v-model="form.nodeKey" :placeholder="t('node.keyPlaceholder')" :disabled="disabled" />
    </wf-form-item>
    <wf-form-item :label="t('node.nameLabel')">
      <wf-input v-model="form.nodeName" :disabled="disabled" />
    </wf-form-item>
    <wf-form-item :label="t('subprocess.childFlow')" prop="fixedChildFlowCode">
      <wf-select
        v-model="fixedChildFlowCode"
        :disabled="disabled"
        :placeholder="t('subprocess.childFlowPlaceholder')"
        filterable
      >
        <wf-option v-if="fixedChildFlowCode && !definitions.some(item => item.flowCode === fixedChildFlowCode)"
          :label="selectedName || fixedChildFlowCode" :value="fixedChildFlowCode" />
        <wf-option
          v-for="definition in definitions"
          :key="definition.id"
          :label="`${definition.flowName} (${definition.flowCode} / ${definition.version})`"
          :value="definition.flowCode"
          :disabled="definition.disabled"
        />
      </wf-select>
      <wf-button v-if="!open" :disabled="disabled" @click="open = true">选择流程</wf-button>
      <template v-else>
        <wf-input v-model="keyword" :disabled="disabled" placeholder="搜索流程名称或编码" />
        <span v-if="state === 'loading'" role="status">加载中...</span>
        <template v-if="state === 'error'">
          <span role="alert">子流程加载失败，已选值保留</span>
          <wf-button :disabled="disabled" @click="retry++">重试</wf-button>
        </template>
        <span v-if="state === 'idle' && !definitions.length" role="status">暂无匹配流程</span>
        <wf-button v-if="hasMore" :disabled="disabled || state !== 'idle'" @click="pageNum++">加载更多</wf-button>
      </template>
    </wf-form-item>
    <wf-form-item :label="t('subprocess.completionPolicy')">
      <wf-input :model-value="t('subprocess.allPolicy')" disabled />
    </wf-form-item>
    <wf-form-item :label="t('subprocess.childLimit')">
      <wf-input :model-value="t('subprocess.engineLimit')" disabled />
    </wf-form-item>
  </wf-form>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { designerResources } from '@/api/flow/definition'
import { unwrapData } from '@/data/contracts'
import { useSubprocessOptions } from '@/composables/useSubprocessOptions'
import { useI18n } from '@/i18n'
import FieldHelpLabel from './FieldHelpLabel.vue'

defineOptions({ name: 'SubProcess' })

const props = withDefaults(defineProps<{
  modelValue?: Record<string, any>
  disabled?: boolean
}>(), {
  modelValue: () => ({}),
  disabled: false,
})

const { t } = useI18n()
const formRef = ref<any>()
const { open, keyword, pageNum, items, hasMore, state, retry } = useSubprocessOptions(
  computed(() => props.disabled), async (query) => unwrapData(await designerResources(query)),
)
const definitions = computed(() => items.value.map((item) => ({
  id: item.code || item.id, flowCode: item.code || item.id, flowName: item.name,
  version: item.metadata?.version, disabled: item.disabled,
})))
const selectedName = ref('')
const fixedChildFlowCode = ref('')
const form = computed(() => props.modelValue)
const rules = computed(() => ({
  fixedChildFlowCode: [{ required: true, message: t('subprocess.childFlowRequired'), trigger: 'change' }],
}))

watch(() => props.modelValue.ext?.subprocessConfig, (value) => {
  try {
    fixedChildFlowCode.value = value ? JSON.parse(value).fixedChildFlowCode || '' : ''
  } catch (_) {
    fixedChildFlowCode.value = ''
  }
}, { immediate: true })

watch(fixedChildFlowCode, (value) => {
  selectedName.value = definitions.value.find(item => item.flowCode === value)?.flowName || ''
  form.value.fixedChildFlowCode = value
  form.value.ext = {
    ...(form.value.ext || {}),
    subprocessConfig: value ? JSON.stringify({
      schemaVersion: 1,
      fixedChildFlowCode: value,
      completionPolicy: 'ALL',
      allowEmpty: false,
    }) : '',
  }
})

function validate() {
  return formRef.value.validate()
}

defineExpose({ validate })
</script>

<style scoped>
:deep(.wf-select),
:deep(.el-select) {
  width: 100%;
}
</style>
