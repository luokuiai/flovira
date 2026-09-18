<template>
  <section class="form-section" :aria-label="nodeType == null ? '流程回调' : '节点回调'">
    <div class="section-title">{{ nodeType == null ? '流程回调' : '节点回调' }}</div>
    <p v-if="error" role="alert">{{ error }}</p>
    <fieldset v-for="(row, index) in config.subscriptions" :key="index" :disabled="disabled">
      <wf-form-item label="监听器 Bean 名"><wf-input :model-value="row.code" :disabled="disabled" @update:model-value="patch(index, { code: $event })" /></wf-form-item>
      <wf-form-item label="回调时机"><wf-select :model-value="row.point" :disabled="disabled" @update:model-value="changePoint(index, $event)">
        <wf-option v-for="[point, label] in points" :key="point" :value="point" :label="label" />
      </wf-select></wf-form-item>
      <wf-form-item label="执行阶段"><wf-select :model-value="row.phase" :disabled="disabled" @update:model-value="patch(index, { phase: $event })">
        <wf-option value="IN_TRANSACTION" label="事务内" />
        <wf-option v-if="!row.point.startsWith('BEFORE_')" value="AFTER_COMMIT" label="提交后" />
      </wf-select></wf-form-item>
      <wf-form-item label="执行顺序"><wf-input type="number" :model-value="String(row.order)" :disabled="disabled" @update:model-value="patch(index, { order: Number($event) })" /></wf-form-item>
      <wf-form-item label="参数"><wf-input :model-value="row.parameters || ''" :disabled="disabled" @update:model-value="patch(index, { parameters: $event })" /></wf-form-item>
      <wf-button :disabled="disabled" @click="save(config.subscriptions.filter((_, i) => i !== index))">删除回调</wf-button>
    </fieldset>
    <wf-button :disabled="disabled || !points.length || invalidJson" @click="save([...config.subscriptions, { code: '', point: points[0][0], phase: 'IN_TRANSACTION', order: 0 }])">添加回调</wf-button>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { lifecyclePoints, parseLifecycle, validateLifecycle } from '@/data/lifecycle'
import type { LifecyclePoint, LifecycleSubscription } from '@/data/lifecycle'
const props = defineProps<{ modelValue?: string; nodeType?: string; disabled?: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()
const parsed = computed(() => {
  try { return { config: parseLifecycle(props.modelValue), error: '' } }
  catch (error) { return { config: { schemaVersion: 1 as const, subscriptions: [] }, error: String(error) } }
})
const config = computed(() => parsed.value.config)
const invalidJson = computed(() => Boolean(parsed.value.error))
const error = computed(() => {
  if (parsed.value.error) return parsed.value.error
  try { validateLifecycle(config.value, props.nodeType); return '' } catch (error) { return String(error) }
})
const points = computed(() => lifecyclePoints(props.nodeType))
const save = (subscriptions: LifecycleSubscription[]) => emit('update:modelValue', JSON.stringify({ schemaVersion: 1, subscriptions }))
const patch = (index: number, value: Partial<LifecycleSubscription>) => save(config.value.subscriptions.map((row, i) => i === index ? { ...row, ...value } : row))
const changePoint = (index: number, point: LifecyclePoint) => patch(index, { point, phase: point.startsWith('BEFORE_') ? 'IN_TRANSACTION' : config.value.subscriptions[index].phase })
</script>

<style scoped>
fieldset {
  min-width: 0;
  margin: 0 0 24px;
  padding: 0 0 20px;
  border: 0;
  border-bottom: 1px solid var(--wf-border-color, #dcdfe6);
}
[role="alert"] { color: var(--wf-danger, #f56c6c); }
</style>
