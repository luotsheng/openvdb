package valkyrie.driver.dm;

import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.*;
import valkyrie.utils.collection.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class DMMetadataProvider implements DBMetadataProvider
{
        private final Driver driver;

        public DMMetadataProvider(Driver driver)
        {
                this.driver = driver;
        }

        @Override
        public List<DBNode> getChildrenOfCatalog(DBCatalogNode catalogNode)
        {
                throw new UnsupportedOperationException("达梦数据库不支持获取数据库(Catalog)列表");
        }

        @Override
        public List<DBNode> getChildrenOfSchema(DBSchemaNode schemaNode)
        {
                List<DBNode> tableNodes = Lists.newArrayList();

                List<Table> tables = driver.getTables(schemaNode.getSession());
                for (Table table : tables)
                        tableNodes.add(new DBTableNode(schemaNode, table));

                return tableNodes;
        }
}
