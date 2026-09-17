import { getFormConditionFields, type FormDefinition, type FormFieldDefinition } from './formDefinition'

export const formFieldTypes: { value: FormFieldDefinition['dataType']; label: string }[] = [
  { value: 'string', label: '文本' }, { value: 'number', label: '数字' },
  { value: 'boolean', label: '布尔值' }, { value: 'date', label: '日期' },
  { value: 'datetime', label: '日期时间' }, { value: 'object', label: '对象' },
  { value: 'array', label: '数组' },
]
export interface FormDesignerValidation { valid: boolean; message: string }
export interface FormDesignerInstance {
  getDefinition(): FormDefinition
  getJson(): string
  validate(): FormDesignerValidation
  focusFirstField(): void
}
export function emptyForm(): FormDefinition { return { schemaVersion: '1', fields: [] } }
export function newFormField(): FormFieldDefinition { return { key: '', label: '', dataType: 'string' } }
export function copyForm(value: FormDefinition): FormDefinition { return JSON.parse(JSON.stringify(value)) }
export function validateForm(value: FormDefinition): FormDesignerValidation {
  try {
    getFormConditionFields(value)
    for (const field of value.fields) {
      const children = field.dataType === 'object' ? field.fields
        : field.dataType === 'array' && field.items?.dataType === 'object' ? field.items.fields : []
      if (field.items?.dataType === 'array' || children?.some(child => ['object', 'array'].includes(child.dataType))) {
        return { valid: false, message: (field.label || '字段') + '不支持多层嵌套，子字段只能使用基础类型' }
      }
    }
    return { valid: true, message: '' }
  }
  catch (error) { return { valid: false, message: error instanceof Error ? error.message : '表单定义无效' } }
}
export function changeFieldType(field: FormFieldDefinition, dataType: FormFieldDefinition['dataType']): FormFieldDefinition {
  const { fields: _fields, items: _items, ...rest } = field
  return { ...rest, dataType, ...(dataType === 'object' ? { fields: [] } : dataType === 'array' ? { items: { dataType: 'string' as const } } : {}) }
}
export function hasNestedFields(field: FormFieldDefinition): boolean {
  return !!field.fields?.length || !!field.items && (field.items.dataType !== 'string' || hasNestedFields(field.items))
}
