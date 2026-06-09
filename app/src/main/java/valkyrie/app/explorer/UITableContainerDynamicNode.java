package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import valkyrie.app.event.RefreshTableNodeEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.pane.TableListPane;
import valkyrie.app.utils.Threads;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBTableContainerNode;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.collection.Maps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UITableContainerDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("展开列表");

        private final Map<String, UITableDynamicNode> tableDynamicNodes = Maps.newHashMap();

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
                EventBus.openNavigationPane(this, new TableListPane(this));
        }

        @Override
        public void onParentCloseEvent()
        {
                super.onParentCloseEvent();
                EventBus.closeNavigationPane(this);
        }

        @Override
        public UIExplorerNode getPathNode()
        {
                return getExplorerParent();
        }

        public UITableDynamicNode getTableDynamicNode(String tableName)
        {
                return tableDynamicNodes.get(tableName);
        }

        public List<Table> getTables()
        {
                return ((DBTableContainerNode) dbNode).getTables();
        }

        @SuppressWarnings("CodeBlock2Expr")
        public void refresh()
        {
                CompletableFuture
                        .supplyAsync(dbNode::getChildren)
                        /* Java 线程 */
                        .thenAccept(children -> {
                                /* 切换到 Fx 线程 */
                                Platform.runLater(() -> {
                                        runPreservingSelection(() -> {
                                                /* 刷新节点 */
                                                getChildren().clear();
                                                loadDynamicChildren(children);
                                                EventBus.publish(new RefreshTableNodeEvent(this));
                                        });
                                });
                        });
        }
}
