/**
 * Monaco 基础编辑器 Worker 的本地入口。
 * <p>
 * 通过本地文件再以 `?worker` 方式引入，避免打包器直接解析包路径后缀。
 */
import "monaco-editor/esm/vs/editor/editor.worker.js";
