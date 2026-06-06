package valkyrie.app;

import lombok.Getter;
import valkyrie.app.event.bus.Event;
import valkyrie.app.explorer.UICatalogDynamicNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class CatalogInitializedEvent extends Event
{
        private final @Getter UICatalogDynamicNode catalogDynamicNode;

        public CatalogInitializedEvent(UICatalogDynamicNode catalogDynamicNode)
        {
                this.catalogDynamicNode = catalogDynamicNode;
        }
}
