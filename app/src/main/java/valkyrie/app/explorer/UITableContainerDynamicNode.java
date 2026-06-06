package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.CloseNavigationPaneEvent;
import valkyrie.app.event.workbench.OpenNavigationPaneEvent;
import valkyrie.app.pane.TableListPane;
import valkyrie.app.utils.Threads;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBTableContainerNode;
import valkyrie.utils.collection.Maps;

import java.util.List;
import java.util.Map;

import static valkyrie.utils.string.StaticLibrary.streq;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UITableContainerDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("展开列表");

        private final Map<String, UITableDynamicNode> tableDynamicNodes = Maps.newHashMap();

        private final TableListPane overviewPane = new TableListPane(this);
        private final OpenNavigationPaneEvent openNavigationPaneEvent = new OpenNavigationPaneEvent(this, overviewPane);
        private final CloseNavigationPaneEvent closeNavigationPaneEvent = new CloseNavigationPaneEvent(this);

        public UITableContainerDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                initializeChildrenFlag = true;
                loadDynamicChildren(dbNode.getChildren());
        }

        @Override
        protected List<UIDynamicNode> loadDynamicChildren(List<DBNode> dbNodes)
        {
                List<UIDynamicNode> dynamicNodes = super.loadDynamicChildren(dbNodes);

                for (UIDynamicNode dynamicNode : dynamicNodes) {
                        UITableDynamicNode tableNode = (UITableDynamicNode) dynamicNode;
                        tableDynamicNodes.put(tableNode.getTable().getName(), tableNode);
                }

                return dynamicNodes;
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

        @Override
        public void onSelectedEvent(UIExplorerNode node)
        {
                EventBus.publish(openNavigationPaneEvent);
        }

        public UITableDynamicNode getTableDynamicNode(String tableName)
        {
                return tableDynamicNodes.get(tableName);
        }

        public List<Table> getTables()
        {
                return ((DBTableContainerNode) dbNode).getTables();
        }

        public void refresh()
        {
                /* 记录当前选中节点 */
                var treeView = getRoot().getTreeView();

                UIExplorerNode oldSelectedNode = (UIExplorerNode)
                        treeView.getSelectionModel().getSelectedItem();

                UIExplorerNode newSelectedNode = null;

                /* 刷新节点 */
                getChildren().clear();
                loadDynamicChildren(dbNode.getChildren());

                /* 恢复选中节点 */
                for (TreeItem<String> child : getChildren()) {
                        UIExplorerNode explorerChild = (UIExplorerNode) child;
                        if (streq(explorerChild.getLabel(), oldSelectedNode.getLabel())) {
                                newSelectedNode = explorerChild;
                                break;
                        }
                }

                if (newSelectedNode == null) {
                        treeView.getSelectionModel().select(oldSelectedNode.getExplorerParent());
                } else {
                        treeView.getSelectionModel().select(newSelectedNode);
                }
        }
}
