export interface AppSettings {
  /** 界面基准字号（px） */
  uiFontSize: number;
  /** 编辑器字号（px） */
  editorFontSize: number;
  /** 编辑器自动换行 */
  editorWordWrap: boolean;
  /** 编辑器智能提示 */
  suggestEnabled: boolean;
  /** 结果表格字号（px） */
  gridFontSize: number;
  /** 结果表格斑马纹 */
  gridZebra: boolean;
  /** 结果表格显示行号列 */
  gridRowNumbers: boolean;
  /** 数据页默认取行数 */
  pageSize: number;
}

export const DEFAULT_SETTINGS: AppSettings = {
  uiFontSize: 14,
  editorFontSize: 14,
  editorWordWrap: false,
  suggestEnabled: true,
  gridFontSize: 14,
  gridZebra: true,
  gridRowNumbers: true,
  pageSize: 200
};

const STORAGE_KEY = "valkyrie.settings";

export const FONT_SIZE_OPTIONS = [12, 13, 14, 15, 16, 17, 18];
export const UI_FONT_SIZE_OPTIONS = [12, 13, 14, 15, 16];
export const GRID_FONT_SIZE_OPTIONS = [11, 12, 13, 14, 15];
export const PAGE_SIZE_OPTIONS = [100, 200, 500, 1000, 2000];

/** 读取本地设置：缺字段用默认值补齐，坏数据不抛异常 */
export function loadSettings(): AppSettings {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) as Partial<AppSettings> : {};

    return { ...DEFAULT_SETTINGS, ...parsed };
  } catch {
    return { ...DEFAULT_SETTINGS };
  }
}

export function saveSettings(settings: AppSettings): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  } catch {
    /* 存储不可用时静默忽略，不影响使用 */
  }
}
