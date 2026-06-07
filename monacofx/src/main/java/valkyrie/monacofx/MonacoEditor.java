package valkyrie.monacofx;

import com.alibaba.fastjson.JSONObject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
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
        private final WebView webView = new WebView();
        private final WebEngine engine = webView.getEngine();
        private final MonacoHook hook = new MonacoHook(this);
        private final PauseTransition pauseTransition = new PauseTransition(Duration.millis(500));
        private ContextMenu contextMenu = null;

        @Setter
        private ShowContextMenuRequestEvent showContextMenuRequestEvent = null;

        @Setter
        private OnDidChangeModelContent onDidChangeModelContent = null;

        public interface ShowContextMenuRequestEvent {
                void onRequest(ContextMenu contextMenu);
        }

        public interface OnDidChangeModelContent {
                void onChange();
        }

        @SuppressWarnings("DataFlowIssue")
        public MonacoEditor()
        {
                webView.setContextMenuEnabled(false);
                engine.load(getClass().getResource("/static/editor.html").toExternalForm());
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

                setHook();

                webView.prefWidthProperty().bind(this.widthProperty());
                webView.prefHeightProperty().bind(this.heightProperty());
        }

        public void dispose()
        {
                waitAndRun(() -> engine.executeScript("""
                        try {
                                const model =
                                        editor.getModel();
                                        
                                if (model)
                                        model.dispose();
                                        
                                editor.dispose();
                        } catch (e) {
                                console.log(e);
                        }
                        """));

                engine.getLoadWorker().cancel();
                engine.loadContent("");

                engine.executeScript("""
                        document.body.innerHTML = '';
                        window.close();
                        """);

                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("hook", null);

                System.gc();
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
                public void println(Object message)
                {
                        System.out.println(message);
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
        }

        private void setHook()
        {
                waitAndRun(() -> {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("hook", hook);
                        engine.executeScript(
                                """
                                   console.log = function(message) {
                                       window.hook.println(message);
                                   };
                                   
                                   window.writeClipboard = function(message) {
                                       window.hook.writeClipboard(message);
                                   };
                                   """
                        );
                });
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
                waitAndRun(() -> {
                        engine.executeScript("window.addSuggestions(" + JSONObject.toJSONString(suggestions) + ")");
                });
        }

        public String getValue()
        {
                return (String) engine.executeScript("editor.getValue()");
        }

        public String getSelectedValue()
        {
                return (String) engine.executeScript(
                        "window.editor.getModel().getValueInRange(editor.getSelection())"
                );
        }

        public void replaceSelection(String text)
        {
                waitAndRun(() -> {
                        engine.executeScript(
                                "editor.executeEdits('', [{ range: editor.getSelection(), text: " + toJsString(text) + " }])"
                        );
                });
        }

        public void clear()
        {
                waitAndRun(() -> engine.executeScript("editor.getModel().setValue('')"));
        }

        public void setValue(String text)
        {
                waitAndRun(() -> {
                        engine.executeScript("""
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
                                """.formatted(toJsString(text)));
                });
        }

        public void setWebViewOnKeyPressedEvent(
                EventHandler<? super KeyEvent> value) {
                webView.setOnKeyPressed(value);
        }

        private void waitAndRun(Runnable task)
        {
                Platform.runLater(() -> {
                        Object ready = engine.executeScript(
                                "window.editor && window.editor.getModel && window.editor.getModel() !== null"
                        );

                        if (Boolean.TRUE.equals(ready)) {
                                task.run();
                        } else {
                                waitAndRun(task);
                        }
                });
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
}
