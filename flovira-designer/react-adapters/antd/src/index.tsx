import { Button, Checkbox, Drawer, Dropdown, Form, Input, Modal, Radio, Select, Tooltip } from 'antd'
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

const AntdButton = ({
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
      type={variant === 'primary' ? 'primary' : variant === 'text' ? 'text' : 'default'}
      danger={variant === 'danger'}
      size={size === 'default' ? 'middle' : 'small'}
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

const AntdInput = ({
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
    size="small"
    disabled={disabled}
    placeholder={placeholder}
    aria-label={ariaLabel}
    min={min}
    className={className}
    onChange={(event) => onValueChange(event.target.value)}
  />
)

const AntdSelect = ({
  value,
  options,
  disabled,
  ariaLabel,
  className,
  onValueChange,
}: DesignerSelectProps) => (
  <Select
    value={value}
    options={options}
    size="small"
    disabled={disabled}
    aria-label={ariaLabel}
    className={className}
    popupMatchSelectWidth={false}
    onChange={(nextValue) => onValueChange(String(nextValue))}
  />
)

const AntdCheckbox = ({
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
    onChange={(event) => onCheckedChange(event.target.checked)}
  >
    {children}
  </Checkbox>
)

const AntdRadioGroup = ({
  value,
  options,
  disabled,
  ariaLabel,
  className,
  direction = 'horizontal',
  onValueChange,
}: DesignerRadioGroupProps) => (
  <Radio.Group
    value={value}
    disabled={disabled}
    aria-label={ariaLabel}
    className={`${className || ''} frd-antd-radio-group frd-antd-radio-group--${direction}`}
    onChange={(event) => onValueChange(String(event.target.value))}
  >
    {options.map((option) => (
      <Radio key={option.value} value={option.value} disabled={option.disabled}>{option.label}</Radio>
    ))}
  </Radio.Group>
)

const AntdField = ({ label, hint, children, className }: DesignerFieldProps) => (
  <Form.Item
    label={label}
    help={hint}
    validateStatus={hint ? 'error' : undefined}
    className={`frd-antd-field ${className || ''}`}
  >
    {children}
  </Form.Item>
)

const AntdTooltip = ({ content, children, placement, disabled }: DesignerTooltipProps) => (
  <Tooltip title={content} placement={placement} open={disabled ? false : undefined}>
    {children}
  </Tooltip>
)

const AntdDropdownMenu = ({ trigger, items, align = 'left', onSelect }: DesignerDropdownMenuProps) => (
  <Dropdown
    trigger={['click']}
    placement={align === 'right' ? 'bottomRight' : 'bottomLeft'}
    menu={{
      items: items.map((item) => ({
        key: item.value,
        disabled: item.disabled,
        icon: item.icon ? (
          <span className="frd-antd-dropdown-icon" style={{ color: item.color }}>{item.icon}</span>
        ) : undefined,
        label: item.label,
      })),
      onClick: ({ key }) => onSelect(key),
    }}
  >
    {trigger}
  </Dropdown>
)

const AntdDrawer = ({ open, title, children, width = 340, ariaLabel = '抽屉', onClose }: DesignerDrawerProps) => (
  <Drawer
    open={open}
    title={title}
    size={width}
    placement="right"
    aria-label={ariaLabel}
    rootClassName="frd-antd-drawer"
    styles={{ body: { padding: 20 } }}
    onClose={onClose}
  >
    {children}
  </Drawer>
)

const AntdDialog = ({
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
    title={title}
    width={width}
    aria-label={ariaLabel}
    okText={confirmText}
    cancelText={cancelText}
    okButtonProps={{ disabled: confirmDisabled }}
    destroyOnHidden
    onOk={onConfirm}
    onCancel={onClose}
  >
    {children}
  </Modal>
)

export const antdDesignerUi: DesignerUiAdapter = {
  Button: AntdButton,
  Input: AntdInput,
  Select: AntdSelect,
  Checkbox: AntdCheckbox,
  RadioGroup: AntdRadioGroup,
  Field: AntdField,
  Tooltip: AntdTooltip,
  DropdownMenu: AntdDropdownMenu,
  Drawer: AntdDrawer,
  Dialog: AntdDialog,
}
