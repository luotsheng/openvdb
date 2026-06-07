package valkyrie.app.event.workbench;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import valkyrie.app.assets.Assets;
import valkyrie.app.explorer.UIQueryDynamicNode;
import valkyrie.app.utils.TabIdFactory;
import valkyrie.app.workbench.QueryEditor;
import valkyrie.core.model.QueryFile;

/**
 * 打开脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class OpenQueryEditorPaneEvent extends OpenTabEvent
{
        private final QueryFile scriptFile;
        private final UIQueryDynamicNode queryDynamicNode;

        public OpenQueryEditorPaneEvent(UIQueryDynamicNode owner)
        {
                super(owner);
                this.queryDynamicNode = owner;
                scriptFile = owner != null ? owner.getQueryFile() : null;
        }

        @Override
        public String tabId()
        {
               return TabIdFactory.buildQueryTabId(queryDynamicNode);
        }

        @Override
        public Node createPane(Tab tab)
        {
                tab.setGraphic(Assets.use("sql"));
                return new QueryEditor(tab, scriptFile);
        }
}
