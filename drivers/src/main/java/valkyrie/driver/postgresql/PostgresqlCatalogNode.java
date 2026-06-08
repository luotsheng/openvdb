package valkyrie.driver.postgresql;

import valkyrie.driver.api.node.DBCatalogNode;
import valkyrie.driver.api.node.DBMetadataProvider;
import valkyrie.driver.api.node.DBNode;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class PostgresqlCatalogNode extends DBCatalogNode
{
        public PostgresqlCatalogNode(String catalog, DBMetadataProvider metadataProvider)
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
