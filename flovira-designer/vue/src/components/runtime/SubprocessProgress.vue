<template>
  <section class="subprocess-progress">
    <div v-if="loading" class="state-text">{{ t('common.loading') }}</div>
    <div v-else-if="!summary" class="state-text">{{ t('subprocess.noRun') }}</div>
    <template v-else>
      <button type="button" class="summary-button" @click="toggleExpanded">
        <span>{{ summary.completed }}/{{ summary.total }}</span>
        <span :class="['status', `status-${String(summary.status).toLowerCase()}`]">{{ summary.status }}</span>
      </button>
      <div v-if="expanded" class="children">
        <div v-if="childrenLoading" class="state-text">{{ t('common.loading') }}</div>
        <div v-else-if="!children.length" class="state-text">{{ t('common.empty') }}</div>
        <button v-for="child in children" :key="child.id" type="button" class="child-row" @click="loadHistory(child.id)">
          <span>{{ child.itemLabel || child.itemKey }}</span>
          <span>{{ child.currentNodeName || child.childStatus }}</span>
        </button>
        <div v-if="historyLoading" class="state-text">{{ t('common.loading') }}</div>
        <ol v-else-if="history.length" class="history-list">
          <li v-for="(entry, index) in history" :key="`${entry.source}-${entry.occurredAt}-${index}`">
            <strong>{{ entry.source }}</strong>
            <span>{{ entry.nodeName || entry.action }}</span>
            <span>{{ entry.outcome }}</span>
          </li>
        </ol>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDataProvider } from '@/data/provider'
import { useI18n } from '@/i18n'

defineOptions({ name: 'SubprocessProgress' })

const props = defineProps<{ parentTaskId: string | number }>()
const { t } = useI18n()
const loading = ref(true)
const childrenLoading = ref(false)
const historyLoading = ref(false)
const expanded = ref(false)
const summary = ref<any>(null)
const children = ref<any[]>([])
const history = ref<any[]>([])

onMounted(async () => {
  try {
    summary.value = (await getDataProvider().subprocessSummary(props.parentTaskId))?.data || null
  } finally {
    loading.value = false
  }
})

async function toggleExpanded() {
  expanded.value = !expanded.value
  if (!expanded.value || children.value.length || !summary.value?.runId) return
  childrenLoading.value = true
  try {
    const response = await getDataProvider().subprocessChildren(summary.value.runId, 1, 64)
    children.value = response?.data?.list || []
  } finally {
    childrenLoading.value = false
  }
}

async function loadHistory(childId: string | number) {
  historyLoading.value = true
  try {
    history.value = (await getDataProvider().subprocessHistory(summary.value.runId, childId))?.data || []
  } finally {
    historyLoading.value = false
  }
}
</script>

<style scoped>
.subprocess-progress { width: 100%; color: var(--wf-text-primary); }
.summary-button, .child-row { width: 100%; border: 0; background: transparent; color: inherit; cursor: pointer; }
.summary-button { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--wf-border-light); }
.child-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 12px; padding: 9px 0; text-align: left; border-bottom: 1px solid var(--wf-border-lighter); }
.child-row span:last-child { text-align: right; color: var(--wf-text-secondary); }
.state-text { padding: 10px 0; color: var(--wf-text-secondary); }
.status-failed, .status-cancelled { color: var(--wf-danger); }
.status-completed { color: var(--wf-success, #67c23a); }
.history-list { margin: 8px 0 0; padding-left: 20px; }
.history-list li { display: grid; grid-template-columns: 110px minmax(0, 1fr) 100px; gap: 8px; padding: 5px 0; }
</style>
