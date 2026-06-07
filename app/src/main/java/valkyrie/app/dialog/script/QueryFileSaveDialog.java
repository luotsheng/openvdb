package valkyrie.app.dialog.script;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.explorer.UICatalogDynamicNode;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.explorer.UISchemaDynamicNode;
import valkyrie.app.widgets.VkComboBox;
import valkyrie.app.workbench.PathSelector;

/**
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
@SuppressWarnings("FieldCanBeLocal")
public class QueryFileSaveDialog extends BorderPane
{
        private final Stage stage;
        private final TextField textField;
        private final PathSelector pathSelector = new PathSelector();
        private final VkComboBox<UIConnectionNode> connectionComboBox;
        private final VkComboBox<UICatalogDynamicNode> catalogComboBox;
        private final VkComboBox<UISchemaDynamicNode> schemaComboBox;

        private boolean isOk = false;

        public QueryFileSaveDialog(Stage stage)
        {
                this.stage = stage;

                Label title = new Label("查询名称：");
                textField = new TextField();
                textField.setPromptText("输入查询名称...");
                Label savePath = new Label("保存位置：");

                connectionComboBox = pathSelector.getConnectionComboBox();
                catalogComboBox = pathSelector.getCatalogComboBox();
                schemaComboBox = pathSelector.getSchemaComboBox();

                connectionComboBox.setPrefWidth(Double.MAX_VALUE);
                catalogComboBox.setPrefWidth(Double.MAX_VALUE);
                schemaComboBox.setPrefWidth(Double.MAX_VALUE);

                VBox topBox = new VBox(title, textField, savePath, connectionComboBox, catalogComboBox, schemaComboBox);
                topBox.setSpacing(10);
                topBox.setPadding(new Insets(20, 10, 5, 10));

                Button ok = new Button("保存");
                ok.setOnAction(e -> {
                        isOk = true;
                        cancel();
                });

                Button cancel = new Button("取消");
                cancel.setOnAction(e -> cancel());
                Region spacer = new Region();
                HBox bottomBox = new HBox(8, spacer, cancel, ok);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                bottomBox.setPadding(new Insets(5, 10, 10, 10));

                setTop(topBox);
                setBottom(bottomBox);
        }

        private void cancel()
        {
                stage.close();
        }

        private String buildPath()
        {
                UIConnectionNode connectionNode = connectionComboBox.getSelectionModel().getSelectedItem();
                UICatalogDynamicNode catalogDynamicNode = catalogComboBox.getSelectionModel().getSelectedItem();
                UISchemaDynamicNode schemaDynamicNode = schemaComboBox.getSelectionModel().getSelectedItem();

                StringBuilder pathBuilder = new StringBuilder("/");
                pathBuilder.append(connectionNode.getLabel());

                if (catalogDynamicNode != null) {
                        pathBuilder.append("/").append(catalogDynamicNode.getLabel());
                }

                if (schemaDynamicNode != null) {
                        pathBuilder.append("/").append(schemaDynamicNode.getLabel());
                }

                return pathBuilder + "/" + textField.getText();
        }

        /**
         * @return 返回用户输入的脚本名称， {@code null} 表示用户取消保存
         */
        public static String showDialog()
        {
                Stage stage = Application.createByPrimaryStage();
                stage.initModality(Modality.APPLICATION_MODAL);

                QueryFileSaveDialog dialog = new QueryFileSaveDialog(stage);

                Platform.runLater(() -> {});
                Scene scene = new Scene(dialog, 600, 300);
                stage.setScene(scene);
                stage.showAndWait();

                if (dialog.isOk)
                        return dialog.buildPath();

                return null;
        }

}
