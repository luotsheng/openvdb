import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { Icon } from "./icons";
import { showMenu, type NativeMenuItem } from "../api";
import { menuIconDataUrl } from "./menuIcon";

export interface MenuEntry {
  label?: string;
  action?: () => void;
  separator?: boolean;
  danger?: boolean;
  disabled?: boolean;
  /** 菜单项左侧图标（lucide 映射名，会栅格化成位图给系统菜单用） */
  icon?: string;
  /** 图标颜色，默认中性灰 */
  iconColor?: string;
}

/**
 * 菜单项渲染：右键菜单与下拉菜单共用同一套样式类（.menu-item）。
 * Radix 会用 data-highlighted / data-disabled 标记状态，键盘与鼠标行为一致。
 */
function renderEntries(
  entries: MenuEntry[],
  components: { Item: typeof DropdownMenu.Item; Separator: typeof DropdownMenu.Separator }
) {
  const { Item, Separator } = components;

  return entries.map((entry, index) => entry.separator
    ? <Separator key={`sep-${index}`} className="ctx-sep" />
    : (
      <Item
        key={`${entry.label}-${index}`}
        className={`menu-item${entry.danger ? " is-danger" : ""}`}
        disabled={entry.disabled}
        onSelect={() => entry.action?.()}
      >
        {entry.icon && <Icon name={entry.icon} size={13} className="menu-item-icon" />}
        {entry.label}
      </Item>
    ));
}

/**
 * 弹系统原生右键菜单并执行选中项。
 * 菜单项里的图标/危险色等样式由系统决定，这里只传文本与可用状态。
 */
export async function popupNativeMenu(entries: MenuEntry[]) {
  if (entries.length === 0)
    return;

  const items: NativeMenuItem[] = await Promise.all(entries.map(async (entry, index) => {
    if (entry.separator)
      return { type: "separator" as const };

    return {
      id: String(index),
      label: entry.label ?? "",
      enabled: !entry.disabled,
      /* 有图标就先栅格化成 PNG，系统菜单才能显示 */
      icon: entry.icon ? await menuIconDataUrl(entry.icon, entry.iconColor) ?? undefined : undefined
    };
  }));

  const chosen = await showMenu(items);

  if (chosen == null)
    return;

  entries[Number(chosen)]?.action?.();
}

/** 顶部菜单栏的一项：点击 / 悬停展开 */
export function MenuButton(props: {
  label: string;
  entries: MenuEntry[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { label, entries, open, onOpenChange } = props;

  return (
    <DropdownMenu.Root open={open} onOpenChange={onOpenChange} modal={false}>
      <DropdownMenu.Trigger
        className={`menu${open ? " is-open" : ""}`}
        /* Windows 习惯：鼠标移上去就展开 */
        onPointerEnter={() => onOpenChange(true)}
      >
        {label}
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          className="menu-dropdown"
          align="start"
          sideOffset={2}
          collisionPadding={8}
        >
          {renderEntries(entries, { Item: DropdownMenu.Item, Separator: DropdownMenu.Separator })}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
