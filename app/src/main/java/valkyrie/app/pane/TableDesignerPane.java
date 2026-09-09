package valkyrie.app.pane;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import valkyrie.app.assets.Assets;
import valkyrie.app.pane.designer.TableColumnDesignerPane;
import valkyrie.app.pane.designer.TableIndexDesignerPane;
import valkyrie.app.widgets.VkSeparatorItem;
import valkyrie.app.widgets.VkTextField;
import valkyrie.app.widgets.VkToolBar;
import valkyrie.app.widgets.VkToolButton;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.app.widgets.table.VkDataTableView;
import valkyrie.app.widgets.table.VkTableColumnFactory;
import valkyrie.app.widgets.table.VkTableView;
import valkyrie.app.widgets.table.cell.VkCheckBoxTableCell;
import valkyrie.app.widgets.table.cell.VkComboBoxTableCell;
import valkyrie.app.widgets.table.cell.VkTextFieldTableCell;
import valkyrie.driver.api.*;
import valkyrie.driver.mysql.MySQL;

import java.util.Collection;
import java.util.List;

import static valkyrie.utils.string.StrStaticImports.lowercase;

/**
 * @author Luo Tiansheng
 * @since 2026/4/3
 */
@SuppressWarnings("unchecked")
public class TableDesignerPane extends BorderPane
{
        private final Tab ownerTab;
        private final Session session;
        private final Driver driver;
        private final Table table;
        private final VkTableView<Column> structureView = new VkDataTableView<>();
        private final VkTableView<Index> indexView = new VkDataTableView<>();
        private final VkToolBar toolBar = new VkToolBar();
        private final TabPane tabPane = new TabPane();
        private final Tab structureTab = new Tab("表结构");
        private final Tab indexTab = new Tab("索引");

        private final Button saveButton = new VkToolButton("保存", "storage");
        private final Button plusButton = new VkToolButton("新增行", "plus");
        private final Button minusButton = new VkToolButton("删除行", "minus");
        private final Button reloadButton = new VkToolButton("刷新", "reload");

        private Node oldGraphic;
        private final ProgressIndicator progressIndicator = Assets.newProgressIndicator();

        private List<Column> columnMetaDatas;
        private List<Index> indexes;

        private final VkTextField search = new VkTextField();
        private final PauseTransition searchDelay = new PauseTransition(Duration.millis(100));
        private final ObservableList<Column> structureSource = FXCollections.observableArrayList();
        private final FilteredList<Column> filteredStructure = new FilteredList<>(structureSource, c -> true);
        private final ObservableList<Index> indexSource = FXCollections.observableArrayList();
        private final FilteredList<Index> filteredIndex = new FilteredList<>(indexSource, i -> true);

        private final Designer<Column> tableStructureDesigner;
        private final Designer<Index> indexColumnDesigner;

        /* 当前选中标签对应的设计接口 */
        private Designer<?> designer;

        public TableDesignerPane(Tab ownerTab, Session session, Driver driver, Table table)
        {
                this.ownerTab = ownerTab;
                this.session = session;
                this.driver = driver;
                this.table = table;
                this.columnMetaDatas = driver.getColumns(session, table.getName());
                this.indexes = driver.getIndexes(session, table);

                this.tableStructureDesigner = new TableColumnDesignerPane(session, driver, table, "表结构");
                this.indexColumnDesigner = new TableIndexDesignerPane(session, driver, table, "索引");

                setupToolBar();

                setupStructureView();
                setupIndexView();
                setupSearch();

                applyReload();

                structureTab.setClosable(false);
                structureTab.setGraphic(Assets.use("struct1"));

                BorderPane borderPane = new BorderPane();
                borderPane.setCenter(structureView);
                structureTab.setContent(borderPane);

                indexTab.setClosable(false);
                indexTab.setContent(indexView);
                indexTab.setGraphic(Assets.use("index0"));

                setupTabPane();

                setTop(toolBar);
                setCenter(tabPane);
        }

        private void setupTabPane()
        {
                tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal == structureTab) {
                                designer = tableStructureDesigner;
                        } else if (newVal == indexTab) {
                                designer = indexColumnDesigner;
                        } else {
                                throw new UnsupportedOperationException("Unsupported tab");
                        }
                });

                tabPane.getTabs().addAll(
                        structureTab,
                        indexTab
                );
        }

        private void setupToolBar()
        {
                saveButton.setOnAction(e -> onSave());
                plusButton.setOnAction(e -> onPlus());
                minusButton.setOnAction(e -> onMinus());
                reloadButton.setOnAction(e -> applyReload());

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                search.setPromptText("搜索...");
                search.setPrefWidth(220);

                HBox searchBox = new HBox(5, Assets.use("search"), search);
                searchBox.setAlignment(Pos.CENTER_LEFT);

                toolBar.getItems().addAll(
                        saveButton,
                        new VkSeparatorItem(),
                        plusButton,
                        minusButton,
                        new VkSeparatorItem(),
                        reloadButton,
                        spacer,
                        searchBox
                );
        }

        /**
         * 搜索接线：同时过滤表结构/索引两个列表（按名称、类型、注释等关键字，忽略大小写）。
         * 搜索期间禁用“新增行/删除行”，避免行号与真实数据错位。
         */
        private void setupSearch()
        {
                searchDelay.setOnFinished(event -> applySearch());

                search.textProperty().addListener((obs, oldVal, newVal) -> searchDelay.playFromStart());
        }

        private void applySearch()
        {
                String text = search.getText();
                boolean searching = text != null && !text.isBlank();

                if (searching) {
                        String keyword = lowercase(text).trim();
                        filteredStructure.setPredicate(c -> match(c.getName(), keyword)
                                || match(c.getType(), keyword)
                                || match(c.getDefaultValue(), keyword)
                                || match(c.getComment(), keyword));

                        filteredIndex.setPredicate(i -> match(i.getName(), keyword)
                                || match(i.getColumnsText(), keyword)
                                || match(i.getType(), keyword));
                } else {
                        filteredStructure.setPredicate(c -> true);
                        filteredIndex.setPredicate(i -> true);
                }

                plusButton.setDisable(searching);
                minusButton.setDisable(searching);
        }

        private static boolean match(String text, String keyword)
        {
                return text != null && lowercase(text).contains(keyword);
        }

        private void onSave()
        {
                try {
                        designer.onSave();
                        applyReload();
                } catch (Exception e) {
                        VkDialogHelper.alert(e);
                }
        }

        private void onPlus()
        {
                switch (designer) {
                        case TableColumnDesignerPane inst -> {
                                Column columnMetaData = new Column();
                                inst.onPlus(columnMetaData);
                                structureSource.add(columnMetaData);
                                structureView.refresh();
                        }

                        case TableIndexDesignerPane inst -> {
                                Index indexMetaData = new Index();
                                inst.onPlus(indexMetaData);
                                indexSource.add(indexMetaData);
                                indexView.refresh();
                        }

                        default -> throw new UnsupportedOperationException("Unsupported designer object instance");
                }

        }

        private void onMinus()
        {
                ObservableList<?> items = switch (designer) {
                        case TableColumnDesignerPane ignored -> structureView.getSelectionModel().getSelectedItems();
                        case TableIndexDesignerPane ignored -> indexView.getSelectionModel().getSelectedItems();
                        default -> FXCollections.emptyObservableList();
                };

                if (items.isEmpty())
                        return;

                if (!VkDialogHelper.askDangerous("确认删除%d条数据？", items.size()))
                        return;

                beginReload();
                minusButton.setDisable(true);

                new Thread(() -> {
                        try {
                                switch (designer) {
                                        case TableColumnDesignerPane inst -> inst.onMinus((Collection<Column>) items);
                                        case TableIndexDesignerPane inst -> inst.onMinus((Collection<Index>) items);
                                        default -> throw new UnsupportedOperationException("Unsupported designer object instance");
                                }
                                doReload();
                        } catch (Exception e) {
                                Platform.runLater(() -> VkDialogHelper.alert(e));
                        } finally {
                                Platform.runLater(() -> {
                                        endReload();
                                        minusButton.setDisable(false);
                                });
                        }
                }).start();
        }

        private void beginReload()
        {
                Platform.runLater(() -> {
                        reloadButton.setDisable(true);
                        oldGraphic = ownerTab.getGraphic();
                        ownerTab.setGraphic(progressIndicator);
                });
        }

        private void endReload()
        {
                Platform.runLater(() -> {
                        reloadButton.setDisable(false);
                        ownerTab.setGraphic(oldGraphic);
                });
        }

        private void applyReload()
        {
                beginReload();
                new Thread(() -> {
                        try {
                                doReload();
                        } finally {
                                endReload();
                        }
                }).start();
        }

        private void doReload()
        {
                this.columnMetaDatas = driver.getColumns(session, table);
                tableStructureDesigner.onReload(columnMetaDatas);

                this.indexes = driver.getIndexes(session, table);
                indexColumnDesigner.onReload(indexes);

                Platform.runLater(() -> {
                        structureSource.setAll(columnMetaDatas);
                        indexSource.setAll(indexes);

                        structureView.refresh();
                        structureView.playFlash();

                        indexView.refresh();
                        indexView.playFlash();
                });
        }

        private void setupStructureView()
        {
                structureView.setItems(filteredStructure);
                structureView.setEditable(true);
                structureView.getSelectionModel().setCellSelectionEnabled(true);
                structureView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                VkTableColumnFactory<Column> factory = new VkTableColumnFactory<>();

                factory.setOnEditCommitEventListener(tableStructureDesigner::onCommitEdit);

                // 列
                TableColumn<Column, String> name = factory.createEditableColumn("名称", "name");
                TableColumn<Column, String> type = factory.createEditableColumn("类型", "type");
                TableColumn<Column, String> defaultValue = factory.createEditableColumn("默认值", "defaultValue");
                TableColumn<Column, Boolean> notNull = factory.createEditableColumn("不允许NULL", "notNull");
                TableColumn<Column, Boolean> primary = factory.createEditableColumn("主键", "primary");
                TableColumn<Column, Boolean> autoIncrement = factory.createEditableColumn("是否自增", "autoIncrement");
                TableColumn<Column, String> comment = factory.createEditableColumn("注释", "comment");

                name.setCellFactory(c -> new VkTextFieldTableCell<>());
                type.setCellFactory(c -> new VkTextFieldTableCell<>());
                defaultValue.setCellFactory(c -> new VkTextFieldTableCell<>());
                comment.setCellFactory(c -> new VkTextFieldTableCell<>());
                notNull.setCellFactory(c -> new VkCheckBoxTableCell<>());
                primary.setCellFactory(c -> new VkCheckBoxTableCell<>());
                autoIncrement.setCellFactory(c -> new VkCheckBoxTableCell<>());

                // 初始化宽度
                name.setPrefWidth(150);
                type.setPrefWidth(150);
                defaultValue.setPrefWidth(200);
                notNull.setPrefWidth(120);
                primary.setPrefWidth(50);
                autoIncrement.setPrefWidth(100);
                comment.setPrefWidth(280);

                structureView.getColumns().addAll(
                        name,
                        type,
                        notNull,
                        primary,
                        autoIncrement,
                        defaultValue,
                        comment
                );
        }

        private void setupIndexView()
        {
                indexView.setItems(filteredIndex);
                indexView.enableCellEdit();

                VkTableColumnFactory<Index> factory = new VkTableColumnFactory<>();

                factory.setOnEditCommitEventListener(indexColumnDesigner::onCommitEdit);

                TableColumn<Index, String> name = factory.createEditableColumn("名称", "name");
                TableColumn<Index, String> columns = factory.createEditableColumn("索引列", "columnsText");
                TableColumn<Index, String> type = factory.createEditableColumn("类型", "type");

                name.setCellFactory(c -> new VkTextFieldTableCell<>());
                columns.setCellFactory(c -> new VkTextFieldTableCell<>());
                type.setCellFactory(c -> new VkComboBoxTableCell<>(driver.getIndexTypes()));

                name.setPrefWidth(150);
                columns.setPrefWidth(350);
                type.setPrefWidth(150);

                indexView.getColumns().addAll(name, columns, type);

                if (driver.getProductMetaData().getMajorVersion() >= MySQL.VERSION_8x) {
                        TableColumn<Index, Boolean> visible = factory.createEditableColumn("是否可见", "visible");
                        visible.setCellValueFactory(new PropertyValueFactory<>("visible"));
                        visible.setCellFactory(c -> new VkCheckBoxTableCell<>());
                        visible.setPrefWidth(120);
                        indexView.getColumns().addLast(visible);
                }
        }
}
