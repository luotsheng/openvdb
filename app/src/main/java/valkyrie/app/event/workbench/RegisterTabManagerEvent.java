package valkyrie.app.event.workbench;

import javafx.scene.control.Tab;
import lombok.Getter;
import valkyrie.app.event.bus.Event;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
@Getter
public class RegisterTabManagerEvent extends Event
{
        private final Object owner;
        private final Tab tab;

        public RegisterTabManagerEvent(Object owner, Tab tab)
        {
                this.owner = owner;
                this.tab = tab;
        }
}
