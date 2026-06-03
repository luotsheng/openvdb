package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import lombok.Getter;
import valkyrie.app.assets.Assets;
import valkyrie.app.event.RefreshQueryNodeEvent;
import valkyrie.app.event.RefreshTableNodeEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.event.workbench.CloseNavigationPaneEvent;
import valkyrie.app.event.workbench.CloseWorkbenchTabEvent;
import valkyrie.app.event.workbench.OpenNavigationPaneEvent;
import valkyrie.app.event.workbench.OpenScriptEditorPaneEvent;
import valkyrie.app.pane.TableOverviewPane;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.core.repository.ScriptFileRepository;
import valkyrie.driver.api.*;
import valkyrie.driver.suggestion.Suggestion;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.collection.Maps;

import java.sql.SQLException;
import java.text.Collator;
import java.util.*;

import static valkyrie.utils.string.StaticLibrary.lowercase;
import static valkyrie.utils.string.StaticLibrary.streq;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
@SuppressWarnings({
        "FieldCanBeLocal"
})
public class UICatalogNode extends UIExplorerNode implements EventListener
{
        @Getter
        private final UIConnectionNode connection;
        @Getter
        private final Driver driver;
        private boolean openFlag = false;
        private final List<Table> tables = new ArrayList<>();
        private final Map<String, UITableNode> tableNodes = Maps.newHashMap();
        private final Map<String, UITableNode> tableIgnoreCaseNodes = Maps.newHashMap();
        @Getter
        private final Session session;

        // Tree Items
        final TreeItem<String> tableItem
                = new UIInternalNode(this, "数据表", Assets.use("table"));;
        final TreeItem<String> queryItem
                = new UIInternalNode(this, "查询脚本", Assets.use("sql"));;

        // Menu Items
        private MenuItem openOrCloseMenuItem;
        private MenuItem newQueryMenuItem;

        private final TableOverviewPane overviewPane = new TableOverviewPane(this);
        private final OpenNavigationPaneEvent openNavigationPaneEvent = new OpenNavigationPaneEvent(this, overviewPane);
        private final CloseNavigationPaneEvent closeNavigationPaneEvent = new CloseNavigationPaneEvent(this);

        /**
         * 内部通用节点
         */
        public static class UIInternalNode extends UIExplorerNode
        {
                private final UICatalogNode parent;

                public UIInternalNode(UICatalogNode parent, String name, ImageView icon)
                {
                        super(name);
                        setGraphic(icon);
                        this.parent = parent;
                }

                @Override
                public ImageView getIcon()
                {
                        return null;
                }

                @Override
                public void onSelectedEvent(UIExplorerNode node)
                {
                        parent.onSelectedEvent(node);
                }
        }

        public UICatalogNode(UIConnectionNode connection, Driver driver, Catalog catalog)
        {
                super(catalog.getLabel(), catalog.getName());
                this.connection = connection;
                setGraphic(getIcon());

                this.session = switch (connection.getDbType()) {
                        case mysql, sqlite, redis -> Session.ofCatalog(catalog.getName());
                        case dm -> Session.ofSchema(catalog.getName());
                };

                this.driver = driver;

                setupTableNode();
                setupListenerEvent();

                EventBus.subscribe(RefreshTableNodeEvent.class, this);
                EventBus.subscribe(RefreshQueryNodeEvent.class, this);
        }

        @Override
        public ImageView getIcon()
        {
                return Assets.use("database1");
        }

        private void setupTableNode()
        {
                var node = (UIExplorerNode) tableItem;

                ContextMenu nodeContextMenu = new ContextMenu();

                MenuItem reloadTableItem = new MenuItem("刷新");
                reloadTableItem.setOnAction(event -> reloadTableNode());

                nodeContextMenu.getItems().addAll(reloadTableItem);
                node.setContextMenu(nodeContextMenu);
        }

        private void reloadTable()
        {
                tables.clear();
                tables.addAll(driver.getTables(session));
                tables.sort(Comparator.comparing(Table::getName, Collator.getInstance(Locale.CHINA)));
        }

        public final void dropTable(Table table) throws SQLException
        {
                driver.dropTable(session, table.getName());
        }

        public void openDatabase()
        {
                if (openFlag)
                        return;

                setLoadingIndicator();

                new Thread(() -> {
                        try {
                                reloadTable();

                                Platform.runLater(() -> {

                                        if (driver.getType() != DbType.redis)
                                                getChildren().add(tableItem);

                                        getChildren().add(queryItem);

                                        reloadTableNode();
                                        reloadQueryNode();

                                        setExpanded(true);
                                        onSelectedEvent(this);

                                        openFlag = true;
                                });
                        } catch (Throwable e) {
                                VkDialogHelper.alert(e);
                        } finally {
                                Platform.runLater(this::removeLoadingIndicator);
                        }
                }).start();
        }

        public void closeDatabase()
        {
                if (!openFlag)
                        return;

                setExpanded(false);
                getChildren().clear();
                EventBus.publish(new CloseWorkbenchTabEvent(this));
                EventBus.publish(closeNavigationPaneEvent);

                openFlag = false;
        }

        public void reloadTableNode()
        {
                reloadTable();

                tableNodes.clear();
                tableIgnoreCaseNodes.clear();
                tableItem.getChildren().clear();

                for (Table table : tables) {
                        UITableNode tableNode = new UITableNode(driver, this, table);
                        tableNode.setSelectedEvent(this::onSelected);
                        tableNodes.put(table.getName(), tableNode);
                        tableIgnoreCaseNodes.put(lowercase(table.getName()), tableNode);
                        tableItem.getChildren().add(tableNode);
                }

                overviewPane.setAndUpdate(tables);
        }

        private void reloadQueryNode()
        {
                var children = queryItem.getChildren();
                var scriptFiles = ScriptFileRepository.loadScriptFiles(connection.getName(), getName(), null);
                List<UIScriptNode> reloadScriptNodes = Lists.newArrayList();
                scriptFiles.forEach(query -> reloadScriptNodes.add(new UIScriptNode(this, query)));

                List<UIScriptNode> newScriptNodes = Lists.newArrayList();
                for (UIScriptNode newScriptNode : reloadScriptNodes) {
                        var exists = children.stream().anyMatch(child ->
                                streq(((UIScriptNode) child).getName(), newScriptNode.getName()));

                        if (!exists)
                                newScriptNodes.add(newScriptNode);
                }

                List<UIScriptNode> removeScriptNodes = Lists.newArrayList();
                for (TreeItem<String> child : children) {
                        var scriptNode = (UIScriptNode) child;

                        var exists = reloadScriptNodes.stream().anyMatch(it ->
                                streq(it.getName(), scriptNode.getName()));

                        if (!exists)
                                removeScriptNodes.add(scriptNode);
                }

                children.removeAll(removeScriptNodes);
                children.addAll(newScriptNodes);

                var collator = Collator.getInstance(Locale.CHINA);
                children.sort(Comparator.comparing(it -> ((UIScriptNode) it).getName(), collator));
        }

        public void onSelected(UIExplorerNode node)
        {
                if (openFlag && node == tableItem)
                        EventBus.publish(openNavigationPaneEvent);
                connection.setSelectedDatabase(this);
        }

        private void newQueryScript()
        {
                EventBus.publish(new OpenScriptEditorPaneEvent(this, connection));
        }

        public void setupListenerEvent()
        {
                setSelectedEvent(this::onSelected);
                setMouseDoubleClickEvent(event -> openDatabase());
        }

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof RefreshTableNodeEvent)
                        reloadTableNode();

                if (event instanceof RefreshQueryNodeEvent)
                        reloadQueryNode();
        }

        @Override
        protected ContextMenu registerContextMenu()
        {
                ContextMenu menu = new ContextMenu();

                openOrCloseMenuItem = new MenuItem("打开数据库");
                openOrCloseMenuItem.setOnAction(event -> openDatabase());

                newQueryMenuItem = new MenuItem("新建查询");
                newQueryMenuItem.setOnAction(event -> newQueryScript());

                menu.getItems().addAll(
                        openOrCloseMenuItem,
                        new SeparatorMenuItem(),
                        newQueryMenuItem
                );

                return menu;
        }

        @Override
        public void showContextMenu(Node node, double x, double y)
        {
                if (openFlag) {
                        openOrCloseMenuItem.setText("关闭数据库");
                        openOrCloseMenuItem.setOnAction(event -> closeDatabase());
                } else {
                        openOrCloseMenuItem.setText("打开数据库");
                        openOrCloseMenuItem.setOnAction(event -> openDatabase());
                }

                super.showContextMenu(node, x, y);
        }

        public List<Suggestion> getSuggestion()
        {
                return driver.getSuggestion(session);
        }

        public List<Suggestion> getTableNameSuggestions()
        {
                return tables.stream()
                        .map(t -> Suggestion.ofClass(t.getName(), t.getComment()))
                        .toList();
        }

        public UITableNode getUITableNode(String name)
        {
                return tableNodes.get(name);
        }

        public UITableNode getUITableNodeIgnoreCase(String name)
        {
                return tableIgnoreCaseNodes.get(lowercase(name));
        }


        public boolean isOpen()
        {
                return openFlag;
        }
}
