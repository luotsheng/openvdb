"use strict";

/**
 * SQL 智能提示（Monaco 内置 suggest 弹窗）冒烟：
 * 打开连接 → 新建查询 → 输入前缀 → 检查弹窗与行内对齐 → 键盘插入 → 校验片段展开。
 *
 *   npx electron scripts/suggest-probe.cjs
 *
 * 需要存在一个名为「本地测试库」的本地 SQLite 连接（见 README）。
 */

const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("node:path");
const os = require("node:os");
const fs = require("node:fs");
const { JavaBridge } = require("../src/main/java-bridge.cjs");

const CONNECTION_NAME = process.env.VALKYRIE_SMOKE_CONNECTION || "本地测试库";
const CAPTURE_DIR = process.env.VALKYRIE_CAPTURE_DIR || os.tmpdir();

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

async function main() {
  const bridge = new JavaBridge();
  bridge.on("log", () => undefined);
  await bridge.start();

  ipcMain.handle("valkyrie:invoke", async (_event, method, params) => {
    try {
      return { ok: true, result: await bridge.call(method, params || {}) };
    } catch (error) {
      return { ok: false, error: error.message };
    }
  });

  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    show: false,
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

  await window.loadFile(path.join(__dirname, "..", "dist", "renderer", "index.html"));
  await delay(1200);

  const connected = await window.webContents.executeJavaScript(`(() => {
    const row = [...document.querySelectorAll(".tree-row")].find(n => n.textContent.includes(${JSON.stringify(CONNECTION_NAME)}));
    if (row) row.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));
    return Boolean(row);
  })()`);

  await delay(2500);

  const tabbed = await window.webContents.executeJavaScript(`(() => {
    const button = document.querySelector(".work-tab-add");
    if (button) button.click();
    return Boolean(button);
  })()`);

  await delay(900);

  await focusEditor();

  /* 1. 关键字前缀：弹窗出现、行内对齐、内容过滤 */
  await typeKeys("sel");
  const keywords = await waitForWidget();

  /* 2. 回车插入当前聚焦项 */
  await pressKey("Return");
  await delay(400);

  const afterEnter = await editorText();

  /* 3. 片段：Monaco 展开 ${1:...} 并选中占位符 */
  await replaceEditorText("sela", true);
  await delay(600);
  await pressKey("Return");
  await delay(400);

  const snippetText = await editorText();
  await typeText("X");
  const snippetTyped = await editorText();

  /* 4. 限定名：o. 只提示该表字段；同时验证 Esc 关闭弹窗 */
  await replaceEditorText("select * from demo_order o where o.");
  await pressKey("Space", ["control"]);
  await delay(900);

  const columns = await readWidget();
  await pressKey("Escape");
  await delay(300);
  const afterEscape = await readWidget();

  /* 5. 表名 */
  await replaceEditorText("select * from demo");
  await delay(900);

  const tables = await readWidget();

  /* 6. Ctrl+R 执行查询；查询报错只落在工作区顶部，不弹窗 */
  await replaceEditorText("select 1;");
  await pressKey("R", ["control"]);
  await delay(1500);

  const runState = await readRunState();

  await replaceEditorText("select * from 不存在的表;");
  await pressKey("R", ["control"]);
  await delay(1500);

  const errorState = await readRunState();

  /* 7. 有结果集 → 切结果页；无结果集 → 留在日志页 */
  await replaceEditorText("select * from demo_order;");
  await pressKey("R", ["control"]);
  await delay(1500);

  const resultPane = await readPaneState();

  await replaceEditorText("create table if not exists probe_tmp (x integer);");
  await pressKey("R", ["control"]);
  await delay(1500);

  const logPane = await readPaneState();

  /* 8. 顶部菜单：鼠标移上去即展开 */
  const menuHover = await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".menubar .menu")].find(node => node.textContent.includes("文件"));
    if (!button) return null;
    /* React 的 onMouseEnter 由 mouseover 合成，直接派发 mouseover 才能触发 */
    button.dispatchEvent(new MouseEvent("mouseover", { bubbles: true, relatedTarget: document.body }));
    return true;
  })()`);

  await delay(250);

  const menuHoverState = await window.webContents.executeJavaScript(`(() => {
    const dropdown = document.querySelector(".menu-dropdown");
    const items = dropdown ? [...dropdown.querySelectorAll("button")].map(node => node.textContent) : null;
    document.querySelector(".work-main")?.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
    window.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    return items;
  })()`);

  /* 9. 表格：右键保留选区 + 列宽可拖动（先跑一条有结果集的查询回到结果页） */
  await focusEditor();
  await replaceEditorText("select * from demo_order;");
  await pressKey("R", ["control"]);
  await delay(1500);

  await window.webContents.executeJavaScript(`(() => {
    const cell = (row, col) => document.querySelectorAll("table.grid tbody tr")[row]?.querySelectorAll("td")[col + 1];
    const first = cell(0, 0);
    const last = cell(1, 1);
    if (!first || !last) return false;

    first.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
    last.dispatchEvent(new MouseEvent("mouseover", { bubbles: true }));
    window.dispatchEvent(new MouseEvent("mouseup", { bubbles: true }));

    return true;
  })()`);

  await delay(200);

  const gridSelection = await countSelectedCells();

  await window.webContents.executeJavaScript(`(() => {
    const rows = document.querySelectorAll("table.grid tbody tr");
    const target = rows[0]?.querySelectorAll("td")[2];
    if (!target) return false;
    target.dispatchEvent(new MouseEvent("contextmenu", { bubbles: true, clientX: 320, clientY: 420 }));
    return true;
  })()`);

  await delay(200);

  const rightClickKeep = await countSelectedCells();

  const columnResize = await window.webContents.executeJavaScript(`(() => {
    const header = document.querySelectorAll("table.grid thead th")[1];
    const handle = header ? header.querySelector(".col-resizer") : null;
    if (!handle) return null;

    const before = Math.round(header.getBoundingClientRect().width);
    const rect = handle.getBoundingClientRect();
    handle.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, clientX: rect.left + 3, clientY: rect.top + 6 }));
    window.dispatchEvent(new MouseEvent("mousemove", { bubbles: true, clientX: rect.left + 63, clientY: rect.top + 6 }));
    window.dispatchEvent(new MouseEvent("mouseup", { bubbles: true }));
    return before;
  })()`);

  await delay(250);

  const columnAfter = await window.webContents.executeJavaScript(
    `(() => { const header = document.querySelectorAll("table.grid thead th")[1]; return header ? Math.round(header.getBoundingClientRect().width) : null; })()`);

  /* 11. 编辑态仍能框选：双击进入编辑后，在别的单元格按下并拖动 */
  await window.webContents.executeJavaScript(`(() => {
    const cell = (row, col) => document.querySelectorAll("table.grid tbody tr")[row]?.querySelectorAll("td")[col + 1];
    cell(0, 0)?.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));
    return true;
  })()`);

  await delay(300);

  const editOpened = await window.webContents.executeJavaScript(`Boolean(document.querySelector(".cell-editor"))`);

  await window.webContents.executeJavaScript(`(() => {
    const cell = (row, col) => document.querySelectorAll("table.grid tbody tr")[row]?.querySelectorAll("td")[col + 1];
    cell(1, 0)?.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, button: 0, buttons: 1 }));
    cell(2, 1)?.dispatchEvent(new MouseEvent("mouseover", { bubbles: true }));
    window.dispatchEvent(new MouseEvent("mouseup", { bubbles: true }));
    return true;
  })()`);

  await delay(250);

  const editDrag = await window.webContents.executeJavaScript(`JSON.stringify({
    selected: document.querySelectorAll("table.grid td.is-range").length,
    editorOpen: Boolean(document.querySelector(".cell-editor"))
  })`);

  /* 12. 菜单打开时，左键点单元格：菜单关闭 + 选区跟随 */
  await window.webContents.executeJavaScript(`(() => {
    document.querySelectorAll("table.grid tbody tr")[0]?.querySelectorAll("td")[1]
      ?.dispatchEvent(new MouseEvent("contextmenu", { bubbles: true, button: 2, buttons: 2, clientX: 320, clientY: 420 }));
    return true;
  })()`);

  await delay(250);
  const menuShown = await window.webContents.executeJavaScript(`Boolean(document.querySelector(".ctx-menu"))`);

  await window.webContents.executeJavaScript(`(() => {
    document.querySelectorAll("table.grid tbody tr")[1]?.querySelectorAll("td")[2]
      ?.dispatchEvent(new PointerEvent("pointerdown", { bubbles: true, button: 0, buttons: 1, isPrimary: true, pointerType: "mouse" }));
    document.querySelectorAll("table.grid tbody tr")[1]?.querySelectorAll("td")[2]
      ?.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, button: 0, buttons: 1 }));
    window.dispatchEvent(new MouseEvent("mouseup", { bubbles: true }));
    return true;
  })()`);

  await delay(250);

  const menuAfterClick = await window.webContents.executeJavaScript(`JSON.stringify({
    menuOpen: Boolean(document.querySelector(".ctx-menu")),
    selected: document.querySelectorAll("table.grid td.is-range").length
  })`);

  /* 10. 顶部工具栏只放全局动作，执行类动作留在查询工具栏 */
  const toolbars = await window.webContents.executeJavaScript(`JSON.stringify({
    top: [...document.querySelectorAll(".toolbar .tbtn")].map(node => node.textContent.trim()),
    pane: [...document.querySelectorAll(".pane-toolbar .tbtn")].map(node => node.textContent.trim())
  })`);

  /* 6. 主题：切到深色后再看弹窗配色是否跟随 */
  await replaceEditorText("sel");
  await waitForWidget();
  const lightTheme = await readTheme();
  fs.writeFileSync(path.join(CAPTURE_DIR, "valkyrie-suggest-light.png"), (await window.webContents.capturePage()).toPNG());

  await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".toolbar .tbtn")]
      .find(node => node.textContent.includes("浅色") || node.textContent.includes("深色"));
    if (button) button.click();
    return Boolean(button);
  })()`);
  await delay(900);

  await replaceEditorText("sel");
  await waitForWidget();
  const darkTheme = await readTheme();
  fs.writeFileSync(path.join(CAPTURE_DIR, "valkyrie-suggest-dark.png"), (await window.webContents.capturePage()).toPNG());

  console.log("连接=" + connected + " 新建查询=" + tabbed);
  console.log("关键字 sel: " + JSON.stringify(keywords));
  console.log("回车插入 → " + JSON.stringify(afterEnter));
  console.log("片段 sela → " + JSON.stringify(snippetText) + " 补字符后=" + JSON.stringify(snippetTyped));
  console.log("字段 o.: " + JSON.stringify({ visible: columns.visible, labels: columns.labels, offsets: columns.offsets })
    + " Esc 关闭后=" + afterEscape.visible);
  console.log("表名 demo: " + JSON.stringify({ visible: tables.visible, labels: tables.labels }));
  console.log("Ctrl+R 执行: " + JSON.stringify(runState));
  console.log("查询报错: " + JSON.stringify(errorState));
  console.log("结果集 → 面板: " + JSON.stringify(resultPane) + " 无结果集 → 面板: " + JSON.stringify(logPane));
  console.log("菜单悬停=" + JSON.stringify(menuHoverState ? menuHoverState.slice(0, 3) : null));
  console.log("右键保留选区: 选区单元格=" + gridSelection + " 右键后=" + rightClickKeep);
  console.log("列宽拖动: " + JSON.stringify({ before: columnResize, after: columnAfter }));
  console.log("编辑态框选: 编辑器已开=" + editOpened + " 拖动后=" + editDrag);
  console.log("菜单打开时点单元格: 菜单已开=" + menuShown + " 点击后=" + menuAfterClick);
  console.log("工具栏: " + toolbars);
  console.log("浅色弹窗: " + JSON.stringify(lightTheme));
  console.log("深色弹窗: " + JSON.stringify(darkTheme));
  console.log("截图=" + path.join(CAPTURE_DIR, "valkyrie-suggest-light.png") + " / valkyrie-suggest-dark.png");

  await bridge.stop();
  app.quit();

  /* ------------------------------ 辅助 ------------------------------ */

  async function focusEditor() {
    /* 隐藏窗口没有系统焦点，Monaco 的「输入即提示」要求编辑器持有 widget focus */
    window.show();
    window.focus();
    window.webContents.focus();
    await delay(300);

    await window.webContents.executeJavaScript(`(() => {
      const lines = document.querySelector(".monaco-editor .view-lines");
      if (!lines) return false;
      const rect = lines.getBoundingClientRect();
      for (const type of ["mousedown", "mouseup", "click"])
        lines.dispatchEvent(new MouseEvent(type, { bubbles: true, clientX: rect.left + 120, clientY: rect.top + 14 }));
      return true;
    })()`);

    await delay(300);
  }

  async function pressKey(keyCode, modifiers) {
    window.webContents.sendInputEvent({ type: "keyDown", keyCode, modifiers });
    window.webContents.sendInputEvent({ type: "keyUp", keyCode, modifiers });
    await delay(140);
  }

  /* 真实按键（keyDown/keyUp）才能触发 Monaco 的 quick suggestions */
  async function typeKeys(text) {
    for (const ch of text) {
      window.webContents.sendInputEvent({ type: "keyDown", keyCode: ch });
      window.webContents.sendInputEvent({ type: "char", keyCode: ch });
      window.webContents.sendInputEvent({ type: "keyUp", keyCode: ch });
      await delay(90);
    }

    await delay(700);
  }

  async function typeText(text) {
    for (const ch of text) {
      window.webContents.sendInputEvent({ type: "char", keyCode: ch });
      await delay(70);
    }

    await delay(700);
  }

  async function replaceEditorText(text, asKeys = false) {
    await pressKey("A", ["control"]);

    if (asKeys)
      await typeKeys(text);
    else
      await typeText(text);
  }

  async function editorText() {
    return window.webContents.executeJavaScript(
      `(document.querySelector(".monaco-editor .view-lines") || {}).textContent || ""`);
  }

  async function readWidget() {
    return window.webContents.executeJavaScript(`(() => {
      const widget = document.querySelector(".suggest-widget");
      const host = document.querySelector(".editor-wrap");
      const rows = [...document.querySelectorAll(".suggest-widget .monaco-list-row")];
      const focused = document.querySelector(".suggest-widget .monaco-list-row.focused");
      const labelOf = row => (row.querySelector(".label-name") || {}).textContent || null;

      return {
        visible: Boolean(widget) && getComputedStyle(widget).display !== "none",
        editorText: (document.querySelector(".monaco-editor .view-lines") || {}).textContent || "",
        rows: rows.length,
        labels: rows.slice(0, 8).map(labelOf),
        kinds: rows.slice(0, 8).map(row => (row.querySelector(".details-label") || {}).textContent || null),
        focused: focused ? labelOf(focused) : null,
        /* 标签相对行盒的垂直偏移：正常 0~3px，超了说明被外部样式污染 */
        offsets: rows.slice(0, 3).map(row => {
          const label = row.querySelector(".label-name");
          return label ? Math.round(label.getBoundingClientRect().y - row.getBoundingClientRect().y) : null;
        }),
        rowHeights: rows.slice(0, 3).map(row => Math.round(row.getBoundingClientRect().height)),
        box: widget ? (() => { const r = widget.getBoundingClientRect(); return [Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height)]; })() : null,
        insideEditor: Boolean(widget && host && host.contains(widget))
      };
    })()`);
  }

  /* 快速补全由 Monaco 自己调度（有延迟 + 去抖），这里轮询等待 */
  async function waitForWidget(timeoutMs = 3000) {
    const deadline = Date.now() + timeoutMs;
    let last = await readWidget();

    while (!last.visible && Date.now() < deadline) {
      await delay(200);
      last = await readWidget();
    }

    return last;
  }

  /* 弹窗主题：底色 / 描边 / 圆角 / 选中行配色，应跟随应用主题变量 */
  async function readTheme() {
    return window.webContents.executeJavaScript(`(() => {
      const widget = document.querySelector(".suggest-widget");
      if (!widget) return null;

      const row = widget.querySelector(".monaco-list-row.focused") || widget.querySelector(".monaco-list-row");
      const icon = row ? row.querySelector(".suggest-icon") : null;
      const cs = getComputedStyle(widget);

      return {
        scheme: getComputedStyle(document.documentElement).colorScheme,
        background: cs.backgroundColor,
        border: cs.borderTopColor,
        radius: cs.borderTopLeftRadius,
        rowBackground: row ? getComputedStyle(row).backgroundColor : null,
        rowColor: row ? getComputedStyle(row).color : null,
        iconColor: icon ? getComputedStyle(icon).color : null
      };
    })()`);
  }

  /* 执行结果：状态栏 + 内联错误条 + 是否弹窗 */
  async function readRunState() {
    return window.webContents.executeJavaScript(`(() => ({
      status: (document.querySelector(".statusbar") || {}).textContent || null,
      rows: document.querySelectorAll(".grid-wrap table.grid tbody tr").length,
      errorBar: document.querySelector(".error-bar") ? document.querySelector(".error-bar").textContent : null,
      modal: Boolean(document.querySelector(".modal-backdrop"))
    }))()`);
  }

  /* 当前激活的结果面板（结果 1 / 消息 / 执行计划 / 日志） */
  async function readPaneState() {
    return window.webContents.executeJavaScript(`(() => {
      const active = document.querySelector(".result-tab.is-active");
      return {
        pane: active ? active.textContent.replace(/\\d+/g, "").trim() : null,
        rows: document.querySelectorAll(".grid-wrap table.grid tbody tr").length
      };
    })()`);
  }

  async function countSelectedCells() {
    return window.webContents.executeJavaScript(
      `document.querySelectorAll("table.grid td.is-range").length`);
  }
}

app.whenReady().then(main).catch(error => {
  console.error(error);
  app.exit(1);
});
