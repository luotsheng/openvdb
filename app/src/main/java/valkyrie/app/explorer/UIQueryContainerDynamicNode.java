package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.utils.Threads;
import valkyrie.core.model.QueryFile;
import valkyrie.core.repository.QueryFileRepository;
import valkyrie.driver.api.node.DBNode;
import valkyrie.utils.collection.Lists;

import java.io.File;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UIQueryContainerDynamicNode extends UIDynamicNode
        implements EventListener
{
        private final MenuItem openOrCloseMenuItem = new MenuItem("展开列表");

        public UIQueryContainerDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                reloadQueryNode();
                // subscribe
                EventBus.subscribe(RefreshQueryNodeEvent.class, this);
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem refreshItem = new MenuItem("刷新列表");
                refreshItem.setOnAction(e -> refreshQueryNode());
                contextMenu.getItems().addAll(
                        openOrCloseMenuItem,
                        refreshItem
                );
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
                var scriptFiles = QueryFileRepository.loadScriptFiles(new File(getPath()).getParent());
                List<UIQueryDynamicNode> nodes = Lists.newArrayList();

                getChildren().clear();

                for (QueryFile scriptFile : scriptFiles)
                        nodes.add(new UIQueryDynamicNode(this, scriptFile));

                getChildren().addAll(nodes);
        }

        private void refreshQueryNode()
        {
                doRefresh(this::reloadQueryNode);
        }

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof RefreshQueryNodeEvent) {
                        refreshQueryNode();
                }
        }
}
