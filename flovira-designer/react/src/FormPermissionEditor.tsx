import { useEffect, useState } from 'react'
import { getNodeFormPermissions, setNodeFieldPermission, type DesignerFormField, type DesignerFormFieldLoader, type NodeFormPermissions } from './formPermissions'
import type { DesignerUiAdapter, FloviraDefinition, FloviraNode } from './types'

export function FormPermissionEditor({ definition, node, fields, queryFields, ui, disabled, onChange }: {
  definition: FloviraDefinition; node: FloviraNode; fields: readonly DesignerFormField[]
  queryFields?: DesignerFormFieldLoader; ui: DesignerUiAdapter; disabled: boolean; onChange(node: FloviraNode): void
}) {
  const [loaded, setLoaded] = useState<readonly DesignerFormField[]>([])
  const [loading, setLoading] = useState(Boolean(queryFields))
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(Boolean(queryFields)); setError('')
    Promise.resolve().then(() => queryFields ? queryFields({ definition, node }) : fields).then(result => {
      if (!active) return
      const codes = new Set<string>()
      if (!Array.isArray(result)) throw new Error('未返回表单字段列表')
      for (const field of result) {
        if (!field || typeof field.code !== 'string' || !field.code.trim() || typeof field.label !== 'string'
          || !field.label.trim() || codes.has(field.code)) throw new Error('表单字段名称或编码无效')
        codes.add(field.code)
      }
      setLoaded(result)
    }).catch(reason => {
      if (active) setError(reason instanceof Error ? reason.message : '表单字段加载失败')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [fields, queryFields, node.nodeCode, definition.formId, retry])
  const { Checkbox, Button } = ui
  let config: NodeFormPermissions
  try { config = getNodeFormPermissions(node) }
  catch (reason) { return <p role="alert" className="frd-error-text">{reason instanceof Error ? reason.message : '表单权限配置无效'}</p> }
  if (loading) return <p role="status" className="frd-condition-hint">正在加载表单字段…</p>
  if (error) return <div><p role="alert" className="frd-error-text">{error}</p><Button onPress={() => setRetry(value => value + 1)}>重新加载</Button></div>
  const rows = [...loaded, ...config.fields.filter(field => !loaded.some(item => item.code === field.code))
    .map(field => ({ code: field.code, label: '未匹配字段' }))]
  if (!rows.length) return <p className="frd-condition-hint">暂无表单字段，请先定义表单或由业务提供字段列表。</p>
  return <div className="frd-form-permissions">
    <table aria-label="节点表单权限">
      <thead><tr><th scope="col">字段</th><th scope="col">可读</th><th scope="col">可写</th></tr></thead>
      <tbody>{rows.map(field => {
        const permission = config.fields.find(item => item.code === field.code) || { code: field.code, readable: true, writable: false }
        const label = field.label + '（' + field.code + '）'
        return <tr key={field.code}>
          <th scope="row"><span>{field.label}</span><small>{field.code}</small></th>
          <td><Checkbox checked={permission.readable} disabled={disabled} ariaLabel={label + '可读'}
            onCheckedChange={readable => onChange(setNodeFieldPermission(node, { ...permission, readable, writable: readable && permission.writable }))} /></td>
          <td><Checkbox checked={permission.writable} disabled={disabled} ariaLabel={label + '可写'}
            onCheckedChange={writable => onChange(setNodeFieldPermission(node, { ...permission, writable, readable: writable || permission.readable }))} /></td>
        </tr>
      })}</tbody>
    </table>
  </div>
}
