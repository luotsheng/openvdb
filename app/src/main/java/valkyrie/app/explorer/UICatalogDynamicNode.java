package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import valkyrie.app.event.CatalogDynamicNodeInitializedEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.utils.Threads;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UICatalogDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("打开数据库");

        public UICatalogDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
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
                if (initializeChildrenFlag) {
                        openOrCloseMenuItem.setText("关闭数据库");
                        openOrCloseMenuItem.setOnAction(e -> {
                                onParentCloseEvent();
                                unexpand();
                        });
                } else {
                        openOrCloseMenuItem.setText("打开数据库");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(this::expand));
                }
        }

        @Override
        public void onInitializedEvent()
        {
                EventBus.publish(new CatalogDynamicNodeInitializedEvent(this));
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
