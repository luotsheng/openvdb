package valkyrie.app.workbench;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import javafx.application.Platform;
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
import valkyrie.app.dialog.ConfirmationDialog;
import valkyrie.app.dialog.queryFile.QueryFileSaveDialog;
import valkyrie.app.event.UpdateQueryFileEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.event.workbench.OpenTableDesignerPaneEvent;
import valkyrie.app.event.workbench.RegisterTabManagerEvent;
import valkyrie.app.explorer.UICatalogDynamicNode;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.explorer.UIQueryContainerDynamicNode;
import valkyrie.app.explorer.UIQueryDynamicNode;
import valkyrie.app.explorer.UISchemaDynamicNode;
import valkyrie.app.explorer.UITableContainerDynamicNode;
import valkyrie.app.explorer.UITableDynamicNode;
import valkyrie.app.pane.ExecuteLoggerPane;
import valkyrie.app.pane.QueryResultDataPane;
import valkyrie.app.utils.TabIdFactory;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.VkSeparatorItem;
import valkyrie.app.widgets.VkStatusBar;
import valkyrie.app.widgets.VkToolBar;
import valkyrie.app.widgets.VkToolButton;
import valkyrie.core.model.QueryFile;
import valkyrie.core.repository.QueryFileRepository;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.ProductMetaData;
import valkyrie.driver.api.QueryResult;
import valkyrie.driver.api.SQLExecuteCallback;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.DBNodePath;
import valkyrie.driver.api.sql.SQL;
import valkyrie.driver.suggestion.SuggestionEngine;
import valkyrie.monacofx.MonacoEditor;
import valkyrie.utils.exception.Causes;
import valkyrie.utils.io.IOUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static valkyrie.utils.string.StrStaticImports.strempty;

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
        private final VkToolBar toolBar;
        private final MonacoEditor editor = createMonacoEditor();
        private final BorderPane topBorderPane = new BorderPane();
        private final Tab sqlExecuteLoggerTab;
        private final ExecuteLoggerPane sqlExecuteLoggerPane;
        private final QueryResultDataPane queryResultDataPane;

        // Thread Pool
        private static final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

        // Selector
        private PathSelector pathSelector = new PathSelector(
                this::onPathSelectorUpdate
        );

        // Tool
        private Button runToolButton;
        private Button stopToolButton;
        private Button beautifyToolButton;

        // Driver
        private Driver driver;
        private Session session;
        private long taskId = System.currentTimeMillis();

        // SQL 智能提示上下文引擎（后台构建，FX 线程读取）
        private volatile SuggestionEngine suggestionEngine;

        // Other
        private Node oldGraphic;

        // File
        private QueryFile queryFile;

        public QueryEditor(Tab tab)
        {
                this(tab, null);
        }

        public QueryEditor(Tab tab, QueryFile file)
        {
                this.tab = tab;
                this.queryFile = file;

                tab.addEventFilter(Tab.TAB_CLOSE_REQUEST_EVENT, e -> {
                        editor.dispose();
                });

                tab.setContent(this);

                toolBar = createToolBar();

                if (queryFile != null)
                        initializeMonacoEditorValue(queryFile);

                // sql logger
                queryResultDataPane = new QueryResultDataPane(tab, false);
                sqlExecuteLoggerPane = new ExecuteLoggerPane();
                sqlExecuteLoggerTab = new Tab("执行日志");
                sqlExecuteLoggerTab.setClosable(false);
                sqlExecuteLoggerTab.setContent(sqlExecuteLoggerPane);

                setupBorderPane();
                setupShortcutEvent();

                // event subscribe
                EventBus.subscribe(this, UpdateQueryFileEvent.class);
        }

        private VkToolBar createToolBar()
        {
                VkToolBar toolBar = new VkToolBar();

                runToolButton = new VkToolButton("运行", "run");
                runToolButton.setText("运行");
                runToolButton.setOnAction(e -> runTask());

                stopToolButton = new VkToolButton("停止运行", "stop");
                stopToolButton.setText("停止");
                stopToolButton.setDisable(true);
                stopToolButton.setOnAction(e -> stopTask());

                beautifyToolButton = new VkToolButton("美化SQL", "beautify");
                beautifyToolButton.setText("美化SQL");
                beautifyToolButton.setOnAction(e -> beautifySQL());

                toolBar.getItems().addAll(
                        pathSelector.getConnectionComboBox(),
                        pathSelector.getCatalogComboBox(),
                        pathSelector.getSchemaComboBox(),
                        new VkSeparatorItem(),
                        runToolButton,
                        stopToolButton,
                        new VkSeparatorItem(),
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

                editor.setOnDidChangeCursorSelection((line, column, selected) -> {
                        if (isActiveTab())
                                VkStatusBar.getInstance().setCursor(line, column, selected);
                });

                editor.setSuggestionProvider((sql, offset) -> {
                        SuggestionEngine engine = suggestionEngine;
                        return engine == null ? List.of() : engine.resolve(sql, offset);
                });

                editor.setOnOpenTableLink(this::openTableDesigner);

                editor.setTableCommentProvider(name -> {
                        SuggestionEngine engine = suggestionEngine;
                        return engine == null ? null : engine.tableComment(name);
                });

                // Context Menu
                ContextMenu contextMenu = createContextMenu(editor);
                editor.bindContextMenu(contextMenu);

                return editor;
        }

        private boolean isActiveTab()
        {
                return tab.getTabPane() != null
                        && tab.getTabPane().getSelectionModel().getSelectedItem() == tab;
        }

        /**
         * 处理编辑器内按住 Shortcut 键点击标识符：仅当它是当前会话下的表时，
         * 跳转并打开该表的设计器。
         */
        private void openTableDesigner(String name)
        {
                String tableName = stripQuotes(name);

                if (tableName == null || tableName.isBlank())
                        return;

                SuggestionEngine engine = suggestionEngine;

                if (engine == null || !engine.hasTable(tableName))
                        return;

                /* 优先复用资源树中已加载的表节点，保证 tabId 与从树打开的完全一致 */
                UITableDynamicNode node = findTableNode(tableName);

                if (node != null) {
                        node.openDesignTablePane();
                        return;
                }

                Driver driver = pathSelector.getDriver();

                if (driver == null)
                        return;

                EventBus.publish(new OpenTableDesignerPaneEvent(
                        pathSelector.getSession(),
                        driver,
                        new Table(tableName),
                        pathLabel(),
                        connectionLabel()));
        }

        private UITableDynamicNode findTableNode(String tableName)
        {
                UIExplorerNode pathNode = pathSelector.getSelectedSchema();

                if (pathNode == null)
                        pathNode = pathSelector.getSelectedCatalog();

                if (pathNode == null)
                        return null;

                for (TreeItem<String> child : pathNode.getChildren()) {
                        if (!(child instanceof UITableContainerDynamicNode container))
                                continue;

                        for (TreeItem<String> table : container.getChildren()) {
                                if (table instanceof UITableDynamicNode node
                                        && node.getTable().getName().equalsIgnoreCase(tableName))
                                        return node;
                        }
                }

                return null;
        }

        private String pathLabel()
        {
                UISchemaDynamicNode schema = pathSelector.getSelectedSchema();

                if (schema != null)
                        return schema.getLabel();

                UICatalogDynamicNode catalog = pathSelector.getSelectedCatalog();

                return catalog == null ? "" : catalog.getLabel();
        }

        private String connectionLabel()
        {
                var connection = pathSelector.getSelectedConnection();
                return connection == null ? "" : connection.getLabel();
        }

        private static String stripQuotes(String value)
        {
                if (value == null)
                        return null;

                String text = value.trim();

                if (text.length() < 2)
                        return text;

                char first = text.charAt(0);
                char last = text.charAt(text.length() - 1);

                if ((first == '`' && last == '`')
                        || (first == '"' && last == '"')
                        || (first == '[' && last == ']'))
                        return text.substring(1, text.length() - 1);

                return text;
        }

        private void initializeMonacoEditorValue(QueryFile queryFile)
        {
                if (queryFile != null) {
                        String fileContent = IOUtils.strread(queryFile);
                        editor.setValue(fileContent);
                }

                editor.setOnDidChangeModelContent(() -> {
                        if (queryFile != null)
                                writeQueryFile();
                });
        }

        private void setupBorderPane()
        {
                topBorderPane.setTop(toolBar);
                topBorderPane.setCenter(editor);
                setOrientation(Orientation.VERTICAL);
                getItems().add(topBorderPane);
        }

        private void onPathSelectorUpdate(Driver driver, Session session)
        {
                updateStatusContext(driver, session);
                updateEditorSuggestions(driver, session);
        }

        /**
         * 将当前编辑器的连接上下文推送到状态栏
         */
        public void publishStatus()
        {
                if (pathSelector == null)
                        return;

                updateStatusContext(pathSelector.getDriver(), pathSelector.getSession());
        }

        private void updateStatusContext(Driver driver, Session session)
        {
                VkStatusBar statusBar = VkStatusBar.getInstance();

                if (driver == null) {
                        statusBar.clearContext();
                        return;
                }

                /*
                 * PathSelector 的构造过程会在字段赋值完成前回调到这里，
                 * 此时 pathSelector 仍为 null，需要容错，等后续选中变化时再补上。
                 */
                PathSelector selector = pathSelector;

                if (selector != null) {
                        var connection = selector.getSelectedConnection();
                        statusBar.setConnection(
                                connection == null ? null : connection.getLabel(),
                                session.catalog(),
                                session.schema());
                }

                ProductMetaData meta = driver.getProductMetaData();
                String database = meta != null && meta.getProductName() != null
                        ? meta.getProductName() + " " + meta.getVersion()
                        : driver.getType().getAlias();

                statusBar.setDatabase(database);
        }

        /**
         * Driver 和 Session 可能为 NULL，需要校验
         */
        private void updateEditorSuggestions(Driver driver, Session session)
        {
                if (driver == null || (session.catalog() == null && session.schema() == null))
                        return;

                Platform.runLater(() -> VkStatusBar.getInstance().setTask("正在加载提示引擎…"));

                singleThreadExecutor.execute(() -> {
                        try {
                                suggestionEngine = SuggestionEngine.of(driver, session);
                        } catch (Exception e) {
                                LOG.warn("构建 SQL 提示数据失败", e);
                        } finally {
                                Platform.runLater(VkStatusBar.getInstance()::clearTask);
                        }
                });
        }

        //////////////////////////////////////////////////////////////////////
        ///                           CONTEXT MENU                         ///
        //////////////////////////////////////////////////////////////////////

        public ContextMenu createContextMenu(MonacoEditor editor)
        {
                VkContextMenu contextMenu = new VkContextMenu();

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

                MenuItem cutItem = new MenuItem("剪切");
                cutItem.setOnAction(event -> editor.trigger("editor.action.clipboardCutAction"));
                cutItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN)
                );

                MenuItem selectAllItem = new MenuItem("全选");
                selectAllItem.setOnAction(event -> editor.trigger("editor.action.selectAll"));
                selectAllItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.A, KeyCodeCombination.SHORTCUT_DOWN)
                );

                MenuItem commentItem = new MenuItem("注释/取消注释");
                commentItem.setOnAction(event -> editor.trigger("editor.action.commentLine"));

                MenuItem upperCaseItem = new MenuItem("转大写");
                upperCaseItem.setOnAction(event -> editor.trigger("editor.action.transformToUppercase"));

                MenuItem lowerCaseItem = new MenuItem("转小写");
                lowerCaseItem.setOnAction(event -> editor.trigger("editor.action.transformToLowercase"));

                editor.setShowContextMenuRequestEvent(ignored -> {
                        /* 有选中并且没有任务运行时才启用菜单 */
                        String selectedValue = editor.getSelectedValue();
                        boolean isTaskRunning = runToolButton.isDisable();
                        boolean disable = strempty(selectedValue) || isTaskRunning;
                        runSelectedSQLItem.setDisable(disable);
                });

                contextMenu.getItems().addAll(
                        runSelectedSQLItem,
                        beautifySelectedSQLItem,
                        new SeparatorMenuItem(),
                        copyItem,
                        cutItem,
                        pasteItem,
                        selectAllItem,
                        new SeparatorMenuItem(),
                        commentItem,
                        upperCaseItem,
                        lowerCaseItem
                );

                return contextMenu;
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
                        } finally {
                                Platform.runLater(() -> tab.setGraphic(oldGraphic));
                        }
                });
        }

        private void updateButtonForExecuting(boolean value)
        {
                if (Platform.isFxApplicationThread()) {
                        runToolButton.setDisable(value);
                        stopToolButton.setDisable(!value);
                        return;
                }

                Platform.runLater(() -> {
                        runToolButton.setDisable(value);
                        stopToolButton.setDisable(!value);
                });
        }

        private void stopTask()
        {
                if (pathSelector.getDriver() != null)
                        pathSelector.getDriver().cancel(taskId);
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

                /* 更新按钮状态 */
                updateButtonForExecuting(true);
                VkStatusBar.getInstance().setTask("执行中…");

                StringBuilder selectedText = new StringBuilder();
                selectedText.append(editor.getSelectedValue());

                if (selectedText.isEmpty())
                        selectedText.append(editor.getValue());

                Driver driver = pathSelector.getDriver();
                Session session = pathSelector.getSession();

                useProgressIndicator(() -> {
                        int[] rows = { -1 };
                        long[] cost = { -1 };

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
                                                rows[0] = value;

                                                Platform.runLater(() -> {
                                                        sqlExecuteLoggerPane.appendRow(value);
                                                        showExecuteLoggerPane();
                                                });
                                        }

                                        @Override
                                        public void cost(long time)
                                        {
                                                cost[0] = time;

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
                                                reportExecution("查询", queryResult.getRows().size(), cost[0]);
                                        });
                                } else {
                                        Platform.runLater(() -> {
                                                showPane(QUERY_EXECUTE_LOGGER_FIRST);
                                                reportExecution(rows[0] >= 0 ? "影响" : "执行完成", rows[0], cost[0]);
                                        });
                                }
                        } catch (Throwable e) {
                                Platform.runLater(() -> {
                                        LOG.error("run task error", e);
                                        sqlExecuteLoggerPane.appendError(Causes.message(e));
                                        showPane(QUERY_EXECUTE_LOGGER_FIRST);
                                        VkStatusBar.getInstance().setExecution("执行失败");
                                });
                                throw e;
                        } finally {
                                updateButtonForExecuting(false);
                                Platform.runLater(VkStatusBar.getInstance()::clearTask);
                        }
                });
        }

        /**
         * 将最近一次执行结果推送到状态栏
         */
        private static void reportExecution(String action, int rowCount, long cost)
        {
                StringBuilder builder = new StringBuilder(action);

                if (rowCount >= 0)
                        builder.append(" ").append(rowCount).append(" 行");

                if (cost >= 0)
                        builder.append(" · ").append(cost).append(" ms");

                VkStatusBar.getInstance().setExecution(builder.toString());
        }

        //////////////////////////////////////////////////////////////////////
        ///                           SHORTCUT                             ///
        //////////////////////////////////////////////////////////////////////

        public void setupShortcutEvent()
        {
                setOnKeyPressed(event -> {
                        if (event.isShortcutDown()) {
                                switch (event.getCode()) {
                                        case R -> runTask();
                                        case S -> writeQueryFile();
                                        default -> {}
                                }
                                event.consume();
                        }


                });
        }

        //////////////////////////////////////////////////////////////////////
        ///                           SAVE FILE                            ///
        //////////////////////////////////////////////////////////////////////

        private void writeQueryFile()
        {
                String content = editor.getValue();

                if (queryFile != null) {
                        QueryFileRepository.write(queryFile, content);
                        return;
                }

                PathSelector pathSelector = new PathSelector();
                pathSelector.useSelector(this.pathSelector);

                QueryFile tmpQueryFile = QueryFileSaveDialog.showDialog(pathSelector);

                if (tmpQueryFile == null)
                        return;

                if (!tmpQueryFile.exists()) {
                        writeNewQueryFile(tmpQueryFile, pathSelector, content);
                        return;
                }

                if (ConfirmationDialog.showDialog("已存在相同名称的查询脚本，是否覆盖？")) {
                        tmpQueryFile.forceDelete();
                        writeNewQueryFile(tmpQueryFile, pathSelector, content);
                }
        }

        private void writeNewQueryFile(QueryFile tmpQueryFile, PathSelector selector, String content)
        {
                pathSelector.useSelector(selector);

                UIQueryContainerDynamicNode queryContainerDynamicNode = getQueryContainerDynamicNode();

                this.queryFile = QueryFileRepository.write(tmpQueryFile, content);

                UIQueryDynamicNode queryDynamicNode = new UIQueryDynamicNode(queryContainerDynamicNode, tmpQueryFile);
                queryContainerDynamicNode.getChildren().add(queryDynamicNode);

                queryContainerDynamicNode.getRoot()
                        .getTreeView()
                        .getSelectionModel()
                        .select(queryDynamicNode);

                tab.setText(TabIdFactory.buildQueryTabId(queryDynamicNode));

                EventBus.publish(new RegisterTabManagerEvent(queryDynamicNode, tab));
        }

        private UIQueryContainerDynamicNode getQueryContainerDynamicNode()
        {
                DBNodePath tail = pathSelector.getNodePath().tail();
                return switch (tail.kind()) {
                        case CATALOG -> pathSelector.getSelectedCatalog().getQueryContainerNode();
                        case SCHEMA -> pathSelector.getSelectedSchema().getQueryContainerNode();
                        default -> throw new UnsupportedOperationException("不支持的 NodeKind: " + tail.kind());
                };
        }

        //////////////////////////////////////////////////////////////////////
        ///                             EVENT                              ///
        //////////////////////////////////////////////////////////////////////

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof UpdateQueryFileEvent updateEvent) {
                        if (updateEvent.getOldQueryFile() == queryFile) {
                                queryFile = updateEvent.getNewQueryFile();
                                tab.setText(TabIdFactory.buildQueryTabId(updateEvent.getQueryDynamicNode()));
                        }
                }
        }
}

