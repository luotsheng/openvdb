import { useEffect, useState } from "react";
import type { TableColumn, TableIndex } from "../api";
import { Icon } from "./icons";

interface TableDesignProps {
  table: string;
  columns: TableColumn[];
  indexes: TableIndex[];
  ddl: string;
  loading: boolean;
  /** 执行编辑后的 DDL（外层负责确认、执行与重新读取结构） */
  onApply?: (ddl: string) => void;
}

export function TableDesign({ table, columns, indexes, ddl, loading, onApply }: TableDesignProps) {
  const [edited, setEdited] = useState(ddl);
  const [copied, setCopied] = useState(false);

  /* 重新读取结构 / 切换表时同步一次，避免把上一张表的编辑串带过来 */
  useEffect(() => setEdited(ddl), [ddl, table]);

  const changed = edited.trim() !== ddl.trim();

  async function copyDdl() {
    await navigator.clipboard?.writeText(edited);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
  }

  if (loading)
    return <div className="empty">正在读取表结构…</div>;

  return (
    <div className="design">
      <div className="design-scroll">
        <table className="design-table">
          <thead>
            <tr>
              <th>#</th>
              <th>字段名</th>
              <th>类型</th>
              <th>允许空</th>
              <th>默认值</th>
              <th>注释</th>
              <th>键</th>
            </tr>
          </thead>
          <tbody>
            {columns.map((column, index) => (
              <tr key={column.name} className={column.primary ? "is-primary" : undefined}>
                <td className="is-num">{index + 1}</td>
                <td className="mono">{column.name}</td>
                <td className="mono is-type">{column.type}{column.autoIncrement ? " · 自增" : ""}</td>
                <td>{column.notNull ? "否" : "是"}</td>
                <td className={column.defaultValue ? "mono" : "is-null"}>{column.defaultValue ?? "NULL"}</td>
                <td className="is-muted">{column.comment || "-"}</td>
                <td>{column.primary ? <span className="key-tag"><Icon name="key" size={11} />PK</span> : ""}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="design-index">
        <div className="prop-group-title"><Icon name="list" />索引（{indexes.length}）</div>
        {indexes.length === 0 && <div className="empty">无索引</div>}
        {indexes.map(index => (
          <div className="prop-row" key={index.name}>
            <span className="prop-key mono">{index.name}</span>
            <span className="prop-val mono">{index.columnsText || "-"}</span>
            <span className="prop-val is-muted">{index.type}{index.visible === false ? " · 不可见" : ""}</span>
          </div>
        ))}
      </div>

      <div className="ddl">
        <div className="ddl-title">
          <Icon name="code" />DDL · {table}
          <span className="ddl-actions">
            {changed && <span className="ddl-dirty">已修改</span>}
            <button type="button" className="mini-btn" onClick={() => void copyDdl()}>
              <Icon name="copy" size={12} />{copied ? "已复制" : "复制"}
            </button>
            <button type="button" className="mini-btn" disabled={!changed} onClick={() => setEdited(ddl)}>
              <Icon name="refresh" size={12} />还原
            </button>
            <button
              type="button"
              className="mini-btn is-danger"
              disabled={!changed || !onApply}
              onClick={() => onApply?.(edited)}
            >
              <Icon name="play" size={12} />执行 DDL
            </button>
          </span>
        </div>
        <textarea
          className="ddl-editor mono"
          value={edited || "-- 无 DDL"}
          spellCheck={false}
          aria-label={`${table} 的 DDL`}
          onChange={event => setEdited(event.target.value)}
        />
      </div>
    </div>
  );
}
