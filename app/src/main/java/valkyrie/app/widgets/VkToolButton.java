package valkyrie.app.widgets;

import static valkyrie.utils.string.StrStaticImports.fmt;

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
                super(tip, text, fmt("%s@18px", icon));
                getStyleClass().add("vk-tool-button");
        }
}
