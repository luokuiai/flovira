import { cloneElement, useEffect, useId, useRef, useState, type ReactElement } from 'react'
import { X } from 'lucide-react'
import type {
  DesignerButtonProps,
  DesignerCheckboxProps,
  DesignerDropdownMenuProps,
  DesignerDialogProps,
  DesignerDrawerProps,
  DesignerFieldProps,
  DesignerInputProps,
  DesignerRadioGroupProps,
  DesignerSelectProps,
  DesignerTooltipProps,
  DesignerUiAdapter,
} from './types'

const classes = (...values: Array<string | undefined>) => values.filter(Boolean).join(' ')

const Button = ({
  children,
  variant = 'default',
  size = 'default',
  disabled,
  title,
  ariaLabel,
  className,
  onPress,
}: DesignerButtonProps) => (
  <button
    type="button"
    title={title}
    aria-label={ariaLabel}
    disabled={disabled}
    onClick={onPress}
    className={classes('frd-button', `frd-button--${variant}`, `frd-button--${size}`, className)}
  >
    {children}
  </button>
)

const Input = ({
  value,
  type = 'text',
  disabled,
  placeholder,
  ariaLabel,
  min,
  className,
  onValueChange,
}: DesignerInputProps) => (
  <input
    value={value}
    type={type}
    disabled={disabled}
    placeholder={placeholder}
    aria-label={ariaLabel}
    min={min}
    className={classes('frd-input', className)}
    onChange={(event) => onValueChange(event.target.value)}
  />
)

const Select = ({
  value,
  options,
  disabled,
  ariaLabel,
  className,
  onValueChange,
}: DesignerSelectProps) => (
  <select
    value={value}
    disabled={disabled}
    aria-label={ariaLabel}
    className={classes('frd-select', className)}
    onChange={(event) => onValueChange(event.target.value)}
  >
    {options.map((option) => (
      <option key={option.value} value={option.value} disabled={option.disabled}>{option.label}</option>
    ))}
  </select>
)

const Checkbox = ({
  checked,
  disabled,
  ariaLabel,
  className,
  children,
  onCheckedChange,
}: DesignerCheckboxProps) => (
  <label className={classes('frd-checkbox', className)}>
    <input
      type="checkbox"
      checked={checked}
      disabled={disabled}
      aria-label={ariaLabel}
      onChange={(event) => onCheckedChange(event.target.checked)}
    />
    {children}
  </label>
)

const RadioGroup = ({
  value,
  options,
  disabled,
  ariaLabel,
  className,
  direction = 'horizontal',
  onValueChange,
}: DesignerRadioGroupProps) => {
  const name = useId()
  return (
    <div
      role="radiogroup"
      aria-label={ariaLabel}
      className={classes('frd-radio-group', `frd-radio-group--${direction}`, className)}
    >
      {options.map((option) => (
        <label className="frd-radio" key={option.value}>
          <input
            type="radio"
            name={name}
            value={option.value}
            checked={value === option.value}
            disabled={disabled || option.disabled}
            onChange={() => onValueChange(option.value)}
          />
          <span>{option.label}</span>
        </label>
      ))}
    </div>
  )
}

const Field = ({ label, hint, children, className }: DesignerFieldProps) => (
  <div className={classes('frd-field', className)}>
    <div className="frd-field__label">{label}</div>
    <div className="frd-field__control">{children}</div>
    {hint && <div className="frd-field__hint">{hint}</div>}
  </div>
)

const Tooltip = ({ content, children, disabled }: DesignerTooltipProps) => (
  <span className="frd-tooltip-trigger" title={disabled ? undefined : String(content)}>
    {children}
  </span>
)

const DropdownMenu = ({ trigger, items, align = 'left', onSelect }: DesignerDropdownMenuProps) => {
  const [open, setOpen] = useState(false)
  const menuId = useId()
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  const triggerElement = trigger as ReactElement<Record<string, unknown>>
  return (
    <div ref={rootRef} className="frd-dropdown">
      {cloneElement(triggerElement, {
        'aria-expanded': open,
        'aria-controls': menuId,
        'aria-haspopup': 'menu',
        onClick: () => setOpen((current) => !current),
      })}
      {open && (
        <div id={menuId} role="menu" className={`frd-dropdown-menu frd-dropdown-menu--${align}`}>
          {items.map((item) => (
            <button
              key={item.value}
              type="button"
              role="menuitem"
              disabled={item.disabled}
              className="frd-dropdown-menu__item"
              onClick={() => {
                onSelect(item.value)
                setOpen(false)
              }}
            >
              {item.icon && <span className="frd-dropdown-menu__icon" style={{ color: item.color }}>{item.icon}</span>}
              <span>{item.label}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

const Drawer = ({
  open,
  title,
  children,
  width = 340,
  ariaLabel = '抽屉',
  onClose,
}: DesignerDrawerProps) => {
  const [retainedChildren, setRetainedChildren] = useState(children)

  useEffect(() => {
    if (open) setRetainedChildren(children)
  }, [children, open])

  return (
    <div className="frd-drawer" data-open={open ? 'true' : 'false'} aria-hidden={!open}>
      <button className="frd-drawer__mask" type="button" tabIndex={open ? 0 : -1} aria-label={`关闭${ariaLabel}`} onClick={onClose} />
      <aside
        className="frd-drawer__panel"
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        style={{ width: `min(${width}px, calc(100% - 24px))` }}
      >
        <div className="frd-drawer__header">
          {title}
          <Button size="icon" variant="text" onPress={onClose} ariaLabel={`关闭${ariaLabel}`}>
            <X size={16} />
          </Button>
        </div>
        <div className="frd-drawer__body">{open ? children : retainedChildren}</div>
      </aside>
    </div>
  )
}

const Dialog = ({
  open,
  title,
  children,
  width = 560,
  ariaLabel = '弹窗',
  confirmText = '确定',
  cancelText = '取消',
  confirmDisabled,
  onConfirm,
  onClose,
}: DesignerDialogProps) => (
  <div className="frd-dialog" data-open={open ? 'true' : 'false'} aria-hidden={!open}>
    <button className="frd-dialog__mask" type="button" tabIndex={open ? 0 : -1} aria-label={`关闭${ariaLabel}`} onClick={onClose} />
    <section
      className="frd-dialog__panel"
      role="dialog"
      aria-modal="true"
      aria-label={ariaLabel}
      style={{ width: `min(${width}px, calc(100% - 32px))` }}
    >
      <div className="frd-dialog__header">
        <strong>{title}</strong>
        <Button size="icon" variant="text" onPress={onClose} ariaLabel={`关闭${ariaLabel}`}>
          <X size={16} />
        </Button>
      </div>
      <div className="frd-dialog__body">{children}</div>
      <div className="frd-dialog__footer">
        <Button onPress={onClose}>{cancelText}</Button>
        <Button variant="primary" disabled={confirmDisabled} onPress={onConfirm}>{confirmText}</Button>
      </div>
    </section>
  </div>
)

export const defaultDesignerUi: DesignerUiAdapter = {
  Button,
  Input,
  Select,
  Checkbox,
  RadioGroup,
  Field,
  Tooltip,
  DropdownMenu,
  Drawer,
  Dialog,
}
