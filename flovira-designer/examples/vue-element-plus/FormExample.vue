<script setup lang="ts">
import { ref } from 'vue'
import { FormDesigner, type FormDefinition, type FormDesignerInstance } from '@luokuiai/flovira-vue-designer'
const form = ref<FormDefinition>({ schemaVersion: '1', fields: [
  { key: 'expense_title', label: '报销事由', dataType: 'string' },
  { key: 'Amount', label: '报销金额', dataType: 'number' },
  { key: 'details', label: '报销明细', dataType: 'array', items: { dataType: 'object', fields: [
    { key: 'item_name', label: '费用名称', dataType: 'string' },
    { key: 'amount', label: '费用金额', dataType: 'number' },
  ] } },
] })
const editor = ref<FormDesignerInstance>()
const saved = ref('')
function save() { if (editor.value?.validate().valid) saved.value = editor.value.getJson() }
</script>
<template>
  <main class="form-example">
    <header><h1>表单定义</h1><el-button type="primary" @click="save">保存</el-button></header>
    <FormDesigner ref="editor" v-model="form" />
    <details v-if="saved"><summary>已保存到示例状态</summary><pre>{{ saved }}</pre></details>
  </main>
</template>
<style scoped>
.form-example { max-width: 1000px; padding: 32px; margin: auto; }
header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
h1 { font-size: 20px; font-weight: 500; }
pre { white-space: pre-wrap; }
</style>
