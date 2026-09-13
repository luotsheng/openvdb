/**
 * 快捷键提示文案。
 *
 * macOS 用 ⌘（Command），其它平台用 Ctrl —— 真正按下的组合键在 Monaco 里走
 * CtrlCmd、在窗口级监听里同时判断 ctrlKey / metaKey，这里只负责「显示什么」。
 * 平台由 preload 从 process.platform 带过来，比 navigator.platform 可靠。
 */
export const IS_MAC = typeof window !== "undefined" && window.valkyrie?.platform === "darwin";

export const KEY = {
  run: IS_MAC ? "⌘R" : "Ctrl+R",
  runEnter: IS_MAC ? "⌘⏎" : "Ctrl+Enter",
  format: IS_MAC ? "⌘⇧F" : "Ctrl+Shift+F",
  save: IS_MAC ? "⌘S" : "Ctrl+S",
  saveAs: IS_MAC ? "⌘⇧S" : "Ctrl+Shift+S",
  selectAll: IS_MAC ? "⌘A" : "Ctrl+A",
  expand: IS_MAC ? "⌘W" : "Ctrl+W",
  shrink: IS_MAC ? "⌘⇧W" : "Ctrl+Shift+W",
  copy: IS_MAC ? "⌘C" : "Ctrl+C",
  cut: IS_MAC ? "⌘X" : "Ctrl+X",
  paste: IS_MAC ? "⌘V" : "Ctrl+V",
  space: IS_MAC ? "⌘Space" : "Ctrl+Space"
};
