package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import valkyrie.app.assets.Assets;
import valkyrie.app.dialog.connection.CreateOrEditConnectionDialog;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.ConnectionOpenedNotifyEvent;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.app.model.UIExplorerStatus;
import valkyrie.app.widgets.VkStatusBar;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.core.repository.ConnectionRepository;
import valkyrie.driver.api.*;
import valkyrie.driver.dm.DMDriver;
import valkyrie.driver.mysql.MySQLDriver;
import valkyrie.driver.redis.RedisDriver;
import valkyrie.driver.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
@SuppressWarnings("FieldCanBeLocal")
public class UIConnectionNode extends UIExplorerNode
{
        @Getter
        private final ConnectionPropertyModel propertyModel;

        private boolean openFlag = false;
        private boolean cancelFlag = false;
        private Thread connecetThread = null;
        private VkDataSource dataSource;

        @Getter
        private Driver driver;

        @Getter
        private DbType dbType;

        // Menu Items
        private MenuItem openOrCloseMenuItem;
        private MenuItem editMenuItem;
        private MenuItem deleteMenuItem;

        @Getter
        private final List<UICatalogNode> catalogNodes = new ArrayList<>();

        @Setter
        @Getter
        private UICatalogNode selectedDatabase;

        @Setter
        private DeleteRequestListener deleteRequestListener;

        private static final DriverExecuteHook DRIVER_EXECUTE_HOOK = new DriverExecuteHook();

        public interface DeleteRequestListener {
                void onDeleteRequest(UIConnectionNode node);
        }

        public static class DriverExecuteHook implements SQLExecuteHook {
                VkStatusBar VSBInstance = VkStatusBar.getInstance();

                @Override
                public void beforeExecute(String sql)
                {
                        Platform.runLater(() -> VSBInstance.updateMessage(sql));
                }

                @Override
                public void afterExecute(String sql, long cost)
                {
                        Platform.runLater(() -> VSBInstance.updateMessage(sql));
                }
        }

        public UIConnectionNode(ConnectionPropertyModel propertyModel)
        {
                super(propertyModel.getName());

                this.dbType = DbType.of(propertyModel.getType());

                setGraphic(getIcon());
                this.propertyModel = propertyModel;
                setupListenerEvent();
        }

        @Override
        public ImageView getIcon()
        {
                return Assets.use(dbType.getIcon());
        }

        private void createDriver()
        {
                ConnectionConfig config = propertyModel.toConnectionConfig();
                dataSource = VkDataSourceFactory.create(config);
                driver = config.getType().createDriver(dataSource);
                driver.registerExecuteHook(DRIVER_EXECUTE_HOOK);
        }

        public void openConnection()
        {
                if (openFlag)
                        return;

                cancelFlag = false;

                setLoadingIndicator();

                connecetThread = new Thread(() -> {
                        try {
                                createDriver();
                                setupDatabases(driver.getCatalogs());
                                setExpanded(true);
                                openFlag = true;
                                UIExplorerStatus.getInstance().selectedConnection(this);
                                EventBus.publish(new ConnectionOpenedNotifyEvent(this));
                        } catch (Throwable e) {
                                if (!cancelFlag)
                                        Platform.runLater(() -> VkDialogHelper.alert(e));
                        } finally {
                                connecetThread = null;
                                if (!cancelFlag)
                                        Platform.runLater(this::removeLoadingIndicator);
                        }
                });

                connecetThread.start();
        }

        public void closeConnection()
        {
                /* cancel */
                if (connecetThread != null) {
                        cancelFlag = true;
                        connecetThread.interrupt();
                        connecetThread = null;
                        removeLoadingIndicator();
                        return;
                }

                if (!openFlag)
                        return;

                setExpanded(false);

                getChildren().forEach(db -> {
                        if (db instanceof UICatalogNode dbNode)
                                dbNode.closeDatabase();
                });

                catalogNodes.clear();
                getChildren().clear();
                VkDialogHelper.runWith(dataSource::close);
                UIExplorerStatus.getInstance().unselectConnection(this);
                selectedDatabase = null;

                openFlag = false;
        }

        private void editConnection()
        {
                if (openFlag) {
                        if (VkDialogHelper.ask("编辑需要关闭当前连接，是否关闭？")) {
                                closeConnection();
                                new CreateOrEditConnectionDialog(propertyModel).showAndWait();
                        }
                } else {
                        new CreateOrEditConnectionDialog(propertyModel).showAndWait();
                }
        }

        private void deleteConnection()
        {
                if (VkDialogHelper.askDangerous("确定要删除“%s”吗？", getName())) {
                        deleteRequestListener.onDeleteRequest(this);
                        ConnectionRepository.deleteConnection(getName());
                        UIExplorerStatus.getInstance().removeConnection(this);
                }
        }

        @Override
        protected ContextMenu registerContextMenu()
        {
                ContextMenu contextMenu = new ContextMenu();

                openOrCloseMenuItem = new MenuItem();

                editMenuItem = new MenuItem("编辑连接");
                editMenuItem.setOnAction(event -> editConnection());

                deleteMenuItem = new MenuItem("删除连接");
                deleteMenuItem.setOnAction(event -> deleteConnection());

                contextMenu.getItems().addAll(
                        openOrCloseMenuItem,
                        new SeparatorMenuItem(),
                        editMenuItem,
                        new SeparatorMenuItem(),
                        deleteMenuItem
                );

                return contextMenu;
        }

        @Override
        public void showContextMenu(Node node, double x, double y)
        {
                if (openFlag || connecetThread != null) {
                        openOrCloseMenuItem.setText("关闭连接");
                        openOrCloseMenuItem.setOnAction(event -> closeConnection());
                } else {
                        openOrCloseMenuItem.setText("打开连接");
                        openOrCloseMenuItem.setOnAction(event -> openConnection());
                }

                super.showContextMenu(node, x, y);
        }

        @Override
        public void onSelectedEvent(UIExplorerNode node)
        {
                UIExplorerStatus.getInstance().selectedConnection(this);
        }

        private void setupListenerEvent()
        {
                setMouseDoubleClickEvent(event -> openConnection());
        }

        private void setupDatabases(List<Catalog> cats)
        {
                for (Catalog cat : cats)
                        catalogNodes.add(new UICatalogNode(this, driver, cat));
                getChildren().addAll(catalogNodes);
        }

        public List<Suggestion> getCatalogSuggestion()
        {
                return getCatalogNodes().stream()
                        .map(t -> Suggestion.ofModule(t.getName()))
                        .toList();
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isOpen()
        {
                return openFlag;
        }
}
