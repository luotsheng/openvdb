package valkyrie.app.widgets;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.stage.Window;
import javafx.util.Duration;
import valkyrie.app.Application;
import valkyrie.app.theme.Stylesheets;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@SuppressWarnings("SameParameterValue")
public class VkContextMenu extends ContextMenu
{
        public void show(double x, double y)
        {
                this.show(Application.primaryStage, x, y);
        }

        public void show(Window ownerWindow, double anchorX, double anchorY)
        {
                super.show(ownerWindow, anchorX, anchorY);
                applyStyles();
                playAnimation();
        }

        public void show(Node anchor, double anchorX, double anchorY)
        {
                super.show(anchor, anchorX, anchorY);
                applyStyles();
                playAnimation();
        }

        /**
         * 弹出菜单是独立的弹出窗口，默认不会带上主场景的应用样式，
         * 这里在显示后手动挂载，保证毛玻璃效果与主题一致。
         */
        private void applyStyles()
        {
                Scene scene = getScene();

                if (scene != null)
                        scene.getStylesheets().setAll(Stylesheets.current());
        }

        private void playAnimation()
        {
                if (getSkin() != null && getSkin().getNode() != null) {
                        getSkin().getNode().setOpacity(0);
                        getSkin().getNode().setScaleX(0.9);
                        getSkin().getNode().setScaleY(0.9);

                        ParallelTransition pt = new ParallelTransition(
                                createFadeTransition(getSkin().getNode(), 0, 1),
                                createScaleTransition(getSkin().getNode(), 0.9, 1)
                        );

                        pt.play();
                }
        }

        private static FadeTransition createFadeTransition(javafx.scene.Node node, double from, double to) {
                FadeTransition ft = new FadeTransition(Duration.millis(100), node);
                ft.setFromValue(from);
                ft.setToValue(to);
                return ft;
        }

        private static ScaleTransition createScaleTransition(javafx.scene.Node node, double from, double to) {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
                st.setFromX(from);
                st.setFromY(from);
                st.setToX(to);
                st.setToY(to);
                return st;
        }
}
