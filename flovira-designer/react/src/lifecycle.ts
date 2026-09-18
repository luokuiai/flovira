export const LIFECYCLE_POINTS = [
  ['BEFORE_OPERATION', '操作前'], ['BEFORE_ASSIGNMENT', '分派前'],
  ['PROCESS_STARTED', '流程开始'], ['PROCESS_WITHDRAWN', '流程撤回'],
  ['PROCESS_RESUBMITTED', '流程重新提交'], ['PROCESS_ENDED', '流程结束'],
  ['NODE_ENTERED', '节点进入'], ['NODE_LEFT', '节点离开'],
  ['APPROVAL_ACTION_COMPLETED', '审批办理完成'], ['ASSIGNEES_CHANGED', '办理人变化'],
] as const
export type LifecyclePoint = typeof LIFECYCLE_POINTS[number][0]
export interface LifecycleSubscription {
  code: string
  point: LifecyclePoint
  phase: 'IN_TRANSACTION' | 'AFTER_COMMIT'
  order: number
  parameters?: string | null
}
export interface LifecycleConfig { schemaVersion: 1; subscriptions: LifecycleSubscription[] }
export const lifecyclePoints = (nodeType?: string) => LIFECYCLE_POINTS.filter(([point]) => {
  if (nodeType === undefined) return true
  if (!['0', '1', '2', '6', '7', '8'].includes(nodeType)) return false
  if (point.startsWith('PROCESS_')) return false
  if (['APPROVAL_ACTION_COMPLETED', 'ASSIGNEES_CHANGED'].includes(point)) return nodeType === '1'
  if (point === 'BEFORE_ASSIGNMENT') return ['1', '8'].includes(nodeType)
  return true
})
export function parseLifecycle(value?: string): LifecycleConfig {
  if (!value) return { schemaVersion: 1, subscriptions: [] }
  const config = JSON.parse(value)
  if (config?.schemaVersion !== 1 || !Array.isArray(config.subscriptions)) throw new Error('监听配置版本无效')
  if (config.subscriptions.some((row: unknown) => !row || typeof row !== 'object'
      || typeof (row as LifecycleSubscription).code !== 'string'
      || typeof (row as LifecycleSubscription).point !== 'string')) throw new Error('监听订阅结构无效')
  return config
}
export function validateLifecycle(config: LifecycleConfig, nodeType?: string): void {
  const keys = new Map<string, string>()
  config.subscriptions.forEach(row => {
    if (!row || typeof row.code !== 'string' || !row.code.trim()) throw new Error('请输入监听器 Bean 名')
    if (!lifecyclePoints(nodeType).some(([point]) => point === row.point)) throw new Error('当前范围不支持此回调')
    if (!['IN_TRANSACTION', 'AFTER_COMMIT'].includes(row.phase)
        || (row.point.startsWith('BEFORE_') && row.phase !== 'IN_TRANSACTION')) throw new Error('执行前回调必须在事务内')
    if (!Number.isInteger(row.order)) throw new Error('执行顺序必须为整数')
    if (row.parameters != null && typeof row.parameters !== 'string') throw new Error('监听参数必须为文本')
    const key = `${row.code}:${row.point}:${row.phase}`
    const settings = JSON.stringify([row.order, row.parameters ?? null])
    if (keys.has(key) && keys.get(key) !== settings) throw new Error('重复订阅的顺序或参数冲突')
    keys.set(key, settings)
  })
}
function extEntries(ext: unknown): { code: string; value: unknown }[] {
  const entries: unknown = typeof ext === 'string' ? JSON.parse(ext || '[]') : ext ?? []
  if (!Array.isArray(entries) || entries.some(entry => !entry || typeof entry.code !== 'string')) throw new Error('扩展配置无效')
  return entries
}
export function lifecycleValue(ext?: unknown): string | undefined {
  const entries = extEntries(ext)
  const matches = entries.filter(entry => entry.code === 'lifecycle')
  if (matches.length > 1) throw new Error('监听配置重复')
  const value = matches[0]?.value
  if (value !== undefined && typeof value !== 'string') throw new Error('监听配置必须为 JSON 文本')
  return value as string | undefined
}
export function withLifecycle(ext: unknown, config: LifecycleConfig): string {
  const entries = extEntries(ext)
  return JSON.stringify([...entries.filter(entry => entry.code !== 'lifecycle'), { code: 'lifecycle', value: JSON.stringify(config) }])
}
