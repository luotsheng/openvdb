package valkyrie.app.explorer;

import lombok.Getter;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.CloseWorkbenchTabEvent;
import valkyrie.app.event.workbench.OpenQueryEditorPaneEvent;
import valkyrie.core.model.ScriptFile;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.utils.collection.Lists;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class UIQueryDynamicNode extends UIDynamicNode
{
        @Getter
        static class QueryNodeWrapper extends DBNode
        {
                private final ScriptFile file;

                public QueryNodeWrapper(ScriptFile file)
                {
                        super(file.getName(), DBNodeKind.QUERY, null);
                        this.file = file;
                }

                @Override
                public boolean hasChildren()
                {
                        return false;
                }

                @Override
                public List<DBNode> getChildren()
                {
                        return Lists.of();
                }
        }

        public UIQueryDynamicNode(UIExplorerNode parent, ScriptFile file)
        {
                super(parent, new QueryNodeWrapper(file));
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                EventBus.publish(new OpenQueryEditorPaneEvent(this));
        }

        @Override
        public void onParentCloseEvent()
        {
                EventBus.publish(new CloseWorkbenchTabEvent(this));
        }

        public UIExplorerNode getDirectParent()
        {
                /* 第一个父节点是 TableContainer 容器节点 */
                return getExplorerParent().getExplorerParent();
        }
}
