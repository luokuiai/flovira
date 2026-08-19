<template>
  <div class="design-toolbar design-toolbar--canvas">
    <div v-if="activeStep === 1">
      <span class="toolbar-group toolbar-group--viewport">
        <wf-tooltip :content="t('flowDesigner.zoomOut')" placement="bottom"><wf-button size="small" @click="emit('zoom-out')"><svg-icon icon-class="ep:zoom-out"/></wf-button></wf-tooltip>
        <!-- 自适应：所有端均 fitView 显示全部节点（最大缩放 100%） -->
        <wf-tooltip :content="t('flowDesigner.fitView')" placement="bottom"><wf-button size="small" @click="emit('fit-view')"><svg-icon icon-class="ep:rank"/></wf-button></wf-tooltip>
        <wf-tooltip :content="t('flowDesigner.zoomIn')" placement="bottom"><wf-button size="small" @click="emit('zoom-in')"><svg-icon icon-class="ep:zoom-in"/></wf-button></wf-tooltip>
      </span>
      <!-- slot: toolbar-extra 追加自定义工具栏按钮（透出底层 lf / disabled） -->
      <slot name="toolbar-extra" :lf="lf" :disabled="disabled" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '@/i18n';

/** 流程设计器画布工具栏：提供缩小、适应和放大三项视口控制。
 *  从 FlowDesigner 抽出的纯展示编排子组件，画布操作仍由容器（useLogicFlowCanvas）持有，经 props 入、事件出。
 *  样式沿用 FlowDesigner 的全局（非 scoped）样式表中的 .design-toolbar / .toolbar-group 等类，无需迁移。 */
defineOptions({ name: 'FlowDesignerToolbar' });

defineProps<{
  /** 当前步骤索引（仅 1=流程设计 时显示工具栏） */
  activeStep: number;
  /** 是否只读 */
  disabled?: boolean;
  /** 底层 LogicFlow 实例（仅用于透出给 toolbar-extra 插槽） */
  lf?: any;
}>();

const emit = defineEmits<{
  (e: 'zoom-out'): void;
  (e: 'fit-view'): void;
  (e: 'zoom-in'): void;
}>();

const { t } = useI18n();
</script>
