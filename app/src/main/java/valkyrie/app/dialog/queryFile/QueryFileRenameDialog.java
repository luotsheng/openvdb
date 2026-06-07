package valkyrie.app.dialog.queryFile;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.widgets.dialog.VkDialog;
import valkyrie.app.widgets.dialog.VkDialogStages;
import valkyrie.core.model.QueryFile;
import valkyrie.core.repository.QueryFileRepository;

/**
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
@SuppressWarnings("FieldCanBeLocal")
public class QueryFileRenameDialog extends VkDialog
{
        private final Stage stage;
        private final TextField textField;

        private QueryFile scriptFile;

        public QueryFileRenameDialog(Stage stage, QueryFile scriptFile)
        {
                this.stage = stage;
                this.scriptFile = scriptFile;

                Label title = new Label("查询名称：");
                textField = new TextField(scriptFile.getName());
                textField.setPromptText("输入查询名称...");
                VBox topBox = new VBox(title, textField);
                topBox.setSpacing(10);
                topBox.setPadding(new Insets(20, 10, 5, 10));

                Button ok = new Button("保存");
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
                QueryFile newScriptFile = new QueryFile(scriptFile.getParentFile(), textField.getText());

                if (QueryFileConfirmOverwriteDialog.showDialog(stage, newScriptFile)) {
                        QueryFileRepository.rename(
                                scriptFile, textField.getText()
                        );

                        scriptFile = newScriptFile;

                        EventBus.publish(new RefreshQueryNodeEvent());
                }

                cancel();
        }

        private void cancel()
        {
                stage.close();
        }

        public static void showDialog(QueryFile scriptFile)
        {
                Stage stage = VkDialogStages.create();

                QueryFileRenameDialog dialog = new QueryFileRenameDialog(stage, scriptFile);

                Scene scene = new Scene(dialog, 400, 150);
                stage.setScene(scene);
                stage.showAndWait();
        }
}
