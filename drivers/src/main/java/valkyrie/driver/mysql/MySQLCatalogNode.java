package valkyrie.driver.mysql;

import valkyrie.driver.api.Catalog;
import valkyrie.driver.api.node.DBCatalogNode;
import valkyrie.driver.api.node.DBMetadataProvider;
import valkyrie.driver.api.node.DBNode;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class MySQLCatalogNode extends DBCatalogNode
{
        public MySQLCatalogNode(Catalog catalog, DBMetadataProvider metadataProvider)
        {
                super(catalog, metadataProvider);
        }

        @Override
        public boolean hasChildren()
        {
                return true;
        }

        @Override
        public List<DBNode> getChildren()
        {
                return getMetadataProvider().getChildrenOfCatalog(this);
        }
}
