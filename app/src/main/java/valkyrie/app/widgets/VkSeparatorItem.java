package valkyrie.app.widgets;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;

/**
 * 工具栏专用垂直分割线
 *
 * @author Luo Tiansheng
 * @since 2026/3/30
 */
public class VkSeparatorItem extends Separator
{
        public VkSeparatorItem()
        {
                super(Orientation.VERTICAL);
                getStyleClass().add("vk-tool-separator");
        }
}
