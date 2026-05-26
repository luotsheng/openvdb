package valkyrie.blueprint;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * 蓝图编辑器
 * <p>
 * 架构应该是长这样：
 * <pre>
 * RootPane
 *   |- GridCanvas
 *   |- Viewport
 *         |- CameraGroup
 *                |- ContentRoot
 *                       |- ConnectionLayer
 *                       |- NodeLayer
 *                       |- OverlayLayer
 * </pre>
 *
 * @author Luo Tiansheng
 * @since 2026/5/26
 */
@SuppressWarnings("ALL")
public class Blueprint extends StackPane
{
        private final Pane viewport = new Pane();

        private final Group cameraGroup = new Group();

        private final Pane contentRoot = new Pane();

        private final Pane connectionLayer = new Pane();

        private final Pane nodeLayer = new Pane();

        private final Pane overlayLayer = new Pane();

        private final Canvas gridCanvas = new Canvas();

        private final Camera camera = new Camera();

        private double btnMouseMiddleLastX = 0.0f;
        private double btnMouseMiddleLastY = 0.0f;
        private boolean btnMouseMiddlePanding = false;

        private final static float BASE_GRID_SIZE = 32.0f;
        private final static float MIN_ZOOM = 0.5f;
        private final static float MAX_ZOOM = 3.0f;

        public Blueprint()
        {
                contentRoot.getChildren().addAll(
                        connectionLayer,
                        nodeLayer,
                        overlayLayer
                );

                cameraGroup.getChildren().addAll(contentRoot);
                viewport.getChildren().addAll(gridCanvas, cameraGroup);

                super.getChildren().add(viewport);

                gridCanvas.widthProperty().bind(viewport.widthProperty());
                gridCanvas.heightProperty().bind(viewport.heightProperty());

                viewport.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                        drawGrid();
                });

                bindKey();
        }

        private void bindKey()
        {
                viewport.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                        if (event.getButton() == MouseButton.MIDDLE) {
                                btnMouseMiddleLastX = event.getSceneX();
                                btnMouseMiddleLastY = event.getSceneY();
                                btnMouseMiddlePanding = true;
                        }
                });

                viewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
                        if (event.getButton() == MouseButton.MIDDLE) {
                                if (!btnMouseMiddlePanding)
                                        return;

                                double x = event.getSceneX();
                                double y = event.getSceneY();

                                double dx = x - btnMouseMiddleLastX;
                                double dy = y - btnMouseMiddleLastY;

                                btnMouseMiddleLastX = x;
                                btnMouseMiddleLastY = y;

                                camera.x += dx / camera.zoom;
                                camera.y += dy / camera.zoom;

                                updateCameraModel();
                        }
                });

                viewport.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                        if (event.getButton() == MouseButton.MIDDLE) {
                                btnMouseMiddlePanding = false;
                        }
                });

                viewport.addEventFilter(ScrollEvent.SCROLL, event -> {
                        double mouseX = event.getSceneX();
                        double mouseY = event.getSceneY();

                        Point2D worldBefore = screenToWorld(mouseX, mouseY);

                        double zoomFactor = 1.05;

                        if (event.getDeltaY() > 0.0f) {
                                camera.zoom *= zoomFactor;
                        } else {
                                camera.zoom /= zoomFactor;
                        }

                        camera.zoom = Math.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);

                        Point2D worldAfter = screenToWorld(mouseX, mouseY);

                        camera.x += worldAfter.getX() - worldBefore.getX();
                        camera.y += worldAfter.getY() - worldBefore.getY();

                        updateCameraModel();
                });
        }

        private void updateCameraModel()
        {
                cameraGroup.setTranslateX(-camera.x * camera.zoom);
                cameraGroup.setTranslateY(-camera.y * camera.zoom);
                cameraGroup.setScaleX(camera.zoom);
                cameraGroup.setScaleY(camera.zoom);

                redraw();
        }

        private void redraw()
        {
                drawGrid();
        }

        private void drawGrid()
        {
                GraphicsContext gc = gridCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

                double scaledGridSize = BASE_GRID_SIZE * camera.zoom;

                double width = gridCanvas.getWidth();
                double height = gridCanvas.getHeight();

                double offsetX = (-camera.x * camera.zoom) % scaledGridSize;
                double offsetY = (-camera.y * camera.zoom) % scaledGridSize;

                gc.setStroke(Color.rgb(60, 60, 60));
                gc.setLineWidth(0.3f);

                for (double x = -offsetX; x < width; x += scaledGridSize)
                        gc.strokeLine(x, 0, x, height);

                for (double y = -offsetY; y < height; y += scaledGridSize)
                        gc.strokeLine(0, y, width, y);
        }

        private Point2D screenToWorld(double screenX, double screenY)
        {
                double worldX = (screenX / camera.zoom) - camera.x;
                double worldY = (screenY / camera.zoom) - camera.y;
                return new Point2D(worldX, worldY);
        }



}
