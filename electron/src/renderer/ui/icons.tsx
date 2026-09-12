import {
  ArrowDownToLine,
  Check,
  ChevronDown,
  ChevronRight,
  Code,
  Columns3,
  Copy,
  Database,
  Download,
  Eraser,
  Folder,
  FolderOpen,
  Info,
  KeyRound,
  Layers,
  List,
  MonitorSmartphone,
  Moon,
  Play,
  Plus,
  RefreshCw,
  Search,
  Square,
  Sun,
  Terminal,
  TextWrap,
  Trash2,
  X
} from "lucide-react";
import type { ComponentType } from "react";

/**
 * 表格图标：Excel 风格的绿色底 + 白色网格，比线性图标更容易一眼认出"表"。
 */
export function SheetIcon({ size = 14, className }: { size?: number; className?: string; strokeWidth?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 16 16"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <rect x="0.5" y="1.5" width="15" height="13" rx="2.2" fill="#21a366" />
      <path
        d="M5.5 1.5v13M10.5 1.5v13M0.5 6h15M0.5 10h15"
        stroke="#ffffff"
        strokeWidth="1"
        opacity="0.92"
      />
    </svg>
  );
}

/**
 * 图标名到 Lucide 组件的映射，保持调用方只依赖语义名。
 */
const ICONS: Record<string, ComponentType<{ size?: number; className?: string; strokeWidth?: number }>> = {
  database: Database,
  table: SheetIcon,
  folder: Folder,
  folderOpen: FolderOpen,
  terminal: Terminal,
  play: Play,
  stop: Square,
  refresh: RefreshCw,
  plus: Plus,
  search: Search,
  columns: Columns3,
  key: KeyRound,
  download: Download,
  check: Check,
  close: X,
  chevronRight: ChevronRight,
  chevronDown: ChevronDown,
  info: Info,
  code: Code,
  list: List,
  layers: Layers,
  trash: Trash2,
  copy: Copy,
  eraser: Eraser,
  wrap: TextWrap,
  latest: ArrowDownToLine,
  sun: Sun,
  moon: Moon,
  system: MonitorSmartphone
};

interface IconProps {
  name: string;
  size?: number;
  className?: string;
}

export function Icon({ name, size = 14, className }: IconProps) {
  const Component = ICONS[name] ?? Info;

  return <Component size={size} className={className} strokeWidth={1.7} />;
}
