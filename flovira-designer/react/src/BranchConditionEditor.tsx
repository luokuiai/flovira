import { useState } from 'react'
import { Plus } from 'lucide-react'
import { compileBranchConditions, getBranchRule, operatorLabels, type BranchRule } from './branchConditions'
import type { DesignerBranchCondition, DesignerConditionField, DesignerConditionGroup, DesignerUiAdapter, FloviraNode } from './types'

export function BranchConditionEditor({ node, index, fields, ui, disabled, compile = compileBranchConditions, onSave }: {
  node: FloviraNode
  index: number
  fields: readonly DesignerConditionField[]
  ui: DesignerUiAdapter
  disabled: boolean
  compile?: (groups: DesignerConditionGroup[]) => string
  onSave: (name: string, rule: BranchRule) => void
}) {
  const [name, setName] = useState(node.skipList[index].skipName || `分支 ${index + 1}`)
  const [rule, setRule] = useState<BranchRule>(() => JSON.parse(JSON.stringify(getBranchRule(node, index))))
  const [error, setError] = useState('')
  const { Input, Select, Button, Field } = ui
  const parallel = node.nodeType === '4'
  const newCondition = (field = fields[0]): DesignerBranchCondition => ({
    fieldCode: field.code, fieldLabel: field.label, fieldType: field.type,
    operator: 'EQ', value: field.type === 'BOOLEAN' ? 'true' : '',
  })
  const update = (groupIndex: number, conditionIndex: number, condition: DesignerBranchCondition) => setRule({ ...rule,
    groups: rule.groups.map((group, i) => i === groupIndex ? {
      conditions: group.conditions.map((item, j) => j === conditionIndex ? condition : item),
    } : group),
  })
  const save = () => {
    try {
      if (!name.trim()) throw new Error('请填写分支名称')
      if (rule.mode === 'default' && node.skipList.some((_, i) => i !== index && getBranchRule(node, i).mode === 'default')) {
        throw new Error('只能有一条其他条件分支，请先调整原分支')
      }
      if (rule.mode === 'rules') {
        rule.groups.forEach((group) => group.conditions.forEach((condition) => {
          const field = fields.find((item) => item.code === condition.fieldCode)
          if (!field || field.type !== condition.fieldType) throw new Error('条件字段已不可用，请重新选择')
        }))
      }
      const expression = parallel || rule.mode === 'default' || rule.mode === 'always' ? ''
        : rule.mode === 'rules' ? compile(rule.groups) : rule.expression.trim()
      if (!parallel && ['rules', 'expression'].includes(rule.mode) && !expression) throw new Error('请设置条件')
      onSave(name.trim(), { ...rule, expression })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '条件保存失败')
    }
  }
  return (
    <div className="frd-condition-editor">
      <Field label="分支名称"><Input ariaLabel="分支名称" value={name} disabled={disabled} onValueChange={setName} /></Field>
      {parallel ? <p className="frd-condition-hint">此分支与其他分支同时执行，无需设置条件。</p> : <>
        {rule.mode === 'default' && <p className="frd-condition-hint">其他分支条件都不满足时，进入此分支。</p>}
        {rule.mode === 'always' && <p className="frd-condition-hint">每次都进入此分支，其他满足条件的分支也会执行。</p>}
        {rule.mode === 'expression' && <Field label="条件表达式" hint="保留已有表达式，也可直接编辑。">
          <Input ariaLabel="条件表达式" value={rule.expression} disabled={disabled}
            onValueChange={(expression) => setRule({ ...rule, expression })} />
        </Field>}
        {rule.mode === 'rules' && <>
          <strong className="frd-condition-title">条件规则</strong>
          {!fields.length && <p className="frd-condition-hint">当前没有可选业务字段，请由接入方提供，或使用表达式配置。</p>}
          {rule.groups.map((group, groupIndex) => <div key={groupIndex}>
            {groupIndex > 0 && <div className="frd-condition-or">或</div>}
            <div className="frd-condition-group">
              <div className="frd-condition-group__heading"><strong>满足以下全部条件</strong></div>
              {group.conditions.map((condition, conditionIndex) => <div key={conditionIndex}>
                {conditionIndex > 0 && <div className="frd-condition-and">且</div>}
                <div className="frd-condition-row">
                  <Select ariaLabel={`条件字段 ${groupIndex + 1}-${conditionIndex + 1}`} value={condition.fieldCode} disabled={disabled}
                    options={[
                      ...(!fields.some((field) => field.code === condition.fieldCode) ? [{ value: condition.fieldCode, label: `${condition.fieldLabel}（字段不可用）`, disabled: true }] : []),
                      ...fields.map((field) => ({ value: field.code, label: field.label })),
                    ]} onValueChange={(code) => { const field = fields.find((item) => item.code === code); if (field) update(groupIndex, conditionIndex, newCondition(field)) }} />
                  <Select ariaLabel={`比较方式 ${groupIndex + 1}-${conditionIndex + 1}`} value={condition.operator} disabled={disabled}
                    options={Object.entries(operatorLabels).filter(([key]) => condition.fieldType === 'NUMBER' || ['EQ', 'NE'].includes(key)).map(([value, label]) => ({ value, label }))}
                    onValueChange={(operator) => update(groupIndex, conditionIndex, { ...condition, operator: operator as DesignerBranchCondition['operator'] })} />
                  {condition.fieldType === 'BOOLEAN' ? <Select ariaLabel={`条件值 ${groupIndex + 1}-${conditionIndex + 1}`} value={condition.value} disabled={disabled}
                    options={[{ value: 'true', label: '是' }, { value: 'false', label: '否' }]}
                    onValueChange={(value) => update(groupIndex, conditionIndex, { ...condition, value })} />
                    : <Input ariaLabel={`条件值 ${groupIndex + 1}-${conditionIndex + 1}`} value={condition.value} disabled={disabled}
                      type={condition.fieldType === 'NUMBER' ? 'number' : 'text'}
                      onValueChange={(value) => update(groupIndex, conditionIndex, { ...condition, value })} />}
                  <Button ariaLabel={`删除条件 ${groupIndex + 1}-${conditionIndex + 1}`} variant="text" className="frd-condition-remove" disabled={disabled}
                    onPress={() => setRule({ ...rule, groups: rule.groups.map((item, i) => i === groupIndex
                      ? { conditions: item.conditions.filter((_, j) => j !== conditionIndex) } : item) })}>删除条件</Button>
                </div>
              </div>)}
              <div className="frd-condition-group-actions">
              <Button disabled={disabled || !fields.length} onPress={() => setRule({ ...rule,
                groups: rule.groups.map((item, i) => i === groupIndex ? { conditions: [...item.conditions, newCondition()] } : item),
              })}><Plus size={14} /> 添加条件</Button>
              <Button ariaLabel={`删除条件组 ${groupIndex + 1}`} variant="text" disabled={disabled}
                onPress={() => setRule({ ...rule, groups: rule.groups.filter((_, i) => i !== groupIndex) })}>删除条件组</Button>
              </div>
            </div>
          </div>)}
          <Button disabled={disabled || !fields.length} onPress={() => setRule({ ...rule, groups: [...rule.groups, { conditions: [newCondition()] }] })}>
            <Plus size={14} /> 添加条件组
          </Button>
        </>}
        {['rules', 'expression', 'always'].includes(rule.mode) && <Button variant="text" size="compact" disabled={disabled}
          onPress={() => setRule({ ...rule, mode: rule.mode === 'expression' || rule.mode === 'always' ? 'rules' : 'expression' })}>
          {rule.mode === 'expression' || rule.mode === 'always' ? '使用条件配置' : '使用表达式'}
        </Button>}
      </>}
      {error && <p className="frd-condition-error" role="alert">{error}</p>}
      {!disabled && <div className="frd-condition-footer"><Button variant="primary" onPress={save}>保存条件</Button></div>}
    </div>
  )
}
