package valkyrie.app.event;

import lombok.Getter;
import valkyrie.app.event.bus.Event;

/**
 * 刷新查询节点事件
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
@Getter
public class RefreshQueryNodeEvent extends Event
{
        public final String selectNodeLabel;

        public RefreshQueryNodeEvent()
        {
                this(null);
        }

        public RefreshQueryNodeEvent(String label)
        {
                this.selectNodeLabel = label;
        }
}
