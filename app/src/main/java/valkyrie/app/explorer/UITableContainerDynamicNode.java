package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UITableContainerDynamicNode extends UIDynamicNode
{
        public UITableContainerDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                initializeChildrenFlag = true;
                loadDynamicChildren(dbNode.getChildren());
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();
                MenuItem openDatabaseItem = new MenuItem("展开列表");
                openDatabaseItem.setOnAction(event -> expand(false));
                MenuItem closeDatabaseItem = new MenuItem("收起列表");
                closeDatabaseItem.setOnAction(event -> unexpand());
                contextMenu.getItems().addAll(openDatabaseItem, closeDatabaseItem);
                return contextMenu;
        }
}
