package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.Setter;
import valkyrie.app.dialog.connection.CreateOrEditConnectionDialog;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.app.model.UIExplorerStatus;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.core.repository.ConnectionRepository;
import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.DriverFactory;
import valkyrie.driver.api.node.DBNode;
import valkyrie.utils.io.IOUtils;

import javax.naming.Context;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UIConnectionNode extends UIExplorerNode
{
        private final ConnectionPropertyModel propertyModel;
        private Driver driver;
        private boolean connectFlag = false;

        private final MenuItem connectOrDisconnectMenuItem = new MenuItem("打开连接");

        @Setter
        private DeleteRequestListener deleteRequestListener;

        public interface DeleteRequestListener {
                void onDeleteRequest(UIConnectionNode node);
        }

        public UIConnectionNode(ConnectionPropertyModel propertyModel)
        {
                super(propertyModel.getName(), propertyModel.getDbType().getIcon());
                this.propertyModel = propertyModel;
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                ContextMenu contextMenu = new ContextMenu();

                MenuItem editMenuItem = new MenuItem("编辑连接");
                editMenuItem.setOnAction(e -> edit());
                MenuItem deleteMenuItem = new MenuItem("删除连接");
                deleteMenuItem.setOnAction(e -> delete());

                contextMenu.getItems().addAll(
                        connectOrDisconnectMenuItem,
                        editMenuItem,
                        deleteMenuItem
                );

                return contextMenu;
        }

        @Override
        public void onContextMenuRequested(ContextMenu contextMenu)
        {
                if (connectFlag) {
                        connectOrDisconnectMenuItem.setText("关闭连接");
                        connectOrDisconnectMenuItem.setOnAction(e -> disconnect());
                } else {
                        connectOrDisconnectMenuItem.setText("打开连接");
                        connectOrDisconnectMenuItem.setOnAction(e -> connect());
                }
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                connect();
        }

        private void edit()
        {
                if (connectFlag) {
                        if (VkDialogHelper.ask("编辑需要关闭当前连接，是否关闭？")) {
                                disconnect();
                                new CreateOrEditConnectionDialog(propertyModel).showAndWait();
                        }
                } else {
                        new CreateOrEditConnectionDialog(propertyModel).showAndWait();
                }
        }

        private void delete()
        {
                if (VkDialogHelper.askDangerous("确定要删除“%s”吗？", getLabel())) {
                        deleteRequestListener.onDeleteRequest(this);
                        ConnectionRepository.deleteConnection(getLabel());
                        UIExplorerStatus.getInstance().removeConnection(this);
                }
        }

        public void connect()
        {
                if (connectFlag)
                        return;

                useProgressIndicator(() -> {
                        ConnectionConfig connectionConfig = propertyModel.toConnectionConfig();
                        driver = DriverFactory.create(connectionConfig);
                        List<DBNode> nodeHierarchy = driver.getNodeHierarchy();
                        loadDynamicChildren(nodeHierarchy);
                        setExpanded(true);
                        connectFlag = true;
                });
        }

        public void disconnect()
        {
                if (!connectFlag)
                        return;

                setExpanded(false);
                getChildren().clear();

                IOUtils.closeQuietly(driver.getDataSource());

                connectFlag = false;
        }
}
