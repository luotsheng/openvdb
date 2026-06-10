package valkyrie.app.tool;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseButton;
import valkyrie.app.Publisher;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenTabEvent;
import valkyrie.app.menu.ConnectionMenuBuilder;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparatorItem;
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
                VkContextMenu contextMenu = ConnectionMenuBuilder.buildContextMenu();
                newConnectionButton.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                                contextMenu.show(event.getScreenX(), event.getScreenY());
                        }
                });

                Button newQueryButton = new VkIconButton("查询", "sql");
                newQueryButton.setText("新建查询");
                newQueryButton.setOnAction(e -> newQueryEditor());

                Button debugButton = new VkIconButton("Debug", "code");
                debugButton.setText("Debug Pane");
                debugButton.setOnAction(event -> debugPane());

                getItems().addAll(
                        newConnectionButton,
                        newQueryButton,
                        new VkSeparatorItem(),
                        debugButton
                );
        }

        private void newQueryEditor()
        {
                Publisher.openQueryEditor();
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
