package valkyrie.app.workbench;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.Application;
import valkyrie.app.assets.Assets;
import valkyrie.app.dialog.SaveScriptDialog;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.event.workbench.ConnectionOpenedNotifyEvent;
import valkyrie.app.explorer.UICatalogNode;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.explorer.UITableNode;
import valkyrie.app.model.UIExplorerStatus;
import valkyrie.app.pane.ResultViewPane;
import valkyrie.app.pane.ExecuteLoggerPane;
import valkyrie.app.widgets.VkComboBox;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparator;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.core.model.ScriptFile;
import valkyrie.core.repository.ScriptFileRepository;
import valkyrie.driver.api.*;
import valkyrie.driver.api.sql.SQL;
import valkyrie.monacofx.MonacoEditor;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.exception.Causes;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static valkyrie.utils.string.StaticLibrary.fmt;
import static valkyrie.utils.string.StaticLibrary.strempty;

/**
 * SQL 脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/29
 */
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class ScriptEditor extends SplitPane implements EventListener
{
        private static final Logger LOG = LoggerFactory.getLogger(ScriptEditor.class);

        static final int QUERY_RESULT_SET_FIRST = 0;
        static final int QUERY_MESSAGE_LOG_FIRST = 1;

        @Getter
        private final Tab owner;
        private final ToolBar toolBar;
        private final MonacoEditor editor;
        private final BorderPane topBorderPane;
        private final ResultViewPane resultViewPane;
        private final Tab sqlMessageTab;
        private final ExecuteLoggerPane sqlMessagePane;

        @Getter
        private String name;
        private Node oldGraphic;
        @Getter
        private ScriptFile scriptFile;
        private Driver driver = null;
        private long currentTaskId = System.currentTimeMillis();
        private boolean saveFlag = true;
        private VkComboBox<UIConnectionNode> connectionComboBox;
        private VkComboBox<UICatalogNode> catalogComboBox;
        private UICatalogNode currentCatalogNode;

        private static int numberCount = 0;

        private Button run;
        private Button stop;
        private Button beautify;

        private MenuItem copyItem;
        private MenuItem pasteItem;
        private MenuItem runSelectedSQLItem;
        private MenuItem beautifySelectedSQLItem;

        public ScriptEditor(UIConnectionNode conn, ScriptFile scriptFile, Tab owner)
        {
                this.scriptFile = scriptFile;

                this.name = scriptFile != null
                        ? scriptFile.getName()
                        : fmt("查询脚本_%s.sql@[ N/A ]", (numberCount++));

                this.owner = owner;

                setOrientation(Orientation.VERTICAL);

                topBorderPane = new BorderPane();
                toolBar = new ToolBar();
                editor = createEditor();
                resultViewPane = new ResultViewPane(owner, false);
                sqlMessagePane = new ExecuteLoggerPane();

                // 绑定日志标签
                sqlMessageTab = new Tab("日志");
                sqlMessageTab.setClosable(false);
                sqlMessageTab.setContent(sqlMessagePane);

                topBorderPane.setTop(toolBar);
                topBorderPane.setCenter(editor);

                if (scriptFile != null)
                        setScriptFile(scriptFile);

                setupPane();
                setupToolbar(conn);
                setupCodeArea();
                setupResultSetCloseEvent();
                setOwnerTabName(name);

                getItems().addAll(topBorderPane);

                initializeDriver();

                EventBus.subscribe(ConnectionOpenedNotifyEvent.class, this);
        }

        private void dispose()
        {
                EventBus.unscribe(ConnectionOpenedNotifyEvent.class, this);

                if (driver != null)
                        driver.cancel(currentTaskId);

                editor.dispose();
        }

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof ConnectionOpenedNotifyEvent e) {
                        UIConnectionNode selectedItem = connectionComboBox.getSelectionModel().getSelectedItem();
                        if (selectedItem == e.connection) {
                                List<UICatalogNode> catalogNodes = selectedItem.getCatalogNodes();
                                catalogComboBox.getItems().setAll(catalogNodes);
                        }
                }
        }

        private MonacoEditor createEditor()
        {
                MonacoEditor editor = new MonacoEditor();
                ContextMenu contextMenu = new ContextMenu();

                editor.setWebViewOnKeyPressedEvent(event -> {
                        if (event.isShortcutDown() && event.getCode() == KeyCode.C)
                                Application.copyToClipboard(editor.getSelectedValue());
                });

                runSelectedSQLItem = new MenuItem("运行当前选择的");
                runSelectedSQLItem.setGraphic(Assets.use("run"));
                runSelectedSQLItem.setOnAction(event -> runTask());
                runSelectedSQLItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN)
                );

                beautifySelectedSQLItem = new MenuItem("美化当前选择的");
                beautifySelectedSQLItem.setGraphic(Assets.use("beautify"));
                beautifySelectedSQLItem.setOnAction(event -> beautifySQL());

                copyItem = new MenuItem("复制");
                copyItem.setOnAction(event -> Application.copyToClipboard(editor.getSelectedValue()));
                copyItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN)
                );

                pasteItem = new MenuItem("粘贴");
                pasteItem.setOnAction(event -> editor.replaceSelection(Application.getClipboardText()));
                pasteItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN)
                );

                editor.bindContextMenu(contextMenu);
                editor.setShowContextMenuRequestEvent(ignored -> {
                        contextMenu.getItems().clear();

                        UITableNode uiTableNode;

                        if (currentCatalogNode != null) {
                                String selectedValue = editor.getSelectedValue();
                                uiTableNode = currentCatalogNode.getUITableNodeIgnoreCase(selectedValue);
                        } else {
                                uiTableNode = null;
                        }

                        if (uiTableNode != null) {
                                MenuItem openTableDesignItem = new MenuItem("打开设计表");
                                openTableDesignItem.setGraphic(Assets.use("table"));
                                openTableDesignItem.setOnAction(event -> uiTableNode.openTableDesignerPane());
                                contextMenu.getItems().addAll(
                                        runSelectedSQLItem,
                                        beautifySelectedSQLItem,
                                        new SeparatorMenuItem(),
                                        openTableDesignItem,
                                        new SeparatorMenuItem(),
                                        copyItem,
                                        pasteItem
                                );
                        } else {
                                contextMenu.getItems().addAll(
                                        runSelectedSQLItem,
                                        beautifySelectedSQLItem,
                                        new SeparatorMenuItem(),
                                        copyItem,
                                        pasteItem
                                );
                        }

                        boolean isUnselected = strempty(editor.getSelectedValue());
                        runSelectedSQLItem.setDisable(isUnselected);
                        beautifySelectedSQLItem.setDisable(isUnselected);
                        copyItem.setDisable(isUnselected);

                        boolean emptyClipboardContent = strempty(Application.getClipboardText());
                        pasteItem.setDisable(emptyClipboardContent);
                });

                return editor;
        }

        private void setupPane()
        {
                addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                        if ((event.isShortcutDown())
                                && event.getCode() == KeyCode.S) {
                                save();
                        }
                });
        }

        private void setupToolbar(UIConnectionNode initConnection)
        {
                UIExplorerStatus instance = UIExplorerStatus.getInstance();
                UIConnectionNode selectedConnection = initConnection != null
                        ? initConnection
                        : instance.getSelectedConnection();

                run = new VkIconButton("运行已选择", "run");
                run.setText("运行");
                run.setOnAction(event -> runTask());

                stop = new VkIconButton("停止当时运行", "stop");
                stop.setText("停止");
                stop.setDisable(true);
                stop.setOnAction(event -> stopTask());

                beautify = new VkIconButton("美化 SQL", "beautify");
                beautify.setText("美化 SQL");
                beautify.setOnAction(event -> beautifySQL());

                connectionComboBox = newConnectionComboBox();
                catalogComboBox = newCatalogComboBox();

                // setup combo
                connectionComboBox.getItems().addAll(instance.getConnections());

                if (selectedConnection != null) {
                        connectionComboBox.getSelectionModel().select(selectedConnection);
                        catalogComboBox.getItems().addAll(selectedConnection.getCatalogNodes());
                        catalogComboBox.getSelectionModel().select(selectedConnection.getSelectedDatabase());
                }

                toolBar.getItems().addAll(
                        connectionComboBox,
                        catalogComboBox,
                        new VkSeparator(),
                        run,
                        stop,
                        new VkSeparator(),
                        beautify);

        }

        public void setupCodeArea()
        {
                setOnKeyPressed(event -> {
                        if ((event.isShortcutDown())
                                && event.getCode() == KeyCode.R) {
                                runTask();
                                event.consume();
                        }
                });
        }

        private void setupResultSetCloseEvent()
        {
                resultViewPane.setOnClosedListener(() ->
                        getItems().remove(resultViewPane));
        }

        private UICatalogNode initializeDriver()
        {
                UIConnectionNode connection = connectionComboBox.getSelectionModel()
                        .getSelectedItem();

                if (connection == null)
                        return null;

                UICatalogNode catalog = catalogComboBox.getSelectionModel()
                        .getSelectedItem();

                this.driver = connection.getDriver();
                this.currentCatalogNode = catalog;

                return catalog;
        }

        private VkComboBox<UIConnectionNode> newConnectionComboBox()
        {
                VkComboBox<UIConnectionNode> connection = new VkComboBox<>();
                configureConnectionComboBox(connection);
                connection.setPrefWidth(200);

                /* 选中连接时打开连接 */
                connection.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal.isOpen())
                                newVal.openConnection();
                });

                return connection;
        }

        private VkComboBox<UICatalogNode> newCatalogComboBox()
        {
                VkComboBox<UICatalogNode> catalog = new VkComboBox<>();
                configureDatabaseComboBox(catalog);
                catalog.setPrefWidth(200);

                catalog.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal.isOpen())
                                newVal.openDatabase();
                });

                return catalog;
        }

        private void configureConnectionComboBox(VkComboBox<UIConnectionNode> comboBox)
        {
                comboBox.getSelectionModel().selectedItemProperty()
                        .addListener((obs, oldVal, newVal) -> {
                                editor.registerSuggestion(newVal.getCatalogSuggestion());
                                newVal.getCatalogNodes().forEach(catalog ->
                                        editor.registerSuggestion(catalog.getTableNameSuggestions()));
                        });

                comboBox.setOnAction(event -> {
                        UIConnectionNode item = comboBox.getSelectionModel().getSelectedItem();
                        if (item != null) {
                                catalogComboBox.getItems().setAll(item.getCatalogNodes());
                        }
                });

                comboBox.setButtonCell(new ListCell<>()
                {
                        @Override
                        protected void updateItem(UIConnectionNode item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getName());
                                setGraphic(item.getIcon());
                        }
                });

                comboBox.setCellFactory(list -> new ListCell<>()
                {
                        @Override
                        protected void updateItem(UIConnectionNode item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getName());
                                setGraphic(item.getIcon());
                        }
                });

                comboBox.setConverter(new StringConverter<>()
                {
                        @Override
                        public String toString(UIConnectionNode connection)
                        {
                                return connection == null ? null : connection.getName();
                        }

                        @Override
                        public UIConnectionNode fromString(String s)
                        {
                                return null;
                        }
                });
        }

        private void configureDatabaseComboBox(VkComboBox<UICatalogNode> comboBox)
        {
                comboBox.getSelectionModel().selectedItemProperty()
                                .addListener((obs, oldVal, newVal) -> {
                                        editor.registerSuggestion(newVal.getSuggestion());
                                });

                comboBox.setButtonCell(new ListCell<>()
                {
                        @Override
                        protected void updateItem(UICatalogNode item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getName());
                                setGraphic(Assets.use("database1"));
                        }
                });

                comboBox.setCellFactory(list -> new ListCell<>()
                {
                        @Override
                        protected void updateItem(UICatalogNode item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getName());
                                setGraphic(Assets.use("database1"));
                        }
                });

                comboBox.setConverter(new StringConverter<>()
                {
                        @Override
                        public String toString(UICatalogNode catalog)
                        {
                                return catalog == null ? null : catalog.getName();
                        }

                        @Override
                        public UICatalogNode fromString(String s)
                        {
                                return null;
                        }
                });
        }

        /**
         * 开始执行 true，结束执行 false
         */
        private void updateButtonForExecuting(boolean value)
        {
                run.setDisable(value);
                stop.setDisable(!value);
        }

        private void setLoadingIndicator()
        {
                oldGraphic = owner.getGraphic();
                owner.setGraphic(Assets.newProgressIndicator());
        }

        private void removeLoadingIndicator()
        {
                owner.setGraphic(oldGraphic);
        }

        private void showLog()
        {
                showResultSetTableViewPane(QUERY_MESSAGE_LOG_FIRST);
        }

        private void showResultSetTableViewPane(int flag)
        {
                if (!resultViewPane.getTabs().contains(sqlMessageTab))
                        resultViewPane.getTabs().add(sqlMessageTab);

                switch (flag) {
                        case QUERY_RESULT_SET_FIRST -> resultViewPane.selectResultSetFirst();
                        case QUERY_MESSAGE_LOG_FIRST -> resultViewPane.select(sqlMessageTab);
                }

                if (!getItems().contains(resultViewPane))
                        getItems().add(resultViewPane);
        }

        /**
         * 执行任务
         */
        private void runTask()
        {
                if (run.isDisable())
                        return;

                updateButtonForExecuting(true);
                setLoadingIndicator();

                Platform.runLater(() -> {
                        String scriptText = editor.getSelectedValue();

                        if (scriptText == null || scriptText.isEmpty())
                                scriptText = editor.getValue();

                        String finalScriptText = scriptText;

                        new Thread(() -> {
                                try {
                                        UICatalogNode catalog = initializeDriver();

                                        if (catalog == null) {
                                                Platform.runLater(() -> VkDialogHelper.alert("未选择数据源"));
                                                return;
                                        }

                                        Session session = catalog.getSession();
                                        currentTaskId = System.currentTimeMillis();
                                        SQL sql = new SQL(finalScriptText);

                                        QueryResult queryResult = driver.execute(currentTaskId, session, sql, new SQLExecuteCallback()
                                        {
                                                @Override
                                                public void execute(String sql)
                                                {
                                                        Platform.runLater(() -> {
                                                                sqlMessagePane.appendExecute(sql);
                                                                showLog();
                                                        });
                                                }

                                                @Override
                                                public void executeQuery(String sql, boolean skip)
                                                {
                                                        Platform.runLater(() -> {
                                                                sqlMessagePane.appendExecuteQuery(sql);
                                                                showLog();
                                                        });
                                                }

                                                @Override
                                                public void executeUpdate(String sql)
                                                {
                                                        Platform.runLater(() -> {
                                                                sqlMessagePane.appendExecuteUpdate(sql);
                                                                showLog();
                                                        });
                                                }

                                                @Override
                                                public void row(int value)
                                                {
                                                        Platform.runLater(() -> {
                                                                sqlMessagePane.appendRow(value);
                                                                showLog();
                                                        });
                                                }

                                                @Override
                                                public void cost(long time)
                                                {
                                                        Platform.runLater(() -> {
                                                                sqlMessagePane.appendCost(time);
                                                                showLog();
                                                        });
                                                }
                                        });

                                        if (queryResult != null) {
                                                Platform.runLater(() -> {
                                                        resultViewPane.reload(sql.getSingleTableName(), queryResult);
                                                        showResultSetTableViewPane(QUERY_RESULT_SET_FIRST);
                                                });
                                        } else {
                                                Platform.runLater(() -> showResultSetTableViewPane(QUERY_MESSAGE_LOG_FIRST));
                                        }
                                } catch (Throwable e) {
                                        Platform.runLater(() -> {
                                                LOG.error("run task error", e);
                                                sqlMessagePane.appendError(Causes.message(e));
                                                showResultSetTableViewPane(QUERY_MESSAGE_LOG_FIRST);
                                        });
                                } finally {
                                        Platform.runLater(() -> {
                                                updateButtonForExecuting(false);
                                                removeLoadingIndicator();
                                        });
                                }
                        }).start();
                });
        }

        private void stopTask()
        {
                if (driver != null)
                        driver.cancel(currentTaskId);
        }

        private void beautifySQL()
        {
                String selected = editor.getSelectedValue();

                String formatted = SqlFormatter.format(selected);
                editor.replaceSelection(formatted);
        }

        public String getCodeAreaContent()
        {
                return editor.getValue();
        }

        public VkComboBox<UIConnectionNode> copyConnectionComboBox()
        {
                VkComboBox<UIConnectionNode> dst = connectionComboBox.copyComboBox();
                configureConnectionComboBox(dst);
                return dst;
        }

        public VkComboBox<UICatalogNode> copyDatabaseComboBox()
        {
                VkComboBox<UICatalogNode> dst = catalogComboBox.copyComboBox();
                configureDatabaseComboBox(dst);
                return dst;
        }

        public void setScriptFile(ScriptFile file)
        {
                if (file != null) {
                        this.scriptFile = file;
                        setOwnerTabName(file.getName());

                        StringBuilder builder = new StringBuilder();
                        char[] buf = new char[(int) file.length()];

                        try (FileReader rw = new FileReader(file)) {
                                int count = rw.read(buf);
                                builder.append(buf, 0, count);
                        } catch (Throwable e) {
                                VkDialogHelper.alert(e);
                        }

                        editor.clear();
                        editor.setValue(builder.toString().trim());


                }
        }

        private void setOwnerTabName(String name)
        {
                UICatalogNode catalog = null;

                if (catalogComboBox != null)
                        catalog = catalogComboBox.getSelectionModel().getSelectedItem();

                String tail = catalog == null
                        ? ""
                        : "@" + catalog.getName();

                owner.setText(name + tail);
        }

        private void autoSave()
        {
                if (scriptFile == null)
                        return;

                save();
        }

        private void save()
        {
                String content = getCodeAreaContent();

                if (scriptFile == null) {
                        String saveScriptName = SaveScriptDialog.showDialog(this);

                        if (saveScriptName == null)
                                return;

                        UIConnectionNode connection = connectionComboBox.getSelectionModel()
                                .getSelectedItem();

                        UICatalogNode catalog = catalogComboBox.getSelectionModel()
                                .getSelectedItem();

                        ScriptFile newScriptFile = ScriptFileRepository.save(
                                connection.getName(),
                                catalog.getName(),
                                null,
                                saveScriptName,
                                content);

                        setScriptFile(newScriptFile);
                } else {
                        ScriptFileRepository.save(scriptFile, content);
                }

                EventBus.publish(new RefreshQueryNodeEvent());
                markSaveFlag();
        }

        public void markSaveFlag()
        {
                saveFlag = true;

                var text = owner.getText();
                if (text.startsWith("* "))
                        owner.setText(text.substring(2));
        }

        public void close()
        {
                if (!saveFlag) {
                        if (VkDialogHelper.ask("%s 未保存，是否保存？", owner.getText()))
                                save();
                }

                dispose();
        }
}

