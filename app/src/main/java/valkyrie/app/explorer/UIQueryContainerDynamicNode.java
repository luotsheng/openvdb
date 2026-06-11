package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import valkyrie.app.Publisher;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.pane.QueryListPane;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.core.model.QueryFile;
import valkyrie.core.repository.QueryFileRepository;
import valkyrie.driver.api.node.DBNode;
import valkyrie.utils.collection.Lists;

import java.io.File;
import java.util.List;

/**
 * QueryContainer 容器较为特殊，该节点不通过驱动提供的能力
 * 加载查询脚本文件节点，而是通过自身 reloadQueryNode() 函数
 * 加载驱动脚本
 *
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UIQueryContainerDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("展开列表");

        public UIQueryContainerDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                reloadQueryNode();
        }

        @Override
        public VkContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem newQueryEditorItem =  new MenuItem("新建查询");
                newQueryEditorItem.setOnAction(e -> Publisher.openQueryEditor());

                MenuItem refreshItem = new MenuItem("刷新列表");
                refreshItem.setOnAction(e -> refreshQueryNode());

                contextMenu.getItems().addAll(
                        newQueryEditorItem,
                        new SeparatorMenuItem(),
                        openOrCloseMenuItem,
                        refreshItem
                );

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
        public void onParentCloseEvent()
        {
                super.onParentCloseEvent();
                EventBus.closeNavigationPane(this);
        }

        @Override
        public void onSelectedEvent(UIExplorerNode node)
        {
                EventBus.openNavigationPane(this, new QueryListPane(this));
        }

        private void reloadQueryNode()
        {
                var queryFiles = QueryFileRepository.loadScriptFiles(new File(getPath()).getParent());
                List<UIQueryDynamicNode> nodes = Lists.newArrayList();

                getChildren().clear();

                for (QueryFile queryFile : queryFiles) {
                        UIQueryDynamicNode queryDynamicNode = new UIQueryDynamicNode(this, queryFile);
                        nodes.add(queryDynamicNode);
                }

                getChildren().addAll(nodes);

                initializeChildrenFlag = true;
                EventBus.publish(new RefreshQueryNodeEvent(this));
        }

        void refreshQueryNode()
        {
                runPreservingSelection(this::reloadQueryNode);
        }

        @Override
        public UIExplorerNode getPathNode()
        {
                return getExplorerParent();
        }

        /**
         * UIQueryDynamicNode 内部文件对象可能被外部修改，或用户重命名，
         * 如果缓存 UIQueryDynamicNode 集合可能会导致意想不到的结果。
         */
        private List<UIQueryDynamicNode> getChildrenAsQueryDynamicNode()
        {
                return getChildren().stream()
                        .map(t -> ((UIQueryDynamicNode) t))
                        .toList();
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        public UIQueryDynamicNode getQueryDynamicNode(QueryFile queryFile)
        {
                return getChildrenAsQueryDynamicNode().stream()
                        .filter(t -> t.getQueryFile() == queryFile)
                        .findFirst()
                        .get();
        }

        public List<QueryFile> getQueryFiles()
        {
                return getChildrenAsQueryDynamicNode().stream()
                        .map(UIQueryDynamicNode::getQueryFile)
                        .toList();
        }
}
