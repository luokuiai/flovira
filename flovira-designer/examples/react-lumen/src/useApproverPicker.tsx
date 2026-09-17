import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Checkbox, Dialog, Input } from '@luokuiai/lumen-ui'
import type { ApproverSelectionContext, ApproverSelectionResult, ApproverSelector, ApproverSubject, DesignerResourcePage } from '@luokuiai/flovira-react-designer'
import { OrganizationParticipantPicker } from './OrganizationParticipantPicker'
import { queryDesignerResources } from './api'

function ResourcePicker({ context, selected, onChange }: {
  context: ApproverSelectionContext; selected: ApproverSubject[]; onChange: (subjects: ApproverSubject[]) => void
}) {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [result, setResult] = useState<DesignerResourcePage>({ items: [], total: 0 })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    queryDesignerResources({ resourceType: context.strategy.resourceType!, keyword, pageNum: page, pageSize: 20 })
      .then(data => { if (active) setResult(data) })
      .catch(() => { if (active) setError('加载失败，请重试') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [context.strategy.resourceType, keyword, page, retry])
  return <div className="host-resource-picker">
    <Input value={keyword} placeholder="搜索名称" aria-label="搜索候选项" onChange={event => { setKeyword(event.target.value); setPage(1) }} />
    {error ? <div role="alert">{error}<Button onClick={() => setRetry(value => value + 1)}>重试</Button></div>
      : loading ? <p role="status">加载中…</p>
        : result.items.map(item => <Checkbox key={item.id} label={item.name} disabled={item.disabled || (context.multiple
          && context.strategy.maxSubjects != null && selected.length >= context.strategy.maxSubjects
          && !selected.some(subject => subject.id === item.id))}
          checked={selected.some(subject => subject.id === item.id)}
          onChange={checked => {
            const rest = selected.filter(subject => subject.id !== item.id)
            onChange(checked ? [...(context.multiple ? rest : []), { id: item.id, name: item.name, type: item.resourceType }] : rest)
          }} />)}
    <div>
      <Button disabled={loading || page === 1} onClick={() => setPage(value => value - 1)}>上一页</Button>
      <Button disabled={loading || page * 20 >= result.total} onClick={() => setPage(value => value + 1)}>下一页</Button>
    </div>
  </div>
}

/** Example host implementation: the application owns the entire modal lifecycle. */
export function useApproverPicker() {
  const [context, setContext] = useState<ApproverSelectionContext | null>(null)
  const [selected, setSelected] = useState<ApproverSubject[]>([])
  const resolver = useRef<((value: ApproverSelectionResult | null) => void) | null>(null)
  const selectApprover = useCallback<ApproverSelector>(next => new Promise(resolve => {
    resolver.current?.(null)
    resolver.current = resolve
    setSelected(next.selected)
    setContext(next)
  }), [])
  const finish = (value: ApproverSelectionResult | null) => {
    resolver.current?.(value)
    resolver.current = null
    setContext(null)
  }
  useEffect(() => () => { resolver.current?.(null); resolver.current = null }, [])
  const picker = context && <Dialog open onRequestClose={() => finish(null)}>
    <section className="host-approver-dialog" role="dialog" aria-modal="true" aria-label="业务人员选择器">
      <header><h2>选择{context.strategy.name}</h2><Button variant="ghost" onClick={() => finish(null)}>关闭</Button></header>
      {context.strategy.resourceType === 'USER'
        ? <OrganizationParticipantPicker selected={selected} multiple={context.multiple} maxSubjects={context.strategy.maxSubjects} onChange={setSelected} />
        : <ResourcePicker context={context} selected={selected} onChange={setSelected} />}
      <footer>
        <Button variant="outline" onClick={() => finish(null)}>取消</Button>
        <Button variant="primary" onClick={() => finish({ subjects: selected })}>确定</Button>
      </footer>
    </section>
  </Dialog>
  return { selectApprover, picker }
}
