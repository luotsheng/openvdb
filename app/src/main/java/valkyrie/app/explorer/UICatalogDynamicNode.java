package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import valkyrie.app.Application;
import valkyrie.app.Publisher;
import valkyrie.app.event.CatalogDynamicNodeInitializedEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkContextMenu;
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
        public VkContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem newQueryItem = new MenuItem("新建查询");
                newQueryItem.setOnAction(e -> Publisher.openQueryEditor());

                MenuItem refreshItem = new MenuItem("刷新");
                refreshItem.setOnAction(e -> refresh());

                MenuItem copyNameItem = new MenuItem("复制名称");
                copyNameItem.setOnAction(e -> Application.copyToClipboard(getLabel()));

                contextMenu.getItems().addAll(
                        openOrCloseMenuItem,
                        newQueryItem,
                        new SeparatorMenuItem(),
                        refreshItem,
                        copyNameItem
                );

                return contextMenu;
        }

        /**
         * 重新加载当前数据库下的对象
         */
        public void refresh()
        {
                if (!initializeChildrenFlag)
                        return;

                unloadNodeData();
                loadNodeData();
        }

        @Override
        public void onContextMenuRequested(ContextMenu contextMenu)
        {
                if (initializeChildrenFlag) {
                        openOrCloseMenuItem.setText("关闭数据库");
                        openOrCloseMenuItem.setOnAction(e -> {
                                onParentCloseEvent();
                                unloadNodeData();
                        });
                } else {
                        openOrCloseMenuItem.setText("打开数据库");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(this::loadNodeData));
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
