import { useEffect, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import type { DesignerResourceItem, DesignerResourceLoader, DesignerUiAdapter } from './types'

export function SubprocessField({ value, label, disabled, queryResources, ui, onChange }: {
  value: string
  label?: string
  disabled?: boolean
  queryResources?: DesignerResourceLoader
  ui: DesignerUiAdapter
  onChange: (value: string, label?: string) => void
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
  const { Field, Button, Input, DropdownMenu, AsyncSelect } = ui
  const loadedCurrent = items.find((item) => (item.code || item.id) === value)
  const current = loadedCurrent
    || (selected && (selected.code || selected.id) === value ? selected : undefined)
    || (value && label ? { id: value, code: value, name: label, resourceType: 'SUBPROCESS' } : undefined)
  useEffect(() => {
    if (value && loadedCurrent?.name && loadedCurrent.name !== label) onChange(value, loadedCurrent.name)
  }, [value, label, loadedCurrent?.name])
  if (AsyncSelect) {
    const options = [
      ...(value ? [{ value: '', label: '清除选择' }] : []),
      ...Array.from(new Map([
        ...(current ? [current] : []),
        ...items,
      ].map((item) => [item.code || item.id, item])).values()).map((item) => ({
        value: item.code || item.id,
        label: item.name,
        disabled: item.disabled,
      })),
    ]
    return <Field label="子流程">
      <AsyncSelect
        value={value}
        options={options}
        disabled={disabled || !queryResources}
        placeholder={queryResources ? '请选择已发布流程' : '请接入方提供子流程查询'}
        searchable
        loading={state === 'loading'}
        loadingText="加载中..."
        searchValue={keyword}
        searchPlaceholder="搜索流程名称或编码"
        emptyText={state === 'error' ? '子流程加载失败，请重新搜索' : '暂无匹配流程'}
        ariaLabel="子流程"
        className="frd-subprocess-select"
        onOpenChange={setOpen}
        onSearchChange={(text) => { setKeyword(text); setPageNum(1); setItems([]); setHasMore(false); setState('loading') }}
        onValueChange={(nextValue) => {
          const item = items.find((candidate) => (candidate.code || candidate.id) === nextValue)
          setSelected(item)
          onChange(nextValue, item?.name)
        }}
      />
    </Field>
  }
  return <Field label="子流程">
    <DropdownMenu items={[]} onSelect={() => {}} onOpenChange={setOpen}
      trigger={<button type="button" className="frd-select frd-subprocess-trigger" disabled={disabled}
        aria-label="子流程"><span>{current?.name || value || '请选择已发布流程'}</span><ChevronDown size={16} /></button>}
      renderContent={({ close }) => <div className="frd-subprocess-options">
      <Input ariaLabel="搜索子流程" placeholder="搜索流程名称或编码" value={keyword} disabled={disabled}
        onValueChange={(text) => { setKeyword(text); setPageNum(1); setItems([]); setHasMore(false); setState('loading') }} />
      <div role="listbox" aria-label="已发布流程" className="frd-subprocess-options__list">
        {value && <button type="button" role="option" aria-selected={false}
          onClick={() => { onChange(''); setSelected(undefined); close() }}>清除选择</button>}
        {items.map(item => <button key={item.code || item.id} type="button" role="option"
          aria-selected={value === (item.code || item.id)} disabled={disabled || item.disabled}
          onClick={() => { setSelected(item); onChange(item.code || item.id, item.name); close() }}>{item.name}</button>)}
      </div>
      {!queryResources && <p role="status">请接入方提供子流程查询</p>}
      {state === 'loading' && <p role="status">加载中...</p>}
      {state === 'error' && <><p role="alert">子流程加载失败，已选值保留</p>
        <Button size="compact" disabled={disabled} onPress={() => setRetry((count) => count + 1)}>重试</Button></>}
      {queryResources && state === 'idle' && !items.length && <p role="status">暂无匹配流程</p>}
      {hasMore && <Button size="compact" disabled={disabled || state !== 'idle'} onPress={() => setPageNum((page) => page + 1)}>加载更多</Button>}
    </div>} />
  </Field>
}
