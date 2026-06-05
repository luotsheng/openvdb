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
        private final ContextMenu contextMenu;
        private Node oldGraphic;
        private final ProgressIndicator progressIndicator = Assets.newProgressIndicator();

        public UIExplorerNode(String label, String icon)
        {
                super(label);
                this.label = label;

                if (icon != null)
                        setGraphic(Assets.use(icon));

                contextMenu = configureContextMenu();
        }

        protected void loadDynamicChildren(List<DBNode> dbNodes)
        {
                List<UIDynamicNode> dynamicNodes = new ArrayList<>();

                for (DBNode dbNode : dbNodes)
                        dynamicNodes.add(new UIDynamicNode(this, dbNode));

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

        /////////////////////////////////////////////////////////////////
        ///                           Event                           ///
        /////////////////////////////////////////////////////////////////
        public void onContextMenuRequested(Node anchor, double x, double y)
        {
                if (contextMenu != null)
                        contextMenu.show(anchor, x, y);
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
