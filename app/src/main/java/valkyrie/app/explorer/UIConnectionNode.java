package valkyrie.app.explorer;

import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.DriverFactory;
import valkyrie.driver.api.node.DBNode;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class UIConnectionNode extends UIExplorerNode
{
        private final ConnectionPropertyModel propertyModel;

        public UIConnectionNode(ConnectionPropertyModel propertyModel)
        {
                super(propertyModel.getName(), propertyModel.getDbType().getIcon());
                this.propertyModel = propertyModel;
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                connect();
        }

        public void connect()
        {
                ConnectionConfig connectionConfig = propertyModel.toConnectionConfig();
                Driver driver = DriverFactory.create(connectionConfig);
                List<DBNode> nodeHierarchy = driver.getNodeHierarchy();
                buildTreeItem(nodeHierarchy);
        }

        public void disconnect()
        {

        }

        public void buildTreeItem(List<DBNode> dbNodes)
        {
                for (DBNode dbNode : dbNodes)
                        new UIDynamicNode(this, dbNode);
        }
}
