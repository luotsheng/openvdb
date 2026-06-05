package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import valkyrie.app.utils.Threads;
import valkyrie.core.model.ScriptFile;
import valkyrie.core.repository.ScriptFileRepository;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBNodeKind;
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

        @Getter
        static class QueryNodeWrapper extends DBNode
        {
                private final ScriptFile file;

                public QueryNodeWrapper(ScriptFile file)
                {
                        super(file.getName(), DBNodeKind.QUERY, null);
                        this.file = file;
                }

                @Override
                public boolean hasChildren()
                {
                        return false;
                }

                @Override
                public List<DBNode> getChildren()
                {
                        return Lists.of();
                }
        }

        @Getter
        static class UIInternalQueryNode extends UIDynamicNode
        {
                public UIInternalQueryNode(UIExplorerNode parent, ScriptFile file)
                {
                        super(parent, new QueryNodeWrapper(file));
                }
        }

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
                List<UIInternalQueryNode> nodes = Lists.newArrayList();

                for (ScriptFile scriptFile : scriptFiles)
                        nodes.add(new UIInternalQueryNode(this, scriptFile));

                getChildren().addAll(nodes);
        }
}
