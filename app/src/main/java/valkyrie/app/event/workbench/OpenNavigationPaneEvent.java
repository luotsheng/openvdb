package valkyrie.app.event.workbench;

import javafx.scene.layout.Pane;
import lombok.Getter;
import valkyrie.app.event.bus.Event;

/**
 * 在工作区导航页 Tab 设置面板事件
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
@Getter
public class OpenNavigationPaneEvent extends Event
{
        private final Object owner;
        private final Pane pane;

        public OpenNavigationPaneEvent(Object owner, Pane pane)
        {
                this.owner = owner;
                this.pane = pane;
        }
}
