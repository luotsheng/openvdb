package valkyrie.app;

import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenScriptEditorPaneEvent;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.model.UIExplorerStatus;

/**
 * @author Luo Tiansheng
 * @since 2026/6/3
 */
public class Publisher
{
        public static void openScriptEditor()
        {
                UIExplorerStatus instance = UIExplorerStatus.getInstance();
                UIConnectionNode selectedConnection = instance.getSelectedConnection();
                EventBus.publish(new OpenScriptEditorPaneEvent(null, selectedConnection));
        }
}
