<template>
  <span class="field-help-label">
    {{ label }}
    <wf-tooltip :content="content" placement="top">
      <span class="field-help-label__icon" tabindex="0" :aria-label="`${label}说明`">?</span>
    </wf-tooltip>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  content: string
}>()
</script>

<style scoped>
.field-help-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.field-help-label__icon {
  display: inline-flex;
  width: 14px;
  height: 14px;
  align-items: center;
  justify-content: center;
  border: 1px solid currentColor;
  border-radius: 50%;
  color: var(--wf-text-color-secondary, #909399);
  cursor: help;
  font-size: 10px;
  line-height: 1;
  outline: none;
}

.field-help-label__icon:hover,
.field-help-label__icon:focus-visible {
  color: var(--wf-color-primary, #409eff);
}
</style>
