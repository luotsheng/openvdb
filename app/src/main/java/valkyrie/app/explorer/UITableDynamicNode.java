package valkyrie.app.explorer;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.Getter;
import valkyrie.app.Application;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.CloseWorkbenchTabEvent;
import valkyrie.app.event.workbench.OpenTableDataPaneEvent;
import valkyrie.app.event.workbench.OpenTableDesignerPaneEvent;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBTableContainerNode;
import valkyrie.driver.api.node.DBTableNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UITableDynamicNode extends UIDynamicNode
{
        private final UITableContainerDynamicNode tableContainerDynamicNode;
        private final @Getter Session session;

        public UITableDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                this.tableContainerDynamicNode = (UITableContainerDynamicNode) parent;
                this.session = ((DBTableContainerNode) dbNode.getParent()).getSession();
        }

        @Override
        public VkContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem openMenuItem = new MenuItem("打开表");
                openMenuItem.setOnAction(e -> openDataPane());

                MenuItem designMenuItem = new MenuItem("设计表");
                designMenuItem.setOnAction(e -> openDesignTablePane());

                MenuItem copyTableNameItem = new MenuItem("复制表名");
                copyTableNameItem.setOnAction(event -> Application.copyToClipboard(getTable().getName()));

                MenuItem copyCreateTableDLLItem = new MenuItem("复制建表语句");
                copyCreateTableDLLItem.setOnAction(event -> Application.copyToClipboard(
                        getDriver().showCreateTable(session, getTable().getName())));

                MenuItem refreshTableItem = new MenuItem("刷新列表");
                refreshTableItem.setOnAction(e -> tableContainerDynamicNode.refresh());

                contextMenu.getItems().addAll(
                        openMenuItem,
                        designMenuItem,
                        new SeparatorMenuItem(),
                        copyTableNameItem,
                        copyCreateTableDLLItem,
                        new SeparatorMenuItem(),
                        refreshTableItem
                );

                return contextMenu;
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                openDataPane();
        }

        @Override
        public void onSelectedEvent(UIExplorerNode node)
        {
                getExplorerParent().onSelectedEvent(node);
        }

        @Override
        public void onParentCloseEvent()
        {
                EventBus.publish(new CloseWorkbenchTabEvent(this));
        }

        public UIExplorerNode getPathNode()
        {
                /* 第一个父节点是 TableContainer 容器节点 */
                return getExplorerParent().getExplorerParent();
        }

        public Table getTable()
        {
                return ((DBTableNode) dbNode).getTable();
        }

        public void openDataPane()
        {
                EventBus.publish(new OpenTableDataPaneEvent(this));
        }

        public void openDesignTablePane()
        {
                EventBus.publish(new OpenTableDesignerPaneEvent(this));
        }
}
