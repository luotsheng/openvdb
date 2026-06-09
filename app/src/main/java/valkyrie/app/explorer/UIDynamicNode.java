package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.node.*;
import valkyrie.utils.collection.Lists;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class UIDynamicNode extends UIExplorerNode
{
        private final UIExplorerNode parent;
        protected final DBNode dbNode;
        protected boolean initializeChildrenFlag = false;

        protected UIDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode.getLabel(), dbNode.getKind().getIcon());
                this.parent = parent;
                this.dbNode = dbNode;
        }

        public static UIDynamicNode create(UIExplorerNode parent, DBNode dbNode)
        {
                return switch (dbNode) {
                        case DBCatalogNode ignored -> new UICatalogDynamicNode(parent, dbNode);
                        case DBSchemaNode ignored -> new UISchemaDynamicNode(parent, dbNode);
                        case DBTableContainerNode ignored -> new UITableContainerDynamicNode(parent, dbNode);
                        case DBTableNode ignored -> new UITableDynamicNode(parent, dbNode);
                        case DBQueryContainerNode ignored -> new UIQueryContainerDynamicNode(parent, dbNode);
                        default -> new UIDynamicNode(parent, dbNode);
                };
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                if (initializeChildrenFlag)
                        return;
                this.loadNodeData();
                setExpanded(true);
        }

        protected void loadNodeData()
        {
                useProgressIndicator(() -> {
                        List<DBNode> children = Lists.of();
                        if (dbNode.hasChildren())
                                children.addAll(dbNode.getChildren());

                        if (!children.isEmpty()) {
                                loadDynamicChildren(children);
                                initializeChildrenFlag = true;
                                onInitializedEvent();
                        }
                });
        }

        protected void unloadNodeData()
        {
                if (!initializeChildrenFlag)
                        return;

                getChildren().clear();
                initializeChildrenFlag = false;
        }

        protected void runPreservingSelection(Runnable action)
        {
                TreeView<String> treeView = getRoot().getTreeView();

                UIExplorerNode selected =
                        (UIExplorerNode) treeView.getSelectionModel().getSelectedItem();

                var children = selected.getParent().getChildren();
                int nodeIndex = children.indexOf(selected);

                action.run();

                if (children.isEmpty()) {
                        treeView.getSelectionModel().select(parent);
                        return;
                }

                if (nodeIndex >= children.size()) {
                        treeView.getSelectionModel().select(children.get(nodeIndex - 1));
                        return;
                }

                treeView.getSelectionModel().select(children.get(nodeIndex));
        }

        public void initialize()
        {
                // 模拟点击
                onMouseDoubleClickEvent();
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isInitialized()
        {
                return initializeChildrenFlag;
        }

        public Driver getDriver()
        {
                return getRoot().getDriver();
        }

        /**
         * 返回最上层路径节点，如（Schema，Catalog）
         */
        public UIExplorerNode getPathNode()
        {
                return this;
        }

        ///////////////////////////////////////////////////////////////////////
        ///                                 EVENT                           ///
        ///////////////////////////////////////////////////////////////////////

        public void onParentCloseEvent()
        {
                for (TreeItem<String> child : getChildren())
                        ((UIDynamicNode) child).onParentCloseEvent();
        }

        public void onInitializedEvent()
        {

        }
}
