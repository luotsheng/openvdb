package valkyrie.app.widgets.table;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;

/**
 * 数据网格。
 * <p>
 * 表格开启了单元格选择模式，此时 JavaFX 不会给 {@code TableRow} 打上
 * {@code :selected} 伪类，整行缺少选中反馈。这里通过行工厂手动给包含选中
 * 单元格的行添加 {@code selected-row} 样式类，由 CSS 渲染浅色底。
 *
 * @author Luo Tiansheng
 * @since 2026/4/6
 */
public class VkDataTableView<S> extends VkTableView<S>
{
        private static final String SELECTED_ROW_CLASS = "selected-row";

        public VkDataTableView()
        {
                getStyleClass().add("vk-data-table-view");

                setRowFactory(tableView -> new TableRow<>()
                {
                        {
                                getStyleClass().add("vk-data-row");

                                tableView.getSelectionModel().getSelectedCells().addListener(
                                        (ListChangeListener<TablePosition>) change -> refreshSelectedRow());
                        }

                        @Override
                        protected void updateItem(S item, boolean empty)
                        {
                                super.updateItem(item, empty);
                                refreshSelectedRow();
                        }

                        private void refreshSelectedRow()
                        {
                                int index = getIndex();

                                boolean selected = index >= 0
                                        && tableView.getSelectionModel().getSelectedCells().stream()
                                                .anyMatch(cell -> cell.getRow() == index);

                                if (selected) {
                                        if (!getStyleClass().contains(SELECTED_ROW_CLASS))
                                                getStyleClass().add(SELECTED_ROW_CLASS);
                                } else {
                                        getStyleClass().remove(SELECTED_ROW_CLASS);
                                }
                        }
                });
        }
}
