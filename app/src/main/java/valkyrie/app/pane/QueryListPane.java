package valkyrie.app.pane;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.RefreshTableNodeEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.event.workbench.OpenTableDataPaneEvent;
import valkyrie.app.explorer.UIQueryContainerDynamicNode;
import valkyrie.app.explorer.UITableContainerDynamicNode;
import valkyrie.app.explorer.UITableDynamicNode;
import valkyrie.app.widgets.VkSeparatorItem;
import valkyrie.app.widgets.VkTextField;
import valkyrie.app.widgets.VkToolBar;
import valkyrie.app.widgets.VkToolButton;
import valkyrie.app.widgets.table.VkTableColumn;
import valkyrie.app.widgets.table.VkTableView;
import valkyrie.app.widgets.table.cell.VkDateTableCell;
import valkyrie.core.model.QueryFile;
import valkyrie.driver.api.Table;

import java.util.Date;
import java.util.List;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * @author Luo Tiansheng
 * @since 2026/6/10
 */
@SuppressWarnings("FieldCanBeLocal")
public class QueryListPane extends BorderPane implements EventListener
{
        private final UIQueryContainerDynamicNode queryContainerDynamicNode;
        private final TableView<QueryFile> tableView;
        private final VkToolBar toolBar;

        private TableColumn<QueryFile, String> nameColumn;
        private TableColumn<QueryFile, String> createUserColumn;
        private TableColumn<QueryFile, Date> createTimeColumn;
        private TableColumn<QueryFile, String> updateUserColumn;
        private TableColumn<QueryFile, Date> updateTimeColumn;
        private TableColumn<QueryFile, String> accessUserColumn;
        private TableColumn<QueryFile, Date> accessTimeColumn;
        private TableColumn<QueryFile, Long> sizeColumn;

        private final VkTextField search = new VkTextField();
        private final ObservableList<QueryFile> observable = FXCollections.observableArrayList();
        private final PauseTransition searchDelay = new PauseTransition(Duration.millis(100));


        public QueryListPane(UIQueryContainerDynamicNode queryContainerDynamicNode)
        {
                this.queryContainerDynamicNode = queryContainerDynamicNode;

                tableView = new VkTableView<>(VkTableView.LITE_STYLE);
                tableView.setItems(observable);
                toolBar = new VkToolBar();

                // setup
                setupToolBar();
                initializeColumn();
                setupCellFactory();
                setupTableView();

                setTop(toolBar);
                setCenter(tableView);

                update(queryContainerDynamicNode.getQueryFiles());

                EventBus.subscribe(this, RefreshTableNodeEvent.class);
        }

        private void setupToolBar()
        {
                toolBar.setOrientation(Orientation.HORIZONTAL);

                Button delTable = new VkToolButton("删除查询", "minus");
                delTable.setOnAction(event -> deleteTable());

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                search.setPromptText("搜索...");
                search.setPrefWidth(300);

                HBox searchBox = new HBox(5, Assets.use("search"), search);
                searchBox.setAlignment(Pos.CENTER_LEFT);

                toolBar.getItems().addAll(
                        delTable,
                        spacer,
                        searchBox);
        }

        private void setupTableView()
        {
        }

        private void deleteTable()
        {
        }

        @SuppressWarnings("unchecked")
        private void initializeColumn()
        {
                // 列
                nameColumn = new VkTableColumn<>("名称");
                createUserColumn = new VkTableColumn<>("创建用户");
                createTimeColumn = new VkTableColumn<>("创建时间");
                updateUserColumn = new VkTableColumn<>("更新用户");
                updateTimeColumn = new VkTableColumn<>("更新时间");
                accessUserColumn = new VkTableColumn<>("访问用户");
                accessTimeColumn = new VkTableColumn<>("访问时间");
                sizeColumn = new VkTableColumn<>("文件大小");

                // 属性配置
                nameColumn.setCellValueFactory(cellData -> {
                        QueryFile queryFile = cellData.getValue();
                        return new SimpleStringProperty(queryFile.getName());
                });

                createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
                updateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("updateTime"));

                sizeColumn.setCellValueFactory(cellData -> {
                        QueryFile queryFile = cellData.getValue();
                        return new SimpleObjectProperty<>(queryFile.length());
                });

                // 初始化宽度
                nameColumn.setPrefWidth(450);
                createUserColumn.setPrefWidth(150);
                createTimeColumn.setPrefWidth(180);
                updateUserColumn.setPrefWidth(150);
                updateTimeColumn.setPrefWidth(180);
                accessUserColumn.setPrefWidth(150);
                accessTimeColumn.setPrefWidth(180);
                sizeColumn.setPrefWidth(130);

                // 绑定列
                tableView.getColumns().addAll(
                        nameColumn,
                        createUserColumn,
                        createTimeColumn,
                        updateUserColumn,
                        updateTimeColumn,
                        accessUserColumn,
                        accessTimeColumn,
                        sizeColumn
                );

                tableView.getColumns().forEach(col -> col.setReorderable(false));
        }

        private void setupCellFactory()
        {
                nameColumn.setCellFactory(col -> new TableCell<>()
                {
                        @Override
                        protected void updateItem(String item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (item != null) {
                                        setText(item);
                                        setGraphic(Assets.use("sql"));
                                }
                        }
                });

                sizeColumn.setCellFactory(col -> new TableCell<>()
                {
                        @Override
                        protected void updateItem(Long item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item != null)
                                        setText(formatStorageSize(item));
                        }
                });

                createTimeColumn.setCellFactory(col -> new VkDateTableCell<>());
                updateTimeColumn.setCellFactory(col -> new VkDateTableCell<>());
        }

        private void update(List<QueryFile> queryFiles)
        {
                long totalSize = queryFiles.stream()
                        .mapToLong(QueryFile::length)
                        .sum();

                sizeColumn.setText(fmt("磁盘 (%s)", formatStorageSize(totalSize)));
                observable.setAll(queryFiles);
                tableView.refresh();
        }

        @Override
        public void onEvent(Event event)
        {

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
