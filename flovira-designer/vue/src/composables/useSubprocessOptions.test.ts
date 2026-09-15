import { expect, test } from 'bun:test'
import { effectScope, nextTick, ref } from 'vue'
import { useSubprocessOptions } from './useSubprocessOptions'
import type { DesignerResourceQuery, DesignerResourcePage } from '../data/contracts'

const settle = async () => { await nextTick(); await new Promise(resolve => setTimeout(resolve, 240)) }
const item = (code: string) => ({ id: code, code, name: code, resourceType: 'SUBPROCESS' })

test('subprocess options load on demand with pagination and search reset', async () => {
  const calls: DesignerResourceQuery[] = []
  const scope = effectScope()
  const options = scope.run(() => useSubprocessOptions(ref(false), async query => {
    calls.push(query)
    return { items: [item(query.keyword || `flow_${query.pageNum}`)], total: query.keyword ? 1 : 21 }
  }))!
  try {
    await settle()
    expect(calls).toHaveLength(0)
    options.open.value = true
    await settle()
    expect(calls[0]).toEqual({ resourceType: 'SUBPROCESS', keyword: '', pageNum: 1, pageSize: 20 })
    options.pageNum.value++
    await settle()
    expect(options.items.value.map(item => item.code)).toEqual(['flow_1', 'flow_2'])
    expect(options.hasMore.value).toBe(false)
    options.keyword.value = 'finance'
    await settle()
    expect(calls[2].pageNum).toBe(1)
    expect(options.items.value.map(item => item.code)).toEqual(['finance'])
  } finally { scope.stop() }
})

test('subprocess options ignore stale responses and allow retry', async () => {
  let resolveOld!: (page: DesignerResourcePage) => void
  let count = 0
  const scope = effectScope()
  const options = scope.run(() => useSubprocessOptions(ref(false), async () => {
    if (++count === 1) return new Promise(resolve => { resolveOld = resolve })
    if (count === 2) throw new Error('network')
    return { items: [item('new')], total: 1 }
  }))!
  try {
    options.open.value = true
    await settle()
    options.keyword.value = 'new'
    await settle()
    expect(options.state.value).toBe('error')
    resolveOld({ items: [item('old')], total: 1 })
    options.retry.value++
    await settle()
    expect(options.items.value.map(item => item.code)).toEqual(['new'])
  } finally { scope.stop() }
})
