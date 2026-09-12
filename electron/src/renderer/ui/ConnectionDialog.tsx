import { useState } from "react";
import { invoke, messageOf, type SavedConnection } from "../api";
import { Icon } from "./icons";
import { Dialog } from "./Dialog";
import { Select } from "./Select";

interface ConnectionDialogProps {
  mode: "new" | "edit" | "copy";
  source?: SavedConnection | null;
  onClose: () => void;
  onSaved: (name: string) => void;
}

interface FormState {
  name: string;
  type: string;
  host: string;
  port: string;
  db: string;
  username: string;
  password: string;
  savePassword: boolean;
  sqlitePath: string;
  timezone: string;
  useSSL: boolean;
  tinyint1isBit: boolean;
}

const DB_TYPES = [
  { value: "mysql", label: "MySQL", port: "3306" },
  { value: "postgresql", label: "PostgreSQL", port: "5432" },
  { value: "sqlite", label: "SQLite", port: "" },
  { value: "dm", label: "达梦数据库", port: "5236" },
  { value: "redis", label: "Redis", port: "6379" }
];

function initialState(mode: ConnectionDialogProps["mode"], source?: SavedConnection | null): FormState {
  const base: FormState = {
    name: "",
    type: "mysql",
    host: "127.0.0.1",
    port: "3306",
    db: "",
    username: "root",
    password: "",
    savePassword: true,
    sqlitePath: "",
    timezone: "Asia/Shanghai",
    useSSL: false,
    tinyint1isBit: false
  };

  if (!source)
    return base;

  return {
    name: mode === "copy" ? `${source.name} - 副本` : source.name,
    type: source.type || "mysql",
    host: source.host || "",
    port: source.port || "",
    db: source.db || "",
    username: source.username || "",
    password: source.password || "",
    savePassword: source.savePassword ?? true,
    sqlitePath: source.sqlitePath || "",
    timezone: source.timezone || "Asia/Shanghai",
    useSSL: source.useSSL ?? false,
    tinyint1isBit: source.tinyint1isBit ?? false
  };
}

export function ConnectionDialog({ mode, source, onClose, onSaved }: ConnectionDialogProps) {
  const [form, setForm] = useState<FormState>(() => initialState(mode, source));
  const [status, setStatus] = useState<{ text: string; ok: boolean } | null>(null);
  const [testing, setTesting] = useState(false);
  const [saving, setSaving] = useState(false);

  const isSqlite = form.type === "sqlite";

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm(previous => ({ ...previous, [key]: value }));
  }

  function jdbcUrl(): string {
    if (isSqlite)
      return `jdbc:sqlite:${form.sqlitePath}`;

    const database = form.db ? `/${form.db}` : "";
    const query = ["mysql", "postgresql", "dm"].includes(form.type)
      ? `?timezone=${form.timezone}&useSSL=${form.useSSL}&tinyint1isBit=${form.tinyint1isBit}`
      : "";

    return `jdbc:${form.type}://${form.host}:${form.port}${database}${query}`;
  }

  function payload(): SavedConnection {
    return {
      name: form.name.trim(),
      type: form.type,
      host: isSqlite ? undefined : form.host,
      port: isSqlite ? undefined : form.port,
      db: form.db || undefined,
      username: isSqlite ? undefined : form.username,
      password: form.password || undefined,
      savePassword: form.savePassword,
      sqlitePath: isSqlite ? form.sqlitePath : undefined,
      jdbcUrl: jdbcUrl(),
      timezone: form.timezone,
      useSSL: form.useSSL,
      tinyint1isBit: form.tinyint1isBit
    };
  }

  async function testConnection() {
    setTesting(true);
    setStatus({ text: "正在连接…", ok: true });

    try {
      const opened = await invoke<{ sessionId: string; product: { productName?: string; version?: string } }>(
        "connection.open",
        { connection: payload() }
      );

      await invoke("connection.close", { sessionId: opened.sessionId }).catch(() => undefined);

      setStatus({
        text: `连接成功 · ${opened.product?.productName ?? ""} ${opened.product?.version ?? ""}`.trim(),
        ok: true
      });
    } catch (error) {
      setStatus({ text: messageOf(error), ok: false });
    } finally {
      setTesting(false);
    }
  }

  async function save() {
    if (!form.name.trim()) {
      setStatus({ text: "连接名不能为空", ok: false });
      return;
    }

    setSaving(true);

    try {
      await invoke("connections.save", {
        connection: payload(),
        oldName: mode === "edit" ? source?.name : undefined
      });

      onSaved(form.name.trim());
    } catch (error) {
      setStatus({ text: messageOf(error), ok: false });
    } finally {
      setSaving(false);
    }
  }

  const title = mode === "new" ? "新建连接" : mode === "edit" ? "编辑连接" : "复制连接";

  return (
    <Dialog
      title={<><Icon name="database" size={14} />{title}</>}
      className="modal-form"
      onClose={onClose}
    >
      <div className="modal-body form-grid">
        <label htmlFor="conn-name">连接名</label>
        <input id="conn-name" value={form.name} onChange={event => update("name", event.target.value)} />

        <label htmlFor="conn-type">类型</label>
        <Select
          id="conn-type"
          value={form.type}
          options={DB_TYPES.map(item => ({ value: item.value, label: item.label }))}
          onChange={type => {
            const preset = DB_TYPES.find(item => item.value === type);
            update("type", type);
            update("port", preset?.port ?? "");
          }}
        />

        {isSqlite ? (
          <>
            <label htmlFor="conn-path">数据库文件</label>
            <input id="conn-path" value={form.sqlitePath} onChange={event => update("sqlitePath", event.target.value)} placeholder="D:/data/demo.db" />
          </>
        ) : (
          <>
            <label htmlFor="conn-host">主机 / IP</label>
            <div className="form-inline">
              <input id="conn-host" value={form.host} onChange={event => update("host", event.target.value)} />
              <span className="form-label-inline">端口</span>
              <input className="is-narrow" value={form.port} onChange={event => update("port", event.target.value)} aria-label="端口" />
            </div>

            <label htmlFor="conn-db">默认数据库</label>
            <input id="conn-db" value={form.db} onChange={event => update("db", event.target.value)} />

            <label htmlFor="conn-user">用户名</label>
            <input id="conn-user" value={form.username} onChange={event => update("username", event.target.value)} />

            <label htmlFor="conn-password">密码</label>
            <input id="conn-password" type="password" value={form.password} onChange={event => update("password", event.target.value)} />
          </>
        )}

        <label htmlFor="conn-timezone">时区</label>
        <Select
          id="conn-timezone"
          value={form.timezone}
          options={["Asia/Shanghai", "UTC", "Asia/Hong_Kong", "Asia/Singapore", "Asia/Tokyo"]
            .map(zone => ({ value: zone, label: zone }))}
          onChange={timezone => update("timezone", timezone)}
        />

        <label htmlFor="conn-options">选项</label>
        <div className="form-inline">
          <label className="form-check"><input type="checkbox" checked={form.savePassword} onChange={event => update("savePassword", event.target.checked)} />保存密码</label>
          {!isSqlite && <label className="form-check"><input type="checkbox" checked={form.useSSL} onChange={event => update("useSSL", event.target.checked)} />使用 SSL</label>}
          {!isSqlite && <label className="form-check"><input type="checkbox" checked={form.tinyint1isBit} onChange={event => update("tinyint1isBit", event.target.checked)} />TINYINT 转布尔</label>}
        </div>

        <label htmlFor="conn-url">JDBC URL</label>
        <input id="conn-url" className="mono" value={jdbcUrl()} readOnly aria-label="JDBC URL" />
      </div>

      <div className="modal-status">
        {status ? <span className={status.ok ? "is-ok" : "is-error"}>{status.text}</span> : <span className="is-muted">填写连接信息后可先测试再保存</span>}
      </div>

      <div className="modal-actions">
        <button type="button" className="mini-btn" disabled={testing} onClick={() => void testConnection()}>
          {testing ? "测试中…" : "测试连接"}
        </button>
        <span className="modal-actions-push" />
        <button type="button" className="mini-btn" onClick={onClose}>取消</button>
        <button type="button" className="mini-btn is-default" disabled={saving} onClick={() => void save()}>
          {saving ? "保存中…" : "保存"}
        </button>
      </div>
    </Dialog>
  );
}
