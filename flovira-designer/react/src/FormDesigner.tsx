import { forwardRef, useEffect, useImperativeHandle, useRef, useState, type CSSProperties } from 'react'
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react'
import type { FormDefinition, FormFieldDefinition } from './formDefinition'
import { changeFieldType, copyForm, emptyForm, formFieldTypes, hasNestedFields, newFormField, validateForm, type FormDesignerInstance } from './formDesignerModel'
import type { DesignerUiAdapter } from './types'
import { defaultDesignerUi } from './ui'
import './formDesigner.css'

export interface FormDesignerProps {
  value?: FormDefinition
  defaultValue?: FormDefinition
  onChange?: (definition: FormDefinition) => void
  appearance?: 'standalone' | 'embedded'
  /** 容器背景，支持 CSS background 值；不传时使用主题默认背景。 */
  background?: CSSProperties['background']
  readOnly?: boolean
  ui?: Partial<DesignerUiAdapter>
  className?: string
  style?: CSSProperties
}

interface RowProps {
  field: FormFieldDefinition
  path: string
  item?: boolean
  depth: number
  readOnly: boolean
  ui: DesignerUiAdapter
  onChange: (field: FormFieldDefinition) => void
  onRemove?: () => void
  onMove?: (offset: number) => void
  first?: boolean
  last?: boolean
}
function FieldRow({ field, path, item, depth, readOnly, ui, onChange, onRemove, onMove, first, last }: RowProps) {
  const [pendingType, setPendingType] = useState<FormFieldDefinition['dataType'] | null>(null)
  useEffect(() => setPendingType(null), [field])
  const { Input, Select, Button } = ui
  const allowedTypes = formFieldTypes.filter(type => depth === 0 || !['object', 'array'].includes(type.value) || item && depth === 1 && type.value === 'object')
  const unsupported = !allowedTypes.some(type => type.value === field.dataType)
  return <div className={`ffd-field${item ? ' ffd-field--item' : ''}`}>
    <div className="ffd-row">
      {item ? <span className="ffd-item-label">数组元素</span> : <>
        <Input ariaLabel={`字段名称 ${path}`} placeholder="请输入字段名称" value={field.label || ''} disabled={readOnly}
          onValueChange={label => onChange({ ...field, label })} />
        <Input ariaLabel={`字段键 ${path}`} placeholder="请输入字段键，如 order_id" value={field.key || ''} disabled={readOnly}
          onValueChange={key => onChange({ ...field, key })} />
      </>}
      <Select ariaLabel={`数据类型 ${path}`} options={unsupported ? [{ value: field.dataType, label: '不支持多层嵌套', disabled: true }, ...allowedTypes] : allowedTypes} value={field.dataType} disabled={readOnly}
        onValueChange={value => {
          const type = value as FormFieldDefinition['dataType']
          if (type === field.dataType) return
          if (hasNestedFields(field)) setPendingType(type)
          else onChange(changeFieldType(field, type))
        }} />
      <div className="ffd-actions">
        {!readOnly && !item && <>
          <Button size="icon" variant="text" className="ffd-action" ariaLabel={`上移字段 ${path}`} title="上移" disabled={first}
            onPress={() => onMove?.(-1)}><ArrowUp size={16} aria-hidden="true" /></Button>
          <Button size="icon" variant="text" className="ffd-action" ariaLabel={`下移字段 ${path}`} title="下移" disabled={last}
            onPress={() => onMove?.(1)}><ArrowDown size={16} aria-hidden="true" /></Button>
          <Button size="icon" variant="text" className="ffd-action ffd-action--delete" ariaLabel={`删除字段 ${path}`} title="删除"
            onPress={() => onRemove?.()}><Trash2 size={16} aria-hidden="true" /></Button>
        </>}
      </div>
    </div>
    {pendingType && <div className="ffd-confirm" role="alert">
      <span>切换类型会清除子字段，是否继续？</span>
      <Button onPress={() => setPendingType(null)}>取消</Button>
      <Button variant="primary" disabled={readOnly} onPress={() => { onChange(changeFieldType(field, pendingType)); setPendingType(null) }}>确认切换</Button>
    </div>}
    {!unsupported && field.dataType === 'object' && <div className="ffd-children">
      <FieldList fields={field.fields || []} path={path} depth={depth + 1} readOnly={readOnly} ui={ui}
        onChange={fields => onChange({ ...field, fields })} />
    </div>}
    {!unsupported && field.dataType === 'array' && <div className="ffd-children">
      <FieldRow field={field.items || { dataType: 'string' }} path={path + '[]'} item depth={depth + 1} readOnly={readOnly} ui={ui}
        onChange={items => onChange({ ...field, items })} />
    </div>}
    {unsupported && <p role="alert" className="ffd-error">不支持多层嵌套，请将子字段调整为基础类型。</p>}
  </div>
}
function FieldList({ fields, path = '', depth = 0, readOnly, ui, onChange }: {
  fields: FormFieldDefinition[]; path?: string; depth?: number; readOnly: boolean; ui: DesignerUiAdapter; onChange: (fields: FormFieldDefinition[]) => void
}) {
  const { Button } = ui
  return <>
    {fields.map((field, index) => <FieldRow key={index} field={field} path={path ? path + '.' + (index + 1) : String(index + 1)}
      depth={depth} readOnly={readOnly} ui={ui} first={index === 0} last={index === fields.length - 1}
      onChange={next => onChange(fields.map((entry, i) => i === index ? next : entry))}
      onRemove={() => onChange(fields.filter((_, i) => i !== index))}
      onMove={offset => {
        const next = [...fields]; const target = index + offset
        if (target < 0 || target >= next.length) return
        ;[next[index], next[target]] = [next[target], next[index]]
        onChange(next)
      }} />)}
    {!readOnly && <Button className="ffd-add" variant="text" onPress={() => onChange([...fields, newFormField()])}>
      <Plus size={16} />{depth ? '添加子字段' : '添加字段'}
    </Button>}
  </>
}

/** 可独立使用的表单元数据编辑器；持久化与业务布局由接入方控制。 */
export const FormDesigner = forwardRef<FormDesignerInstance, FormDesignerProps>(function FormDesigner({
  value, defaultValue, onChange, appearance = 'standalone', background, readOnly = false, ui: overrides, className = '', style,
}, ref) {
  const [internal, setInternal] = useState(() => copyForm(defaultValue || emptyForm()))
  const definition = value ?? internal
  const [error, setError] = useState('')
  const root = useRef<HTMLDivElement>(null)
  const ui = { ...defaultDesignerUi, ...overrides }
  useImperativeHandle(ref, () => ({
    getDefinition: () => copyForm(definition),
    getJson: () => JSON.stringify(definition),
    validate: () => { const result = validateForm(definition); setError(result.message); return result },
    focusFirstField: () => root.current?.querySelector<HTMLInputElement>('input:not(:disabled)')?.focus(),
  }), [definition])
  const update = (fields: FormFieldDefinition[]) => {
    if (readOnly) return
    const next = copyForm({ ...definition, fields })
    setInternal(next); setError(''); onChange?.(copyForm(next))
  }
  return <div ref={root} className={`ffd-form ffd-react ${className}`} data-appearance={appearance} style={{ background, ...style }}>
    <div className="ffd-heading"><span>表单字段</span><span className="ffd-count">{definition.fields.length} 个字段</span></div>
    <div className="ffd-scroll">
      <div className="ffd-table">
        <div className="ffd-row ffd-columns" aria-hidden="true"><span>字段名称</span><span>字段键</span><span>数据类型</span><span /></div>
        {!definition.fields.length && <div className="ffd-empty">暂无字段{!readOnly && '，添加字段开始定义表单'}</div>}
        <FieldList fields={definition.fields} readOnly={readOnly} ui={ui} onChange={update} />
      </div>
    </div>
    {error && <p className="ffd-error" role="alert">{error}</p>}
  </div>
})
