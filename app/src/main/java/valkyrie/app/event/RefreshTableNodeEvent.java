package valkyrie.app.event;

import valkyrie.app.event.bus.Event;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.explorer.UITableContainerDynamicNode;

/**
 * 刷新表节点事件
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class RefreshTableNodeEvent extends Event
{
        private final UITableContainerDynamicNode tableContainerDynamicNode;

        public RefreshTableNodeEvent(UITableContainerDynamicNode tableContainerDynamicNode)
        {
                this.tableContainerDynamicNode = tableContainerDynamicNode;
        }

        public boolean nodeEquals(UIExplorerNode explorerNode)
        {
                return explorerNode == tableContainerDynamicNode;
        }
}
