<template>
  <div class="design-header">
    <!-- 左侧：流程名称（slot: header-left 可整体替换，透出 flowName） -->
    <div class="header-left">
      <slot name="header-left" :flow-name="flowName">
        <div class="flow-name-wrapper">
          <span class="flow-brand-mark"><svg-icon icon-class="flowDesign" /></span>
          <wf-tooltip :content="flowName" placement="bottom" :show-after="500">
            <div class="flow-name">
              {{ flowName || t('flowDesigner.untitled') }}
            </div>
          </wf-tooltip>
          <span v-if="dirty" class="flow-dirty-indicator"></span>
        </div>
      </slot>
    </div>

    <!-- 中间：步骤切换（slot: header-center 可整体替换，透出 activeStep / steps / goToStep） -->
    <div v-if="showSteps" class="header-center">
      <slot name="header-center" :active-step="activeStep" :steps="steps" :go-to-step="goToStep">
        <div class="steps-tabs">
          <div
            v-for="(step, index) in steps"
            :key="index"
            class="step-tab"
            :class="{ active: activeStep === index }"
            @click="goToStep(index)"
          >
            <svg-icon :icon-class="step.icon" class="tab-icon" />
            <span class="tab-text">{{ step.title }}</span>
          </div>
        </div>
      </slot>
    </div>

    <!-- 右侧操作区；外部插槽上下文由 FlowDesigner 提供。 -->
    <div class="header-right">
      <slot name="header-actions">
        <span class="workbench-header-group workbench-header-group--history">
          <wf-tooltip :content="t('flowDesigner.undo')" placement="bottom"><wf-button @click="emit('undo')"><svg-icon icon-class="ep:d-arrow-left" /></wf-button></wf-tooltip>
          <wf-tooltip :content="t('flowDesigner.redo')" placement="bottom"><wf-button @click="emit('redo')"><svg-icon icon-class="ep:d-arrow-right" /></wf-button></wf-tooltip>
          <wf-tooltip v-if="!disabled" :content="t('flowDesigner.clear')" placement="bottom"><wf-button @click="emit('clear')"><svg-icon icon-class="ep:delete" /></wf-button></wf-tooltip>
        </span>
        <span class="workbench-header-group workbench-header-group--export">
          <wf-tooltip :content="t('flowDesigner.downloadImage')" placement="bottom"><wf-button @click="emit('download-image')"><svg-icon icon-class="ep:picture" /></wf-button></wf-tooltip>
        </span>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '@/i18n';

/** 流程设计器顶部导航：流程名（左） + 步骤切换（中） + 画布操作（右）。
 *  从 FlowDesigner 抽出的纯展示编排子组件，状态/逻辑仍由容器持有，经 props 入、事件出。
 *  样式沿用 FlowDesigner 的全局（非 scoped）样式表中的 .design-header 等类，无需迁移。 */
defineOptions({ name: 'FlowDesignerHeader' });

export interface StepItem {
  title: string;
  icon: string;
}

withDefaults(defineProps<{
  flowName?: string;
  activeStep: number;
  steps: StepItem[];
  disabled?: boolean;
  dirty?: boolean;
  showSteps?: boolean;
}>(), {
  showSteps: true,
});

const emit = defineEmits<{
  (e: 'step-click', index: number): void;
  (e: 'undo'): void;
  (e: 'redo'): void;
  (e: 'clear'): void;
  (e: 'download-image'): void;
}>();

const { t } = useI18n();

const goToStep = (index: number) => emit('step-click', index);
</script>

<style scoped>
.workbench-header-group--history {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--wf-border-light, #e4e7ed);
  border-radius: 999px;
  background: var(--wf-bg-white, #fff);
}

.workbench-header-group--history :deep(button) {
  border-radius: 999px;
}
</style>
