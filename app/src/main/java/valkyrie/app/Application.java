package valkyrie.app;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.layout.MainLayout;
import valkyrie.utils.system.OS;

import java.awt.*;
import java.net.URL;
import java.util.Objects;

/**
 * @author Luo Tiansheng
 * @since 2026/3/23
 */
public final class Application extends javafx.application.Application {
        private static final Logger LOG = LoggerFactory.getLogger(Application.class);
        public static final String TITLE = "VALKYRIE v1.6.0";

        public static Stage primaryStage;

        public static void copyToClipboard(String text) {
                Platform.runLater(() -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(text);
                        clipboard.setContent(content);
                });
        }

        public static String getClipboardText() {
                return Clipboard.getSystemClipboard().getString();
        }

        public static Stage createModalStage() {
                if (primaryStage == null)
                        throw new IllegalStateException("primaryStage not initialized, you called too early");
                Stage stage = new Stage();
                stage.initOwner(primaryStage);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.centerOnScreen();
                return stage;
        }

        @Override
        public void start(Stage stage) {
                primaryStage = stage;
                setDockIcon(stage);

                setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
                Scene scene = new Scene(new MainLayout(), 1200, 800);
                addStylesheets(scene);
                stage.setTitle(TITLE);
                stage.setScene(scene);
                stage.setMaximized(true);

                stage.show();
        }

        private void addStylesheets(Scene scene) {
                String[] sheets = {
                        "/css/vk-theme-root.css",
                        "/css/vk-theme-menu.css",
                        "/css/vk-list-cell.css",
                        "/css/vk-table-view.css",
                        "/css/vk-icon-button.css",
                        "/css/vk-code-area.css",
                        "/css/vk-status-bar.css",
                        "/css/vk-tool-bar.css"
                };

                for (String path : sheets) {
                        URL url = getClass().getResource(path);
                        if (url != null) {
                                scene.getStylesheets().add(url.toExternalForm());
                        } else {
                                LOG.warn("Stylesheet not found: {}", path);
                        }
                }
        }

        private void setDockIcon(Stage stage) {
                final String iconPath = "/assets/icons/main_2.png";

                if (OS.isWindows()) {
                        var icon = new javafx.scene.image.Image(
                                Objects.requireNonNull(getClass().getResourceAsStream(iconPath),
                                        "Icon resource missing: " + iconPath));
                        stage.getIcons().add(icon);
                }

                if (!Taskbar.isTaskbarSupported())
                        return;

                Taskbar taskbar = Taskbar.getTaskbar();
                if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE))
                        return;

                try {
                        Image image = Toolkit.getDefaultToolkit().getImage(
                                Objects.requireNonNull(getClass().getResource("/assets/icons/main_2.png")));
                        taskbar.setIconImage(image);
                } catch (Exception e) {
                        LOG.error("Failed to set dock icon", e);
                }
        }

        public static void start() {
                launch();
        }
}