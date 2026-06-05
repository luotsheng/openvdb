package valkyrie.driver.postgresql;

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
public class PostgresqlMetadataProvider implements DBMetadataProvider
{
        private final Driver driver;

        public PostgresqlMetadataProvider(Driver driver)
        {
                this.driver = driver;
        }

        @Override
        public List<DBNode> getChildrenOfCatalog(DBCatalogNode catalogNode)
        {
                List<DBNode> ret = new ArrayList<>();

                List<String> schemas = driver.getSchemas();
                for (String schema : schemas)
                        ret.add(new PostgresqlSchemaNode(catalogNode, schema, this));

                return ret;
        }

        @Override
        public List<DBNode> getChildrenOfSchema(DBSchemaNode schemaNode)
        {
                List<Table> tables = driver.getTables(schemaNode.getSession());
                return Lists.of(
                        new DBTableContainerNode(schemaNode, tables),
                        new DBQueryNode(schemaNode)
                );
        }
}
