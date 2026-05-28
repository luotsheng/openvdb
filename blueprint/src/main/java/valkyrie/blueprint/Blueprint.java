package valkyrie.blueprint;

import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * 蓝图编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/5/26
 */
@SuppressWarnings("ALL")
public class Blueprint extends StackPane
{
        private final WebView webView = new WebView();
        private final WebEngine engine = webView.getEngine();

        public Blueprint()
        {
                webView.setContextMenuEnabled(false);
                engine.load(getClass().getResource("/static/index.html").toExternalForm());
                getChildren().add(webView);
        }
}
