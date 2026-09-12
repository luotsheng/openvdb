# Valkyrie Desktop（Electron + Java 数据层）

Electron 只负责界面，数据库相关能力全部保留在 Java 侧。两者以**本地子进程 + 标准输入输出管道**通信，
不监听任何端口、不依赖任何服务端部署；安装包内自带精简 JRE，客户端无需安装 Java。

```
┌─ Electron ──────────────────────────┐        ┌─ valkyrie-server.jar ─────────────┐
│ 渲染进程  React / Monaco / 结果表格 │        │  JSON-RPC（逐行 JSON）            │
│        ↕ IPC                        │        │  core / drivers / utils           │
│ 主进程    窗口 · 生命周期 · 转发     │ ⇄ stdio │  JDBC · HikariCP · JSqlParser     │
└─────────────────────────────────────┘        └───────────────────────────────────┘
                                                          ↓ JDBC
                                          MySQL / PostgreSQL / SQLite / 达梦 / Redis
```

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `src/main/main.cjs` | Electron 主进程：拉起数据层、转发 IPC、单实例锁、退出清理 |
| `src/main/java-bridge.cjs` | 数据层进程管理与 JSON-RPC 客户端 |
| `src/preload/preload.cjs` | 渲染层唯一通道（contextBridge） |
| `src/renderer/` | React + Monaco 界面 |
| `scripts/build-runtime.ps1` | jlink 生成数据层专用精简运行时 |
| `scripts/rpc-smoke.cjs` | 无界面冒烟测试（本地 SQLite，不访问外部数据库） |
| `scripts/ui-smoke.cjs` | 界面冒烟测试：真实窗口点击连接并执行，输出截图 |

## 开发

```powershell
cd electron
npm start
```

`npm start` 会自动完成：检查/安装依赖 → 数据层缺失或源码更新时用 Maven 重建 → 构建界面 → 启动客户端窗口。
也可以直接双击 `electron\start.cmd`。改动前端界面时只需重启这一条命令；
改动 Java 侧后 `dev.cjs` 会检测到源码比产物新并自动重新打包。

其它启动方式：

| 命令 | 用途 |
| --- | --- |
| `npm start` | 默认：按需重建后启动 |
| `npm run start:force` | 强制重建数据层后启动（改了 Java 但没触发重建时用） |
| `npm run start:ui` | 跳过数据层检查，只重建界面并启动（最快） |
| `npm run smoke` | 无界面自检数据层链路 |
| `npm run dist` | 打安装包 |

自检：

```powershell
npm run smoke        # 数据层链路：连接 → DDL → 查询 → 对象树
```

界面自检需要先存在一个名为「本地测试库」的 SQLite 连接：

```powershell
npx electron scripts/ui-smoke.cjs
```

编辑器自检（补全弹窗出现与行内对齐、片段展开、字段/表名候选、Ctrl+R 执行、
查询报错只出现在工作区顶部、浅色/深色弹窗配色）：

```powershell
npx electron scripts/suggest-probe.cjs
```

补全弹窗出现「文字被裁 / 行内错位」时，用几何诊断脚本定位污染弹窗样式的那组 CSS 规则
（正常时标签相对行盒的偏移为 0~3px）：

```powershell
$env:VALKYRIE_CANDIDATE="containers"; npx electron scripts/dom-probe.cjs
```

## 打包

```powershell
# 生成精简运行时（约 47MB，不含 JavaFX）
pwsh scripts/build-runtime.ps1

# 产出安装包：release/Valkyrie-0.1.0-setup.exe
npm run dist
```

`electron-builder.yml` 会把三部分打进同一个安装包：

```
Valkyrie/
├── Valkyrie.exe
├── resources/app.asar          # 主进程 + 预加载 + 渲染产物
├── resources/runtime/          # 精简 JRE（jlink）
└── resources/server/valkyrie-server.jar
```

启动流程：主进程先 spawn `runtime/bin/javaw.exe -jar server/valkyrie-server.jar`，
收到 `server.ready` 后再创建窗口；关闭窗口时主进程关闭管道，数据层读到 EOF 自行退出，
不会留下僵尸进程。

## 通信协议

标准输入输出按行分隔的 JSON，标准输出只用于协议，数据层日志走标准错误。

```jsonc
// 请求
{"id": 1, "method": "query.execute", "params": {"sessionId": "s1", "sql": "select 1"}}
// 响应
{"id": 1, "result": {"jobId": 1757, "hasResultSet": true, "columns": [], "rows": []}}
// 通知（无 id）
{"method": "event", "params": {"channel": "query.progress", "kind": "cost", "detail": "12"}}
```

| 方法 | 说明 |
| --- | --- |
| `ping` | 连通性检查 |
| `connections.list` / `connections.save` / `connections.delete` | 连接配置增删查（密码沿用现有 AES-GCM 加密落盘） |
| `connection.open` / `connection.close` | 打开/关闭连接，返回会话号与根节点 |
| `schema.children` | 按节点 id 懒加载子节点（库 → 表容器 → 表） |
| `query.execute` / `query.cancel` | 执行 SQL / 取消执行，执行过程通过 `query.progress` 推送 |

## 已知事项

- 渲染层当前会拿到解密后的连接密码（与 JavaFX 版本行为一致），后续建议改为渲染层不持有密码、
  由数据层在 `connection.open` 时按名称取用。
- Monaco 目前全量引入，bundle 约 4MB；后续只需 SQL 语言即可显著瘦身。
- 样式里大量使用 `light-dark()` 跟随 `document.documentElement.style.colorScheme` 切换主题，
  因此 `vite.config.ts` 的 `build.target` 必须保持支持该特性的 Chromium 版本；
  一旦被降级成 `--lightningcss-*` 变量，深色主题在打包版里会完全失效。
- 本机 `npm` 全局 `.npmrc` 配了 `https-proxy`，会让 npm 11 静默退出；安装依赖时可加
  `--userconfig <空文件>` 绕过。`electron-builder` 首次打包需要下载 winCodeSign 等工具，
  网络受限时可用 `ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/`
  或预先放入 `%LOCALAPPDATA%\electron-builder\Cache`。
