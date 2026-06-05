package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UICatalogDynamicNode extends UIDynamicNode
{
        public UICatalogDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();
                MenuItem openDatabaseItem = new MenuItem("打开数据库");
                openDatabaseItem.setOnAction(event -> expand());
                MenuItem closeDatabaseItem = new MenuItem("关闭数据库");
                closeDatabaseItem.setOnAction(event -> unexpand());
                contextMenu.getItems().addAll(openDatabaseItem, closeDatabaseItem);
                return contextMenu;
        }
}
