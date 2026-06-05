package valkyrie.app.explorer;

import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBTableContainerNode;

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

        private boolean initializeFlag = false;

        public UIDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(dbNode.getLabel(), dbNode.getKind().getIcon());
                this.parent = parent;
                this.dbNode = dbNode;

                if (dbNode instanceof DBTableContainerNode) {
                        initializeFlag = true;
                        expandItem();
                }
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                if (initializeFlag)
                        return;

                useProgressIndicator(() -> {
                        expandItem();
                        setExpanded(true);
                        initializeFlag = true;
                });
        }

        private void expandItem()
        {
                if (dbNode.hasChildren())
                        loadDynamicChildren(dbNode.getChildren());
        }
}
