import { Dialog } from "./Dialog";
import { Select } from "./Select";
import { KEY } from "../keys";
import {
  FONT_SIZE_OPTIONS,
  GRID_FONT_SIZE_OPTIONS,
  PAGE_SIZE_OPTIONS,
  UI_FONT_SIZE_OPTIONS,
  type AppSettings
} from "../settings";

type ThemeMode = "light" | "dark" | "system";

interface OptionsDialogProps {
  settings: AppSettings;
  theme: ThemeMode;
  onChange: (patch: Partial<AppSettings>) => void;
  onThemeChange: (theme: ThemeMode) => void;
  onClose: () => void;
}

function sizeOptions(values: number[]) {
  return values.map(value => ({ value: String(value), label: `${value} px` }));
}

/** 选项：集中管理外观 / 编辑器 / 数据表格 / 查询的客户端配置，改完立即生效 */
export function OptionsDialog(props: OptionsDialogProps) {
  const { settings, theme, onChange, onThemeChange, onClose } = props;

  return (
    <Dialog title="选项" className="modal-form options-form" onClose={onClose}>
      <div className="modal-body options-body">
        <section className="options-section">
          <h4>外观</h4>
          <div className="options-row">
            <span className="options-label">主题</span>
            <Select
              value={theme}
              options={[
                { value: "light", label: "浅色" },
                { value: "dark", label: "深色" },
                { value: "system", label: "跟随系统" }
              ]}
              onChange={value => onThemeChange(value as ThemeMode)}
            />
          </div>
          <div className="options-row">
            <span className="options-label">界面字号</span>
            <Select
              value={String(settings.uiFontSize)}
              options={sizeOptions(UI_FONT_SIZE_OPTIONS)}
              onChange={value => onChange({ uiFontSize: Number(value) })}
            />
          </div>
        </section>

        <section className="options-section">
          <h4>编辑器</h4>
          <div className="options-row">
            <span className="options-label">字号</span>
            <Select
              value={String(settings.editorFontSize)}
              options={sizeOptions(FONT_SIZE_OPTIONS)}
              onChange={value => onChange({ editorFontSize: Number(value) })}
            />
          </div>
          <label className="options-row is-check">
            <input
              type="checkbox"
              checked={settings.editorWordWrap}
              onChange={event => onChange({ editorWordWrap: event.target.checked })}
            />
            <span>自动换行</span>
          </label>
          <label className="options-row is-check">
            <input
              type="checkbox"
              checked={settings.suggestEnabled}
              onChange={event => onChange({ suggestEnabled: event.target.checked })}
            />
            <span>智能提示（{KEY.space} 可手动触发）</span>
          </label>
        </section>

        <section className="options-section">
          <h4>数据表格</h4>
          <div className="options-row">
            <span className="options-label">字号</span>
            <Select
              value={String(settings.gridFontSize)}
              options={sizeOptions(GRID_FONT_SIZE_OPTIONS)}
              onChange={value => onChange({ gridFontSize: Number(value) })}
            />
          </div>
          <label className="options-row is-check">
            <input
              type="checkbox"
              checked={settings.gridZebra}
              onChange={event => onChange({ gridZebra: event.target.checked })}
            />
            <span>斑马纹（隔行浅色）</span>
          </label>
          <label className="options-row is-check">
            <input
              type="checkbox"
              checked={settings.gridRowNumbers}
              onChange={event => onChange({ gridRowNumbers: event.target.checked })}
            />
            <span>显示行号列</span>
          </label>
        </section>

        <section className="options-section">
          <h4>查询</h4>
          <div className="options-row">
            <span className="options-label">默认行数限制</span>
            <Select
              value={String(settings.pageSize)}
              options={PAGE_SIZE_OPTIONS.map(value => ({ value: String(value), label: `${value} 行` }))}
              onChange={value => onChange({ pageSize: Number(value) })}
            />
          </div>
        </section>
      </div>

      <div className="modal-actions">
        <button type="button" className="mini-btn is-default" onClick={onClose}>关闭</button>
      </div>
    </Dialog>
  );
}
