<template>
  <div class="start">
    <!-- 基础设置 -->
    <div class="tabPane">
      <wf-form ref="formRef" class="startForm" :model="form" label-width="110px" :disabled="disabled">
        <div class="base-settings-section">
          <div class="base-settings-content">
            <slot name="form-item-task-name" :model="form" field="nodeCode">
              <wf-form-item :label="t('node.codeLabel')">
                <wf-input v-model="form.nodeCode" disabled></wf-input>
              </wf-form-item>
            </slot>
            <slot name="form-item-task-name" :model="form" field="nodeName">
              <wf-form-item :label="t('node.nameLabel')">
                <wf-input v-model="form.nodeName" ref="nodeInput" :disabled="disabled" @change="nodeNameChange"></wf-input>
              </wf-form-item>
            </slot>
            <SubmitterEditor ref="submitterEditor" v-model="form" :disabled="disabled" />
            <!-- 自定义扩展点：消费方可注入额外表单项（透出 { form, disabled }） -->
            <slot name="node-form-extra" :form="form" :disabled="disabled" />
          </div>
        </div>
      </wf-form>
    </div>

  </div>
</template>

<script setup lang="ts">
import SubmitterEditor from './SubmitterEditor.vue'
import { getCurrentInstance, ref, watch } from 'vue';
import { useI18n } from '@/i18n';

defineOptions({ name: 'Start' });

const { t } = useI18n();
const submitterEditor = ref<InstanceType<typeof SubmitterEditor>>()
defineExpose({ validate: () => submitterEditor.value?.validate() })

interface StartProps {
  /** 节点表单数据（v-model） */
  modelValue?: Record<string, any>;
  /** 是否只读 */
  disabled?: boolean;
}
const props = withDefaults(defineProps<StartProps>(), {
  modelValue: () => ({}),
  disabled: false,
});

const form = ref<Record<string, any>>(props.modelValue);
const emit = defineEmits<{ (e: 'change', value: any): void }>();

const proxy = getCurrentInstance()!.proxy as any;

watch(() => form, n => {
  if (n) {
    emit('change', n)
  }
},{ deep: true });

function nodeNameChange() {
  proxy.$refs.nodeInput.focus();
}

</script>

<style scoped lang="scss">
@import '@/assets/styles/_common.scss';

.start { width: 100%; html.dark & { background: var(--wf-bg-color); border-radius: var(--wf-radius-lg); } }
.startForm { border-top: 0; width: 100%; }

/* 引入公共样式：现代化页签 + 基础配置卡片 + 监听器卡片 */
@include base-settings-card;
@include section-card;
@include responsive-adaption;

/* 紫色主题 - 监听器（扁平：保留紫色标题/图标做语义，去卡片底/边框/渐变头） */
.section-purple {
  .section-purple-header {
    background: transparent;
    border-bottom-color: var(--wf-border-lighter);
    html.dark & { background: transparent; border-bottom-color: var(--wf-border-color); }
  }
  .section-purple-icon { color: #8960dc; }
  .section-purple-title { color: #8960dc; }
}

/* 增加行按钮：虚线主色（与 baseInfo / between 统一） */
.action-buttons {
  margin-top: 12px;
}
.add-row-btn {
  width: 100%;
  border: 1.5px dashed var(--wf-primary) !important;
  color: var(--wf-primary) !important;
  background: transparent !important;
  border-radius: 10px;
  transition: all 0.3s ease;
  height: 40px;
  letter-spacing: 2px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: var(--wf-primary-light) !important;
    border-style: solid !important;
    border-color: var(--wf-primary) !important;
    box-shadow: var(--wf-shadow-primary);
  }
}

/* 表格内表单左对齐 */
:deep(.el-table .el-form-item) {
  margin-bottom: 0;
  .el-form-item__label {
    display: none !important; width: 0 !important; min-width: 0 !important;
    padding-right: 0 !important; overflow: hidden !important; visibility: hidden !important;
    height: 0 !important;
  }
  .el-form-item__content {
    display: block !important; margin-left: 0 !important;
  }
}
:deep(.listenerItem) .el-table .el-form-item { margin-bottom: 0; }
</style>
