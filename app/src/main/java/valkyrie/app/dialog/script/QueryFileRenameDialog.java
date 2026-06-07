package valkyrie.app.dialog.script;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.core.model.ScriptFile;
import valkyrie.core.repository.ScriptFileRepository;

/**
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
@SuppressWarnings("FieldCanBeLocal")
public class QueryFileRenameDialog extends BorderPane
{
        private final Stage stage;
        private final TextField textField;

        private ScriptFile scriptFile;

        public QueryFileRenameDialog(Stage stage, ScriptFile scriptFile)
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
                ScriptFile newScriptFile = scriptFile;

                newScriptFile = ScriptFileRepository.rename(
                        scriptFile, textField.getText()
                );

                scriptFile = newScriptFile;

                EventBus.publish(new RefreshQueryNodeEvent());

                cancel();
        }

        private void cancel()
        {
                stage.close();
        }

        public static void showDialog(ScriptFile scriptFile)
        {
                Stage stage = Application.createByPrimaryStage();

                QueryFileRenameDialog dialog = new QueryFileRenameDialog(stage, scriptFile);

                Scene scene = new Scene(dialog, 400, 150);
                stage.setScene(scene);
                stage.showAndWait();
        }

}
