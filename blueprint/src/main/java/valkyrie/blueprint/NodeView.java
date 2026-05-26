package valkyrie.blueprint;

import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 节点容器对象
 * <p>
 * 架构设想如下：
 * <pre>
 * NodeView(Pane)
 *     |- NodeModel
 *     |- VBox
 *         |- Component
 *         |- ....
 * </pre>
 *
 * @author Luo Tiansheng
 * @since 2026/5/26
 */
public class NodeView extends Pane
{
        private final VBox vbox = new VBox();

        private final NodeModel model;

        public NodeView(NodeModel model)
        {
                this.model = model;

                getChildren().add(vbox);

                vbox.setStyle("""
                    -fx-border-color: #4a4a4a;
                    -fx-border-radius: 8;
                    -fx-background-radius: 8;
                """);

                vbox.prefWidthProperty().bind(widthProperty());
                vbox.prefHeightProperty().bind(heightProperty());

                updateFromModel();
        }

        public void updateFromModel()
        {
                relocate(model.x, model.y);
                resize(model.w, model.h);
        }
}
