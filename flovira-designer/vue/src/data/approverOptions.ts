import type { ApproverSubject, DesignerApproverOption, DesignerApproverStrategy } from './contracts'

export function visibleApproverOptions(strategy?: DesignerApproverStrategy): DesignerApproverOption[] {
  return (strategy?.options || []).filter(option => {
    if (option.nodeTypes?.length && !option.nodeTypes.includes('1')) return false
    // 多人审批沿用基础设置中的或签、会签、票签控件。
    if (option.code === 'approvalMode') return false
    const cardinality = strategy?.resultCardinality || (strategy?.resourceType === 'USER'
      && !strategy.relationType ? strategy.multiple ? 'ONE_OR_MORE' : 'EXACTLY_ONE' : 'ZERO_OR_MORE')
    if (option.condition === 'EMPTY') return cardinality === 'ZERO_OR_ONE' || cardinality === 'ZERO_OR_MORE'
    if (option.condition === 'MULTIPLE') return cardinality === 'ONE_OR_MORE' || cardinality === 'ZERO_OR_MORE'
    return true
  })
}

export function changeApproverOption(config: Record<string, unknown>, option: DesignerApproverOption, value: string) {
  const next = { ...config, [option.code]: value }
  option.choices.forEach(choice => {
    if (choice.selectionConfigKey && choice.value !== value) delete next[choice.selectionConfigKey]
  })
  return next
}

export function approverOptionError(config: Record<string, unknown>, options: DesignerApproverOption[],
  strategies: DesignerApproverStrategy[]): string | undefined {
  for (const [key, subjectKey] of [['emptyPolicy', 'emptyPolicySubjects'], ['sameAsStarterAction', 'sameAsStarterSubjects']]) {
    const value = config[key]
    const allowed = key === 'emptyPolicy' ? ['ERROR', 'SKIP', 'TRANSFER_TO_USER']
      : ['SELF_APPROVE', 'AUTO_SKIP_OR_TRANSFER', 'TRANSFER_TO_USER']
    if (value != null && !allowed.includes(String(value))) return '审批人处理策略无效'
    if (value === 'TRANSFER_TO_USER') {
      const subjects = config[subjectKey] as ApproverSubject[] | undefined
      if (!strategies.some(strategy => strategy.code === 'USER') || !Array.isArray(subjects) || !subjects.length
        || subjects.some(subject => !subject || typeof subject.id !== 'string' || !subject.id.trim() || subject.type !== 'USER')) {
        return '需要选择有效的转交人员'
      }
    }
  }
  for (const option of options) {
    const value = config[option.code] ?? option.defaultValue ?? option.choices[0]?.value
    const choice = option.choices.find(item => item.value === value && !item.disabled)
    if (!choice) return `${option.name}的配置无效`
    if (!choice.selectionConfigKey) continue
    const target = strategies.find(item => item.code === choice.selectionStrategy)
    const subjects = config[choice.selectionConfigKey] as ApproverSubject[] | undefined
    if (!target || !Array.isArray(subjects) || !subjects.length
      || subjects.some(item => !item || typeof item.id !== 'string' || !item.id.trim() || item.type !== target.resourceType)) {
      return `${option.name}需要选择有效的转交人员`
    }
  }
}
