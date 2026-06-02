package valkyrie.app;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.layout.MainLayout;
import valkyrie.utils.system.OS;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Luo Tiansheng
 * @since 2026/3/23
 */
public class Application extends javafx.application.Application
{
        private static final Logger LOG = LoggerFactory.getLogger(Application.class);

        public static final String TITLE = "VALKYRIE v1.0.0-arch.1";

        private static final Class<Application> aClass = Application.class;
        private static final List<LauncherTask> tasks = new ArrayList<>();

        @Getter
        private static Stage primaryStage = null;

        public interface LauncherTask
        {
                void run(Stage stage, Scene scene);
        }

        public static void runLater(LauncherTask runnable)
        {
                tasks.add(runnable);
        }

        public static void copyToClipboard(String text)
        {
                Platform.runLater(() -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent clipboardContent = new ClipboardContent();
                        clipboardContent.putString(text);
                        clipboard.setContent(clipboardContent);
                });
        }

        public static String getClipboardText()
        {
                return Clipboard.getSystemClipboard().getString();
        }

        private static void addStylesheet(Scene scene, String path)
        {
                URL url = aClass.getResource(path);
                ObservableList<String> stylesheets = scene.getStylesheets();

                if (url != null)
                        stylesheets.add(url.toExternalForm());

        }

        public static Stage createByPrimaryStage()
        {
                Stage stage = new Stage();
                
                stage.initOwner(primaryStage);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.centerOnScreen();

                return stage;
        }

        static void initialize(Scene scene)
        {
                addStylesheet(scene, "/css/vk-table-view.css");
                addStylesheet(scene, "/css/vk-icon-button.css");
                addStylesheet(scene, "/css/vk-code-area.css");
                addStylesheet(scene, "/css/vk-status-bar.css");
        }

        @Override
        @SuppressWarnings("CommentedOutCode")
        public void start(Stage stage)
        {
                new Thread(() -> Platform.runLater(WebView::new)).start();

                setDockIcon(stage, "/assets/icons/main_2.png");

                primaryStage = stage;

                javafx.application.Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
                Scene scene = new Scene(new MainLayout(), 1200, 800);
                initialize(scene);
                stage.setTitle(TITLE);
                stage.setScene(scene);
                stage.setMaximized(true);

                // ObservableList<Screen> screens = Screen.getScreens();
                //
                // if (screens.size() > 1) {
                //         Screen second = screens.get(1);
                //         Rectangle2D bounds = second.getVisualBounds();
                //         stage.setX(bounds.getMinX());
                //         stage.setY(bounds.getMinY());
                // }

                tasks.forEach(task -> task.run(stage, scene));

                stage.show();
        }

        public static void start()
        {
                launch();
        }

        public static void setDockIcon(Stage stage, String iconPath)
        {
                try {
                        // Windows
                        if (OS.isWindows()) {
                                var icon = new javafx.scene.image.Image(Objects.requireNonNull(
                                        Application.class.getResourceAsStream(iconPath)));
                                stage.getIcons().add(icon);
                        }

                        // MacOS
                        if (!Taskbar.isTaskbarSupported())
                                return;

                        Taskbar taskbar = Taskbar.getTaskbar();

                        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE))
                                return;

                        Image image = Toolkit.getDefaultToolkit().getImage(
                                Application.class.getResource(iconPath));

                        taskbar.setIconImage(image);
                } catch (Exception e) {
                        LOG.error("set dock icon failed", e);
                }
        }
}
