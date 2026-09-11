package valkyrie.app.layout;

import javafx.application.Platform;
import javafx.scene.control.SplitPane;
import valkyrie.app.pane.ObjectExplorerPane;
import valkyrie.app.workbench.Workbench;

import static valkyrie.utils.collection.Lists.first;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class ContainerLayout extends SplitPane
{
        private double ratio = 0.2f;

        /**
         * 是否正在拖动分割线；拖动期间不再因宽度变化重设分割位置，
         * 否则每次拖动都会多触发一轮布局（含 WebView 重排）导致卡顿。
         */
        private boolean dividerDragging = false;

        public ContainerLayout()
        {
                ObjectExplorerPane navigator = new ObjectExplorerPane();
                Workbench workbench = new Workbench();

                getItems().addAll(navigator, workbench);
                setDividerPositions(ratio);

                SplitPane.Divider divider = first(getDividers());

                Platform.runLater(() -> lookupAll(".split-pane-divider").forEach(node -> {

                        // 只监听分割条才触发事件，避免因窗口大小导致 ratio 记忆失效
                        node.setOnMousePressed(e -> {
                                node.setUserData(Boolean.TRUE);
                                dividerDragging = true;
                        });
                        node.setOnMouseReleased(e -> {
                                node.setUserData(Boolean.FALSE);
                                dividerDragging = false;
                        });

                        // Pane 比例变化监听
                        divider.positionProperty().addListener((obs, oldVal, newVal) -> {
                                if (node.getUserData() == Boolean.TRUE)
                                        ratio = newVal.doubleValue();
                        });

                }));

                navigator.widthProperty().addListener((obs, oldVal, newVal) -> {
                        if (!dividerDragging)
                                setDividerPositions(ratio);
                });
        }

}
