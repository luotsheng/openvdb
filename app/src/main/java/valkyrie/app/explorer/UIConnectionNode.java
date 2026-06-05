package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import valkyrie.app.model.ConnectionPropertyModel;
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

        public UIConnectionNode(ConnectionPropertyModel propertyModel)
        {
                super(propertyModel.getName(), propertyModel.getDbType().getIcon());
                this.propertyModel = propertyModel;
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem connectItem = new MenuItem("打开连接");
                connectItem.setOnAction(e -> connect());
                MenuItem disconnectItem = new MenuItem("关闭连接");
                disconnectItem.setOnAction(e -> disconnect());
                contextMenu.getItems().addAll(connectItem, disconnectItem);
                return contextMenu;
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                connect();
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
