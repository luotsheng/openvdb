package valkyrie.app.explorer;

import javafx.scene.control.TreeItem;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.node.*;

import static valkyrie.utils.string.StaticLibrary.streq;

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
                        case DBQueryNode ignored -> new UIQueryContainerDynamicNode(parent, dbNode);
                        default -> new UIDynamicNode(parent, dbNode);
                };
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                if (initializeChildrenFlag)
                        return;
                useProgressIndicator(this::expand);
        }

        protected void expand()
        {
                expand(true);
        }

        @SuppressWarnings("SameParameterValue")
        protected void expand(boolean isExpanded)
        {
                if (dbNode.hasChildren())
                        loadDynamicChildren(dbNode.getChildren());
                setExpanded(isExpanded);
                initializeChildrenFlag = true;
                onInitializedEvent();
        }

        protected void unexpand()
        {
                if (!initializeChildrenFlag)
                        return;

                getChildren().clear();
                initializeChildrenFlag = false;
        }

        protected void doRefresh(Runnable runnable)
        {
                /* 记录当前选中节点 */
                var treeView = getRoot().getTreeView();

                UIExplorerNode oldSelectedNode = (UIExplorerNode)
                        treeView.getSelectionModel().getSelectedItem();

                UIExplorerNode newSelectedNode = null;

                runnable.run();

                /* 恢复选中节点 */
                for (TreeItem<String> child : getChildren()) {
                        UIExplorerNode explorerChild = (UIExplorerNode) child;
                        if (streq(explorerChild.getLabel(), oldSelectedNode.getLabel())) {
                                newSelectedNode = explorerChild;
                                break;
                        }
                }

                if (newSelectedNode == null) {
                        if (oldSelectedNode == treeView.getSelectionModel().getSelectedItem()) {
                                treeView.getSelectionModel().select(oldSelectedNode);
                        } else {
                                treeView.getSelectionModel().select(oldSelectedNode.getExplorerParent());
                        }
                } else {
                        treeView.getSelectionModel().select(newSelectedNode);
                }
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
