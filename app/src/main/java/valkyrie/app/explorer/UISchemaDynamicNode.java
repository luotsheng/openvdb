package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UISchemaDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("打开模式");

        public UISchemaDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
        }

        @Override
        public VkContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();
                contextMenu.getItems().addAll(openOrCloseMenuItem);
                return contextMenu;
        }

        @Override
        public void onContextMenuRequested(ContextMenu contextMenu)
        {
                if (initializeChildrenFlag) {
                        openOrCloseMenuItem.setText("关闭模式");
                        openOrCloseMenuItem.setOnAction(e -> {
                                onParentCloseEvent();
                                unloadNodeData();
                        });
                } else {
                        openOrCloseMenuItem.setText("打开模式");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(this::loadNodeData));
                }
        }

        public UIQueryContainerDynamicNode getQueryContainerNode()
        {
                for (TreeItem<String> child : getChildren()) {
                        if (child instanceof UIQueryContainerDynamicNode node)
                                return node;
                }

                return null;
        }
}
