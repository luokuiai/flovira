import { onScopeDispose, ref, watch, type Ref } from 'vue'
import type { DesignerResourceItem, DesignerResourcePage, DesignerResourceQuery } from '../data/contracts'

export function useSubprocessOptions(disabled: Ref<boolean>, query: (query: DesignerResourceQuery) => Promise<DesignerResourcePage | undefined>) {
  const open = ref(false)
  const keyword = ref('')
  const pageNum = ref(1)
  const items = ref<DesignerResourceItem[]>([])
  const hasMore = ref(false)
  const state = ref<'idle' | 'loading' | 'error'>('idle')
  const retry = ref(0)
  let generation = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  watch([open, keyword, pageNum, disabled, retry], (_, previous) => {
    const request = ++generation
    clearTimeout(timer)
    if (previous && keyword.value !== previous[1]) {
      items.value = []
      if (pageNum.value !== 1) { pageNum.value = 1; return }
    }
    if (!open.value || disabled.value) return
    state.value = 'loading'
    timer = setTimeout(async () => {
      try {
        const page = await query({ resourceType: 'SUBPROCESS', keyword: keyword.value, pageNum: pageNum.value, pageSize: 20 })
        if (request !== generation) return
        if (!page || !Array.isArray(page.items) || !Number.isFinite(page.total)) throw new Error('无效分页响应')
        items.value = Array.from(new Map([...(pageNum.value === 1 ? [] : items.value), ...page.items]
          .map((item) => [item.code || item.id, item])).values())
        hasMore.value = page.items.length > 0 && pageNum.value * 20 < page.total
        state.value = 'idle'
      } catch (_) {
        if (request === generation) state.value = 'error'
      }
    }, 200)
  })
  onScopeDispose(() => { generation++; clearTimeout(timer) })
  return { open, keyword, pageNum, items, hasMore, state, retry }
}
