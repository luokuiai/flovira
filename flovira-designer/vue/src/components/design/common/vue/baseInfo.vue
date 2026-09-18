<template>
  <div class="app-container">
    <wf-form ref="formRef" :model="form" class="dialogForm" :rules="rules" label-width="150px" :disabled="disabled">
      <div class="form-section">
        <div class="section-title">{{ t('baseInfo.sectionBasic') }}</div>
        <wf-form-item :label="t('baseInfo.flowCode')" prop="flowCode">
          <wf-input v-model="form.flowCode" :placeholder="t('baseInfo.flowCodePlaceholder')" maxlength="40" />
        </wf-form-item>

        <wf-form-item :label="t('baseInfo.flowName')" prop="flowName">
          <wf-input v-model="form.flowName" :placeholder="t('baseInfo.flowNamePlaceholder')" maxlength="100" @input="nameChange" />
        </wf-form-item>

        <wf-form-item :label="t('baseInfo.category')" prop="category">
          <wf-tree-select
              v-model="form.category"
              :data="categoryList"
              :props="{ value: 'id', label: 'name', children: 'children' }"
              value-key="id"
              :placeholder="t('baseInfo.categoryPlaceholder')"
              check-strictly/>
        </wf-form-item>

        <wf-form-item :label="t('baseInfo.formId')" prop="formId">
          <wf-tree-select v-if="formOptions.length" v-model="form.formId"
              :data="formOptions" :props="{ value: 'id', label: 'name', children: 'children' }"
              value-key="id" :placeholder="t('baseInfo.formIdPlaceholder')" clearable check-strictly/>
          <wf-input v-else v-model="form.formId" :placeholder="t('baseInfo.formIdPlaceholder')" maxlength="100"/>
        </wf-form-item>
      </div>

      <p v-if="legacyCallbacks" role="alert">旧监听配置已移除，请清空 listenerType 和 listenerPath 后再保存或发布。</p>
    </wf-form>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, onUnmounted, ref, watch } from "vue";
import { useI18n } from '@/i18n';

defineOptions({ name: 'BaseInfo' });

const proxy = getCurrentInstance()!.proxy as any;
const { t } = useI18n();
const emit = defineEmits<{
  (e: 'update:flow-name', flowName: string): void;
  (e: 'validate-error', fields?: Record<string, any>): void;
}>();

// 响应式屏幕检测
const isMobile = ref(false);

function checkMobile() {
  isMobile.value = window.innerWidth <= 768;
}

onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
});

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
});
interface BaseInfoProps {
  /** 是否只读 */
  disabled?: boolean;
  /** 流程基础信息（数据源） */
  logicJson?: Record<string, any>;
  /** 流程类别树 */
  categoryList?: any[];
  /** 自定义表单路径树 */
  formOptions?: any[];
  /** 流程定义 id（新建态为 null） */
  definitionId?: string | null;
}
const props = withDefaults(defineProps<BaseInfoProps>(), {
  disabled: false,
  logicJson: () => ({}),
  categoryList: () => [],
  formOptions: () => [],
  definitionId: null,
});

const form = ref({
  id: null,
  flowCode: "",
  flowName: "",
  category: "",
  formId: "",
  listenerType: "",
  listenerPath: "",
  ext: '{}'
});

watch(() => props.logicJson, newValue => {
  if (newValue && Object.keys(newValue).length > 0) {
    Object.assign(form.value, newValue);
  }
});

const legacyCallbacks = computed(() => Boolean(form.value.listenerType || form.value.listenerPath))
const definitionList = ref([]);


const rules = computed(() => ({
  flowCode: [
    { required: true, message: t('baseInfo.ruleFlowCodeRequired'), trigger: "blur" }
  ],
  flowName: [
    { required: true, message: t('baseInfo.ruleFlowNameRequired'), trigger: "blur" }
  ]
}));

// 表单引用（用于校验）
const formRef = ref();

// 表单必填校验
function validate() {
  return new Promise((resolve) => {
    proxy.$nextTick(() => {
      // EP 回调签名为 (valid, fields)，校验失败透出 invalid fields；antd 适配器仅回传 valid（fields 为空）
      proxy.$refs.formRef.validate((valid: boolean, fields?: Record<string, any>) => {
        if (valid) {
          resolve(true);
        } else {
          // 透出校验失败字段，供上层 FlowDesigner 转发 validate-error 事件（best-effort，随 UI 适配器）
          emit('validate-error', fields);
          resolve(false);
        }
      });
    });
  });
}

function nameChange(flowName: string) {
  // 可以在这里添加额外的逻辑，比如验证或格式化
  emit('update:flow-name', flowName); // 如果需要通知父组件
}

function getFormData() {
  return form.value;
}

defineExpose({ getFormData, validate });

</script>

<style scoped lang="scss">
@import './baseInfo.scoped.scss';
</style>
