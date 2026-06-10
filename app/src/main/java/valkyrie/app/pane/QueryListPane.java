package valkyrie.app.pane;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import valkyrie.app.assets.Assets;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.RefreshTableNodeEvent;
import valkyrie.app.event.UpdateQueryFileEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.event.workbench.OpenQueryEditorPaneEvent;
import valkyrie.app.explorer.UIQueryContainerDynamicNode;
import valkyrie.app.explorer.UIQueryDynamicNode;
import valkyrie.app.widgets.VkTextField;
import valkyrie.app.widgets.VkToolBar;
import valkyrie.app.widgets.VkToolButton;
import valkyrie.app.widgets.table.VkTableColumn;
import valkyrie.app.widgets.table.VkTableView;
import valkyrie.app.widgets.table.cell.VkDateTableCell;
import valkyrie.core.model.QueryFile;

import java.util.Date;
import java.util.List;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * @author Luo Tiansheng
 * @since 2026/6/10
 * @noinspection DuplicatedCode
 */
@SuppressWarnings("FieldCanBeLocal")
public class QueryListPane extends BorderPane implements EventListener
{
        private final UIQueryContainerDynamicNode queryContainerDynamicNode;
        private final TableView<QueryFile> tableView;
        private final VkToolBar toolBar;

        private TableColumn<QueryFile, String> nameColumn;
        private TableColumn<QueryFile, String> creatingUserColumn;
        private TableColumn<QueryFile, Date> creatingTimeColumn;
        private TableColumn<QueryFile, Date> lastModifiedTimeColumn;
        private TableColumn<QueryFile, Date> lastAccessTimeColumn;
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

                EventBus.subscribe(this, RefreshQueryNodeEvent.class);
        }

        private void setupToolBar()
        {
                toolBar.setOrientation(Orientation.HORIZONTAL);

                Button delTable = new VkToolButton("删除查询", "minus");
                delTable.setOnAction(event -> deleteQuery());

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

                // subscribe
                EventBus.subscribe(this,
                        RefreshQueryNodeEvent.class,
                        UpdateQueryFileEvent.class);
        }

        private void setupTableView()
        {
                tableView.setRowFactory(tv -> {
                        TableRow<QueryFile> r = new TableRow<>();

                        r.setOnMouseClicked(e -> {
                                if (e.getClickCount() == 2 && !r.isEmpty()) {
                                        QueryFile queryFile = r.getItem();
                                        UIQueryDynamicNode queryDynamicNode =
                                                queryContainerDynamicNode.getQueryDynamicNode(queryFile);
                                        EventBus.publish(new OpenQueryEditorPaneEvent(queryDynamicNode));
                                }
                        });

                        r.itemProperty().addListener((obs, oldItem, queryFile) -> {
                                if (queryFile == null) {
                                        r.setContextMenu(null);
                                        return;
                                }

                                UIQueryDynamicNode queryDynamicNode =
                                        queryContainerDynamicNode.getQueryDynamicNode(queryFile);

                                if (queryDynamicNode != null)
                                        r.setContextMenu(queryDynamicNode.getContextMenu());
                        });

                        return r;
                });
        }

        private void deleteQuery()
        {
        }

        @SuppressWarnings({"unchecked", "CodeBlock2Expr"})
        private void initializeColumn()
        {
                // 列
                nameColumn = new VkTableColumn<>("名称");
                creatingUserColumn = new VkTableColumn<>("创建用户");
                creatingTimeColumn = new VkTableColumn<>("创建时间");
                lastModifiedTimeColumn = new VkTableColumn<>("最后更新时间");
                lastAccessTimeColumn = new VkTableColumn<>("最后访问时间");
                sizeColumn = new VkTableColumn<>("文件大小");

                // 属性配置
                nameColumn.setCellValueFactory(cellData -> {
                        QueryFile queryFile = cellData.getValue();
                        return new SimpleStringProperty(queryFile.getName());
                });

                creatingUserColumn.setCellValueFactory(cellData -> {
                        return new SimpleStringProperty(cellData.getValue().getOwner());
                });

                creatingTimeColumn.setCellValueFactory(cellData -> {
                        return new SimpleObjectProperty<>(cellData.getValue().getCreatingTime());
                });

                lastModifiedTimeColumn.setCellValueFactory(cellData -> {
                        return new SimpleObjectProperty<>(cellData.getValue().getLastModifiedTime());
                });

                lastAccessTimeColumn.setCellValueFactory(cellData -> {
                        return new SimpleObjectProperty<>(cellData.getValue().getLastAccessTime());
                });


                sizeColumn.setCellValueFactory(cellData -> {
                        QueryFile queryFile = cellData.getValue();
                        return new SimpleObjectProperty<>(queryFile.length());
                });

                // 初始化宽度
                nameColumn.setPrefWidth(450);
                creatingUserColumn.setPrefWidth(150);
                creatingTimeColumn.setPrefWidth(200);
                lastModifiedTimeColumn.setPrefWidth(200);
                lastAccessTimeColumn.setPrefWidth(200);
                sizeColumn.setPrefWidth(130);

                // 绑定列
                tableView.getColumns().addAll(
                        nameColumn,
                        creatingUserColumn,
                        creatingTimeColumn,
                        lastModifiedTimeColumn,
                        lastAccessTimeColumn,
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

                creatingTimeColumn.setCellFactory(col -> new VkDateTableCell<>());
                lastModifiedTimeColumn.setCellFactory(col -> new VkDateTableCell<>());
                lastAccessTimeColumn.setCellFactory(col -> new VkDateTableCell<>());
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
                if (event instanceof UpdateQueryFileEvent e)
                        if (e.getQueryContainerDynamicNode() == queryContainerDynamicNode)
                                update(queryContainerDynamicNode.getQueryFiles());

                if (event instanceof RefreshQueryNodeEvent e)
                        if (e.nodeEquals(queryContainerDynamicNode))
                                update(queryContainerDynamicNode.getQueryFiles());
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
