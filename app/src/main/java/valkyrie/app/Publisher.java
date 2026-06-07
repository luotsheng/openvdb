package valkyrie.app;

import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenQueryEditorPaneEvent;

/**
 * @author Luo Tiansheng
 * @since 2026/6/3
 */
public class Publisher
{
        public static void openQueryEditor()
        {
                EventBus.publish(new OpenQueryEditorPaneEvent(null));
        }
}
