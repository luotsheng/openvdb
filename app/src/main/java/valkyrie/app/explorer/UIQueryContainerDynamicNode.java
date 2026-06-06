package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.utils.Threads;
import valkyrie.core.model.ScriptFile;
import valkyrie.core.repository.ScriptFileRepository;
import valkyrie.driver.api.node.DBNode;
import valkyrie.utils.collection.Lists;

import java.io.File;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UIQueryContainerDynamicNode extends UIDynamicNode
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("展开列表");

        public UIQueryContainerDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                reloadQueryNode();
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
                if (isExpanded()) {
                        openOrCloseMenuItem.setText("收起列表");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(() -> setExpanded(false)));
                } else {
                        openOrCloseMenuItem.setText("展开列表");
                        openOrCloseMenuItem.setOnAction(e -> Threads.runLater(() -> setExpanded(true)));
                }
        }

        private void reloadQueryNode()
        {
                var scriptFiles = ScriptFileRepository.loadScriptFiles(new File(getPath()).getParent());
                List<UIQueryDynamicNode> nodes = Lists.newArrayList();

                for (ScriptFile scriptFile : scriptFiles)
                        nodes.add(new UIQueryDynamicNode(this, scriptFile));

                getChildren().addAll(nodes);
        }
}
