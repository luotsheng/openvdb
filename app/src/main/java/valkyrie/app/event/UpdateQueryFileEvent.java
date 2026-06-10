package valkyrie.app.event;

import lombok.Getter;
import valkyrie.app.event.bus.Event;
import valkyrie.app.explorer.UIQueryContainerDynamicNode;
import valkyrie.app.explorer.UIQueryDynamicNode;
import valkyrie.core.model.QueryFile;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
@Getter
public class UpdateQueryFileEvent extends Event
{
        private final QueryFile oldQueryFile;
        private final QueryFile newQueryFile;

        private final UIQueryDynamicNode queryDynamicNode;
        private final UIQueryContainerDynamicNode queryContainerDynamicNode;

        public UpdateQueryFileEvent(QueryFile oldQueryFile, QueryFile newQueryFile, UIQueryDynamicNode queryDynamicNode)
        {
                this.oldQueryFile = oldQueryFile;
                this.newQueryFile = newQueryFile;
                this.queryDynamicNode = queryDynamicNode;
                this.queryContainerDynamicNode = (UIQueryContainerDynamicNode) queryDynamicNode.getParent();
        }
}
