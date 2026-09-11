package valkyrie.app.pane;

import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lombok.Setter;
import valkyrie.app.Application;
import valkyrie.app.assets.Assets;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.VkSeparatorItem;
import valkyrie.app.widgets.VkTextField;
import valkyrie.app.widgets.VkToolBar;
import valkyrie.app.widgets.VkToolButton;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.app.widgets.table.VkDataTableView;
import valkyrie.app.widgets.table.VkTableView;
import valkyrie.app.widgets.table.cell.VkTextFieldTableCell;
import valkyrie.app.workbench.ModifyCell;
import valkyrie.core.utils.JSONUtils;
import valkyrie.driver.api.Column;
import valkyrie.driver.api.GridRow;
import valkyrie.driver.api.QueryResult;
import valkyrie.utils.Optional;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.io.UFile;
import valkyrie.utils.poi.WorkBook;
import valkyrie.utils.time.DateFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static valkyrie.utils.string.StrStaticImports.*;

/**
 * @author Luo Tiansheng
 * @since 2026/3/30
 */
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class QueryResultDataPane extends BorderPane
{
        private String tableName;

        private final TabPane tabPane = new TabPane();
        private final Tab viewTab = new Tab();
        private final VkTableView<GridRow> tableView = new VkDataTableView<>();
        private final VkToolBar toolBar = new VkToolBar();
        private final VBox vContainer;
        private final Tab attachedToTab;

        private final Button plus = new VkToolButton("新增", "plus");
        private final Button minus = new VkToolButton("删除", "minus");
        private final Button submit = new VkToolButton("提交", "check");
        private final Button cross = new VkToolButton("取消", "cross");
        private final Button reload = new VkToolButton("刷新", "reload");
        private final Button export = new VkToolButton("", "导出", "export");

        private final Node progressIndicator = Assets.newProgressIndicator();

        private final VkTextField search = new VkTextField();
        private final PauseTransition searchDelay = new PauseTransition(Duration.millis(100));
        private final ObservableList<GridRow> gridRows = FXCollections.observableArrayList();
        private final FilteredList<GridRow> filteredRows = new FilteredList<>(gridRows, row -> true);

        /**
         * 当前搜索关键字，供单元格高亮命中的文本
         */
        private String activeKeyword = "";

        private QueryResult queryResult;

        public interface ReloadProgressListener {
                void start();
                void end();
        }

        public interface AllTabClosedListener {
                void closed();
        }

        @Setter
        private AllTabClosedListener onClosedListener;

        @Setter
        private ReloadProgressListener reloadProgressListener;

        public QueryResultDataPane(Tab attachedToTab, boolean isPreview)
        {
                this(null, attachedToTab, isPreview);
        }

        public QueryResultDataPane(String tableName, Tab attachedToTab, boolean isPreview)
        {
                this.tableName = tableName;
                this.attachedToTab = attachedToTab;

                viewTab.setClosable(false);

                setupTableView();

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                search.setPromptText("搜索...");
                search.setPrefWidth(220);

                HBox searchBox = new HBox(5, Assets.use("search"), search);
                searchBox.setAlignment(Pos.CENTER_LEFT);

                toolBar.getItems().addAll(
                        plus, minus,
                        new VkSeparatorItem(),
                        submit, cross,
                        new VkSeparatorItem(),
                        reload,
                        spacer,
                        searchBox,
                        export);

                vContainer = new VBox(tableView);
                VBox.setVgrow(tableView, Priority.ALWAYS);
                viewTab.setContent(vContainer);

                setTop(toolBar);
                setCenter(tabPane);

                /*
                 * 允许外部父容器监听当前表格预览组件中的标签页是否完全关闭。
                 *
                 * 父容器可以设置监听器，当所有标签都被关闭后，父容器可以选
                 * 择同时关闭当前预览页面。
                 */
                tabPane.getTabs().addListener((ListChangeListener<? super Tab>) change -> {
                        if (!isPreview && tabPane.getTabs().isEmpty()) {
                                if (onClosedListener != null)
                                        onClosedListener.closed();
                        }
                });

                updateCheckCross();
                setupToolButtonAction();
                setupSearch();
        }

        /**
         * 搜索接线：按任意单元格内容过滤（忽略大小写、纯子串匹配）。
         * 搜索生效期间切换为只读，避免过滤后的行号与真实数据行错位导致误编辑/误删除。
         */
        private void setupSearch()
        {
                searchDelay.setOnFinished(event -> applySearchState());

                search.textProperty().addListener((obs, oldVal, newVal) -> searchDelay.playFromStart());
        }

        private void applySearchState()
        {
                String text = search.getText();
                boolean searching = text != null && !text.isBlank();

                activeKeyword = searching ? text.trim() : "";

                if (searching) {
                        String keyword = lowercase(text).trim();
                        filteredRows.setPredicate(row -> row != null && row.stream()
                                .anyMatch(cell -> contains(cell, keyword)));
                } else {
                        filteredRows.setPredicate(row -> true);
                }

                tableView.refresh();
                refreshEditingState(searching);
        }

        private void refreshEditingState(boolean searching)
        {
                if (queryResult == null)
                        return;

                if (searching) {
                        /* 过滤状态下只读，防止行号错位 */
                        tableView.setEditable(false);
                        plus.setDisable(true);
                        minus.setDisable(true);
                        submit.setDisable(true);
                        cross.setDisable(true);
                        return;
                }

                setToolButtonStatus(queryResult.isAddable(), queryResult.isEditable());
                updateCheckCross();
        }

        private static boolean contains(String cell, String keyword)
        {
                return cell != null && lowercase(cell).contains(keyword);
        }

        private void setToolButtonStatus(boolean addable, boolean editable)
        {
                /* 如果可新增，说明在表数据页面 */
                if (addable) {
                        tableView.setEditable(true);
                        plus.setDisable(false);
                        minus.setDisable(false);
                        return;
                }

                /* 可编辑但不可新增，说明是查询页 */
                if (editable) {
                        tableView.setEditable(true);
                        minus.setDisable(false);
                        return;
                }

                /* 执行例如关联查询，SHOW 之类的语句不可用 */
                tableView.setEditable(false);
                plus.setDisable(true);
                minus.setDisable(true);
        }

        private void updateCheckCross()
        {
                boolean disable = (queryResult == null || !queryResult.isUpdatable());

                submit.setDisable(disable);
                cross.setDisable(disable);
        }

        private void setupToolButtonAction()
        {
                plus.setOnAction(event -> onPlus());
                minus.setOnAction(event -> onMinus());
                submit.setOnAction(event -> applySubmit());
                cross.setOnAction(event -> applyCross());
                reload.setOnAction(event -> reloadAndBlinkTable(true));
                export.setOnAction(event -> applyExport());
        }

        private void onPlus()
        {
                queryResult.addEmptyRow();
                syncGridRows();
                tableView.refresh();
                tableView.playFlash();
        }

        /**
         * 把结果行复制到显示列表中。
         * <p>
         * 显示用副本而非原对象，保证「设置为 NULL」等仅改显示的操不会污染
         * {@code QueryResult.rows}（原值仍用于生成 UPDATE 的 WHERE 条件）。
         */
        private void syncGridRows()
        {
                List<GridRow> copies = new ArrayList<>(queryResult.getRows().size());

                for (GridRow row : queryResult.getRows()) {
                        GridRow copy = new GridRow();
                        copy.addAll(row);
                        copies.add(copy);
                }

                gridRows.setAll(copies);
        }

        /**
         * 将选中的单元格设置为 NULL（写入待提交缓冲区并更新显示）
         */
        private void setSelectedCellsNull()
        {
                if (queryResult == null)
                        return;

                var selected = tableView.getSelectionModel().getSelectedCells();

                for (TablePosition<?, ?> position : selected) {
                        int row = position.getRow();
                        int column = dataColumnIndex(position.getColumn());

                        if (column < 0 || row < 0 || row >= gridRows.size())
                                continue;

                        gridRows.get(row).set(column, null);
                        queryResult.addUpdateRow(column, row, null);
                }

                tableView.refresh();
                updateCheckCross();
        }

        private void onMinus()
        {
                var indices = tableView.getSelectionModel().getSelectedIndices();

                if (indices == null || indices.isEmpty())
                        return;

                if (!VkDialogHelper.askDangerous("选中%s条数据，是否删除？", indices.size()))
                        return;

                setProgressIndicator();

                new Thread(() -> {
                        try {
                                queryResult.remove(List.copyOf(indices));
                                queryResult.reload();

                                Platform.runLater(() -> {
                                        reload(tableName, queryResult);
                                        tableView.playFlash();
                                });
                        } catch (Throwable e) {
                                alert(e);
                        } finally {
                                removeProgressIndicator();
                        }
                }).start();
        }

        private void applySubmit()
        {
                if (queryResult == null || !queryResult.isUpdatable())
                        return;

                /* 提交期间冻结编辑，避免后台生成/执行 UPDATE 时缓冲区被并发修改 */
                submit.setDisable(true);
                cross.setDisable(true);
                tableView.setEditable(false);
                setProgressIndicator();

                new Thread(() -> {
                        try {
                                queryResult.update();

                                Platform.runLater(() -> {
                                        updateCheckCross();
                                        reload(tableName, queryResult);
                                });
                        } catch (Throwable e) {
                                Platform.runLater(() -> {
                                        tableView.setEditable(true);
                                        updateCheckCross();
                                        VkDialogHelper.alert(e);
                                });
                        } finally {
                                removeProgressIndicator();
                        }
                }).start();
        }

        private void applyCross()
        {
                queryResult.clearUpdateBuffer();
                updateCheckCross();
                reload(tableName, queryResult);
        }

        public void setProgressIndicator()
        {
                if (reloadProgressListener != null) {
                        Platform.runLater(reloadProgressListener::start);
                } else {
                        Platform.runLater(() -> viewTab.setGraphic(progressIndicator));
                }
        }

        public void removeProgressIndicator()
        {
                if (reloadProgressListener != null) {
                        Platform.runLater(reloadProgressListener::end);
                } else {
                        Platform.runLater(() -> viewTab.setGraphic(null));
                }
        }

        private void reloadAndBlinkTable(boolean enableProgressIndicator)
        {
                reload.setDisable(true);

                if (enableProgressIndicator)
                        setProgressIndicator();

                new Thread(() -> {
                        try {
                                queryResult.reload();
                                Platform.runLater(() -> reload(tableName, queryResult));
                        } catch (Throwable e) {
                                alert(e);
                        } finally {
                                Platform.runLater(() -> {
                                        tableView.playFlash();
                                        reload.setDisable(false);
                                });

                                if (enableProgressIndicator)
                                        removeProgressIndicator();
                        }
                }).start();
        }

        /**
         * 后台线程中安全地弹出异常提示
         */
        private static void alert(Throwable e)
        {
                Platform.runLater(() -> VkDialogHelper.alert(e));
        }

        private void applyExport()
        {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("选择保存文件");

                String initName = attachedToTab.getText();
                initName = strcut(initName, 0, initName.indexOf("."));
                initName = initName + DateFormatter.format("yyyyMMddHHmmss") + ".xlsx";

                fileChooser.setInitialFileName(initName);

                // 打开对话框
                File saveDirectory = fileChooser.showSaveDialog(Application.primaryStage);

                if (saveDirectory != null) {
                        WorkBook wb = WorkBook.create();
                        wb.addRow(Lists.map(queryResult.getColumns(), Column::getLabel).toArray());
                        queryResult.getRows().forEach(row -> wb.addRow(row.toArray()));
                        wb.transferTo(UFile.wrap(saveDirectory));
                }
        }

        private void setupTableView()
        {
                tableView.enableRectangularSelection();

                VkContextMenu contextMenu = new VkContextMenu();

                Menu copyItem = new Menu("复制为");
                MenuItem copyAsInsert = new MenuItem("复制为 INSERT 语句");
                copyAsInsert.setOnAction(event -> copyAsSql("INSERT"));
                MenuItem copyAsUpdate = new MenuItem("复制为 UPDATE 语句");
                copyAsUpdate.setOnAction(event -> copyAsSql("UPDATE"));
                MenuItem normalCopyItem = new MenuItem("复制为 EXCEL 格式");
                normalCopyItem.setOnAction(event -> copyTableViewSelectedCell());
                MenuItem jsonCopyItem = new MenuItem("复制为 JSON 格式");
                jsonCopyItem.setOnAction(event -> copyTableViewAsJSON(SerializationFeature.INDENT_OUTPUT));
                MenuItem jsonlCopyItem = new MenuItem("复制为 JSON 压缩格式");
                jsonlCopyItem.setOnAction(event -> copyTableViewAsJSON());

                copyItem.getItems().addAll(
                        copyAsInsert,
                        copyAsUpdate,
                        new SeparatorMenuItem(),
                        normalCopyItem,
                        new SeparatorMenuItem(),
                        jsonCopyItem,
                        jsonlCopyItem
                );

                MenuItem selectAllItem = new MenuItem("全选");
                selectAllItem.setOnAction(event -> tableView.getSelectionModel().selectAll());
                MenuItem refreshItem = new MenuItem("刷新");
                refreshItem.setOnAction(event -> reloadAndBlinkTable(true));

                MenuItem submitItem = new MenuItem("提交修改");
                submitItem.setOnAction(event -> applySubmit());
                MenuItem plusItem = new MenuItem("新增行");
                plusItem.setOnAction(event -> onPlus());
                MenuItem minusItem = new MenuItem("删除选中行");
                minusItem.setOnAction(event -> onMinus());
                minusItem.getStyleClass().add("danger-menu-item");
                MenuItem setNullItem = new MenuItem("设置为 NULL");
                setNullItem.setOnAction(event -> setSelectedCellsNull());
                MenuItem exportItem = new MenuItem("导出");
                exportItem.setOnAction(event -> applyExport());

                contextMenu.getItems().addAll(
                        submitItem,
                        plusItem,
                        setNullItem,
                        minusItem,
                        new SeparatorMenuItem(),
                        copyItem,
                        selectAllItem,
                        new SeparatorMenuItem(),
                        exportItem,
                        refreshItem);

                /* 根据当前结果集是否可编辑/可提交，动态启用菜单项 */
                contextMenu.setOnShowing(event -> {
                        boolean hasResult = queryResult != null;
                        boolean addable = hasResult && queryResult.isAddable();
                        boolean editable = hasResult && queryResult.isEditable();

                        submitItem.setDisable(!hasResult || !queryResult.isUpdatable());
                        plusItem.setDisable(!addable);
                        minusItem.setDisable(!addable && !editable);
                        setNullItem.setDisable(!addable && !editable);
                });

                tableView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
                        tableView.setContextMenu(contextMenu);
                });

                tableView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if ((event.isShortcutDown())
                                && event.getCode() == KeyCode.C)
                                copyTableViewSelectedCell();
                });

                tableView.setOnKeyPressed(event -> {
                        if ((event.isShortcutDown())
                                && event.getCode() == KeyCode.S) {
                                if (queryResult.isUpdatable())
                                        applySubmit();
                                event.consume();
                        }
                });

                tableView.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                        if (event.getCode() == KeyCode.ALT || event.getCode() == KeyCode.ALT_GRAPH) {
                                var indices = tableView.getSelectionModel().getSelectedIndices();
                                tableView.getSelectionModel().selectRange(indices.getFirst(), indices.getLast() + 1);
                        }
                });
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void copyTableViewSelectedCell()
        {
                ObservableList<TablePosition> cells =
                        tableView.getSelectionModel().getSelectedCells();

                if (cells == null || cells.isEmpty())
                        return;

                int start = cells.getFirst().getRow();
                int rows = start + Math.toIntExact(cells.stream().map(TablePosition::getRow).distinct().count());

                FilteredList<TablePosition> filtered = new FilteredList<>(cells, predicate -> true);

                StringBuilder builder = new StringBuilder();

                for (int i = start; i < rows; i++) {
                        int ROW = i;
                        filtered.setPredicate(cell -> cell.getRow() == ROW);

                        int[] count = { 0 };
                        filtered.forEach(cell -> {
                                int col = dataColumnIndex(cell.getColumn());

                                if (col < 0)
                                        return;

                                List<String> tableGridRow = (List<String>) cell.getTableView()
                                        .getItems().get(ROW);

                                if (col >= tableGridRow.size())
                                        return;

                                String text = tableGridRow.get(col);

                                if (strnempty(text))
                                        builder.append(text);

                                builder.append("\t");
                                count[0]++;
                        });

                        if (count[0] == 0)
                                continue;

                        builder.deleteCharAt(builder.length() - 1);
                        builder.append("\n");
                }

                if (builder.length() > 0)
                        builder.deleteCharAt(builder.length() - 1);

                Application.copyToClipboard(builder.toString());
        }

        @SuppressWarnings({"rawtypes"})
        private void copyAsSql(String type)
        {
                ObservableList<TablePosition> cells = tableView.getSelectionModel().getSelectedCells();
                if (cells == null || cells.isEmpty())
                        return;

                int minRow = cells.stream().map(TablePosition::getRow).min(Integer::compareTo).orElse(0);
                int maxRow = cells.stream().map(TablePosition::getRow).max(Integer::compareTo).orElse(0);
                int tableMinCol = cells.stream().map(TablePosition::getColumn).min(Integer::compareTo).orElse(0);
                int tableMaxCol = cells.stream().map(TablePosition::getColumn).max(Integer::compareTo).orElse(0);

                int[] range = dataColumnRange(tableMinCol, tableMaxCol);
                int minCol = range[0];
                int maxCol = range[1];

                if (minCol > maxCol || queryResult == null)
                        return;

                String tableName = Optional.ifBlank(this.tableName, "?");

                List<Column> columns = queryResult.getColumns();
                ObservableList<GridRow> visibleRows = tableView.getItems();

                StringBuilder sql = new StringBuilder();
                if ("INSERT".equals(type)) {
                        for (int r = minRow; r <= maxRow; r++) {
                                GridRow row = visibleRows.get(r);
                                sql.append("INSERT INTO ").append(tableName).append(" (");
                                for (int c = minCol; c <= maxCol; c++) {
                                        sql.append(columns.get(c).getLabel());
                                        if (c < maxCol) sql.append(", ");
                                }
                                sql.append(") VALUES (");
                                for (int c = minCol; c <= maxCol; c++) {
                                        sql.append("'").append(row.get(c)).append("'");
                                        if (c < maxCol) sql.append(", ");
                                }
                                sql.append(");\n");
                        }
                } else if ("UPDATE".equals(type)) {
                        for (int r = minRow; r <= maxRow; r++) {
                                GridRow row = visibleRows.get(r);
                                sql.append("UPDATE ").append(tableName).append(" SET ");
                                for (int c = minCol; c <= maxCol; c++) {
                                        sql.append(columns.get(c).getLabel()).append(" = '").append(row.get(c)).append("'");
                                        if (c < maxCol) sql.append(", ");
                                }
                                sql.append(" WHERE ");
                                Column pkCol = columns.stream().filter(Column::isPrimary).findFirst().orElse(null);
                                if (pkCol != null) {
                                        int pkIndex = columns.indexOf(pkCol);
                                        sql.append(pkCol.getLabel()).append(" = '").append(row.get(pkIndex)).append("';\n");
                                } else {
                                        sql.append("<COND> = ?;");
                                }
                        }
                }

                Application.copyToClipboard(sql.toString());
        }

        private void copyTableViewAsJSON(SerializationFeature ...features)
        {
                ObservableList<GridRow> rows = tableView.getSelectionModel().getSelectedItems();

                if (rows == null || rows.isEmpty())
                        return;

                List<Map<String, Object>> ret = new ArrayList<>();
                List<Column> columns = queryResult.getColumns();

                for (GridRow row : rows) {
                        Map<String, Object> jsonMap = new LinkedHashMap<>();
                        for (int k = 0; k < row.size(); k++) {
                                Column column = columns.get(k);
                                jsonMap.put(column.getLabel(), row.get(k));
                        }
                        ret.add(jsonMap);
                }

                Application.copyToClipboard(JSONUtils.toJSONString(ret, features));
        }

        public void selectResultSetFirst()
        {
                select(viewTab);
        }

        public void select(Tab tab)
        {
                if (tabPane.getSelectionModel().getSelectedItem() != tab)
                        tabPane.getSelectionModel().select(tab);
        }

        public ObservableList<Tab> getTabs()
        {
                return tabPane.getTabs();
        }

        /**
         * 当有数据被编辑时触发（不论是否修改）
         */
        private void commit(ModifyCell cell)
        {
                if (cell.isUnmodified())
                        return;

                queryResult.addUpdateRow(dataColumnIndex(cell.getColumnIndex()), cell.getRowIndex(), cell.getNewValue());
        }

        public void reload(String tableName, QueryResult queryResult)
        {
                this.tableName = tableName;
                
                if (this.queryResult != queryResult) {
                        this.queryResult = queryResult;
                        this.queryResult.setUpdateListener(r -> updateCheckCross());
                }

                tableView.getColumns().clear();

                /* 首列行选择列（类似 Navicat：点击/拖拽选择整行） */
                tableView.addRowSelectorColumn();

                if (!tabPane.getTabs().contains(viewTab))
                        tabPane.getTabs().addFirst(viewTab);

                setToolButtonStatus(queryResult.isAddable(), queryResult.isEditable());

                viewTab.setText(fmt("查询结果集 (%d条)", queryResult.getRows().size()));

                for (int i = 0; i < queryResult.getColumns().size(); i++) {
                        int index = i;

                        Column columnMetaData = queryResult.getColumns().get(i);
                        StringBuilder labelBuilder = new StringBuilder(columnMetaData.getLabel());

                        labelBuilder.append("\n# ")
                                .append(columnMetaData.getType());

                        if (columnMetaData.isPrimary())
                                labelBuilder.append(" ").append("PK");

                        String label = labelBuilder.toString();

                        TableColumn<GridRow, String> col =
                                new TableColumn<>(label);

                        col.setEditable(true);
                        col.setPrefWidth(calcColWidth(label, queryResult.getRows(), i));
                        col.setMaxWidth(1000);
                        col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(index)));

                        col.setCellFactory(c -> new VkTextFieldTableCell<>(this::commit, () -> activeKeyword));

                        tableView.getColumns().add(col);
                }

                syncGridRows();
                tableView.setItems(filteredRows);
                tableView.refresh();

                applySearchState();

                tableView.playFlash();
        }

        private static int calcColWidth(String colText, List<GridRow> values, int index)
        {
                int V = 12, MAX = 200;
                int SCALE = 1;

                if (colText.matches(".*[\\u4e00-\\u9fa5].*"))
                        SCALE = 2;

                int CM = colText.length() * SCALE;
                int CW = CM * V;

                for (List<String> value : values) {
                        String cellValue = value.get(index);

                        if (cellValue == null || cellValue.isEmpty())
                                continue;

                        if (cellValue.length() > CM)
                                CM = cellValue.length();
                }

                int FW = Math.max(CM * V, 64);

                return Math.min(Math.max(CW, FW), MAX); /* px */
        }

        /**
         * 首列是行选择列，表格列位置需要 -1 才是真实数据列索引。
         */
        private int dataColumnIndex(int tableColumnIndex)
        {
                return tableColumnIndex - 1;
        }

        /**
         * 将选中单元格的列位置区间换算为真实数据列区间（跳过首列行选择列）。
         */
        private int[] dataColumnRange(int tableMinCol, int tableMaxCol)
        {
                int min = Math.max(0, dataColumnIndex(tableMinCol));
                int max = dataColumnIndex(tableMaxCol);

                if (queryResult != null)
                        max = Math.min(max, queryResult.getColumns().size() - 1);

                return new int[] { min, max };
        }

}
