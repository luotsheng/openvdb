package valkyrie.app.event;

import lombok.Getter;
import valkyrie.app.event.bus.Event;
import valkyrie.app.explorer.UIDynamicNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
public class CatalogDynamicNodeInitializedEvent extends Event
{
        private final @Getter UIDynamicNode dynamicNode;

        public CatalogDynamicNodeInitializedEvent(UIDynamicNode dynamicNode)
        {
                this.dynamicNode = dynamicNode;
        }
}
