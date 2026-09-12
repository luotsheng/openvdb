import type { SchemaNode } from "../api";
import { Icon } from "./icons";
import { popupNativeMenu, type MenuEntry } from "./Menu";

interface TreeProps {
  root: SchemaNode;
  childrenMap: Record<string, SchemaNode[]>;
  expanded: Set<string>;
  loading: Set<string>;
  activeId: string | null;
  filter: string;
  onToggle: (node: SchemaNode) => void;
  onSelect: (node: SchemaNode) => void;
  onActivate: (node: SchemaNode) => void;
  onOpenData: (node: SchemaNode) => void;
  onDesign: (node: SchemaNode) => void;
  onCopyName: (node: SchemaNode) => void;
  menuFor: (node: SchemaNode) => MenuEntry[];
}

export type { MenuEntry };

function iconFor(node: SchemaNode): string {
  switch (node.kind) {
    case "ROOT":
      return "layers";
    case "CONNECTION":
      return "database";
    case "CATALOG":
      return "database";
    case "SCHEMA":
      return "folder";
    case "QUERY":
      return "terminal";
    default:
      /* 表容器用文件夹，实际数据表用表格图标 */
      return node.hasChildren ? "folderOpen" : "table";
  }
}

function matches(node: SchemaNode, childrenMap: Record<string, SchemaNode[]>, keyword: string): boolean {
  if (!keyword)
    return true;

  const needle = keyword.toLowerCase();

  if (node.label.toLowerCase().includes(needle))
    return true;

  return (childrenMap[node.id] ?? []).some(child => matches(child, childrenMap, keyword));
}

export function Tree(props: TreeProps) {
  const { root, childrenMap, expanded, loading, activeId, filter, onToggle, onSelect, onActivate, menuFor } = props;
  function renderNode(node: SchemaNode) {
    if (!matches(node, childrenMap, filter))
      return null;

    const open = expanded.has(node.id) || (filter.length > 0 && Boolean(childrenMap[node.id]));
    const children = childrenMap[node.id];
    const isTable = node.kind === "TABLE" && !node.hasChildren && Boolean(node.table);
    const count = isTable ? node.table?.rows : undefined;

    return (
      <div className="tree-node" key={node.id}>
        <button
          type="button"
          data-node-id={node.id}
          className={`tree-row${activeId === node.id ? " is-active" : ""}`}
          /* 单击只选中，双击才展开 / 连接 / 打开数据（与 Navicat、DBeaver 一致） */
          onClick={() => onSelect(node)}
          onDoubleClick={() => onActivate(node)}
          onContextMenu={event => {
            event.preventDefault();
            onSelect(node);
            void popupNativeMenu(menuFor(node));
          }}
        >
          <span
            className="tree-caret"
            role="presentation"
            onClick={event => {
              event.stopPropagation();
              onToggle(node);
            }}
          >
            {node.hasChildren && <Icon name={open ? "chevronDown" : "chevronRight"} size={13} />}
          </span>
          <span
            className={`tree-icon kind-${node.kind.toLowerCase()}${node.path ? " is-script" : ""}`}
          >
            <Icon name={iconFor(node)} size={14} />
          </span>
          <span className="tree-label">{node.label}</span>

          {node.badge && <span className="tree-badge">{node.badge}</span>}
          {node.kind === "CONNECTION" && <span className={`tree-dot${node.connected ? " is-on" : ""}`} aria-hidden="true" />}
          {loading.has(node.id) && <span className="tree-spinner" role="status" aria-label="加载中" />}
          {!loading.has(node.id) && count != null && <span className="tree-count">{count.toLocaleString()}</span>}
        </button>

        {open && children && children.length > 0 && (
          <div className="tree-children">{children.map(child => renderNode(child))}</div>
        )}
      </div>
    );
  }

  return (
    <div className="object-tree" role="group" aria-label="数据库对象">
      {renderNode(root)}
    </div>
  );
}
