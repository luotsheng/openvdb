import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  root: path.resolve(__dirname, "src/renderer"),
  /* 打包后通过 file:// 加载，必须使用相对路径 */
  base: "./",
  plugins: [react()],
  build: {
    outDir: path.resolve(__dirname, "dist/renderer"),
    emptyOutDir: true,
    /*
     * 目标设为 Electron 自带的 Chromium：构建时不能把 CSS 的 light-dark() 降级
     * （降级后会改写成 --lightningcss-* 变量，主题切换只认系统配色，页面里
     *  document.documentElement.style.colorScheme 的切换就失效了）。
     */
    target: "chrome130",
    chunkSizeWarningLimit: 4096
  }
});
