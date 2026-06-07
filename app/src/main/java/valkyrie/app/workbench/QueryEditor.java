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
import valkyrie.app.dialog.queryFile.QueryFileOverwriteDialog;
import valkyrie.app.dialog.queryFile.QueryFileSaveDialog;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.pane.ExecuteLoggerPane;
import valkyrie.app.pane.QueryResultDataPane;
import valkyrie.app.utils.Threads;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparator;
import valkyrie.core.model.QueryFile;
import valkyrie.core.repository.QueryFileRepository;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.QueryResult;
import valkyrie.driver.api.SQLExecuteCallback;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.sql.SQL;
import valkyrie.monacofx.MonacoEditor;
import valkyrie.utils.exception.Causes;
import valkyrie.utils.io.IOUtils;

import static valkyrie.utils.string.StaticLibrary.strempty;

/**
 * SQL 脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/29
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class QueryEditor extends SplitPane
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

        // Selector
        private final PathSelector pathSelector = new PathSelector();

        // Tool
        private Button runToolButton;
        private Button stopToolButton;
        private Button beautifyToolButton;

        // Driver
        private long taskId = System.currentTimeMillis();

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

                tab.setContent(this);

                toolBar = createToolBar();
                editor = createMonacoEditor();

                // sql logger
                queryResultDataPane = new QueryResultDataPane(tab, false);
                sqlExecuteLoggerPane = new ExecuteLoggerPane();
                sqlExecuteLoggerTab = new Tab("执行日志");
                sqlExecuteLoggerTab.setClosable(false);
                sqlExecuteLoggerTab.setContent(sqlExecuteLoggerPane);

                setupBorderPane();
                setupShortcutEvent();
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

                toolBar.getItems().addAll(
                        pathSelector.getConnectionComboBox(),
                        pathSelector.getCatalogComboBox(),
                        pathSelector.getSchemaComboBox(),
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

                if (queryFile != null) {
                        String fileContent = IOUtils.strread(queryFile);
                        editor.setValue(fileContent);
                }

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
                runToolButton.setDisable(value);
                stopToolButton.setDisable(!value);
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

                updateButtonForExecuting(true);
                String selectedText = editor.getSelectedValue();

                Driver driver = pathSelector.getDriver();
                Session session = pathSelector.getSession();

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
                setOnKeyPressed(event -> {
                        if (event.isShortcutDown()) {
                                switch (event.getCode()) {
                                        case R -> runTask();
                                        case S -> saveToFile();
                                        default -> {}
                                }
                                event.consume();
                        }


                });
        }

        //////////////////////////////////////////////////////////////////////
        ///                           SAVE FILE                            ///
        //////////////////////////////////////////////////////////////////////

        private void saveToFile()
        {
                String content = editor.getValue();

                if (queryFile == null) {
                        QueryFile tmpQueryFile = QueryFileSaveDialog.showDialog();
                        if (tmpQueryFile != null) {
                                if (tmpQueryFile.exists()) {
                                        overwriteQueryFile(tmpQueryFile, content);
                                } else {
                                        _realSaveToFile(tmpQueryFile, content);
                                }
                        }
                } else {
                        QueryFileRepository.save(queryFile, content);
                }
        }

        private void overwriteQueryFile(QueryFile tmpQueryFile, String content)
        {
                if (QueryFileOverwriteDialog.showDialog()) {
                        tmpQueryFile.forceDelete();
                        _realSaveToFile(tmpQueryFile, content);
                }
        }

        private void _realSaveToFile(QueryFile queryFile, String content)
        {
                QueryFileRepository.save(queryFile, content);
                this.queryFile = queryFile;
                EventBus.publish(new RefreshQueryNodeEvent());
        }
}

