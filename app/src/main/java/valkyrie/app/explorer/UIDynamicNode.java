package valkyrie.app.explorer;

import valkyrie.driver.api.node.*;

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
        }

        protected void unexpand()
        {
                if (!initializeChildrenFlag)
                        return;

                getChildren().clear();
                initializeChildrenFlag = false;
        }
}
