package valkyrie.app.event;

import lombok.Getter;
import valkyrie.app.event.bus.Event;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.explorer.UIQueryContainerDynamicNode;

/**
 * 刷新查询节点事件
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
@Getter
public class RefreshQueryNodeEvent extends Event
{
        private final UIQueryContainerDynamicNode queryContainerDynamicNode;

        public RefreshQueryNodeEvent(UIQueryContainerDynamicNode queryContainerDynamicNode)
        {
                this.queryContainerDynamicNode = queryContainerDynamicNode;
        }

        public boolean nodeEquals(UIExplorerNode explorerNode)
        {
                return explorerNode == queryContainerDynamicNode;
        }
}
