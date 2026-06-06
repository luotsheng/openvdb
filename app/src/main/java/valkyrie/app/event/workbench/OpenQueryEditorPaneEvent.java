package valkyrie.app.event.workbench;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import valkyrie.app.assets.Assets;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.workbench.QueryEditor;

/**
 * 打开脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class OpenQueryEditorPaneEvent extends OpenTabEvent
{
        private final UIExplorerNode owner;

        public OpenQueryEditorPaneEvent(UIExplorerNode owner)
        {
                super(owner);
                this.owner = owner;
        }

        @Override
        public String tabId()
        {
                return owner.getLabel();
        }

        @Override
        public Node createPane(Tab tab)
        {
                tab.setGraphic(Assets.use("sql"));
                return new QueryEditor(tab);
        }
}
