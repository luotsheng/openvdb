package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import valkyrie.app.assets.Assets;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.driver.api.node.DBNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */

public abstract class UIExplorerNode extends TreeItem<String>
{
        private @Getter String label;

        private final @Getter UIExplorerNode explorerParent;
        private final String icon;

        protected AtomicBoolean progressing = new AtomicBoolean(false);
        private VkContextMenu contextMenu;
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

        public VkContextMenu getContextMenu()
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

                /* 子节点挂载属于 UI 变更，若当前在后台线程则切回 FX 线程 */
                runOnFxThread(() -> getChildren().addAll(dynamicNodes));

                return dynamicNodes;
        }

        /**
         * 在 JavaFX 线程执行；已在 FX 线程则直接执行，保证同步语义
         */
        protected static void runOnFxThread(Runnable action)
        {
                if (Platform.isFxApplicationThread())
                        action.run();
                else
                        Platform.runLater(action);
        }

        protected void useProgressIndicator(Runnable action)
        {
                if (progressing.get())
                        return;

                progressing.set(true);

                oldGraphic = getGraphic();
                setGraphic(Assets.newProgressIndicator());

                Threads.start(() -> {
                        try {
                                action.run();
                        } catch (Exception ex) {
                                Platform.runLater(() -> VkDialogHelper.alert(ex));
                        } finally {
                                progressing.set(false);
                                Platform.runLater(() -> setGraphic(oldGraphic));
                        }
                });
        }

        /**
         * 配置右键菜单
         */
        public VkContextMenu configureContextMenu()
        {
                return null;
        }

        public void showContextMenu(double x, double y)
        {
                VkContextMenu contextMenu = getContextMenu();

                if (contextMenu != null) {
                        onContextMenuRequested(contextMenu);
                        contextMenu.show(x, y);
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
