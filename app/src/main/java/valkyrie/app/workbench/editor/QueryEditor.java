package valkyrie.app.workbench.editor;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import valkyrie.app.Application;
import valkyrie.app.assets.Assets;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.explorer.*;
import valkyrie.app.widgets.VkComboBox;
import valkyrie.app.widgets.VkIconButton;
import valkyrie.app.widgets.VkSeparator;
import valkyrie.monacofx.MonacoEditor;

/**
 * SQL 脚本编辑器
 *
 * @author Luo Tiansheng
 * @since 2026/3/29
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
public class QueryEditor extends SplitPane implements EventListener
{
        private final Tab tab;
        private final ToolBar toolBar;
        private final MonacoEditor editor;
        private final BorderPane topBorderPane = new BorderPane();

        // combobox
        private final VkComboBox<UIConnectionNode> connectionComboBox = new VkComboBox<>();
        private final VkComboBox<UICatalogDynamicNode> catalogComboBox = new VkComboBox<>();
        private final VkComboBox<UISchemaDynamicNode> schemaComboBox = new VkComboBox<>();

        // Tool
        private Button runToolButton;
        private Button stopToolButton;
        private Button beautifyToolButton;

        public QueryEditor(Tab tab)
        {
                this.tab = tab;
                tab.setContent(this);

                toolBar = createToolBar();
                editor = createMonacoEditor();

                setupComboBox();
                setupBorderPane();
        }

        private ToolBar createToolBar()
        {
                ToolBar toolBar = new ToolBar();

                runToolButton = new VkIconButton("运行已选择", "run");
                runToolButton.setText("运行");

                stopToolButton = new VkIconButton("停止当时运行", "stop");
                stopToolButton.setText("停止");
                stopToolButton.setDisable(true);

                beautifyToolButton = new VkIconButton("美化 SQL", "beautify");
                beautifyToolButton.setText("美化 SQL");

                schemaComboBox.setVisible(false);
                schemaComboBox.setManaged(false);

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
                ContextMenu contextMenu = new ContextMenu();

                MenuItem runSelectedSQLItem = new MenuItem("运行已选择");
                runSelectedSQLItem.setGraphic(Assets.use("run"));
                runSelectedSQLItem.setAccelerator(
                        new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN)
                );

                MenuItem beautifySelectedSQLItem = new MenuItem("美化已选择");
                beautifySelectedSQLItem.setGraphic(Assets.use("beautify"));

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

                contextMenu.getItems().addAll(
                        runSelectedSQLItem,
                        beautifySelectedSQLItem,
                        new SeparatorMenuItem(),
                        copyItem,
                        pasteItem
                );

                editor.bindContextMenu(contextMenu);

                return editor;
        }

        private void setupComboBox()
        {
                configureComboBox(connectionComboBox);
                configureComboBox(catalogComboBox);
                configureComboBox(schemaComboBox);

                // connection
                connectionComboBox.setOnAction(event -> {
                        UIConnectionNode item = connectionComboBox.getSelectionModel().getSelectedItem();
                        if (item != null) {
                                if (!item.isConnect())
                                        item.connect();
                        }
                });

                for (UIConnectionNode connectionNode : GlobalDynamicNodeContext.getConnectionNodes())
                        connectionComboBox.getItems().add(connectionNode);
        }

        private void setupBorderPane()
        {
                topBorderPane.setTop(toolBar);
                topBorderPane.setCenter(editor);
                getItems().add(topBorderPane);
        }

        private static <T extends UIExplorerNode> void configureComboBox(VkComboBox<T> comboBox)
        {
                comboBox.setButtonCell(new ListCell<>()
                {
                        @Override
                        protected void updateItem(T item, boolean empty)
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
                        protected void updateItem(T item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getLabel());
                                setGraphic(item.createGraphic());
                        }
                });
        }

        @Override
        public void onEvent(Event event)
        {

        }
}

