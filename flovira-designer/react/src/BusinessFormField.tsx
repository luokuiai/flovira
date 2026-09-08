import { useEffect, useState } from 'react'
import type { DesignerResourceItem, DesignerResourceLoader, DesignerUiAdapter } from './types'

export function BusinessFormField({ value, inherited = false, disabled, queryResources, ui, onChange }: {
  value: string
  inherited?: boolean
  disabled?: boolean
  queryResources?: DesignerResourceLoader
  ui: DesignerUiAdapter
  onChange: (value: string) => void
}) {
  const [items, setItems] = useState<DesignerResourceItem[]>([])
  const [state, setState] = useState<'idle' | 'loading' | 'error'>('idle')
  useEffect(() => {
    setItems([])
    if (!queryResources) { setState('idle'); return }
    let active = true
    setState('loading')
    queryResources({ resourceType: 'FORM', pageNum: 1, pageSize: 1000 })
      .then((response) => {
        if (!active) return
        const page = 'items' in response ? response : response.data
        if (!page) throw new Error('未返回表单列表')
        setItems(page.items)
        setState('idle')
      })
      .catch(() => { if (active) setState('error') })
    return () => { active = false }
  }, [queryResources])
  const { Field, Input, Select } = ui
  const placeholder = inherited ? '继承流程表单' : '不指定表单'
  return (
    <Field label={inherited ? '节点表单' : '流程表单'}>
      {queryResources ? (
        <Select value={value} disabled={disabled || state !== 'idle'} ariaLabel="业务表单"
          options={[
            { value: '', label: placeholder },
            ...(value && !items.some((item) => item.id === value) ? [{ value, label: value }] : []),
            ...items.map((item) => ({ value: item.id, label: item.name, disabled: item.disabled })),
          ]} onValueChange={onChange} />
      ) : (
        <Input value={value} disabled={disabled} ariaLabel="业务表单标识"
          placeholder={inherited ? '留空继承流程表单' : '填写业务表单标识'} onValueChange={onChange} />
      )}
      {state === 'loading' && <p className="frd-condition-hint">正在加载表单…</p>}
      {state === 'error' && <p role="alert" className="frd-condition-hint">表单加载失败，请检查业务数据接口。</p>}
    </Field>
  )
}
