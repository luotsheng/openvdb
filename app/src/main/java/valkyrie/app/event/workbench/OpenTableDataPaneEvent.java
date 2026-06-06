package valkyrie.app.event.workbench;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import valkyrie.app.assets.Assets;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.explorer.UITableDynamicNode;
import valkyrie.app.pane.QueryResultPane;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class OpenTableDataPaneEvent extends OpenTabEvent
{
        private final UITableDynamicNode tableDynamicNode;

        public OpenTableDataPaneEvent(UITableDynamicNode tableDynamicNode)
        {
                super(tableDynamicNode);
                this.tableDynamicNode = tableDynamicNode;
        }

        @Override
        public String tabId()
        {
                return "D#" + tableDynamicNode.getDirectParent().getLabel() + "@" + tableDynamicNode.getLabel();
        }

        @Override
        public Node createPane(Tab tab)
        {
                tab.setGraphic(Assets.use("table"));
                QueryResultPane pane = new QueryResultPane(tab,
                        tableDynamicNode.getSession(),
                        tableDynamicNode.getDriver(),
                        tableDynamicNode.getTable());
                pane.asyncUpdate();
                return pane;
        }
}
