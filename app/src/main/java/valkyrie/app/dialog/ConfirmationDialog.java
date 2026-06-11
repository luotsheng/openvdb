package valkyrie.app.dialog;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import valkyrie.app.widgets.dialog.VkDialog;
import valkyrie.app.widgets.dialog.VkDialogStages;

import static valkyrie.utils.string.StrStaticImports.fmt;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class ConfirmationDialog extends VkDialog
{
        private final Stage stage;

        private boolean isOk = false;

        public ConfirmationDialog(Stage stage, String label)
        {
                this.stage = stage;

                VBox topBox = new VBox(new Label(label));
                topBox.setSpacing(10);
                topBox.setPadding(new Insets(20, 10, 5, 10));

                Button ok = new Button("确认");
                ok.setOnAction(e -> save());
                Button cancel = new Button("取消");
                cancel.setOnAction(e -> cancel());
                Region spacer = new Region();
                HBox bottomBox = new HBox(8, spacer, cancel, ok);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                bottomBox.setPadding(new Insets(5, 10, 10, 10));

                setTop(topBox);
                setBottom(bottomBox);
        }

        private void save()
        {
                isOk = true;
                cancel();
        }

        private void cancel()
        {
                stage.close();
        }

        public static boolean showDialog(String label, Object... args)
        {
                Stage stage = VkDialogStages.create();

                ConfirmationDialog dialog = new ConfirmationDialog(stage, fmt(label, args));

                Scene scene = new Scene(dialog, 400, 150);
                stage.setScene(scene);
                stage.showAndWait();

                return dialog.isOk;
        }
}
