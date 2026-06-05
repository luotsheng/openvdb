package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import valkyrie.app.assets.Assets;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.driver.api.node.DBNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */

public abstract class UIExplorerNode extends TreeItem<String>
{
        private final @Getter String label;
        private final @Getter UIExplorerNode explorerParent;
        private ContextMenu contextMenu;
        private Node oldGraphic;
        private final ProgressIndicator progressIndicator = Assets.newProgressIndicator();

        public UIExplorerNode(UIExplorerNode parent, String label, String icon)
        {
                super(label);
                this.explorerParent = parent;
                this.label = label;

                if (icon != null)
                        setGraphic(Assets.use(icon));
        }

        public String getPath()
        {
                if (explorerParent == null)
                        return label;

                return explorerParent.getPath() + "/" + label;
        }

        protected void loadDynamicChildren(List<DBNode> dbNodes)
        {
                List<UIDynamicNode> dynamicNodes = new ArrayList<>();

                for (DBNode dbNode : dbNodes)
                        dynamicNodes.add(UIDynamicNode.create(this, dbNode));

                getChildren().addAll(dynamicNodes);
        }

        protected void useProgressIndicator(Runnable runnable)
        {
                oldGraphic = getGraphic();
                setGraphic(progressIndicator);

                new Thread(() -> {
                        try {
                                runnable.run();
                        } catch (Exception ex) {
                                Platform.runLater(() -> VkDialogHelper.alert(ex));
                        } finally {
                                Platform.runLater(() -> setGraphic(oldGraphic));
                        }
                }).start();
        }

        /**
         * 配置右键菜单
         */
        public ContextMenu configureContextMenu()
        {
                return null;
        }

        public void showContextMenu(Node anchor, double x, double y)
        {
                if (contextMenu == null)
                        contextMenu = configureContextMenu();

                if (contextMenu != null) {
                        onContextMenuRequested(contextMenu);

                        if (contextMenu != null)
                                contextMenu.show(anchor, x, y);
                }
        }

        /////////////////////////////////////////////////////////////////
        ///                           Event                           ///
        /////////////////////////////////////////////////////////////////
        public void onContextMenuRequested(ContextMenu contextMenu)
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
