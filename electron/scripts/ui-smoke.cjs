"use strict";

/**
 * 界面冒烟测试：创建真实窗口，走通「连接 → 对象树 → 表数据 → 查询 → 表设计」。
 *
 *   npx electron scripts/ui-smoke.cjs
 *
 * 需要存在一个名为「本地测试库」的本地 SQLite 连接（见 README）。
 */

const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("node:path");
const fs = require("node:fs");
const os = require("node:os");
const { JavaBridge } = require("../src/main/java-bridge.cjs");
const { registerWindowControls, attachWindowState, disableBrowserShortcuts } = require("../src/main/window-controls.cjs");

const CONNECTION_NAME = process.env.VALKYRIE_SMOKE_CONNECTION || "本地测试库";
const CAPTURE_PATH = process.env.VALKYRIE_CAPTURE || path.join(os.tmpdir(), "valkyrie-ui-smoke.png");

/* 用独立的用户数据目录：冒烟里的拖动 / 主题切换不会污染真实客户端的本地状态 */
app.setPath("userData", path.join(os.tmpdir(), "valkyrie-smoke-profile"));

/* 错误提示走系统原生消息框，自动化时跳过，否则会弹窗阻塞 */
process.env.VALKYRIE_SUPPRESS_DIALOGS = "1";

let bridge = null;
let window = null;

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

/* 轮询等待对象树里出现某个节点后点击（懒加载需要时间） */
async function clickTreeRow(text, options = {}) {
  const { doubleClick = false, contextMenu = false, pick = null } = options;

  /* 原生菜单不在 DOM 里：用主进程钩子按标签选中菜单项 */
  process.env.VALKYRIE_MENU_PICK = pick || "";

  for (let attempt = 0; attempt < 16; attempt++) {
    const clicked = await window.webContents.executeJavaScript(`(() => {
      const row = [...document.querySelectorAll(".tree-row")]
        .find(node => node.textContent.includes(${JSON.stringify(text)}));

      if (!row) return false;

      ${contextMenu
        ? `row.dispatchEvent(new MouseEvent("contextmenu", { bubbles: true, button: 2, buttons: 2, clientX: 220, clientY: 320 }));`
        : doubleClick
          ? `row.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));`
          : `row.click();`}

      return true;
    })()`);

    if (clicked)
      return true;

    await delay(700);
  }

  return false;
}

async function waitForTreeRow(text) {
  for (let attempt = 0; attempt < 16; attempt++) {
    const found = await window.webContents.executeJavaScript(`(() => {
      return [...document.querySelectorAll(".tree-row")]
        .some(node => node.textContent.includes(${JSON.stringify(text)}));
    })()`);

    if (found)
      return true;

    await delay(700);
  }

  return false;
}

/* 拖动分隔条：mousedown 在分隔条上，mousemove/mouseup 在 window 上 */
async function dragSplitter(selector, index, dx, dy) {
  return window.webContents.executeJavaScript(`(() => {
    const element = document.querySelectorAll(${JSON.stringify(selector)})[${index}];
    if (!element) return false;

    const rect = element.getBoundingClientRect();
    const x = Math.round(rect.left + rect.width / 2);
    const y = Math.round(rect.top + rect.height / 2);

    /* react-resizable-panels 监听 pointer 事件 */
    const base = { bubbles: true, pointerId: 1, isPrimary: true, pointerType: "mouse", button: 0 };

    try {
      element.dispatchEvent(new PointerEvent("pointerdown", { ...base, buttons: 1, clientX: x, clientY: y }));
    } catch (error) {
      /* 合成事件的指针捕获可能失败，忽略 */
    }

    document.dispatchEvent(new PointerEvent("pointermove", { ...base, buttons: 1, clientX: x + ${dx}, clientY: y + ${dy} }));
    document.dispatchEvent(new PointerEvent("pointerup", { ...base, buttons: 0, clientX: x + ${dx}, clientY: y + ${dy} }));

    return true;
  })()`);
}

async function measureLayout() {
  return window.webContents.executeJavaScript(`(() => ({
    side: Math.round(document.querySelector(".side").getBoundingClientRect().width),
    info: Math.round(document.querySelector(".info").getBoundingClientRect().width),
    editor: Math.round(document.querySelector(".editor-wrap").getBoundingClientRect().height),
    result: Math.round(document.querySelector(".result").getBoundingClientRect().height),
    main: Math.round(document.querySelector(".work-main").getBoundingClientRect().height),
    chrome: (() => {
      const main = document.querySelector(".work-main");
      if (!main) return null;
      return [...main.children]
        .filter(el => !el.classList.contains("editor-wrap") && !el.classList.contains("result"))
        .reduce((sum, el) => sum + Math.round(el.getBoundingClientRect().height), 0);
    })(),
    innerHeight: window.innerHeight
  }))()`);
}

async function prepareData() {
  const opened = await bridge.call("connection.open", { name: CONNECTION_NAME });
  const sessionId = opened.sessionId;

  await bridge.call("query.execute", {
    sessionId,
    sql: [
      "CREATE TABLE IF NOT EXISTS demo_order (id INTEGER PRIMARY KEY, title TEXT, amount REAL);",
      "DELETE FROM demo_order;",
      "INSERT INTO demo_order (id, title, amount) VALUES (1, '测试订单', 88.5), (2, '第二条', 12.0), (3, '第三条', 268.0);"
    ].join("\n")
  });

  await bridge.call("connection.close", { sessionId });
}

async function main() {
  /* 防止异常情况下挂住 */
  const guard = setTimeout(() => {
    console.error("界面冒烟测试超时");
    app.exit(2);
  }, 120000);

  guard.unref();

  bridge = new JavaBridge();
  bridge.on("log", () => undefined);

  await bridge.start();
  await prepareData();

  ipcMain.handle("valkyrie:invoke", async (_event, method, params) => {
    try {
      return { ok: true, result: await bridge.call(method, params || {}) };
    } catch (error) {
      return { ok: false, error: error && error.message ? error.message : String(error) };
    }
  });

  window = new BrowserWindow({
    width: 1360,
    height: 900,
    show: false,
    backgroundColor: "#eef0f4",
    /* 与正式窗口一致：无原生标题栏 */
    frame: false,
    thickFrame: true,
    webPreferences: {
      preload: path.join(__dirname, "..", "src", "preload", "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  bridge.on("event", params => {
    if (!window.isDestroyed())
      window.webContents.send("valkyrie:event", params);
  });

  registerWindowControls();
  attachWindowState(window);
  disableBrowserShortcuts(window);

  await window.loadFile(path.join(__dirname, "..", "dist", "renderer", "index.html"));
  await delay(1200);

  const result = {};

  /* 0. 无边框窗口校验：内容区应与窗口区完全一致（有原生标题栏时会长上去） */
  const bounds = window.getBounds();
  const contentBounds = window.getContentBounds();

  result.frame = {
    bounds: [bounds.x, bounds.y, bounds.width, bounds.height],
    contentBounds: [contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height],
    frameless: bounds.height === contentBounds.height && bounds.width === contentBounds.width
  };

  /* 1. 双击连接节点建立连接 */
  result.connect = await clickTreeRow(CONNECTION_NAME, { doubleClick: true });

  await delay(3000);

  /* 2. 对象树：连接后自动展开数据库，双击展开「数据表」 */
  result.treeCatalog = await waitForTreeRow("Master");
  result.treeCatalogExpand = await clickTreeRow("Master", { doubleClick: true });

  await delay(800);

  /* 2.1 双击展开「数据表」容器，再右键打开表列表总览 */
  result.treeContainer = await clickTreeRow("数据表", { doubleClick: true });

  await delay(1200);

  result.tableListOpen = await clickTreeRow("数据表", { contextMenu: true, pick: "表列表" });

  await delay(1200);

  const tableListState = await window.webContents.executeJavaScript(`(() => {
    const headers = [...document.querySelectorAll("table.table-list thead th")]
      .map(node => node.textContent.replace(/[▲▼]/g, "").trim());
    const rows = [...document.querySelectorAll("table.table-list tbody tr")];
    const active = document.querySelector(".work-tab.is-active");

    return {
      tab: active ? active.textContent : null,
      headers,
      rows: rows.length,
      firstName: rows[0] ? rows[0].querySelector("td").textContent.trim() : null,
      search: Boolean(document.querySelector(".toolbar-search input"))
    };
  })()`);

  /* 搜索过滤 + 表头排序 */
  const tableListFiltered = await window.webContents.executeJavaScript(`(() => {
    const input = document.querySelector(".toolbar-search input");
    const header = [...document.querySelectorAll("table.table-list thead th")].find(node => node.textContent.includes("数据条数"));
    if (!input || !header) return null;

    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
    setter.call(input, "demo");
    input.dispatchEvent(new Event("input", { bubbles: true }));
    header.click();

    return true;
  })()`);

  await delay(300);

  const tableListFilterState = await window.webContents.executeJavaScript(`(() => ({
    rows: document.querySelectorAll("table.table-list tbody tr").length,
    sortedHeader: (() => {
      const header = document.querySelector("table.table-list thead th.is-sorted");
      return header ? header.textContent.replace(/[▲▼]/g, "").trim() : null;
    })(),
    count: document.querySelector(".pane-toolbar .toolbar-text") ? document.querySelector(".pane-toolbar .toolbar-text").textContent : null
  }))()`);

  fs.writeFileSync(path.join(os.tmpdir(), "valkyrie-tables.png"), (await window.webContents.capturePage()).toPNG());

  /* 双击列表行 → 打开数据页；再用展开箭头展开对象树，后续步骤沿用 */
  const tableListOpenRow = await window.webContents.executeJavaScript(`(() => {
    const row = document.querySelector("table.table-list tbody tr");
    if (!row) return false;
    row.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));
    return true;
  })()`);

  await delay(2000);

  const tableListOpened = await window.webContents.executeJavaScript(
    `JSON.stringify([...document.querySelectorAll(".work-tab-title")].map(node => node.textContent))`);

  const treeSnapshot = await window.webContents.executeJavaScript(`(() => ({
    rows: [...document.querySelectorAll(".tree-row")].map(node => node.textContent.trim())
  }))()`);

  /* 3. 双击表 → 数据页 */
  result.openData = await clickTreeRow("demo_order", { doubleClick: true });

  await delay(2500);

  const dataState = await window.webContents.executeJavaScript(`(() => ({
    tabs: [...document.querySelectorAll(".work-tab-title")].map(node => node.textContent),
    headers: [...document.querySelectorAll("table.grid thead th")].map(node => node.textContent.trim()),
    rowCount: document.querySelectorAll("table.grid tbody tr").length,
    firstRow: document.querySelector("table.grid tbody tr") ? document.querySelector("table.grid tbody tr").textContent.trim() : null,
    propTitle: document.querySelector(".prop-group-title") ? document.querySelector(".prop-group-title").textContent : null
  }))()`);

  /* 3.1 表格编辑：双击单元格 → 输入 → 回车 → 提交修改 */
  result.editCell = await window.webContents.executeJavaScript(`(() => {
    const cell = document.querySelector("table.grid tbody tr td:nth-child(3)");
    if (!cell) return { ok: false, reason: "no cell" };
    cell.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));
    return { ok: true };
  })()`);

  await delay(400);

  /* 编辑框应铺满单元格（否则看起来像两个框） */
  const editorBox = await window.webContents.executeJavaScript(`(() => {
    const input = document.querySelector(".cell-editor");
    const cell = input ? input.closest("td") : null;
    if (!input || !cell) return null;
    const i = input.getBoundingClientRect();
    const c = cell.getBoundingClientRect();
    return {
      cell: [Math.round(c.width), Math.round(c.height)],
      input: [Math.round(i.width), Math.round(i.height)],
      offset: [Math.round(i.left - c.left), Math.round(i.top - c.top)],
      covers: Math.abs(i.width - c.width) <= 3 && Math.abs(i.height - c.height) <= 3
    };
  })()`);

  await window.webContents.executeJavaScript(`(() => {
    const input = document.querySelector(".cell-editor");
    if (!input) return false;

    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
    setter.call(input, "编辑测试");
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true }));

    return true;
  })()`);

  await delay(1200);

  const dirtyState = await window.webContents.executeJavaScript(`(() => ({
    dirtyRows: document.querySelectorAll("table.grid tbody tr.is-dirty").length,
    tools: document.querySelector(".grid-tools") ? document.querySelector(".grid-tools").textContent : null,
    value: document.querySelector("table.grid tbody tr td:nth-child(3)") ? document.querySelector("table.grid tbody tr td:nth-child(3)").textContent : null
  }))()`);

  result.commit = await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".grid-tools .tbtn")].find(node => node.textContent.includes("提交修改"));
    if (!button) return { ok: false };
    button.click();
    return { ok: true, disabled: button.disabled, busyClass: button.className.includes("is-busy") };
  })()`);

  await delay(250);

  /* 动作反馈：提交后应有浮层提示 */
  const commitToast = await window.webContents.executeJavaScript(
    `(() => { const toast = document.querySelector("[data-sonner-toast]"); return toast ? toast.textContent : null; })()`);

  await delay(1500);

  const committedState = await window.webContents.executeJavaScript(`(() => ({
    dirtyRows: document.querySelectorAll("table.grid tbody tr.is-dirty").length,
    value: document.querySelector("table.grid tbody tr td:nth-child(3)") ? document.querySelector("table.grid tbody tr td:nth-child(3)").textContent : null
  }))()`);

  /* 斑马纹 / 选中底色（Navicat 风格浅蓝） */
  const gridStyle = await window.webContents.executeJavaScript(`(() => {
    const rows = [...document.querySelectorAll("table.grid tbody tr")];
    const background = row => {
      const cell = row ? row.querySelector("td:nth-child(3)") : null;
      return cell ? getComputedStyle(cell).backgroundColor : null;
    };
    const selected = document.querySelector("table.grid td.is-range");

    return {
      rowCount: rows.length,
      plain: background(rows[0]),
      zebra: background(rows[1]),
      selected: selected ? getComputedStyle(selected).backgroundColor : null
    };
  })()`);

  fs.writeFileSync(path.join(os.tmpdir(), "valkyrie-grid.png"), (await window.webContents.capturePage()).toPNG());

  /* 4. 查询控制台执行 */
  result.emptyState = await window.webContents.executeJavaScript(`(() => ({
    tabs: document.querySelectorAll(".work-tab").length,
    welcomeVisible: Boolean(document.querySelector(".welcome"))
  }))()`);

  result.run = await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".toolbar .tbtn")]
      .find(node => node.textContent.includes("新建查询"));
    if (!button) return { ok: false };
    button.click();
    return { ok: true };
  })()`);

  await delay(800);

  /* 4.0 标签页右键菜单：关闭 / 关闭左侧 / 关闭右侧 / 全部关闭（系统原生菜单，不选中任何项） */
  process.env.VALKYRIE_MENU_PICK = "";

  await window.webContents.executeJavaScript(`(() => {
    const tabs = [...document.querySelectorAll(".work-tab")];
    const tab = tabs[tabs.length - 1];
    if (!tab) return false;
    const rect = tab.getBoundingClientRect();
    tab.dispatchEvent(new MouseEvent("contextmenu", { bubbles: true, button: 2, buttons: 2, clientX: rect.left + 20, clientY: rect.top + 10 }));
    return true;
  })()`);

  await delay(250);

  /* 原生菜单不在 DOM 里：这里只确认没有残留的自绘菜单 */
  const tabMenuItems = await window.webContents.executeJavaScript(
    `document.querySelectorAll(".ctx-menu").length`);

  /* 4.1 顶部上下文选择器：连接 / 数据库 / 模式 / 表 */
  const pathState = await window.webContents.executeJavaScript(`(() => {
    const items = [...document.querySelectorAll(".path-item")].map(item => ({
      label: item.querySelector("label") ? item.querySelector("label").textContent : null,
      value: item.querySelector(".vk-select-value") ? item.querySelector(".vk-select-value").textContent : null,
      disabled: item.querySelector(".vk-select-btn") ? item.querySelector(".vk-select-btn").disabled : null
    }));
    return items;
  })()`);

  /* 4.1.1 展开「连接」下拉：浮层几何 + 选项 */
  await window.webContents.executeJavaScript(`(() => {
    const button = document.querySelectorAll(".path-item .vk-select-btn")[0];
    if (!button) return false;
    button.click();
    return true;
  })()`);
  await delay(300);

  const pathMenuState = await window.webContents.executeJavaScript(`(() => {
    const menu = document.querySelector(".vk-select-menu");
    if (!menu) return null;
    const cs = getComputedStyle(menu);
    const trigger = document.querySelectorAll(".path-item .vk-select-btn")[0].getBoundingClientRect();
    const box = menu.getBoundingClientRect();

    return {
      options: [...menu.querySelectorAll(".vk-select-option")].map(node => node.textContent),
      radius: cs.borderTopLeftRadius,
      gapBelowTrigger: Math.round(box.top - trigger.bottom),
      inViewport: box.left >= 0 && box.top >= 0 && box.right <= window.innerWidth && box.bottom <= window.innerHeight
    };
  })()`);

  /* 「表」下拉里应是当前库的全部表 */
  const tableMenuState = await window.webContents.executeJavaScript(`(() => {
    /* 先确保没有残留的浮层，再点最后一个下拉（表） */
    window.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
    const buttons = document.querySelectorAll(".path-item .vk-select-btn");
    const button = buttons[buttons.length - 1];
    if (!button) return null;
    button.click();
    return true;
  })()`);

  await delay(300);

  const tableMenuOptions = await window.webContents.executeJavaScript(`(() => {
    const menus = [...document.querySelectorAll(".vk-select-menu")];
    const options = menus.map(menu => [...menu.querySelectorAll(".vk-select-option")].map(node => node.textContent));
    document.querySelector(".pane-toolbar")?.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
    return options[options.length - 1] ?? null;
  })()`);

  fs.writeFileSync(path.join(os.tmpdir(), "valkyrie-select.png"), (await window.webContents.capturePage()).toPNG());

  /* 收起浮层，避免影响后续步骤 */
  await window.webContents.executeJavaScript(`(() => {
    document.querySelector(".pane-toolbar")?.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
    return true;
  })()`);
  await delay(200);

  await window.webContents.executeJavaScript(`(() => {
    const button = document.querySelector(".pane-toolbar .tbtn.is-primary");
    if (button) button.click();
    return Boolean(button);
  })()`);

  await delay(2500);

  /* 4.2 面板拖动：对象树宽度 / 对象信息宽度 / 编辑器高度 */
  const layoutBefore = await measureLayout();

  await dragSplitter(".splitter-v", 0, 80, 0);
  await delay(300);
  const layoutAfterSide = await measureLayout();

  await dragSplitter(".splitter-v", 1, -60, 0);
  await delay(300);
  const layoutAfterInfo = await measureLayout();

  await dragSplitter(".splitter-h", 0, 0, 60);
  await delay(300);
  const layoutAfterEditor = await measureLayout();

  /* 往下猛拖：结果区必须留出最小高度 */
  await dragSplitter(".splitter-h", 0, 0, 600);
  await delay(300);
  const layoutEditorClamp = await measureLayout();

  result.layout = {
    before: layoutBefore,
    afterSide: layoutAfterSide,
    afterInfo: layoutAfterInfo,
    afterEditor: layoutAfterEditor,
    afterEditorClamp: layoutEditorClamp
  };

  /* 5. 表设计器 */
  result.design = { ok: await clickTreeRow("demo_order", { contextMenu: true, pick: "设计表" }) };

  await delay(2600);

  /* 5.1 关闭标签页 */
  const tabsBefore = await window.webContents.executeJavaScript(`document.querySelectorAll(".work-tab").length`);

  result.closeTab = await window.webContents.executeJavaScript(`(() => {
    const tab = [...document.querySelectorAll(".work-tab")].find(node => node.textContent.includes("设计: demo_order"));
    if (!tab) return { ok: false, tabs: [...document.querySelectorAll(".work-tab-title")].map(n => n.textContent) };
    const close = tab.querySelector(".work-tab-close");
    if (!close) return { ok: false, reason: "no close button" };
    close.click();
    return { ok: true };
  })()`);

  await delay(600);

  const tabsAfter = await window.webContents.executeJavaScript(`document.querySelectorAll(".work-tab").length`);

  /* 5.2 关闭最后一个标签后应该回到空工作区，而不是自动补一个 */
  result.closeAll = await window.webContents.executeJavaScript(`(() => {
    document.querySelectorAll(".work-tab-close").forEach(button => button.click());
    return true;
  })()`);

  await delay(600);

  const emptyAfterClose = await window.webContents.executeJavaScript(`(() => ({
    tabs: document.querySelectorAll(".work-tab").length,
    welcomeVisible: Boolean(document.querySelector(".welcome"))
  }))()`);

  /* 5.3 查询脚本：右键「查询脚本」→ 新建 → 出现在树里 → 双击打开 → 删除 */
  result.newScriptMenu = { ok: await clickTreeRow("查询脚本", { contextMenu: true, pick: "新建查询脚本" }) };

  await delay(500);

  await window.webContents.executeJavaScript(`(() => {
    const input = document.querySelector(".dialog-input");
    if (!input) return false;

    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
    setter.call(input, "冒烟脚本.sql");
    input.dispatchEvent(new Event("input", { bubbles: true }));

    const ok = [...document.querySelectorAll(".modal .mini-btn")].find(node => node.textContent === "确定");
    if (ok) ok.click();

    return true;
  })()`);

  await delay(1800);

  const scriptTree = await window.webContents.executeJavaScript(`(() => ({
    rows: [...document.querySelectorAll(".tree-row")].map(node => node.textContent.trim()),
    dialog: document.querySelector(".dialog-input") ? document.querySelector(".dialog-input").value : null,
    modal: document.querySelector(".modal-backdrop .modal") ? document.querySelector(".modal-backdrop .modal").textContent.slice(0, 160) : null
  }))()`);

  await clickTreeRow("冒烟脚本", { doubleClick: true });
  await delay(1200);

  const scriptTab = await window.webContents.executeJavaScript(`(() => ({
    tabs: [...document.querySelectorAll(".work-tab-title")].map(node => node.textContent)
  }))()`);

  /* 删除脚本 */
  result.deleteScriptMenu = { ok: await clickTreeRow("冒烟脚本", { contextMenu: true, pick: "删除脚本" }) };

  await delay(400);

  await window.webContents.executeJavaScript(`(() => {
    const ok = [...document.querySelectorAll(".modal .mini-btn")].find(node => node.textContent === "确定");
    if (ok) ok.click();
    return Boolean(ok);
  })()`);

  await delay(1500);

  const scriptAfterDelete = await window.webContents.executeJavaScript(`(() => ({
    rows: [...document.querySelectorAll(".tree-row")].map(node => node.textContent.trim())
  }))()`);

  /* 7. 菜单栏 + 连接对话框 + 错误弹窗 */
  result.fileMenu = await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".menubar .menu")].find(node => node.textContent.includes("文件"));
    if (!button) return { ok: false };
    /* Radix DropdownMenu 在 pointerdown 上展开（真实鼠标也是这样） */
    button.dispatchEvent(new PointerEvent("pointerdown", { bubbles: true, button: 0, buttons: 1, isPrimary: true, pointerType: "mouse" }));
    button.dispatchEvent(new PointerEvent("pointerup", { bubbles: true, button: 0, buttons: 0, isPrimary: true, pointerType: "mouse" }));
    return { ok: true };
  })()`);

  await delay(400);

  const menuState = await window.webContents.executeJavaScript(`(() => ({
    items: [...document.querySelectorAll(".menu-dropdown [role='menuitem']")].map(node => node.textContent)
  }))()`);

  result.connectionDialog = await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".menu-dropdown [role='menuitem']")].find(node => node.textContent.includes("新建连接"));
    if (!button) return { ok: false };
    /* 菜单项在 click 上触发选择 */
    button.click();
    return { ok: true };
  })()`);

  await delay(500);

  const dialogState = await window.webContents.executeJavaScript(`(() => ({
    title: document.querySelector(".conn-dialog .modal-title") ? document.querySelector(".conn-dialog .modal-title").textContent : null,
    fields: document.querySelectorAll(".conn-dialog input").length + document.querySelectorAll(".conn-dialog .vk-select").length,
    selects: [...document.querySelectorAll(".conn-dialog .vk-select")].map(item => item.querySelector(".vk-select-value").textContent),
    buttons: [...document.querySelectorAll(".conn-dialog .mini-btn")].map(node => node.textContent)
  }))()`);

  /* 对话框可拖动（按标题栏拖 60×40），位移在 React 重渲染后测量 */
  const dialogBefore = await window.webContents.executeJavaScript(`(() => {
    const modal = document.querySelector(".conn-dialog");
    const title = modal ? modal.querySelector(".modal-title") : null;
    if (!modal || !title) return null;

    const rect = title.getBoundingClientRect();
    const box = modal.getBoundingClientRect();

    title.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, clientX: rect.left + 40, clientY: rect.top + 8 }));
    window.dispatchEvent(new MouseEvent("mousemove", { bubbles: true, clientX: rect.left + 100, clientY: rect.top + 48 }));
    window.dispatchEvent(new MouseEvent("mouseup", { bubbles: true }));

    return { left: Math.round(box.left), top: Math.round(box.top), cursor: getComputedStyle(title).cursor };
  })()`);

  await delay(250);

  const dialogAfter = await window.webContents.executeJavaScript(`(() => {
    const modal = document.querySelector(".conn-dialog");
    if (!modal) return null;
    const box = modal.getBoundingClientRect();
    return { left: Math.round(box.left), top: Math.round(box.top) };
  })()`);

  const dialogDrag = dialogBefore && dialogAfter
    ? { moved: [dialogAfter.left - dialogBefore.left, dialogAfter.top - dialogBefore.top], cursor: dialogBefore.cursor }
    : null;

  result.closeDialog = await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".conn-dialog .mini-btn")].find(node => node.textContent === "取消");
    if (!button) return { ok: false };
    button.click();
    return { ok: true };
  })()`);

  await delay(300);

  /*
   * 错误提示：双击一个连不上的连接（127.0.0.1:27491 未监听）。
   * 现在走系统原生消息框，DOM 里不会有弹层；脚本通过
   * VALKYRIE_SUPPRESS_DIALOGS 让主进程跳过弹框，避免阻塞自动化。
   */
  result.errorTrigger = await clickTreeRow("ROOT_天汉电竞_读写", { doubleClick: true });

  await delay(6000);

  const errorState = await window.webContents.executeJavaScript(`(() => {
    return {
      statusbar: document.querySelector(".statusbar") ? document.querySelector(".statusbar").textContent : null,
      innerModal: Boolean(document.querySelector(".modal-backdrop .modal"))
    };
  })()`);

  const designState = await window.webContents.executeJavaScript(`(() => ({
    tabs: [...document.querySelectorAll(".work-tab-title")].map(node => node.textContent),
    designRows: document.querySelectorAll("table.design-table tbody tr").length,
    designHeaders: [...document.querySelectorAll("table.design-table thead th")].map(node => node.textContent.trim()),
    ddl: document.querySelector(".ddl pre") ? document.querySelector(".ddl pre").textContent.slice(0, 160) : null,
    indexRows: document.querySelectorAll(".design-index .prop-row").length
  }))()`);

  const finalState = await window.webContents.executeJavaScript(`(() => ({
    statusbar: document.querySelector(".statusbar") ? document.querySelector(".statusbar").textContent : null,
    error: document.querySelector(".error-bar") ? document.querySelector(".error-bar").textContent : null,
    theme: getComputedStyle(document.documentElement).colorScheme,
    appBackground: getComputedStyle(document.querySelector(".app")).backgroundColor,
    panelBackground: getComputedStyle(document.querySelector(".work-main")).backgroundColor,
    fontSize: getComputedStyle(document.body).fontSize,
    userSelect: getComputedStyle(document.body).userSelect,
    consoleSelect: document.querySelector(".console") ? getComputedStyle(document.querySelector(".console")).userSelect : null,
    sideWidth: Math.round(document.querySelector(".side").getBoundingClientRect().width),
    toolbarHeight: Math.round(document.querySelector(".toolbar").getBoundingClientRect().height),
    treeRowHeight: document.querySelector(".tree-row") ? Math.round(document.querySelector(".tree-row").getBoundingClientRect().height) : null,
    iconCount: document.querySelectorAll("svg").length,
    tree: {
      rows: [...document.querySelectorAll(".tree-row")].map(node => node.className.replace(/tree-row\\s*/, "") + "|" + node.textContent.trim()),
      childContainers: document.querySelectorAll(".tree-children").length,
      guideLineWidth: (() => {
        const el = document.querySelector(".tree-children");
        return el ? getComputedStyle(el).borderLeftWidth : null;
      })(),
      countPills: document.querySelectorAll(".tree-count").length,
      iconColor: (() => {
        const el = document.querySelector(".tree-icon");
        return el ? getComputedStyle(el).color : null;
      })(),
      activeRowBackground: (() => {
        const el = document.querySelector(".tree-row.is-active");
        return el ? getComputedStyle(el).backgroundColor : null;
      })()
    },
    logs: [...document.querySelectorAll(".console div")].slice(-3).map(node => node.textContent)
  }))()`);

  const image = await window.webContents.capturePage();

  fs.writeFileSync(CAPTURE_PATH, image.toPNG());

  /* 6. 自绘标题栏的窗口按钮（截图后再动窗口，避免影响截图） */
  /* 先验证浏览器快捷键已被拦截：Ctrl+R 若能刷新页面，页面里的标记会丢失 */
  await window.webContents.executeJavaScript(`window.__vkMarker = "alive"`);
  window.webContents.sendInputEvent({ type: "keyDown", keyCode: "R", modifiers: ["control"] });
  window.webContents.sendInputEvent({ type: "keyUp", keyCode: "R", modifiers: ["control"] });
  await delay(900);

  result.shortcuts = await window.webContents.executeJavaScript(`(() => ({
    marker: window.__vkMarker || null,
    title: document.title
  }))()`);

  window.show();
  await delay(500);

  result.maximize = await window.webContents.executeJavaScript(`(() => {
    const button = document.querySelector('.win-btn[aria-label="最大化"], .win-btn[aria-label="还原"]');
    if (!button) return { ok: false };
    button.click();
    return { ok: true, label: button.getAttribute("aria-label") };
  })()`);

  await delay(900);
  result.maximizedState = window.isMaximized();

  await window.webContents.executeJavaScript(`(() => {
    const button = document.querySelector('.win-btn[aria-label="还原"], .win-btn[aria-label="最大化"]');
    if (button) button.click();
    return Boolean(button);
  })()`);

  await delay(700);
  result.restoredState = window.isMaximized();

  console.log(JSON.stringify({
    result,
    treeSnapshot,
    dataState,
    editState: { dirty: dirtyState, committed: committedState },
    pathState,
    pathMenuState,
    tableMenuState,
    tableMenuOptions,
    tableListState,
    tableListFiltered,
    tableListFilterState,
    tableListOpenRow,
    tableListOpened,
    editorBox,
    gridStyle,
    commitToast,
    tabMenuItems,
    dialogDrag,
    designState,
    tabs: { before: tabsBefore, after: tabsAfter, emptyAfterClose },
    scripts: { tree: scriptTree, openTabs: scriptTab, afterDelete: scriptAfterDelete },
    menuState,
    dialogState,
    errorState,
    finalState
  }, null, 2));
  console.log("截图:", CAPTURE_PATH);

  await bridge.stop();
  app.quit();
}

app.whenReady().then(main).catch(async error => {
  console.error(error);

  if (bridge)
    await bridge.stop().catch(() => undefined);

  app.exit(1);
});
