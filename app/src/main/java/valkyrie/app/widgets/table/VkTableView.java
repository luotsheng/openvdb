package valkyrie.app.widgets.table;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * @author Luo Tiansheng
 * @since 2026/4/6
 */
public class VkTableView<S> extends TableView<S>
{
        private TablePosition<?, ?> start;

        public VkTableView()
        {
                getStyleClass().add("vk-table-view");
                setFixedCellSize(26);
        }

        public void enableCellEdit()
        {
                setEditable(true);
                getSelectionModel().setCellSelectionEnabled(true);
                getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        /**
         * 启用矩形选择
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void enableRectangularSelection()
        {
                enableCellEdit();

                setOnMousePressed(event -> {
                        start = getTablePosition(event);
                });

                setOnMouseDragged(event -> {
                        var cur = getTablePosition(event);

                        if (start != null && cur != null) {
                                getSelectionModel().clearSelection();
                                getSelectionModel().selectRange(
                                        start.getRow(), (TableColumn) start.getTableColumn(),
                                        cur.getRow(), cur.getTableColumn()
                                );
                        }
                });
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private TablePosition getTablePosition(MouseEvent event)
        {
                var pick = event.getPickResult();
                Node node = pick.getIntersectedNode();

                while (node != null && !(node instanceof TableCell<?, ?>))
                        node = node.getParent();

                if (node instanceof TableCell cell && !cell.isEmpty()) {
                        return new TablePosition<>(
                                this,
                                cell.getIndex(),
                                cell.getTableColumn()
                        );
                }

                return null;
        }

        public void playFlash()
        {
                FadeTransition ft = new FadeTransition(Duration.millis(600), this);
                ft.setFromValue(0.1);
                ft.setToValue(1.0);
                ft.setCycleCount(1);
                ft.setAutoReverse(true);

                ft.setOnFinished(event -> this.setOpacity(1.0));

                ft.play();
        }

        @Override
        public void refresh()
        {
                super.refresh();
        }

}
