package valkyrie.app.explorer;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.Getter;
import lombok.Setter;
import valkyrie.app.dialog.connection.CreateOrEditConnectionDialog;
import valkyrie.app.event.ConnectedSuccessEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.core.repository.ConnectionRepository;
import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.DriverFactory;
import valkyrie.driver.api.node.DBNode;
import valkyrie.utils.io.IOUtils;

import java.util.List;

/**
 * Explorer Node 体系下的 Root 节点
 *
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UIConnectionNode extends UIExplorerNode
{
        private final @Getter TreeView<String> treeView;
        private final ConnectionPropertyModel propertyModel;
        private @Getter Driver driver;
        private boolean connectFlag = false;

        private final MenuItem connectOrDisconnectMenuItem = new MenuItem("打开连接");

        @Setter
        private DeleteRequestListener deleteRequestListener;

        public interface DeleteRequestListener {
                void onDeleteRequest(UIConnectionNode node);
        }

        public UIConnectionNode(TreeView<String> treeView, ConnectionPropertyModel propertyModel)
        {
                super(null, propertyModel.getName(), propertyModel.getDbType().getIcon());
                this.treeView = treeView;
                this.propertyModel = propertyModel;
                GlobalDynamicNodeContext.getConnectionNodes().add(this);
        }

        @Override
        public VkContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

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
                if (progressing.get()) {
                        connectOrDisconnectMenuItem.setDisable(true);
                        return;
                }

                if (connectFlag) {
                        connectOrDisconnectMenuItem.setText("关闭连接");
                        connectOrDisconnectMenuItem.setOnAction(e -> disconnect());
                } else {
                        connectOrDisconnectMenuItem.setText("打开连接");
                        connectOrDisconnectMenuItem.setOnAction(e -> connect());
                }

                connectOrDisconnectMenuItem.setDisable(false);
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                connect();
        }

        public boolean isConnect()
        {
                return connectFlag;
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
                        Platform.runLater(() -> {
                                loadDynamicChildren(nodeHierarchy);
                                setExpanded(true);
                        });
                        connectFlag = true;
                        // 发布事件
                        EventBus.publish(new ConnectedSuccessEvent(this));
                });
        }

        public void disconnect()
        {
                if (!connectFlag)
                        return;

                for (TreeItem<String> child : getChildren())
                        ((UIDynamicNode) child).onParentCloseEvent();

                setExpanded(false);
                getChildren().clear();

                IOUtils.closeQuietly(driver.getDataSource());

                connectFlag = false;
        }
}
