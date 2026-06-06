package valkyrie.app.event.workbench;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import valkyrie.app.assets.Assets;
import valkyrie.app.explorer.UITableDynamicNode;
import valkyrie.app.pane.TableDesignerPane;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * 打开设计表面板事件
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class OpenTableDesignerPaneEvent extends OpenTabEvent
{
        private final UITableDynamicNode tableDynamicNode;

        public OpenTableDesignerPaneEvent(UITableDynamicNode owner)
        {
                super(owner);
                this.tableDynamicNode = owner;
        }

        @Override
        public String tabId()
        {
                return fmt("D#%s@%s(%s)",
                        tableDynamicNode.getDirectParent().getLabel(),
                        tableDynamicNode.getLabel(),
                        tableDynamicNode.getRoot().getLabel());
        }

        @Override
        public Node createPane(Tab tab)
        {
                TableDesignerPane pane = new TableDesignerPane(tab,
                        tableDynamicNode.getSession(),
                        tableDynamicNode.getDriver(),
                        tableDynamicNode.getTable());
                tab.setGraphic(Assets.use("struct1"));
                return pane;
        }
}
