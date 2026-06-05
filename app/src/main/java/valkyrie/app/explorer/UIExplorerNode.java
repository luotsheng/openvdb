package valkyrie.app.explorer;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import valkyrie.app.assets.Assets;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */

public abstract class UIExplorerNode extends TreeItem<String>
{
        private final @Getter String label;

        public UIExplorerNode(String label, String icon)
        {
                super(label);
                this.label = label;

                if (icon != null)
                        setGraphic(Assets.use(icon));
        }

        /////////////////////////////////////////////////////////////////
        ///                           Event                           ///
        /////////////////////////////////////////////////////////////////
        public void onContextMenuRequested(Node node, double x, double y)
        {
                /* DO NOTHING... */
        }

        public void onMouseDoubleClickEvent()
        {
                /* DO NOTHING... */
        }

        public void onSelectedEvent(UIExplorerNode node)
        {
                /* DO NOTHING... */
        }
}
