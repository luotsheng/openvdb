package valkyrie.monacofx;

import com.alibaba.fastjson.JSONObject;
import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.Setter;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static valkyrie.utils.TypeConverter.atos;

/**
 * Monaco 编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/5/6
 */
@SuppressWarnings("ALL")
public class MonacoEditor extends StackPane
{
        private static final Logger LOG = LoggerFactory.getLogger(MonacoEditor.class);

        private final WebView webView = new WebView();
        private final WebEngine engine = webView.getEngine();
        private final MonacoHook hook = new MonacoHook(this);
        private final PauseTransition pauseTransition = new PauseTransition(Duration.millis(500));
        private ContextMenu contextMenu = null;

        /**
         * 编辑器是否已加载完成；未完成时把操作排队，避免在 FX 线程轮询 JS
         */
        private boolean ready = false;
        private boolean loading = false;
        private final java.util.List<Runnable> pendingTasks = new java.util.ArrayList<>();

        @Setter
        private ShowContextMenuRequestEvent showContextMenuRequestEvent = null;

        @Setter
        private OnDidChangeModelContent onDidChangeModelContent = null;

        @Setter
        private OnDidChangeCursorSelection onDidChangeCursorSelection = null;

        @Setter
        private SuggestionProvider suggestionProvider = null;

        @Setter
        private OnOpenTableLink onOpenTableLink = null;

        /**
         * 返回表注释：{@code null} 表示不是表；空串表示表存在但无注释
         */
        @Setter
        private java.util.function.Function<String, String> tableCommentProvider = null;

        private final Tooltip tableTooltip = new Tooltip();

        public interface ShowContextMenuRequestEvent {
                void onRequest(ContextMenu contextMenu);
        }

        public interface OnDidChangeModelContent {
                void onChange();
        }

        public interface OnDidChangeCursorSelection {
                void onChange(int line, int column, int selected);
        }

        /**
         * 上下文感知补全提供者：入参为编辑器全文与光标偏移，返回可序列化的提示项集合
         */
        public interface SuggestionProvider {
                Collection<?> suggest(String sql, int offset);
        }

        /**
         * 按住 Shortcut 键点击标识符时触发（用于表名跳转等）
         */
        public interface OnOpenTableLink {
                void onOpen(String name);
        }

        @SuppressWarnings("DataFlowIssue")
        public MonacoEditor()
        {
                webView.setContextMenuEnabled(false);
                getChildren().add(webView);

                webView.setOnMousePressed(event -> {
                        if (event.getButton() == MouseButton.SECONDARY) {
                                showContextMenu(event.getScreenX(), event.getScreenY());
                        } else {
                                hideContextMenu();
                        }
                });

                pauseTransition.setOnFinished(event -> {
                        if (onDidChangeModelContent != null)
                                onDidChangeModelContent.onChange();
                });

                /* 页面加载完成后安装 JS 钩子 */
                engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED)
                                installHook();
                });

                /* 进入场景后再加载 Monaco，避免打开标签页时同步等待 WebView 初始化 */
                sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null)
                                startLoad();
                });

                webView.prefWidthProperty().bind(this.widthProperty());
                webView.prefHeightProperty().bind(this.heightProperty());
        }

        private void startLoad()
        {
                if (loading)
                        return;

                loading = true;
                engine.load(getClass().getResource("/static/editor.html").toExternalForm());
        }

        public void dispose()
        {
                ready = false;
                loading = false;
                pendingTasks.clear();

                engine.getLoadWorker().cancel();
                engine.load("about:blank");

                StackPane parent = (StackPane) webView.getParent();

                if (parent != null)
                        parent.getChildren().remove(webView);
        }

        /**
         * 钩子函数
         */
        @SuppressWarnings("unused")
        public static class MonacoHook {

                private final MonacoEditor editor;

                public MonacoHook(MonacoEditor editor)
                {
                        this.editor = editor;
                }

                /**
                 * 打印日志
                 */
                public void info(Object message)
                {
                        LOG.info("Monaco editor: {}", message);
                }

                /**
                 * 写入剪贴板
                 */
                public void writeClipboard(Object text)
                {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(atos(text));
                        Clipboard.getSystemClipboard().setContent(content);
                }

                /**
                 * 用户输入监听
                 */
                public void onDidChangeModelContent()
                {
                        editor.pauseTransition.playFromStart();
                }

                /**
                 * 光标/选区变化监听
                 */
                public void onDidChangeCursorSelection(Object line, Object column, Object selected)
                {
                        if (editor.onDidChangeCursorSelection == null)
                                return;

                        editor.onDidChangeCursorSelection.onChange(
                                toInt(line), toInt(column), toInt(selected));
                }

                /**
                 * 上下文补全：由 JS 在补全请求时同步调用，返回提示项 JSON
                 */
                public String getSuggestions(Object sql, Object offset)
                {
                        if (editor.suggestionProvider == null)
                                return "[]";

                        try {
                                return JSONObject.toJSONString(
                                        editor.suggestionProvider.suggest(String.valueOf(sql), toInt(offset)));
                        } catch (Exception e) {
                                LOG.error("Suggestion provider failed", e);
                                return "[]";
                        }
                }

                /**
                 * 按住 Shortcut 键点击标识符
                 */
                public void openTable(Object name)
                {
                        if (editor.onOpenTableLink != null)
                                editor.onOpenTableLink.onOpen(String.valueOf(name));
                }

                /**
                 * 判断标识符是否为可跳转的表名（用于悬停高亮）
                 */
                public boolean isTable(Object name)
                {
                        return editor.tableCommentProvider != null
                                && editor.tableCommentProvider.apply(String.valueOf(name)) != null;
                }

                /**
                 * Shortcut 悬停表名：展示表注释，返回是否为表
                 */
                public boolean hoverTable(Object name, Object x, Object y)
                {
                        if (editor.tableCommentProvider == null)
                                return false;

                        String word = String.valueOf(name);
                        String comment = editor.tableCommentProvider.apply(word);

                        if (comment == null) {
                                editor.hideTableTooltip();
                                return false;
                        }

                        editor.showTableTooltip(word, comment, toDouble(x), toDouble(y));
                        return true;
                }

                public void hideTableTooltip()
                {
                        editor.hideTableTooltip();
                }

                private static double toDouble(Object value)
                {
                        if (value instanceof Number number)
                                return number.doubleValue();

                        try {
                                return Double.parseDouble(String.valueOf(value));
                        } catch (Exception e) {
                                return 0;
                        }
                }

                /**
                 * 编辑器创建完成（由 JS 回调），执行排队的操作
                 */
                public void onEditorReady()
                {
                        editor.onEditorReady();
                }

                private static int toInt(Object value)
                {
                        if (value instanceof Number number)
                                return number.intValue();

                        try {
                                return (int) Double.parseDouble(String.valueOf(value));
                        } catch (Exception e) {
                                return 0;
                        }
                }
        }

        private void installHook()
        {
                try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("hook", hook);
                        engine.executeScript(
                                """
                                   console.log = function(message) {
                                       window.hook.info(message);
                                   };
                                   
                                   window.writeClipboard = function(message) {
                                       window.hook.writeClipboard(message);
                                   };
                                   """
                        );
                } catch (Exception e) {
                        LOG.error("Install Monaco hook failed", e);
                }
        }

        private void onEditorReady()
        {
                ready = true;

                java.util.List<Runnable> tasks = new java.util.ArrayList<>(pendingTasks);
                pendingTasks.clear();

                tasks.forEach(Runnable::run);
        }

        private void showTableTooltip(String name, String comment, double x, double y)
        {
                tableTooltip.setText(comment == null || comment.isBlank() ? name : name + "  —  " + comment);

                javafx.geometry.Point2D point = webView.localToScreen(x, y);

                if (point == null)
                        return;

                tableTooltip.show(webView, point.getX() + 12, point.getY() + 14);
        }

        private void hideTableTooltip()
        {
                if (tableTooltip.isShowing())
                        tableTooltip.hide();
        }

        public void bindContextMenu(ContextMenu contextMenu)
        {
                this.contextMenu = contextMenu;
        }

        private void showContextMenu(double x, double y)
        {
                if (contextMenu == null)
                        return;

                if (showContextMenuRequestEvent != null)
                        showContextMenuRequestEvent.onRequest(contextMenu);

                contextMenu.show(webView, x, y);
        }

        private void hideContextMenu()
        {
                if (contextMenu == null)
                        return;

                contextMenu.hide();
        }

        /**
         * 使用 Suggestion 对象注册提示
         */
        public void registerSuggestions(Collection<?> suggestions)
        {
                runWhenReady(() -> engine.executeScript(
                        "window.addSuggestions(" + JSONObject.toJSONString(suggestions) + ")"));
        }

        public String getValue()
        {
                if (!ready)
                        return "";

                return (String) engine.executeScript("editor.getValue()");
        }

        public String getSelectedValue()
        {
                if (!ready)
                        return "";

                return (String) engine.executeScript(
                        "window.editor.getModel().getValueInRange(editor.getSelection())"
                );
        }

        public void replaceSelection(String text)
        {
                runWhenReady(() -> engine.executeScript(
                        "editor.executeEdits('', [{ range: editor.getSelection(), text: " + toJsString(text) + " }])"));
        }

        public void clear()
        {
                runWhenReady(() -> engine.executeScript("editor.getModel().setValue('')"));
        }

        public void setValue(String text)
        {
                runWhenReady(() -> engine.executeScript("""
                        setTimeout(() => {
                            const model = editor.getModel();
                        
                            editor.executeEdits('', [{
                                range: model.getFullModelRange(),
                                text: %s
                            }]);
                        
                            editor.layout();
                        
                            requestAnimationFrame(() => {
                                editor.layout();
                                editor.focus();
                            });
                        }, 0);
                        """.formatted(toJsString(text))));
        }

        public void setWebViewOnKeyPressedEvent(
                EventHandler<? super KeyEvent> value) {
                webView.setOnKeyPressed(value);
        }

        /**
         * 编辑器就绪前先排队，就绪后立即执行，避免在 FX 线程轮询 JS
         */
        private void runWhenReady(Runnable task)
        {
                if (ready)
                        task.run();
                else
                        pendingTasks.add(task);
        }

        private static String toJsString(String str)
        {
                if (str == null)
                        return "''";

                return "'" + str
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\r", "")
                        .replace("\n", "\\n")
                        + "'";
        }

        @Override
        protected void finalize() throws Throwable
        {
                System.out.println("MonacoEditor finalize: " + this);
        }
}
