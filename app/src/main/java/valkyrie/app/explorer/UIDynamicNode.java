package valkyrie.app.explorer;

import valkyrie.driver.api.node.DBNode;

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
        private final DBNode dbNode;

        public UIDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(dbNode.getLabel(), dbNode.getKind().getIcon());
                this.parent = parent;
                this.dbNode = dbNode;
                parent.getChildren().add(this);
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                if (dbNode.hasChildren()) {
                        getChildren().clear();
                        buildTreeItem(dbNode.getChildren());
                }
        }

        public void buildTreeItem(List<DBNode> dbNodes)
        {
                for (DBNode dbNode : dbNodes)
                        new UIDynamicNode(this, dbNode);
        }
}
