import { cloneElement, type ReactElement } from 'react'
import { Button, Checkbox, Drawer, DropdownMenu, FormField, Input, Modal, RadioGroup, Select, Tooltip } from '@luokuiai/lumen-ui'
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
} from '@luokuiai/flovira-react-designer'
import './styles.css'

const buttonVariants = {
  default: 'outline',
  primary: 'primary',
  danger: 'destructive',
  text: 'ghost',
} as const

const selectLabel = (label: DesignerSelectProps['options'][number]['label'], fallback: string): string => {
  if (typeof label === 'string' || typeof label === 'number') return String(label)
  return fallback
}

const LumenButton = ({
  children,
  variant = 'default',
  size = 'default',
  disabled,
  title,
  ariaLabel,
  className,
  onPress,
}: DesignerButtonProps) => {
  const iconOnly = size === 'icon'
  return (
    <Button
      variant={buttonVariants[variant]}
      size={size === 'default' ? 'md' : 'sm'}
      iconOnly={iconOnly}
      icon={iconOnly ? children : undefined}
      disabled={disabled}
      title={title}
      aria-label={ariaLabel}
      className={className}
      onClick={onPress}
    >
      {iconOnly ? null : children}
    </Button>
  )
}

const LumenInput = ({
  value,
  type = 'text',
  disabled,
  placeholder,
  ariaLabel,
  min,
  className,
  onValueChange,
}: DesignerInputProps) => (
  <Input
    value={value}
    type={type}
    size="md"
    disabled={disabled}
    placeholder={placeholder}
    aria-label={ariaLabel}
    min={min}
    className={className}
    onChange={(event) => onValueChange(event.target.value)}
  />
)

const LumenSelect = ({
  value,
  options,
  disabled,
  ariaLabel,
  className,
  onValueChange,
}: DesignerSelectProps) => (
  <Select
    value={value}
    options={options.map((option) => ({
      value: option.value,
      label: selectLabel(option.label, option.value),
      disabled: option.disabled,
    }))}
    size="md"
    disabled={disabled}
    aria-label={ariaLabel}
    className={className}
    onChange={(nextValue) => {
      if (!Array.isArray(nextValue) && nextValue !== null) onValueChange(String(nextValue))
    }}
  />
)

const LumenCheckbox = ({
  checked,
  disabled,
  ariaLabel,
  className,
  children,
  onCheckedChange,
}: DesignerCheckboxProps) => (
  <Checkbox
    checked={checked}
    disabled={disabled}
    aria-label={ariaLabel}
    className={className}
    label={children ? <span className="frd-lumen-checkbox-label">{children}</span> : undefined}
    onChange={onCheckedChange}
  />
)

const LumenRadioGroup = ({
  value,
  options,
  disabled,
  ariaLabel,
  className,
  direction = 'horizontal',
  onValueChange,
}: DesignerRadioGroupProps) => (
  <RadioGroup
    value={value}
    options={options}
    size="md"
    direction={direction}
    disabled={disabled}
    aria-label={ariaLabel}
    className={className}
    onChange={onValueChange}
  />
)

const LumenField = ({ label, hint, children, className }: DesignerFieldProps) => (
  <FormField label={label} required={false} error={hint} size="sm" className={`frd-lumen-field ${className || ''}`}>
    {children}
  </FormField>
)

const LumenTooltip = ({ content, children, placement, disabled }: DesignerTooltipProps) => (
  <Tooltip content={content} placement={placement} disabled={disabled}>
    {children}
  </Tooltip>
)

const LumenDropdownMenu = ({ trigger, items, align = 'left', onSelect }: DesignerDropdownMenuProps) => (
  <DropdownMenu
    align={align}
    menuMode
    menuClassName="frd-lumen-dropdown-menu"
    trigger={({ open, menuId, toggle }) => cloneElement(
      trigger as ReactElement<Record<string, unknown>>,
      {
        'aria-expanded': open,
        'aria-controls': menuId,
        'aria-haspopup': 'menu',
        onClick: toggle,
      },
    )}
  >
    {({ close }) => items.map((item) => (
      <button
        key={item.value}
        type="button"
        role="menuitem"
        disabled={item.disabled}
        className="frd-lumen-dropdown-item"
        onClick={() => {
          onSelect(item.value)
          close()
        }}
      >
        {item.icon && (
          <span className="frd-lumen-dropdown-icon" style={{ color: item.color }}>
            {item.icon}
          </span>
        )}
        <span>{item.label}</span>
      </button>
    ))}
  </DropdownMenu>
)

const LumenDrawer = ({ open, title, children, width = 340, ariaLabel = '抽屉', onClose }: DesignerDrawerProps) => (
  <Drawer
    open={open}
    placement="right"
    onRequestClose={onClose}
    panelClassName="frd-lumen-drawer"
  >
    <div className="frd-lumen-drawer__shell" role="dialog" aria-modal="true" aria-label={ariaLabel} style={{ width }}>
      <div className="frd-lumen-drawer__header">
        {title}
        <Button variant="ghost" size="sm" iconOnly icon={<span className="frd-lumen-drawer__close-icon" />} aria-label={`关闭${ariaLabel}`} onClick={onClose} />
      </div>
      <div className="frd-lumen-drawer__body">{children}</div>
    </div>
  </Drawer>
)

const LumenDialog = ({
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
  <Modal
    open={open}
    modalId="flovira-participant-picker"
    overlayId="flovira-participant-picker-overlay"
    panelClassName="frd-lumen-dialog"
    onRequestClose={onClose}
  >
    <div className="frd-lumen-dialog__shell" role="dialog" aria-modal="true" aria-label={ariaLabel} style={{ width }}>
      <div className="frd-lumen-dialog__header">
        <strong>{title}</strong>
        <Button variant="ghost" size="sm" iconOnly icon={<span className="frd-lumen-drawer__close-icon" />} aria-label={`关闭${ariaLabel}`} onClick={onClose} />
      </div>
      <div className="frd-lumen-dialog__body">{children}</div>
      <div className="frd-lumen-dialog__footer">
        <Button variant="outline" size="md" onClick={onClose}>{cancelText}</Button>
        <Button variant="primary" size="md" disabled={confirmDisabled} onClick={onConfirm}>{confirmText}</Button>
      </div>
    </div>
  </Modal>
)

export const lumenDesignerUi: DesignerUiAdapter = {
  Button: LumenButton,
  Input: LumenInput,
  Select: LumenSelect,
  Checkbox: LumenCheckbox,
  RadioGroup: LumenRadioGroup,
  Field: LumenField,
  Tooltip: LumenTooltip,
  DropdownMenu: LumenDropdownMenu,
  Drawer: LumenDrawer,
  Dialog: LumenDialog,
}
