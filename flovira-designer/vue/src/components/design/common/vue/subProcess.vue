<template>
  <wf-form ref="formRef" :model="form" :rules="rules" label-width="110px" :disabled="disabled">
    <wf-form-item :label="t('node.codeLabel')">
      <wf-input v-model="form.nodeCode" :disabled="disabled" />
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
        <wf-option
          v-for="definition in definitions"
          :key="definition.id"
          :label="`${definition.flowName} (${definition.flowCode} / ${definition.version})`"
          :value="definition.flowCode"
        />
      </wf-select>
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
import { computed, onMounted, ref, watch } from 'vue'
import { designerResourceItems } from '@/api/flow/definition'
import { useI18n } from '@/i18n'

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
const definitions = ref<any[]>([])
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

onMounted(async () => {
  const items = await designerResourceItems({ resourceType: 'SUBPROCESS', pageNum: 1, pageSize: 1000 })
  definitions.value = items.map((item) => ({
    id: item.id,
    flowCode: item.code,
    flowName: item.name,
    version: item.metadata?.version,
  }))
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
