import * as RadixSelect from "@radix-ui/react-select";
import { Icon } from "./icons";

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps {
  value: string;
  options: SelectOption[];
  onChange: (value: string) => void;
  disabled?: boolean;
  id?: string;
  title?: string;
  /** 触发器左侧的图标名（lucide 映射名） */
  icon?: string;
}

/* Radix 不允许空字符串作为选项值，这里做一层编解码 */
const EMPTY = "\u0000empty";

const encode = (value: string) => (value === "" ? EMPTY : value);
const decode = (value: string) => (value === EMPTY ? "" : value);

/**
 * 下拉框（Radix Select）：键盘导航、typeahead、焦点管理与浮层定位都交给组件，
 * 视觉仍走应用自己的样式类（.vk-select-*）。
 */
export function Select({ value, options, onChange, disabled, id, title, icon }: SelectProps) {
  const current = options.find(option => option.value === value);

  return (
    <RadixSelect.Root
      value={encode(value)}
      onValueChange={next => onChange(decode(next))}
      disabled={disabled}
    >
      <RadixSelect.Trigger
        id={id}
        className="vk-select-btn"
        title={title ?? current?.label}
        aria-label={title}
      >
        {icon && <Icon name={icon} size={13} className="vk-select-icon" />}
        <RadixSelect.Value className="vk-select-value" placeholder="—" />
        <RadixSelect.Icon className="vk-select-caret">
          <Icon name="chevronDown" size={12} />
        </RadixSelect.Icon>
      </RadixSelect.Trigger>

      <RadixSelect.Portal>
        <RadixSelect.Content className="vk-select-menu" position="popper" sideOffset={4} collisionPadding={8}>
          <RadixSelect.Viewport className="vk-select-viewport">
            {options.map(option => (
              <RadixSelect.Item key={option.value} value={encode(option.value)} className="vk-select-option">
                {/* 下拉列表里的每一项也带上图标，和触发器保持一致 */}
                {icon && <Icon name={icon} size={13} className="vk-select-option-icon" />}
                <RadixSelect.ItemText>{option.label}</RadixSelect.ItemText>
                <RadixSelect.ItemIndicator className="vk-select-check">
                  <Icon name="check" size={12} />
                </RadixSelect.ItemIndicator>
              </RadixSelect.Item>
            ))}
          </RadixSelect.Viewport>
        </RadixSelect.Content>
      </RadixSelect.Portal>
    </RadixSelect.Root>
  );
}
