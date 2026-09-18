import { getFormConditionFields, type FormDefinition, type FormFieldDefinition } from './formDefinition'
import { readNodeExt, setNodeExtConfig } from './model'
import type { FloviraDefinition, FloviraNode } from './types'

export interface DesignerFormField { code: string; label: string; dataType?: FormFieldDefinition['dataType'] }
export interface NodeFieldPermission { code: string; readable: boolean; writable: boolean }
/** Unconfigured fields are readable and not writable. Enforcement belongs to the host. */
export interface NodeFormPermissions { schemaVersion: 1; fields: NodeFieldPermission[] }
export type DesignerFormFieldLoader = (context: { definition: FloviraDefinition; node: FloviraNode }) =>
  Promise<readonly DesignerFormField[]>

/** Only actual fields, never derived array counts; nested labels remain human-readable. */
export function getFormPermissionFields(form: FormDefinition | string): DesignerFormField[] {
  const fields = getFormConditionFields(form).filter(field => !field.code.endsWith('.$count'))
  const definition: FormDefinition = typeof form === 'string' ? JSON.parse(form) : form
  const types = new Map<string, FormFieldDefinition['dataType']>()
  const visit = (field: FormFieldDefinition, path: string) => {
    if (field.dataType === 'object') field.fields!.forEach(child => visit(child, path + '.' + child.key))
    else if (field.dataType === 'array') visit(field.items!, path + '[]')
    else types.set(path, field.dataType)
  }
  definition.fields.forEach(field => visit(field, field.key!))
  return fields.map(({ code, label }) => ({ code, label, dataType: types.get(code) }))
}

export function getNodeFormPermissions(node: FloviraNode): NodeFormPermissions {
  const value = readNodeExt(node).formPermissions
  if (value == null) return { schemaVersion: 1, fields: [] }
  const config = typeof value === 'string' ? JSON.parse(value) : value
  if (!config || config.schemaVersion !== 1 || !Array.isArray(config.fields)) throw new Error('表单权限配置格式无效')
  const codes = new Set<string>()
  for (const field of config.fields) {
    if (!field || typeof field.code !== 'string' || !field.code || codes.has(field.code)
      || typeof field.readable !== 'boolean' || typeof field.writable !== 'boolean'
      || field.writable && !field.readable) throw new Error('表单字段权限配置无效')
    codes.add(field.code)
  }
  return JSON.parse(JSON.stringify(config))
}

export function setNodeFieldPermission(node: FloviraNode, permission: NodeFieldPermission): FloviraNode {
  if (!permission.code.trim() || typeof permission.readable !== 'boolean' || typeof permission.writable !== 'boolean') {
    throw new Error('表单字段权限配置无效')
  }
  const config = getNodeFormPermissions(node)
  const next = { ...permission, readable: permission.readable || permission.writable }
  const found = config.fields.some(field => field.code === permission.code)
  return setNodeExtConfig(node, 'formPermissions', {
    ...config,
    fields: found ? config.fields.map(field => field.code === permission.code ? next : field) : [...config.fields, next],
  })
}
