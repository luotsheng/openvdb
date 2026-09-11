package valkyrie.app.widgets.table;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * @author Luo Tiansheng
 * @since 2026/4/6
 */
public class VkTableView<S> extends TableView<S>
{
        private TablePosition<?, ?> start;
        private int rowAnchor = -1;
        private boolean rowDragSelecting = false;
        private boolean rowSelectorEnabled = false;

        public VkTableView()
        {
                getStyleClass().add("vk-table-view");
                setFixedCellSize(26);

                /* 右键仅用于呼出上下文菜单，不应改变/清除当前选中 */
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                        if (event.getButton() == MouseButton.SECONDARY)
                                event.consume();
                });
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
                        if (event.getButton() != MouseButton.PRIMARY)
                                return;

                        var pressed = getTablePosition(event);

                        if (pressed == null)
                                return;

                        /* 从行选择列开始按压 → 整行选择模式 */
                        if (rowSelectorEnabled && pressed.getColumn() == 0) {
                                rowDragSelecting = true;
                                rowAnchor = pressed.getRow();
                                selectRows(rowAnchor, rowAnchor);
                                return;
                        }

                        start = pressed;
                });

                setOnMouseDragged(event -> {
                        var cur = getTablePosition(event);

                        if (cur == null)
                                return;

                        if (rowDragSelecting) {
                                selectRows(rowAnchor, cur.getRow());
                                return;
                        }

                        if (start != null) {
                                getSelectionModel().clearSelection();
                                getSelectionModel().selectRange(
                                        start.getRow(), (TableColumn) start.getTableColumn(),
                                        cur.getRow(), cur.getTableColumn()
                                );
                        }
                });

                setOnMouseReleased(event -> rowDragSelecting = false);
        }

        private static final String ROW_SELECTOR_PRESSED_CLASS = "row-selector-pressed";

        /**
         * 在数据表首列前插入一个空白的“行选择列”（类似 Navicat）：
         * 点击该列选中整行，纵向拖拽可连续选择多整行；点击时有按钮式按压反馈。
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        public TableColumn<S, Void> addRowSelectorColumn()
        {
                TableColumn<S, Void> rowSelector = new TableColumn<>();

                rowSelector.setPrefWidth(20);
                rowSelector.setMinWidth(20);
                rowSelector.setMaxWidth(20);
                rowSelector.setSortable(false);
                rowSelector.setResizable(false);
                rowSelector.setReorderable(false);
                rowSelector.setEditable(false);

                rowSelector.setCellFactory(c -> new TableCell<>()
                {
                        {
                                getStyleClass().add("row-selector-cell");

                                /* 按下加深、抬起/离开还原，形成类似按钮的按压效果 */
                                setOnMousePressed(event -> getStyleClass().add(ROW_SELECTOR_PRESSED_CLASS));
                                setOnMouseReleased(event -> getStyleClass().remove(ROW_SELECTOR_PRESSED_CLASS));
                                setOnMouseExited(event -> getStyleClass().remove(ROW_SELECTOR_PRESSED_CLASS));
                        }

                        @Override
                        protected void updateItem(Void item, boolean empty)
                        {
                                super.updateItem(item, empty);
                                setText(null);
                                setGraphic(null);
                        }
                });

                getColumns().add(0, rowSelector);
                rowSelectorEnabled = true;

                return rowSelector;
        }

        /**
         * 整行选择：框选全部可见列（含首列行选择列，让点击处同样有选中反馈）。
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        private void selectRows(int row0, int row1)
        {
                if (getColumns().isEmpty())
                        return;

                int minRow = Math.min(row0, row1);
                int maxRow = Math.max(row0, row1);

                TableColumn firstColumn = getColumns().get(0);
                TableColumn lastColumn = getColumns().get(getColumns().size() - 1);

                getSelectionModel().clearSelection();
                getSelectionModel().selectRange(minRow, firstColumn, maxRow, lastColumn);
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
