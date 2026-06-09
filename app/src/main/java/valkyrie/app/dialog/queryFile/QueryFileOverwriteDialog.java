package valkyrie.app.dialog.queryFile;

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

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class QueryFileOverwriteDialog extends VkDialog
{
        private final Stage stage;

        private boolean isOk = false;

        public QueryFileOverwriteDialog(Stage stage)
        {
                this.stage = stage;

                VBox topBox = new VBox(new Label("已存在相同名称的查询脚本，是否覆盖？"));
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

        public static boolean showDialog()
        {
                Stage stage = VkDialogStages.create();

                QueryFileOverwriteDialog dialog = new QueryFileOverwriteDialog(stage);

                Scene scene = new Scene(dialog, 400, 150);
                stage.setScene(scene);
                stage.showAndWait();

                return dialog.isOk;
        }
}
