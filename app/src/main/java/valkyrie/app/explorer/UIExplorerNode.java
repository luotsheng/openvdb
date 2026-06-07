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
        private @Getter String label;

        private final @Getter UIExplorerNode explorerParent;
        private final String icon;

        private ContextMenu contextMenu;
        private Node oldGraphic;

        public UIExplorerNode(UIExplorerNode parent, String label, String icon)
        {
                super(label);
                this.explorerParent = parent;
                this.label = label;
                this.icon = icon;

                if (icon != null)
                        setGraphic(Assets.use(icon));
        }

        public void setLabel(String label)
        {
                this.label = label;
                setValue(label);
        }

        public Node createGraphic()
        {
                return Assets.use(icon);
        }

        public UIConnectionNode getRoot()
        {
                UIExplorerNode root = explorerParent;

                while (root.getExplorerParent() != null) {
                        root = root.getExplorerParent();
                }

                return (UIConnectionNode) root;
        }

        public String getPath()
        {
                if (explorerParent == null)
                        return label;

                return explorerParent.getPath() + "/" + label;
        }

        public ContextMenu getContextMenu()
        {
                if (contextMenu == null)
                        contextMenu = configureContextMenu();
                return contextMenu;
        }

        protected List<UIDynamicNode> loadDynamicChildren(List<DBNode> dbNodes)
        {
                List<UIDynamicNode> dynamicNodes = new ArrayList<>();

                for (DBNode dbNode : dbNodes)
                        dynamicNodes.add(UIDynamicNode.create(this, dbNode));

                getChildren().addAll(dynamicNodes);

                return dynamicNodes;
        }

        protected void useProgressIndicator(Runnable runnable)
        {
                oldGraphic = getGraphic();
                setGraphic(Assets.newProgressIndicator());

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
                ContextMenu contextMenu = getContextMenu();

                if (contextMenu != null) {
                        onContextMenuRequested(contextMenu);
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
