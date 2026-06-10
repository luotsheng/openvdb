package valkyrie.app.widgets;

/**
 * 图标按钮
 *
 * @author Luo Tiansheng
 * @since 2026/4/9
 */
public class VkToolButton extends VkIconButton
{
        public VkToolButton(String tip, String icon)
        {
                this(tip, null, icon);
        }

        public VkToolButton(String tip, String text, String icon)
        {
                super(tip, text, icon);
                getStyleClass().add("vk-tool-button");
        }
}
