package valkyrie.app.pane;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import valkyrie.app.assets.Assets;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenTableDataPaneEvent;
import valkyrie.app.explorer.UITableContainerDynamicNode;
import valkyrie.app.explorer.UITableDynamicNode;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparator;
import valkyrie.app.widgets.VkTextField;
import valkyrie.app.widgets.table.VkTableColumn;
import valkyrie.app.widgets.table.VkTableView;
import valkyrie.app.widgets.table.cell.VkDateTableCell;
import valkyrie.driver.api.Table;

import java.util.Date;
import java.util.List;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * 表列表总览
 *
 * @author Luo Tiansheng
 * @since 2026/3/27
 */
@SuppressWarnings("FieldCanBeLocal")
public class TableListPane extends BorderPane
{
        private final UITableContainerDynamicNode tableContainerDynamicNode;
        private final TableView<Table> tableView;
        private final ToolBar toolBar;

        private TableColumn<Table, String> name;
        private TableColumn<Table, Date> createTime;
        private TableColumn<Table, Date> updateTime;
        private TableColumn<Table, String> engine;
        private TableColumn<Table, Float> size;
        private TableColumn<Table, String> rows;
        private TableColumn<Table, String> comment;

        private final VkTextField search = new VkTextField();
        private final ObservableList<Table> observable = FXCollections.observableArrayList();
        private final PauseTransition searchDelay = new PauseTransition(Duration.millis(100));


        public TableListPane(UITableContainerDynamicNode tableContainerDynamicNode)
        {
                this.tableContainerDynamicNode = tableContainerDynamicNode;

                tableView = new VkTableView<>(VkTableView.LITE_STYLE);
                tableView.setItems(observable);
                toolBar = new ToolBar();

                // setup
                setupToolBar();
                initializeColumn();
                setupCellFactory();
                setupTableView();

                setTop(toolBar);
                setCenter(tableView);

                update(tableContainerDynamicNode.getTables());
        }

        private void setupToolBar()
        {
                toolBar.setOrientation(Orientation.HORIZONTAL);

                Button modifyTable = new VkIconButton("编辑表", "modify");
                Button newTable = new VkIconButton("创建表", "plus");
                Button delTable = new VkIconButton("删除表", "minus");
                delTable.setOnAction(event -> deleteTable());

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                search.setPromptText("搜索...");
                search.setPrefWidth(300);

                HBox searchBox = new HBox(5, Assets.use("search"), search);
                searchBox.setAlignment(Pos.CENTER_LEFT);

                toolBar.getItems().addAll(
                        modifyTable,
                        newTable,
                        delTable,
                        new VkSeparator(),
                        spacer,
                        searchBox);
        }

        private void setupTableView()
        {
                tableView.setRowFactory(tv -> {
                        TableRow<Table> r = new TableRow<>();

                        r.setOnMouseClicked(e -> {
                                if (e.getClickCount() == 2 && !r.isEmpty()) {
                                        Table data = r.getItem();
                                        UITableDynamicNode tableDynamicNode =
                                                tableContainerDynamicNode.getTableDynamicNode(data.getName());
                                        EventBus.publish(new OpenTableDataPaneEvent(tableDynamicNode));
                                }
                        });

                        r.itemProperty().addListener((obs, oldItem, table) -> {
                                if (table == null) {
                                        r.setContextMenu(null);
                                        return;
                                }

                                UITableDynamicNode tableDynamicNode =
                                        tableContainerDynamicNode.getTableDynamicNode(table.getName());

                                if (tableDynamicNode != null)
                                        r.setContextMenu(tableDynamicNode.getContextMenu());
                        });

                        return r;
                });
        }

        private void deleteTable()
        {
        }

        @SuppressWarnings("unchecked")
        private void initializeColumn()
        {
                // 列
                name = new VkTableColumn<>("名称");
                createTime = new VkTableColumn<>("创建时间");
                updateTime = new VkTableColumn<>("更新时间");
                engine = new VkTableColumn<>("存储引擎");
                size = new VkTableColumn<>("表大小");
                rows = new VkTableColumn<>("数据条数");
                comment = new VkTableColumn<>("注释");

                // 属性配置
                name.setCellValueFactory(new PropertyValueFactory<>("name"));
                createTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
                updateTime.setCellValueFactory(new PropertyValueFactory<>("updateTime"));
                engine.setCellValueFactory(new PropertyValueFactory<>("engine"));
                size.setCellValueFactory(new PropertyValueFactory<>("size"));
                rows.setCellValueFactory(new PropertyValueFactory<>("rows"));
                comment.setCellValueFactory(new PropertyValueFactory<>("comment"));

                // 初始化宽度
                name.setPrefWidth(450);
                createTime.setPrefWidth(170);
                updateTime.setPrefWidth(170);
                engine.setPrefWidth(120);
                size.setPrefWidth(130);
                rows.setPrefWidth(100);
                comment.setPrefWidth(600);

                // 绑定列
                tableView.getColumns().addAll(
                        name,
                        createTime,
                        updateTime,
                        engine,
                        size,
                        rows,
                        comment
                );

                tableView.getColumns().forEach(col -> col.setReorderable(false));
        }

        private void setupCellFactory()
        {
                name.setCellFactory(col -> new TableCell<>()
                {
                        @Override
                        protected void updateItem(String item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (item != null) {
                                        setText(item);
                                        setGraphic(Assets.use("table"));
                                }
                        }
                });

                size.setCellFactory(col -> new TableCell<>()
                {
                        @Override
                        protected void updateItem(Float item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (item != null)
                                        setText(formatStorageSize(item.longValue()));
                        }
                });

                createTime.setCellFactory(col -> new VkDateTableCell<>());
                updateTime.setCellFactory(col -> new VkDateTableCell<>());
        }

        private void update(List<Table> tables)
        {
                long totalSize = tables.stream()
                        .mapToLong(t -> {
                                if (t.getSize() == null)
                                        return 0;
                                return t.getSize().longValue();
                        })
                        .sum();

                size.setText(fmt("磁盘 (%s)", formatStorageSize(totalSize)));
                observable.setAll(tables);
                tableView.refresh();
        }

        private static String formatStorageSize(long size)
        {
                final long K = 1024;
                final long M = K * 1024;
                final long G = M * 1024;
                final long T = G * 1024;

                String unit;
                double value;

                if (size > T) {
                        value = ((double) size / T);
                        unit = "T";
                } else if (size > G) {
                        value = ((double) size / G);
                        unit = "G";
                } else if (size > M) {
                        value = ((double) size / M);
                        unit = "M";
                } else if (size > K) {
                        value = ((double) size / K);
                        unit = "K";
                } else {
                        value = size;
                        unit = "B";
                }

                return fmt("%.2f%s", value, unit);
        }
}
