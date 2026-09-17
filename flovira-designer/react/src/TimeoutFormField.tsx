import { useEffect, useState } from 'react'
import type { DesignerFormField, DesignerFormFieldLoader } from './formPermissions'
import type { DesignerUiAdapter, FloviraDefinition, FloviraNode } from './types'

export function TimeoutFormField({ definition, node, fields, queryFields, value, label, disabled, ui, onChange }: {
  definition: FloviraDefinition; node: FloviraNode; fields: readonly DesignerFormField[]
  queryFields?: DesignerFormFieldLoader; value: string; label: string; disabled: boolean
  ui: DesignerUiAdapter; onChange(code: string, label: string): void
}) {
  const [options, setOptions] = useState<DesignerFormField[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    Promise.resolve().then(() => queryFields ? queryFields({ definition, node }) : fields).then(result => {
      if (!active) return
      if (!Array.isArray(result)) throw new Error('未返回表单字段列表')
      const dates = result.filter(field => ['date', 'datetime'].includes(field?.dataType || '') && !field.code?.includes('[]'))
      const codes = new Set<string>()
      for (const field of dates) {
        if (!/^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$/.test(field.code)
          || field.code.split('.').length > 32 || !field.label?.trim() || codes.has(field.code)) throw new Error('日期时间字段定义无效')
        codes.add(field.code)
      }
      setOptions(dates)
    }).catch(reason => { if (active) setError(reason instanceof Error ? reason.message : '表单字段加载失败') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [fields, queryFields, node.nodeCode, definition.formId, retry])
  const { Field, Select, Button } = ui
  const missing = value && !options.some(field => field.code === value)
  return <Field label="表单日期时间字段">
    <Select ariaLabel="表单日期时间字段" value={value} disabled={disabled || loading || !!error}
      options={[{ value: '', label: '请选择日期时间字段' },
        ...(missing ? [{ value, label: `${label || value}（未匹配字段）`, disabled: true }] : []),
        ...options.map(field => ({ value: field.code, label: field.label }))]}
      onValueChange={code => onChange(code, options.find(field => field.code === code)?.label || '')} />
    {loading ? <p role="status">正在加载表单字段…</p> : error ? <>
      <p role="alert">{error}</p><Button size="compact" onPress={() => setRetry(count => count + 1)}>重新加载</Button>
    </> : !options.length ? <p role="status">暂无日期时间字段，请先在表单定义中添加。</p> : null}
    {!loading && !error && missing && <p role="alert">已选字段不可用，请重新选择。</p>}
    {!value && <p className="frd-condition-hint">请选择作为超时截止时间的表单字段。</p>}
  </Field>
}
