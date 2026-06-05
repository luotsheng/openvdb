package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UITableContainerDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("展开列表");

        public UITableContainerDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                initializeChildrenFlag = true;
                loadDynamicChildren(dbNode.getChildren());
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.getItems().addAll(openOrCloseMenuItem);
                return contextMenu;
        }

        @Override
        public void onContextMenuRequested(ContextMenu contextMenu)
        {
                if (isExpanded()) {
                        openOrCloseMenuItem.setText("收起列表");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(() -> setExpanded(false)));
                } else {
                        openOrCloseMenuItem.setText("展开列表");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(() -> setExpanded(true)));
                }
        }
}
