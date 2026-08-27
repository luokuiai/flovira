<template>
  <div class="carbon-copy">
    <wf-form ref="formRef" :model="form" label-width="110px" :disabled="disabled">
      <wf-form-item :label="t('node.codeLabel')">
        <wf-input v-model="form.nodeCode" :disabled="disabled" />
      </wf-form-item>
      <wf-form-item :label="t('node.nameLabel')">
        <wf-input v-model="form.nodeName" :disabled="disabled" />
      </wf-form-item>
      <wf-form-item :label="t('carbonCopy.recipientType')">
        <wf-select v-model="recipientStrategy" :disabled="disabled" @change="handleStrategyChange">
          <wf-option
            v-for="strategy in recipientStrategies"
            :key="strategy.code"
            :label="strategy.name"
            :value="strategy.code"
          />
        </wf-select>
      </wf-form-item>
      <wf-form-item
        v-if="strategyDescriptor?.selectionType === 'EXPRESSION'"
        :label="t('carbonCopy.expression')"
      >
        <wf-input v-model="recipientExpression" :disabled="disabled" @change="syncRule" />
      </wf-form-item>
      <template v-else-if="strategyDescriptor?.selectionType === 'RESOURCE'">
        <wf-table :data="recipientRows" style="width: 100%">
          <wf-table-column prop="handlerName" :label="t('carbonCopy.recipient')" />
          <wf-table-column :label="t('common.operation')" width="65" align="center" v-if="!disabled">
            <template #default="scope">
              <wf-button link size="small" type="danger" @click="removeRecipient(scope.$index)">
                <svg-icon icon-class="ep:delete" />
              </wf-button>
            </template>
          </wf-table-column>
        </wf-table>
        <div class="carbon-copy-actions">
          <wf-button v-if="!disabled" @click="recipientVisible = true">{{ t('carbonCopy.selectRecipient') }}</wf-button>
        </div>
      </template>
    </wf-form>

    <wf-dialog
      v-if="recipientVisible"
      v-model="recipientVisible"
      :title="t('carbonCopy.selectRecipient')"
      width="80%"
      append-to-body
    >
      <selectUser
        v-model:selectUser="form.permissionFlag"
        v-model:userVisible="recipientVisible"
        :permissionRows="recipientRows"
        :resource-type="strategyDescriptor?.resourceType"
        :multiple="strategyDescriptor?.multiple"
        @handleUserSelect="handleRecipientSelect"
      />
    </wf-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import selectUser from './selectUser.vue'
import { designerCapabilities, designerSubjects } from '@/api/flow/definition'
import { DEFAULT_DESIGNER_CAPABILITIES, unwrapData, type DesignerApproverStrategy } from '@/data/contracts'
import { useI18n } from '@/i18n'

defineOptions({ name: 'CarbonCopy' })

const props = withDefaults(defineProps<{
  modelValue?: Record<string, any>
  disabled?: boolean
}>(), {
  modelValue: () => ({}),
  disabled: false,
})

const { t } = useI18n()
const formRef = ref<any>()
const form = computed(() => props.modelValue)
const recipientVisible = ref(false)
const recipientRows = ref<any[]>([])
const recipientStrategies = ref<DesignerApproverStrategy[]>(DEFAULT_DESIGNER_CAPABILITIES.approverStrategies)
const recipientStrategy = ref('USER')
const recipientExpression = ref('')
const strategyDescriptor = computed(() => recipientStrategies.value.find(item => item.code === recipientStrategy.value))

function syncRule() {
  const descriptor = strategyDescriptor.value
  const subjects = descriptor?.selectionType === 'RESOURCE'
    ? recipientRows.value.filter(item => item.storageId).map(item => ({
      id: item.storageId,
      type: descriptor.resourceType,
      name: item.handlerName,
    }))
    : []
  const rule = {
    schemaVersion: 1,
    strategy: recipientStrategy.value,
    selectionType: descriptor?.selectionType,
    relationType: descriptor?.relationType,
    subjects,
    expression: descriptor?.selectionType === 'EXPRESSION' ? recipientExpression.value : undefined,
  }
  form.value.ext = Object.assign({}, form.value.ext, { carbonCopyRule: JSON.stringify(rule) })
}

function handleStrategyChange() {
  form.value.permissionFlag = []
  recipientRows.value = []
  recipientExpression.value = ''
  syncRule()
}

function handleRecipientSelect(items: any[]) {
  recipientRows.value = items
  form.value.permissionFlag = items.map(item => item.storageId).filter(Boolean)
  syncRule()
}

function removeRecipient(index: number) {
  recipientRows.value.splice(index, 1)
  form.value.permissionFlag = recipientRows.value.map(item => item.storageId).filter(Boolean)
  syncRule()
}

async function hydrateRule() {
  const rawRule = form.value.ext?.carbonCopyRule
  if (rawRule) {
    try {
      const rule = typeof rawRule === 'string' ? JSON.parse(rawRule) : rawRule
      recipientStrategy.value = rule.strategy || 'USER'
      recipientExpression.value = rule.expression || ''
      recipientRows.value = (rule.subjects || []).map((subject: any) => ({
        storageId: subject.id,
        handlerName: subject.name || subject.id,
        resourceType: subject.type,
      }))
      form.value.permissionFlag = recipientRows.value.map(item => item.storageId)
      return
    } catch (_) {
      // 后端保存时会校验非法规则；此处继续按旧 permissionFlag 尝试回显。
    }
  }
  const permissions = typeof form.value.permissionFlag === 'string'
    ? form.value.permissionFlag.split('@@').filter(Boolean)
    : (form.value.permissionFlag || []).filter(Boolean)
  form.value.permissionFlag = permissions
  if (permissions.length) {
    recipientRows.value = await designerSubjects(permissions)
    syncRule()
  }
}

async function validate() {
  await formRef.value?.validate()
  const descriptor = strategyDescriptor.value
  const valid = descriptor?.selectionType === 'EXPRESSION'
    ? Boolean(recipientExpression.value.trim())
    : descriptor?.selectionType !== 'RESOURCE' || recipientRows.value.length > 0
  return valid ? true : Promise.reject(false)
}

designerCapabilities().then(response => {
  recipientStrategies.value = unwrapData(response)?.approverStrategies
    || DEFAULT_DESIGNER_CAPABILITIES.approverStrategies
})
hydrateRule()

defineExpose({ validate })
</script>

<style scoped>
.carbon-copy {
  padding: 4px 0;
}

.carbon-copy-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
