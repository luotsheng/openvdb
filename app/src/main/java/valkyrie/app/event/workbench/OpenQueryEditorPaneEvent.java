package valkyrie.app.event.workbench;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import valkyrie.app.assets.Assets;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.explorer.UIQueryDynamicNode;
import valkyrie.app.workbench.QueryEditor;
import valkyrie.core.model.ScriptFile;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * 打开脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class OpenQueryEditorPaneEvent extends OpenTabEvent
{
        private static int count = 0;

        private final ScriptFile scriptFile;
        private final UIQueryDynamicNode queryDynamicNode;

        public OpenQueryEditorPaneEvent(UIQueryDynamicNode owner)
        {
                super(owner);
                this.queryDynamicNode = owner;
                scriptFile = owner != null ? owner.getScriptFile() : null;
        }

        @Override
        public String tabId()
        {
                if (queryDynamicNode == null)
                        return "新建查询脚本_" + (count++);

                return fmt("Q#%s@%s(%s)",
                        queryDynamicNode.getDirectParent().getLabel(),
                        queryDynamicNode.getLabel(),
                        queryDynamicNode.getRoot().getLabel());
        }

        @Override
        public Node createPane(Tab tab)
        {
                tab.setGraphic(Assets.use("sql"));
                return new QueryEditor(tab, scriptFile);
        }
}
