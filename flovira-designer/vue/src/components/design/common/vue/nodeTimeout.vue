<template>
  <div class="timeout-settings">
    <wf-form-item :label="t('timeout.enabled')">
      <wf-switch v-model="enabled" :disabled="disabled" />
    </wf-form-item>
    <template v-if="enabled">
      <wf-form-item :label="t('timeout.duration')" :error="durationError">
        <div class="duration-row">
          <wf-input-number v-model="duration" :min="1" :precision="0" :disabled="disabled" />
          <wf-select v-model="unit" :disabled="disabled">
            <wf-option :label="t('timeout.unitMinutes')" value="MINUTES" />
            <wf-option :label="t('timeout.unitHours')" value="HOURS" />
            <wf-option :label="t('timeout.unitDays')" value="DAYS" />
          </wf-select>
        </div>
      </wf-form-item>
      <wf-form-item :label="t('timeout.action')">
        <wf-select v-model="action" :disabled="disabled">
          <wf-option
            v-for="item in actions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </wf-select>
      </wf-form-item>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from '@/i18n'

defineOptions({ name: 'NodeTimeout' })

const props = withDefaults(defineProps<{
  modelValue?: Record<string, any>
  nodeType: 'between' | 'wait'
  disabled?: boolean
}>(), {
  modelValue: () => ({}),
  disabled: false,
})

const { t } = useI18n()
const enabled = ref(false)
const duration = ref(1)
const unit = ref('HOURS')
const action = ref(props.nodeType === 'wait' ? 'RESUME_WAIT' : 'AUTO_PASS')
const durationError = ref('')

const actions = computed(() => props.nodeType === 'wait'
  ? [{ label: t('timeout.resumeWait'), value: 'RESUME_WAIT' }]
  : [
      { label: t('timeout.autoPass'), value: 'AUTO_PASS' },
      { label: t('timeout.autoReject'), value: 'AUTO_REJECT' },
    ])

watch(() => props.modelValue.ext?.timeoutConfig, (value) => {
  try {
    const config = value ? JSON.parse(value) : null
    enabled.value = !!config?.enabled
    duration.value = Number(config?.duration) || 1
    unit.value = config?.durationUnit || 'HOURS'
    action.value = config?.action || (props.nodeType === 'wait' ? 'RESUME_WAIT' : 'AUTO_PASS')
  } catch (_) {
    enabled.value = false
  }
}, { immediate: true })

watch([enabled, duration, unit, action], () => {
  durationError.value = ''
  props.modelValue.ext = {
    ...(props.modelValue.ext || {}),
    timeoutConfig: enabled.value ? JSON.stringify({
      schemaVersion: 1,
      enabled: true,
      duration: duration.value,
      durationUnit: unit.value,
      action: action.value,
    }) : '',
  }
})

function validate() {
  if (enabled.value && (!Number.isInteger(duration.value) || duration.value <= 0)) {
    durationError.value = t('timeout.durationRequired')
    return Promise.reject(false)
  }
  durationError.value = ''
  return Promise.resolve(true)
}

defineExpose({ validate })
</script>

<style scoped>
.timeout-settings {
  padding-top: 12px;
  border-top: 1px solid var(--wf-border-light);
}

.duration-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(110px, 0.7fr);
  gap: 8px;
  width: 100%;
}

:deep(.wf-select),
:deep(.el-select) {
  width: 100%;
}
</style>
