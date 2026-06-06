package valkyrie.app.workbench;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.Application;
import valkyrie.app.assets.Assets;
import valkyrie.app.event.CatalogDynamicNodeInitializedEvent;
import valkyrie.app.event.ConnectedSuccessEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.explorer.*;
import valkyrie.app.pane.ExecuteLoggerPane;
import valkyrie.app.pane.QueryResultDataPane;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkComboBox;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparator;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.QueryResult;
import valkyrie.driver.api.SQLExecuteCallback;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.driver.api.node.DBNodePath;
import valkyrie.driver.api.sql.SQL;
import valkyrie.monacofx.MonacoEditor;
import valkyrie.utils.exception.Causes;

import static valkyrie.utils.string.StaticLibrary.strempty;

/**
 * SQL 脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/29
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class QueryEditor extends SplitPane implements EventListener
{
        private static final Logger LOG = LoggerFactory.getLogger(QueryEditor.class);

        static final int QUERY_RESULT_SET_FIRST = 0;
        static final int QUERY_EXECUTE_LOGGER_FIRST = 1;
        
        private final Tab tab;
        private final ToolBar toolBar;
        private final MonacoEditor editor;
        private final BorderPane topBorderPane = new BorderPane();
        private final Tab sqlExecuteLoggerTab;
        private final ExecuteLoggerPane sqlExecuteLoggerPane;
        private final QueryResultDataPane queryResultDataPane;

        // ComboBox
        private final VkComboBox<UIConnectionNode> connectionComboBox = new VkComboBox<>();
        private final VkComboBox<UICatalogDynamicNode> catalogComboBox = new VkComboBox<>();
        private final VkComboBox<UISchemaDynamicNode> schemaComboBox = new VkComboBox<>();

        private UIConnectionNode selectedConnectionNode = null;
        private UICatalogDynamicNode selectedCatalogDynamicNode = null;
        private UISchemaDynamicNode selectedSchemaDynamicNode = null;

        // Tool
        private Button runToolButton;
        private Button stopToolButton;
        private Button beautifyToolButton;

        // Driver
        private Driver driver;
        private Session session = new Session();
        private DBNodePath dbNodePath;
        private long taskId = System.currentTimeMillis();

        // Other
        private Node oldGraphic;

        public QueryEditor(Tab tab)
        {
                this.tab = tab;
                tab.setContent(this);

                toolBar = createToolBar();
                editor = createMonacoEditor();

                // sql logger
                queryResultDataPane = new QueryResultDataPane(tab, false);
                sqlExecuteLoggerPane = new ExecuteLoggerPane();
                sqlExecuteLoggerTab = new Tab("执行日志");
                sqlExecuteLoggerTab.setClosable(false);
                sqlExecuteLoggerTab.setContent(sqlExecuteLoggerPane);

                setupComboBox();
                setupBorderPane();
                setupShortcutEvent();

                // subscribe
                EventBus.subscribe(ConnectedSuccessEvent.class, this);
                EventBus.subscribe(CatalogDynamicNodeInitializedEvent.class, this);
        }

        private ToolBar createToolBar()
        {
                ToolBar toolBar = new ToolBar();

                runToolButton = new VkIconButton("运行已选择", "run");
                runToolButton.setText("运行");
                runToolButton.setOnAction(e -> runTask());

                stopToolButton = new VkIconButton("停止运行", "stop");
                stopToolButton.setText("停止");
                stopToolButton.setDisable(true);
                stopToolButton.setOnAction(e -> stopTask());

                beautifyToolButton = new VkIconButton("美化SQL", "beautify");
                beautifyToolButton.setText("美化SQL");
                beautifyToolButton.setOnAction(e -> beautifySQL());

                schemaComboBox.setHidden(true);

                toolBar.getItems().addAll(
                        connectionComboBox,
                        catalogComboBox,
                        schemaComboBox,
                        new VkSeparator(),
                        runToolButton,
                        stopToolButton,
                        new VkSeparator(),
                        beautifyToolButton);

                return toolBar;
        }

        private MonacoEditor createMonacoEditor()
        {
                MonacoEditor editor = new MonacoEditor();

                editor.setWebViewOnKeyPressedEvent(event -> {
                        if (event.isShortcutDown() && event.getCode() == KeyCode.C)
                                Application.copyToClipboard(editor.getSelectedValue());
                });

                // Context Menu
                ContextMenu contextMenu = createContextMenu(editor);
                editor.bindContextMenu(contextMenu);

                return editor;
        }

        private void setupBorderPane()
        {
                topBorderPane.setTop(toolBar);
                topBorderPane.setCenter(editor);
                setOrientation(Orientation.VERTICAL);
                getItems().add(topBorderPane);
        }

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof ConnectedSuccessEvent connectedSuccessEvent) {
                        if (connectedSuccessEvent.getConnectionNode() == selectedConnectionNode)
                                updateConnectionNodeComboBox(selectedConnectionNode);
                }

                if (event instanceof CatalogDynamicNodeInitializedEvent catalogDynamicNodeInitializedEvent) {
                        if (catalogDynamicNodeInitializedEvent.getDynamicNode() == selectedCatalogDynamicNode)
                                updateSchemaDynamicNodeComboBox(selectedCatalogDynamicNode);
                }
        }

        //////////////////////////////////////////////////////////////////////
        ///                           CONTEXT MENU                         ///
        //////////////////////////////////////////////////////////////////////

        public ContextMenu createContextMenu(MonacoEditor editor)
        {
                ContextMenu contextMenu = new ContextMenu();

                MenuItem runSelectedSQLItem = new MenuItem("运行已选择");
                runSelectedSQLItem.setGraphic(Assets.use("run"));
                runSelectedSQLItem.setOnAction(e -> runTask());
                runSelectedSQLItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN)
                );

                MenuItem beautifySelectedSQLItem = new MenuItem("美化已选择");
                beautifySelectedSQLItem.setGraphic(Assets.use("beautify"));
                beautifySelectedSQLItem.setOnAction(e -> beautifySQL());

                MenuItem copyItem = new MenuItem("复制");
                copyItem.setOnAction(event -> Application.copyToClipboard(editor.getSelectedValue()));
                copyItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN)
                );

                MenuItem pasteItem = new MenuItem("粘贴");
                pasteItem.setOnAction(event -> editor.replaceSelection(Application.getClipboardText()));
                pasteItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN)
                );

                editor.setShowContextMenuRequestEvent(ignored -> {
                        /* 有选中并且没有任务运行时才启用菜单 */
                        String selectedValue = editor.getSelectedValue();
                        boolean isTaskRunning = runToolButton.isDisable();
                        boolean disable = strempty(selectedValue) || isTaskRunning;
                        runSelectedSQLItem.setDisable(isTaskRunning);
                });

                contextMenu.getItems().addAll(
                        runSelectedSQLItem,
                        beautifySelectedSQLItem,
                        new SeparatorMenuItem(),
                        copyItem,
                        pasteItem
                );

                return contextMenu;
        }

        //////////////////////////////////////////////////////////////////////
        ///                        ON SELECTED EVENT                       ///
        //////////////////////////////////////////////////////////////////////

        private void onSelectedConnectionNode(UIConnectionNode connectionNode)
        {
                this.selectedConnectionNode = connectionNode;

                if (!connectionNode.isConnect())
                        connectionNode.connect();
        }

        private void onSelectedCatalogDynamicNode(UICatalogDynamicNode catalogDynamicNode)
        {
                this.selectedCatalogDynamicNode = catalogDynamicNode;
                session.setCatalog(catalogDynamicNode.getLabel());

                if (!catalogDynamicNode.isInitialized())
                        catalogDynamicNode.initialize();
        }

        private void onSelectedSchemaDynamicNode(UISchemaDynamicNode schemaDynamicNode)
        {
                this.selectedSchemaDynamicNode = schemaDynamicNode;
                session.setSchema(schemaDynamicNode.getLabel());
        }

        //////////////////////////////////////////////////////////////////////
        ///                       UPDATE COMBO BOX                         ///
        //////////////////////////////////////////////////////////////////////

        private static <Node extends UIExplorerNode> void configureComboBox(VkComboBox<Node> comboBox)
        {
                comboBox.setButtonCell(new ListCell<>()
                {
                        @Override
                        protected void updateItem(Node item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getLabel());
                                setGraphic(item.createGraphic());
                        }
                });

                comboBox.setCellFactory(list -> new ListCell<>()
                {
                        @Override
                        protected void updateItem(Node item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getLabel());
                                setGraphic(item.createGraphic());
                        }
                });
        }

        private void setupComboBox()
        {
                configureComboBox(connectionComboBox);
                configureComboBox(catalogComboBox);
                configureComboBox(schemaComboBox);

                // connection
                connectionComboBox.getSelectionModel().selectedItemProperty()
                        .addListener((obs, oldVal, newVal) -> {
                                if (newVal != null)
                                        onSelectedConnectionNode(newVal);
                });

                catalogComboBox.getSelectionModel().selectedItemProperty()
                        .addListener((obs, oldVal, newVal) -> {
                                if (newVal != null)
                                        onSelectedCatalogDynamicNode(newVal);
                });

                schemaComboBox.getSelectionModel().selectedItemProperty()
                        .addListener((obs, oldVal, newVal) -> {
                                if (newVal != null)
                                        onSelectedSchemaDynamicNode(newVal);
                        });

                // 初始化 ComboBox 数据
                initializeFromCurrentSelectedNode();
        }

        private void initializeFromCurrentSelectedNode()
        {
                for (UIConnectionNode connectionNode :
                        GlobalDynamicNodeContext.getConnectionNodes())
                        connectionComboBox.getItems().add(connectionNode);

                UIExplorerNode node =
                        GlobalDynamicNodeContext.getSelectedExplorerNode();

                if (node == null)
                        return;

                switch (node) {
                        case UIConnectionNode connectionNode ->
                                restoreConnection(connectionNode);

                        case UICatalogDynamicNode catalogDynamicNode ->
                                restoreCatalog(catalogDynamicNode);

                        case UISchemaDynamicNode schemaDynamicNode ->
                                restoreSchema(schemaDynamicNode);

                        default ->
                                throw new UnsupportedOperationException("不支持节点类型：" + node);
                }
        }

        private void restoreConnection(UIConnectionNode connectionNode)
        {
                connectionComboBox.getSelectionModel().select(connectionNode);
                updateConnectionNodeComboBox(connectionNode);
        }

        private void restoreCatalog(UICatalogDynamicNode catalogDynamicNode)
        {
                UIConnectionNode connectionNode =
                        (UIConnectionNode) catalogDynamicNode.getParent();

                restoreConnection(connectionNode);

                catalogComboBox.getSelectionModel()
                        .select(catalogDynamicNode);

                updateSchemaDynamicNodeComboBox(catalogDynamicNode);
        }

        private void restoreSchema(UISchemaDynamicNode schemaDynamicNode)
        {
                if (schemaDynamicNode.getParent() instanceof UIConnectionNode connectionNode)
                        restoreConnection(connectionNode);

                if (schemaDynamicNode.getParent() instanceof UICatalogDynamicNode catalogDynamicNode) {
                        restoreCatalog(catalogDynamicNode);
                }

                if (!schemaDynamicNode.isInitialized())
                        schemaDynamicNode.initialize();

                schemaComboBox.getSelectionModel()
                        .select(schemaDynamicNode);
        }

        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        private void updateConnectionNodeComboBox(UIConnectionNode connectionNode)
        {
                driver = connectionNode.getDriver();
                dbNodePath = driver.getNodeHierarchyPath();

                catalogComboBox.setHidden(true);
                schemaComboBox.setHidden(true);

                // parent
                switch (dbNodePath.kind()) {
                        case CATALOG -> {
                                catalogComboBox.setHidden(false);
                                setCatalogComboBoxItem(connectionNode);
                        }
                        case SCHEMA -> {
                                schemaComboBox.setHidden(false);
                                setSchemaComboBoxItem(connectionNode);
                        }
                        default ->
                                throw new UnsupportedOperationException("查询编辑器 Parent 不支持类型：" + dbNodePath.kind());
                }

                // child
                DBNodePath child = dbNodePath.child();
                if (child != null) {
                        switch (child.kind()) {
                                case SCHEMA -> schemaComboBox.setHidden(false);
                                default ->
                                        throw new UnsupportedOperationException("查询 Child 编辑器不支持类型：" + dbNodePath.kind());
                        }
                }
        }

        private void updateSchemaDynamicNodeComboBox(UICatalogDynamicNode catalogDynamicNode)
        {
                DBNodePath child = dbNodePath.child();
                if (child != null && child.kind() == DBNodeKind.SCHEMA)
                        setSchemaComboBoxItem(catalogDynamicNode);
        }

        private void setCatalogComboBoxItem(UIExplorerNode parentNode)
        {
                ObservableList<TreeItem<String>> children = parentNode.getChildren();

                for (TreeItem<String> child : children) {
                        UICatalogDynamicNode catalogDynamicNode = (UICatalogDynamicNode) child;
                        catalogComboBox.getItems().add(catalogDynamicNode);
                }
        }

        private void setSchemaComboBoxItem(UIExplorerNode parentNode)
        {
                ObservableList<TreeItem<String>> children = parentNode.getChildren();

                for (TreeItem<String> child : children) {
                        UISchemaDynamicNode schemaDynamicNode = (UISchemaDynamicNode) child;
                        schemaComboBox.getItems().add(schemaDynamicNode);
                }

        }

        //////////////////////////////////////////////////////////////////////
        ///                           RUN TASK                             ///
        //////////////////////////////////////////////////////////////////////

        private void showExecuteLoggerPane()
        {
                showPane(QUERY_EXECUTE_LOGGER_FIRST);
        }

        private void showPane(int flag)
        {
                if (!queryResultDataPane.getTabs().contains(sqlExecuteLoggerTab))
                        queryResultDataPane.getTabs().add(sqlExecuteLoggerTab);

                switch (flag) {
                        case QUERY_RESULT_SET_FIRST -> queryResultDataPane.selectResultSetFirst();
                        case QUERY_EXECUTE_LOGGER_FIRST -> queryResultDataPane.select(sqlExecuteLoggerTab);
                }

                if (!getItems().contains(queryResultDataPane))
                        getItems().add(queryResultDataPane);
        }

        private void useProgressIndicator(Runnable runnable)
        {
                ProgressIndicator progressIndicator = Assets.newProgressIndicator();
                oldGraphic = tab.getGraphic();
                tab.setGraphic(progressIndicator);

                Threads.start(() -> {
                        try {
                                runnable.run();
                        } catch (Exception e) {
                                Platform.runLater(() -> VkDialogHelper.alert(e));
                        } finally {
                                Platform.runLater(() -> tab.setGraphic(oldGraphic));
                        }
                });
        }

        private void updateButtonForExecuting(boolean value)
        {
                runToolButton.setDisable(value);
                stopToolButton.setDisable(!value);
        }

        private void stopTask()
        {
                if (driver != null)
                        driver.cancel(taskId);
        }

        private void beautifySQL()
        {
                String selected = editor.getSelectedValue();

                String formatted = SqlFormatter.format(selected);
                editor.replaceSelection(formatted);
        }

        public void runTask()
        {
                if (runToolButton.isDisabled())
                        return;

                updateButtonForExecuting(true);
                String selectedText = editor.getSelectedValue();

                useProgressIndicator(() -> {
                        try {
                                taskId = System.currentTimeMillis();
                                SQL sql = new SQL(selectedText);

                                QueryResult queryResult = driver.execute(taskId, session, sql, new SQLExecuteCallback() {
                                        @Override
                                        public void execute(String sql)
                                        {
                                                Platform.runLater(() -> {
                                                        sqlExecuteLoggerPane.appendExecute(sql);
                                                        showExecuteLoggerPane();
                                                });
                                        }

                                        @Override
                                        public void executeQuery(String sql, boolean skip)
                                        {
                                                Platform.runLater(() -> {
                                                        sqlExecuteLoggerPane.appendExecuteQuery(sql);
                                                        showExecuteLoggerPane();
                                                });
                                        }

                                        @Override
                                        public void executeUpdate(String sql)
                                        {
                                                Platform.runLater(() -> {
                                                        sqlExecuteLoggerPane.appendExecuteUpdate(sql);
                                                        showExecuteLoggerPane();
                                                });
                                        }

                                        @Override
                                        public void row(int value)
                                        {
                                                Platform.runLater(() -> {
                                                        sqlExecuteLoggerPane.appendRow(value);
                                                        showExecuteLoggerPane();
                                                });
                                        }

                                        @Override
                                        public void cost(long time)
                                        {
                                                Platform.runLater(() -> {
                                                        sqlExecuteLoggerPane.appendCost(time);
                                                        showExecuteLoggerPane();
                                                });
                                        }
                                });

                                if (queryResult != null) {
                                        Platform.runLater(() -> {
                                                queryResultDataPane.reload(sql.getSingleTableName(), queryResult);
                                                showPane(QUERY_RESULT_SET_FIRST);
                                        });
                                } else {
                                        Platform.runLater(() -> showPane(QUERY_EXECUTE_LOGGER_FIRST));
                                }
                        } catch (Throwable e) {
                                Platform.runLater(() -> {
                                        LOG.error("run task error", e);
                                        sqlExecuteLoggerPane.appendError(Causes.message(e));
                                        showPane(QUERY_EXECUTE_LOGGER_FIRST);
                                });
                                throw e;
                        } finally {
                                updateButtonForExecuting(false);
                        }
                });
        }

        //////////////////////////////////////////////////////////////////////
        ///                           SHORTCUT                             ///
        //////////////////////////////////////////////////////////////////////

        public void setupShortcutEvent()
        {
                /* Ctrl + R */
                setOnKeyPressed(event -> {
                        if ((event.isShortcutDown())
                                && event.getCode() == KeyCode.R) {
                                runTask();
                                event.consume();
                        }
                });
        }

}

