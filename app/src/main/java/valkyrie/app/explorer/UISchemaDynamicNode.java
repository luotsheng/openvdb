package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.utils.Threads;
import valkyrie.driver.api.node.DBNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UISchemaDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("打开模式");

        public UISchemaDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.getItems().addAll(openOrCloseMenuItem);
                return contextMenu;
        }

        @Override
        public void onContextMenuRequested(ContextMenu contextMenu)
        {
                if (initializeChildrenFlag) {
                        openOrCloseMenuItem.setText("关闭模式");
                        openOrCloseMenuItem.setOnAction(e -> {
                                onParentCloseEvent();
                                unexpand();
                        });
                } else {
                        openOrCloseMenuItem.setText("打开模式");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(this::expand));
                }
        }
}
