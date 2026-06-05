package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UISchemaDynamicNode extends UIDynamicNode
{
        public UISchemaDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();
                MenuItem openDatabaseItem = new MenuItem("打开模式");
                openDatabaseItem.setOnAction(event -> expand());
                MenuItem closeDatabaseItem = new MenuItem("关闭模式");
                closeDatabaseItem.setOnAction(event -> unexpand());
                contextMenu.getItems().addAll(openDatabaseItem, closeDatabaseItem);
                return contextMenu;
        }
}
