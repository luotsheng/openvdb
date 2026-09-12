"use strict";

/**
 * 补全弹窗几何诊断：按候选分组剔除 CSS 规则，量「标签相对行盒的垂直偏移」。
 * 正常应为 0~3px；若某组剔除后偏移归零，该组即元凶。
 *
 *   $env:VALKYRIE_CANDIDATE="body"; npx electron scripts/dom-probe.cjs
 */

const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("node:path");
const { JavaBridge } = require("../src/main/java-bridge.cjs");

const CONNECTION_NAME = "本地测试库";
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

const PATTERNS = {
  none: [],
  boxsizing: ["*"],
  root: [":root"],
  body: ["body"],
  containers: [".editor-wrap", ".work-main", ".result", ".work-body", ".app", "#root"],
  chrome: [".object-tree", ".tbtn", ".work-tab", ".result-", ".pane-toolbar", ".side", ".info", ".titlebar", ".menubar"]
};

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

  await window.webContents.executeJavaScript(`(() => {
    const row = [...document.querySelectorAll(".tree-row")].find(n => n.textContent.includes(${JSON.stringify(CONNECTION_NAME)}));
    if (row) row.dispatchEvent(new MouseEvent("dblclick", { bubbles: true }));
    return Boolean(row);
  })()`);
  await delay(2500);

  await window.webContents.executeJavaScript(`(() => {
    const button = [...document.querySelectorAll(".toolbar .tbtn")].find(n => n.textContent.includes("新建查询"));
    if (button) button.click();
    return Boolean(button);
  })()`);
  await delay(900);

  /* 按候选剔除规则（不可逆，所以一次进程只测一组） */
  const candidate = process.env.VALKYRIE_CANDIDATE || "none";
  const keys = PATTERNS[candidate] || [];

  const removed = await window.webContents.executeJavaScript(`(() => {
    const keys = ${JSON.stringify(keys)};
    let removed = 0;
    for (const sheet of Array.from(document.styleSheets)) {
      let rules;
      try { rules = Array.from(sheet.cssRules); } catch { continue; }
      for (let i = rules.length - 1; i >= 0; i--) {
        const rule = rules[i];
        const sel = rule.selectorText;
        if (sel && keys.some(k => sel === k || sel.includes(k))) { sheet.deleteRule(i); removed++; }
      }
    }
    return removed;
  })()`);

  /* 聚焦编辑器 → 输入 → 触发补全 */
  window.focus();
  window.webContents.focus();

  const focused = await window.webContents.executeJavaScript(`(() => {
    const lines = document.querySelector(".monaco-editor .view-lines");
    if (!lines) return "no-lines";
    const rect = lines.getBoundingClientRect();
    for (const type of ["mousedown", "mouseup", "click"]) {
      lines.dispatchEvent(new MouseEvent(type, { bubbles: true, clientX: rect.left + 80, clientY: rect.top + 14 }));
    }
    return document.activeElement ? document.activeElement.className : "none";
  })()`);

  await delay(300);

  /* 输入 "select " 触发补全（空格是触发字符，比 Ctrl+Space 更可靠） */
  for (const ch of ["s", "e", "l", "e", "c", "t", " "]) {
    window.webContents.sendInputEvent({ type: "char", keyCode: ch });
    await delay(120);
  }

  await delay(500);
  window.webContents.sendInputEvent({ type: "keyDown", keyCode: " ", modifiers: ["control"] });
  window.webContents.sendInputEvent({ type: "keyUp", keyCode: " ", modifiers: ["control"] });
  await delay(1500);

  const editorText = await window.webContents.executeJavaScript(
    `(document.querySelector(".monaco-editor .view-lines") || {}).textContent || ""`);

  const geometry = await window.webContents.executeJavaScript(`(() => {
    const rows = [...document.querySelectorAll(".suggest-widget .monaco-list-row")];
    if (!rows.length) return { rows: 0, text: ${JSON.stringify("")} };

    return {
      rows: rows.length,
      items: rows.slice(0, 3).map(row => {
        const label = row.querySelector(".label-name");
        const rr = row.getBoundingClientRect();
        const lr = label ? label.getBoundingClientRect() : null;
        const cs = getComputedStyle(row);
        return {
          rowY: Math.round(rr.y),
          rowH: Math.round(rr.height),
          labelY: lr ? Math.round(lr.y) : null,
          labelH: lr ? Math.round(lr.height) : null,
          offset: lr ? Math.round(lr.y - rr.y) : null,
          overflow: cs.overflow,
          display: cs.display,
          flexWrap: cs.flexWrap,
          lineHeight: cs.lineHeight
        };
      })
    };
  })()`);

  const detail = await window.webContents.executeJavaScript(`(() => {
    const row = document.querySelector(".suggest-widget .monaco-list-row");
    if (!row) return null;
    const cs = getComputedStyle(row);
    const rect = el => { const r = el.getBoundingClientRect(); return [Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height)]; };
    return {
      colors: (() => {
        const focused = row.querySelector(".label-name");
        const icon = row.querySelector(".suggest-icon");
        const widgetEl = document.querySelector(".suggest-widget");
        const cs = el => { if (!el) return null; const s = getComputedStyle(el); return { color: s.color, fill: s.webkitTextFillColor, opacity: s.opacity }; };
        const vars = ["--vscode-editorSuggestWidget-foreground", "--vscode-editorSuggestWidget-background",
          "--vscode-editorSuggestWidget-selectedForeground", "--vscode-editorSuggestWidget-selectedBackground",
          "--vscode-editorSuggestWidget-highlightForeground", "--vscode-symbolIcon-textForeground"];
        return {
          rows: [...document.querySelectorAll(".suggest-widget .monaco-list-row")].map(r => ({
            cls: r.className,
            color: getComputedStyle(r).color,
            bg: getComputedStyle(r).backgroundColor,
            labelColor: r.querySelector(".label-name") ? getComputedStyle(r.querySelector(".label-name")).color : null
          })),
          label: cs(focused),
          icon: cs(icon),
          row: cs(row),
          widget: cs(widgetEl),
          themeVars: Object.fromEntries(vars.map(v => [v, getComputedStyle(widgetEl).getPropertyValue(v).trim()]))
        };
      })(),
      innerHtml: row.innerHTML.slice(0, 1200),
      insideMonacoEditor: Boolean(row.closest(".monaco-editor")),
      widgetParentChain: (() => {
        const chain = [];
        let el = row;
        while (el && chain.length < 8) { chain.push(el.tagName + "." + (el.className || "").toString().split(" ")[0]); el = el.parentElement; }
        return chain;
      })(),
      mainDisplay: (() => {
        const el = row.querySelector(".contents .main");
        if (!el) return null;
        const cs2 = getComputedStyle(el);
        return { display: cs2.display, alignItems: cs2.alignItems, height: cs2.height, rect: (() => { const r = el.getBoundingClientRect(); return [Math.round(r.y), Math.round(r.height)]; })() };
      })(),
      rowInlineStyle: row.getAttribute("style"),
      align: cs.alignItems,
      position: cs.position,
      children: [...row.children].map(el => ({
        cls: el.className,
        inline: el.getAttribute("style"),
        position: getComputedStyle(el).position,
        top: getComputedStyle(el).top,
        rect: rect(el)
      })),
      iconLabel: (() => {
        const el = row.querySelector(".monaco-icon-label");
        if (!el) return null;
        const cs2 = getComputedStyle(el);
        return { inline: el.getAttribute("style"), position: cs2.position, top: cs2.top, height: cs2.height, lineHeight: cs2.lineHeight, rect: rect(el) };
      })()
    };
  })()`);

  console.log("候选=" + candidate + " 删除规则=" + removed
    + " 聚焦=" + focused
    + " 编辑器内容=" + JSON.stringify(editorText.slice(0, 40))
    + " " + JSON.stringify(geometry));
  console.log("行内结构:", JSON.stringify(detail, null, 2));

  await bridge.stop();
  app.quit();
}

app.whenReady().then(main).catch(error => {
  console.error(error);
  app.exit(1);
});
