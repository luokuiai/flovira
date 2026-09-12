<template>
  <main class="example-shell">
    <header class="example-header">
      <div><span class="eyebrow">Composable Flovira example</span><h1>Vue + Ant Design Vue</h1></div>
      <label>Demo identity (not authentication)
        <a-select v-model:value="user" :options="identityOptions" @change="changeUser" />
      </label>
    </header>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-alert v-if="notice" type="success" show-icon :message="notice" />

    <section class="card toolbar">
      <a-select v-model:value="selectedId" placeholder="Choose a definition" :options="definitionOptions" @change="openDefinition" />
      <a-button @click="newDefinition">New</a-button>
      <a-button :disabled="!selectedId" @click="publish">Publish</a-button>
      <a-button type="primary" :disabled="!selectedId || !purchases[0]" @click="start">Start sample request</a-button>
      <a-button @click="refresh">Refresh</a-button>
    </section>

    <section class="card designer-card">
      <FlowDesigner
        :key="designerKey"
        :definition-id="selectedId ?? null"
        :initial-json="selectedId ? null : initialDefinition"
        @saved="saved"
      />
    </section>

    <div class="example-grid">
      <section class="card"><h2>Host-owned purchase request</h2>
        <dl v-for="purchase in purchases" :key="purchase.id">
          <dt>{{ purchase.title }}</dt><dd>{{ purchase.department }} · {{ purchase.amount }} · {{ purchase.status }}</dd>
        </dl>
      </section>
      <section class="card"><h2>My tasks</h2>
        <p v-if="!tasks.length">No pending tasks for {{ user }}.</p>
        <div v-for="task in tasks" :key="task.id" class="task">
          <span>{{ task.nodeName }} · process {{ task.instanceId }}</span>
          <a-button size="small" type="primary" @click="handle(task, 'approve')">Approve</a-button>
          <a-button size="small" danger @click="handle(task, 'reject')">Reject</a-button>
        </div>
      </section>
    </div>

    <section class="card"><h2>Process progress {{ instanceId ? `#${instanceId}` : '' }}</h2>
      <a-button v-if="instanceId" @click="loadProgress">Refresh progress</a-button>
      <pre>{{ progress ? JSON.stringify(progress, null, 2) : 'Start or handle a process to inspect database-backed progress.' }}</pre>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { FlowDesigner } from '@luokuiai/flovira-vue-designer'
import {
  exampleApi,
  type DefinitionSummary,
  type DemoIdentity,
  type InstanceProgress,
  type PurchaseRequest,
  type TaskSummary,
} from '@flovira-example/common'

const props = defineProps<{ onUserChange: (user: string) => void }>()
const identities = ref<DemoIdentity[]>([])
const definitions = ref<DefinitionSummary[]>([])
const purchases = ref<PurchaseRequest[]>([])
const tasks = ref<TaskSummary[]>([])
const user = ref('alice')
const selectedId = ref<number>()
const instanceId = ref<number>()
const progress = ref<InstanceProgress>()
const designerKey = ref(0)
const error = ref('')
const notice = ref('')

const identityOptions = computed(() => identities.value.map(item => ({ label: item.name, value: item.id })))
const definitionOptions = computed(() => definitions.value.map(item => ({ label: `${item.flowName} · v${item.version}`, value: item.id })))
const initialDefinition = computed(() => ({
  flowCode: `purchase_${Date.now()}`,
  flowName: 'Purchase approval',
  version: '1',
  publishStatus: 0,
  formId: 'purchase-request-v1',
  nodeList: [
    { nodeType: 0, nodeCode: 'start', nodeName: 'Start', nodeRatio: '0', coordinate: '180,260|180,260', skipList: [{ id: 'edge_1', sourceNodeCode: 'start', targetNodeCode: 'manager', skipName: '', skipType: 'PASS' }] },
    { nodeType: 1, nodeCode: 'manager', nodeName: 'Manager approval', nodeRatio: '0', permissionFlag: 'manager', coordinate: '430,260|430,260', skipList: [{ id: 'edge_2', sourceNodeCode: 'manager', targetNodeCode: 'end', skipName: '', skipType: 'PASS' }, { id: 'edge_3', sourceNodeCode: 'manager', targetNodeCode: 'end', skipName: 'Reject request', skipType: 'REJECT' }] },
    { nodeType: 2, nodeCode: 'end', nodeName: 'End', nodeRatio: '0', coordinate: '680,260|680,260', skipList: [] },
  ],
}))

async function run(operation: () => Promise<void>) {
  error.value = ''; notice.value = ''
  try { await operation() } catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
}
async function refresh() {
  await run(async () => {
    const result = await Promise.all([exampleApi.definitions(), exampleApi.purchases(), exampleApi.tasks(user.value)])
    definitions.value = result[0]; purchases.value = result[1]; tasks.value = result[2]
  })
}
async function changeUser(value: unknown) {
  user.value = String(value); props.onUserChange(user.value); await refresh()
}
function openDefinition() { designerKey.value++ }
function newDefinition() { selectedId.value = undefined; designerKey.value++ }
async function saved(payload: { data?: { id?: number } }) {
  if (payload.data?.id) selectedId.value = payload.data.id
  await refresh(); notice.value = 'Definition saved'
}
async function publish() {
  if (!selectedId.value) return
  await run(async () => { await exampleApi.publishDefinition(selectedId.value!, user.value); await refresh(); notice.value = 'Definition published' })
}
async function start() {
  if (!selectedId.value || !purchases.value[0]) return
  await run(async () => {
    const instance = await exampleApi.start(purchases.value[0].id, selectedId.value!, user.value)
    instanceId.value = Number(instance.id); await refresh(); await loadProgress(); notice.value = `Started process ${instanceId.value}`
  })
}
async function handle(task: TaskSummary, action: 'approve' | 'reject') {
  await run(async () => {
    await exampleApi.handle(task.id, action, `${action} from ${user.value}`, user.value)
    instanceId.value = task.instanceId; await refresh(); await loadProgress(); notice.value = `${action} task ${task.id}`
  })
}
async function loadProgress() {
  if (instanceId.value) progress.value = await exampleApi.progress(instanceId.value, user.value)
}
onMounted(async () => {
  await run(async () => { identities.value = await exampleApi.identities(); await refresh() })
})
</script>
