package valkyrie.app;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.layout.MainLayout;
import valkyrie.utils.Optional;
import valkyrie.utils.io.UFile;
import valkyrie.utils.system.OS;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import static valkyrie.utils.string.StrStaticImports.strrstr;

/**
 * @author Luo Tiansheng
 * @since 2026/3/23
 */
public final class Application extends javafx.application.Application {
        private static final Logger LOG = LoggerFactory.getLogger(Application.class);
        public static final String TITLE = "VALKYRIE v1.6.0";
        public static final ClassLoader classLoader = Application.class.getClassLoader();

        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private static WebView _WarmUp_WebView_ = null;

        public static Stage primaryStage;

        @SuppressWarnings("DataFlowIssue")
        public static URI getResourceURI(String name)
        {
                return Optional.ifError(() -> classLoader.getResource(name).toURI(), null);
        }

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

        private void warmUp()
        {
                _WarmUp_WebView_ = new WebView();
        }

        @Override
        public void start(Stage stage) {
                warmUp();

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
                URI uri = getResourceURI("css");
                UFile cssDir = new UFile(uri);

                for (UFile cssFile : Objects.requireNonNull(cssDir.listFiles())) {
                        String path = cssFile.getPath();
                        int rIndex = strrstr(path, "/", 2);
                        path = path.substring(rIndex);

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