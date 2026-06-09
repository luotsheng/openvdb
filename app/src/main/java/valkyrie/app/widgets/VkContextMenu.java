package valkyrie.app.widgets;

import javafx.scene.control.ContextMenu;
import valkyrie.app.Application;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class VkContextMenu extends ContextMenu
{
        public void show(double x, double y)
        {
                this.show(Application.getPrimaryStage(), x, y);
        }
}
