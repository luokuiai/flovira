import { lifecyclePoints, lifecycleValue, parseLifecycle, validateLifecycle, withLifecycle } from './lifecycle'
import type { LifecycleSubscription } from './lifecycle'
import type { DesignerUiAdapter } from './types'

export function LifecycleExtEditor({ ext, onChange, ...props }: {
  ext?: unknown; nodeType?: string; ui: DesignerUiAdapter; disabled: boolean; onChange(ext: string): void
}) {
  let value
  try { value = lifecycleValue(ext) } catch (error) { return <p role="alert">{String(error)}</p> }
  return <LifecycleEditor {...props} value={value} onChange={value => onChange(withLifecycle(ext, parseLifecycle(value)))} />
}

export function LifecycleEditor({ value, nodeType, ui, disabled, onChange }: {
  value?: string; nodeType?: string; ui: DesignerUiAdapter; disabled: boolean; onChange(value: string): void
}) {
  const { Field, Input, Select, Button } = ui
  let config
  try { config = parseLifecycle(value) } catch (error) { return <p role="alert">{String(error)}</p> }
  let error = ''
  try { validateLifecycle(config, nodeType) } catch (failure) { error = String(failure) }
  const options = lifecyclePoints(nodeType).map(([value, label]) => ({ value, label }))
  const change = (index: number, patch: Partial<LifecycleSubscription>) => onChange(JSON.stringify({ ...config,
    subscriptions: config.subscriptions.map((row, i) => i === index ? { ...row, ...patch } : row),
  }))
  return <section aria-label={nodeType === undefined ? '流程回调' : '节点回调'} className="frd-settings-group">
    <h4>{nodeType === undefined ? '流程回调' : '节点回调'}</h4>
    {error && <p role="alert">{error}</p>}
    {config.subscriptions.map((row, index) => <fieldset className="frd-lifecycle-row" key={index} disabled={disabled}>
      <Field label="监听器 Bean 名"><Input ariaLabel={`监听器 Bean 名 ${index + 1}`} value={row.code} disabled={disabled}
        onValueChange={code => change(index, { code })} /></Field>
      <Field label="回调时机"><Select ariaLabel={`回调时机 ${index + 1}`} value={row.point} disabled={disabled} options={options}
        onValueChange={point => change(index, { point: point as LifecycleSubscription['point'],
          phase: point.startsWith('BEFORE_') ? 'IN_TRANSACTION' : row.phase })} /></Field>
      <Field label="执行阶段"><Select ariaLabel={`执行阶段 ${index + 1}`} value={row.phase} disabled={disabled}
        options={[{ value: 'IN_TRANSACTION', label: '事务内' }, ...(!row.point.startsWith('BEFORE_') ? [{ value: 'AFTER_COMMIT', label: '提交后' }] : [])]}
        onValueChange={phase => change(index, { phase: phase as LifecycleSubscription['phase'] })} /></Field>
      <Field label="执行顺序"><Input ariaLabel={`执行顺序 ${index + 1}`} type="number" value={String(row.order)} disabled={disabled}
        onValueChange={order => change(index, { order: Number(order) })} /></Field>
      <Field label="参数"><Input ariaLabel={`监听参数 ${index + 1}`} value={row.parameters || ''} disabled={disabled}
        onValueChange={parameters => change(index, { parameters })} /></Field>
      <Button disabled={disabled} onPress={() => onChange(JSON.stringify({ ...config, subscriptions: config.subscriptions.filter((_, i) => i !== index) }))}>删除回调</Button>
    </fieldset>)}
    <Button disabled={disabled || !options.length} onPress={() => onChange(JSON.stringify({ ...config,
      subscriptions: [...config.subscriptions, { code: '', point: options[0].value, phase: 'IN_TRANSACTION', order: 0 }],
    }))}>添加回调</Button>
  </section>
}
