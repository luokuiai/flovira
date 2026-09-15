import { useEffect, useState } from 'react'
import type { DesignerResourceItem, DesignerResourceLoader, DesignerUiAdapter } from './types'

export function SubprocessField({ value, disabled, queryResources, ui, onChange }: {
  value: string
  disabled?: boolean
  queryResources?: DesignerResourceLoader
  ui: DesignerUiAdapter
  onChange: (value: string) => void
}) {
  const [open, setOpen] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [pageNum, setPageNum] = useState(1)
  const [items, setItems] = useState<DesignerResourceItem[]>([])
  const [selected, setSelected] = useState<DesignerResourceItem>()
  const [hasMore, setHasMore] = useState(false)
  const [state, setState] = useState<'loading' | 'idle' | 'error'>('idle')
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    setOpen(false)
    setItems([])
    setSelected(undefined)
    setKeyword('')
    setPageNum(1)
  }, [queryResources])
  useEffect(() => {
    if (!open || disabled || !queryResources) return
    let active = true
    setState('loading')
    const timer = setTimeout(() => {
      Promise.resolve().then(() => queryResources({ resourceType: 'SUBPROCESS', keyword, pageNum, pageSize: 20 }))
        .then((response) => {
          if (!active) return
          const page = 'items' in response ? response : response.data
          if (!page || !Array.isArray(page.items) || !Number.isFinite(page.total)) throw new Error('无效分页响应')
          setItems((previous) => Array.from(new Map(
            [...(pageNum === 1 ? [] : previous), ...page.items].map((item) => [item.code || item.id, item]),
          ).values()))
          setHasMore(page.items.length > 0 && pageNum * 20 < page.total)
          setState('idle')
        }).catch(() => { if (active) setState('error') })
    }, 200)
    return () => { active = false; clearTimeout(timer) }
  }, [open, disabled, queryResources, keyword, pageNum, retry])
  const { Field, Button, Input, Select } = ui
  const current = items.find((item) => (item.code || item.id) === value)
    || (selected && (selected.code || selected.id) === value ? selected : undefined)
  return <Field label="固定子流程">
    <Select ariaLabel="固定子流程" value={value} disabled={disabled}
      options={[
        { value: '', label: '请选择已发布流程' },
        ...(value && !items.some((item) => (item.code || item.id) === value)
          ? [{ value, label: current?.name || value }] : []),
        ...items.map((item) => ({ value: item.code || item.id, label: item.name, disabled: item.disabled })),
      ]} onValueChange={(code) => {
        setSelected(items.find((item) => (item.code || item.id) === code))
        onChange(code)
      }} />
    {queryResources && !open && <Button disabled={disabled} onPress={() => setOpen(true)}>选择流程</Button>}
    {open && <>
      <Input ariaLabel="搜索子流程" placeholder="搜索流程名称或编码" value={keyword} disabled={disabled}
        onValueChange={(text) => { setKeyword(text); setPageNum(1); setItems([]) }} />
      {state === 'loading' && <p role="status">加载中...</p>}
      {state === 'error' && <><p role="alert">子流程加载失败，已选值保留</p>
        <Button disabled={disabled} onPress={() => setRetry((count) => count + 1)}>重试</Button></>}
      {state === 'idle' && !items.length && <p role="status">暂无匹配流程</p>}
      {hasMore && <Button disabled={disabled || state !== 'idle'} onPress={() => setPageNum((page) => page + 1)}>加载更多</Button>}
    </>}
  </Field>
}
