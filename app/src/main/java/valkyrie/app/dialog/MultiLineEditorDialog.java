package valkyrie.app.dialog;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import valkyrie.app.Application;
import valkyrie.app.theme.Stylesheets;

/**
 * 多行文本编辑弹窗。
 * <p>
 * 表格单元格内容含换行时，内联单行编辑器不方便，改为弹出该编辑框。
 *
 * @author Luo Tiansheng
 * @since 2026/9/12
 */
public class MultiLineEditorDialog
{
        private String result;

        private MultiLineEditorDialog(String initial, Stage stage)
        {
                TextArea area = new TextArea(initial);
                area.setWrapText(true);

                Button ok = new Button("确定");
                ok.setDefaultButton(true);
                ok.setOnAction(event -> {
                        result = area.getText();
                        stage.close();
                });

                Button cancel = new Button("取消");
                cancel.setCancelButton(true);
                cancel.setOnAction(event -> {
                        result = null;
                        stage.close();
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox bottom = new HBox(8, spacer, cancel, ok);
                bottom.setPadding(new Insets(10));

                BorderPane root = new BorderPane();
                root.setCenter(area);
                root.setBottom(bottom);
                BorderPane.setMargin(area, new Insets(10, 10, 0, 10));

                Scene scene = new Scene(root, 560, 360);
                Stylesheets.apply(scene);

                stage.setTitle("编辑文本");
                stage.setResizable(true);
                stage.setScene(scene);
        }

        /**
         * @return 编辑后的文本；用户取消时返回 {@code null}
         */
        public static String showDialog(String initial)
        {
                Stage stage = Application.createModalStage();

                MultiLineEditorDialog dialog = new MultiLineEditorDialog(initial, stage);
                stage.showAndWait();

                return dialog.result;
        }
}
