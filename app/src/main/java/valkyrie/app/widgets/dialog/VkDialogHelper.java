package valkyrie.app.widgets.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.Application;
import valkyrie.app.assets.Assets;
import valkyrie.utils.exception.Causes;
import valkyrie.utils.exception.SystemRuntimeException;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static valkyrie.utils.string.StrStaticImports.fmt;

/**
 * Dialog
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
@SuppressWarnings("DuplicatedCode")
public class VkDialogHelper
{
        private static final Logger LOG = LoggerFactory.getLogger(VkDialogHelper.class);

        public interface DialogCallback {
                void run() throws Throwable;
        }

        public interface DialogReturnValueCallback<T> {
                T run() throws Throwable;
        }

        public static void runWith(DialogCallback callback)
        {
                try {
                        callback.run();
                } catch (Throwable e) {
                        alert(e);
                }
        }

        public static <T> T runWith(DialogReturnValueCallback<T> callback)
        {
                try {
                        return callback.run();
                } catch (Throwable e) {
                        alert(e);
                        throw new SystemRuntimeException(e);
                }
        }

        /**
         * 普通询问提示框
         */
        public static boolean ask(String fmt, Object... args)
        {
                return showDialog(fmt, args);
        }

        /**
         * 危险操作提示框
         */
        public static boolean askDangerous(String fmt, Object... args)
        {
                return showCheckDialog(fmt, args);
        }

        /**
         * 警告提示框
         */
        public static void alert(String fmt, Object... args)
        {
                showAlert(fmt, args);
        }

        /**
         * 异常提示框
         */
        public static void alert(Throwable e)
        {
                LOG.error("error", e);
                alert(Causes.message(e));
        }

        private static void createAndOpenDialog(Stage stage, String message, Node... children)
        {
                Toolkit.getDefaultToolkit().beep();

                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(Application.TITLE);

                Label label = new Label(message);

                HBox labelHBox = new HBox(label);
                labelHBox.setAlignment(Pos.CENTER);
                labelHBox.setPadding(new Insets(20, 0, 0, 0));

                HBox containerHBox = new HBox(children);
                containerHBox.setPadding(new Insets(10));
                containerHBox.setAlignment(Pos.CENTER_RIGHT);
                containerHBox.setSpacing(10);

                BorderPane root = new BorderPane();
                root.setCenter(labelHBox);
                root.setBottom(containerHBox);

                Dimension dimension = new Dimension(message);
                Scene scene = new Scene(root, dimension.width, dimension.height);

                stage.setResizable(false);
                stage.setScene(scene);
                stage.sizeToScene();
                stage.showAndWait();
        }

        private static boolean showCheckDialog(String fmt, Object... args)
        {
                Stage stage = Application.createModalStage();

                AtomicBoolean flag = new AtomicBoolean(true);

                CheckBox checkBox = new CheckBox();
                checkBox.setText("我晓得操作没法恢复！");
                checkBox.setGraphic(Assets.use("warning@2x"));

                Button ok = new Button("确认");
                ok.setDisable(true);
                ok.setDefaultButton(true);
                ok.setOnAction(e -> {
                        flag.set(true);
                        stage.close();
                });

                Button cancel = new Button("取消");
                cancel.setDefaultButton(true);
                cancel.setOnAction(e -> {
                        flag.set(false);
                        stage.close();
                });

                checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        ok.setDisable(!newVal);
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                createAndOpenDialog(stage, fmt(fmt, args), checkBox, spacer, cancel, ok);

                return flag.get();
        }

        private static boolean showDialog(String fmt, Object... args)
        {
                Stage stage = Application.createModalStage();

                AtomicBoolean flag = new AtomicBoolean(true);

                Button ok = new Button("确认");
                ok.setDefaultButton(true);
                ok.setOnAction(e -> {
                        flag.set(true);
                        stage.close();
                });

                Button cancel = new Button("取消");
                cancel.setDefaultButton(true);
                cancel.setOnAction(e -> {
                        flag.set(false);
                        stage.close();
                });

                createAndOpenDialog(stage, fmt(fmt, args), cancel, ok);

                return flag.get();
        }

        private static void showAlert(String fmt, Object... args)
        {
                Stage stage = Application.createModalStage();

                String text = fmt(fmt, args);

                Button copyText = new Button("复制并关闭");
                copyText.setDefaultButton(true);
                copyText.setOnAction(e -> {
                        Application.copyToClipboard(text);
                        stage.close();
                });

                Button ok = new Button("确认");
                ok.setDefaultButton(true);
                ok.setOnAction(e -> stage.close());

                createAndOpenDialog(stage, text, copyText, ok);
        }

}
