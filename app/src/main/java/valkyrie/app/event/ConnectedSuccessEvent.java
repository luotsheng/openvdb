package valkyrie.app.event;

import lombok.Getter;
import valkyrie.app.event.bus.Event;
import valkyrie.app.explorer.UIConnectionNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class ConnectedSuccessEvent extends Event
{
        private final @Getter UIConnectionNode connectionNode;

        public ConnectedSuccessEvent(UIConnectionNode connectionNode)
        {
                this.connectionNode = connectionNode;
        }
}
