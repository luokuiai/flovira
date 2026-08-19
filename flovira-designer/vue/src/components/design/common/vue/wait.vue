<template>
  <wf-form ref="formRef" :model="form" :rules="rules" label-width="110px" :disabled="disabled">
    <wf-form-item :label="t('node.codeLabel')" prop="nodeCode">
      <wf-input v-model="form.nodeCode" :disabled="disabled" />
    </wf-form-item>
    <wf-form-item :label="t('node.nameLabel')" prop="nodeName">
      <wf-input v-model="form.nodeName" :disabled="disabled" />
    </wf-form-item>
    <wf-form-item :label="t('wait.key')" prop="waitKey">
      <wf-input v-model="form.waitKey" :placeholder="t('wait.keyPlaceholder')" :disabled="disabled" />
    </wf-form-item>
    <node-timeout ref="timeoutRef" v-model="form" node-type="wait" :disabled="disabled" />
  </wf-form>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import NodeTimeout from './nodeTimeout.vue'
import { useI18n } from '@/i18n'

defineOptions({ name: 'Wait' })

const props = withDefaults(defineProps<{
  modelValue?: Record<string, any>
  disabled?: boolean
}>(), {
  modelValue: () => ({}),
  disabled: false,
})

const { t } = useI18n()
const formRef = ref<any>()
const timeoutRef = ref<any>()
const form = computed(() => props.modelValue)
const WAIT_KEY_PATTERN = /^[A-Za-z][A-Za-z0-9_.:-]{0,127}$/

const rules = computed(() => ({
  waitKey: [
    { required: true, message: t('wait.keyRequired'), trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        callback(WAIT_KEY_PATTERN.test(value || '') ? undefined : new Error(t('wait.keyInvalid')))
      },
      trigger: ['change', 'blur'],
    },
  ],
}))

watch(() => props.modelValue.ext?.waitConfig, (value) => {
  try {
    form.value.waitKey = value ? JSON.parse(value).waitKey || '' : ''
  } catch (_) {
    form.value.waitKey = ''
  }
}, { immediate: true })

watch(() => form.value.waitKey, (value) => {
  form.value.ext = {
    ...(form.value.ext || {}),
    waitConfig: value ? JSON.stringify({ schemaVersion: 1, waitKey: value.trim() }) : '',
  }
})

async function validate() {
  await formRef.value.validate()
  return timeoutRef.value.validate()
}

defineExpose({ validate })
</script>
