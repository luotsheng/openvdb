package valkyrie.app.tool;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenScriptEditorPaneEvent;
import valkyrie.app.event.workbench.OpenTabEvent;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.menu.ConnectionMenuBuilder;
import valkyrie.app.model.UIExplorerStatus;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparator;
import valkyrie.blueprint.Blueprint;
import valkyrie.utils.Generator;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class AppToolBar extends ToolBar
{
        public AppToolBar()
        {
                Button newConnectionButton = new VkIconButton(null, "新建连接", "chain");
                ContextMenu contextMenu = ConnectionMenuBuilder.buildContextMenu();
                newConnectionButton.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                                contextMenu.show(newConnectionButton,
                                        event.getScreenX(),
                                        event.getScreenY());
                        }
                });

                Button newQueryButton = new VkIconButton("查询", "sql");
                newQueryButton.setText("新建查询");
                newQueryButton.setOnAction(event -> newScriptEditor());

                Button debugButton = new VkIconButton("Debug", "code");
                debugButton.setText("Debug Pane");
                debugButton.setOnAction(event -> debugPane());

                getItems().addAll(
                        newConnectionButton,
                        newQueryButton,
                        new VkSeparator(),
                        debugButton
                );
        }

        private void newScriptEditor()
        {
                UIExplorerStatus instance = UIExplorerStatus.getInstance();
                UIConnectionNode selectedConnection = instance.getSelectedConnection();
                EventBus.publish(new OpenScriptEditorPaneEvent(null, selectedConnection));
        }

        private void debugPane()
        {
                EventBus.publish(new OpenTabEvent(null)
                {
                        @Override
                        public String tabId()
                        {
                                return "Debug-" + Generator.randomCode(6);
                        }

                        @Override
                        public Node createPane(Tab tab)
                        {
                                return new Blueprint();
                        }
                });
        }
}
