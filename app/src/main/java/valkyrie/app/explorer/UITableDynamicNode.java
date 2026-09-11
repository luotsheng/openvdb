package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.Getter;
import valkyrie.app.Application;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.CloseWorkbenchTabEvent;
import valkyrie.app.event.workbench.OpenTableDataPaneEvent;
import valkyrie.app.event.workbench.OpenTableDesignerPaneEvent;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.driver.api.DbType;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBTableContainerNode;
import valkyrie.driver.api.node.DBTableNode;
import valkyrie.driver.api.sql.SQL;

import static valkyrie.utils.string.StrStaticImports.fmt;

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

                MenuItem copySelectItem = new MenuItem("复制查询语句");
                copySelectItem.setOnAction(e -> copySelectSQL());

                MenuItem clearTableItem = new MenuItem("清空表");
                clearTableItem.setOnAction(e -> clearTable());

                MenuItem dropTableItem = new MenuItem("删除表");
                dropTableItem.setOnAction(e -> dropTable());

                MenuItem refreshTableItem = new MenuItem("刷新列表");
                refreshTableItem.setOnAction(e -> tableContainerDynamicNode.refresh());

                contextMenu.getItems().addAll(
                        openMenuItem,
                        designMenuItem,
                        new SeparatorMenuItem(),
                        copyTableNameItem,
                        copyCreateTableDLLItem,
                        copySelectItem,
                        new SeparatorMenuItem(),
                        clearTableItem,
                        dropTableItem,
                        new SeparatorMenuItem(),
                        refreshTableItem
                );

                return contextMenu;
        }

        private void copySelectSQL()
        {
                String quoted = getDriver().getDialect().quote(getTable().getName());
                Application.copyToClipboard(fmt("SELECT * FROM %s;", quoted));
        }

        private void clearTable()
        {
                if (!VkDialogHelper.askDangerous("确定清空表 %s 的全部数据？此操作不可恢复！", getTable().getName()))
                        return;

                useProgressIndicator(() -> {
                        String quoted = getDriver().getDialect().quote(getTable().getName());
                        String sql = getDriver().getType() == DbType.sqlite
                                ? "DELETE FROM " + quoted
                                : "TRUNCATE TABLE " + quoted;

                        getDriver().execute(session, new SQL(sql));
                        Platform.runLater(tableContainerDynamicNode::refresh);
                });
        }

        public void dropTable()
        {
                if (!VkDialogHelper.askDangerous("确定删除表 %s？此操作不可恢复！", getTable().getName()))
                        return;

                useProgressIndicator(() -> {
                        getDriver().dropTable(session, getTable().getName());

                        Platform.runLater(() -> {
                                EventBus.publish(new CloseWorkbenchTabEvent(this));
                                tableContainerDynamicNode.refresh();
                        });
                });
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
