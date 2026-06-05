package valkyrie.driver.redis;

import valkyrie.driver.api.Driver;
import valkyrie.driver.api.node.DBCatalogNode;
import valkyrie.driver.api.node.DBMetadataProvider;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBSchemaNode;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class RedisMetadataProvider implements DBMetadataProvider
{
        private final Driver driver;

        public RedisMetadataProvider(Driver driver)
        {
                this.driver = driver;
        }

        @Override
        public List<DBNode> getChildrenOfCatalog(DBCatalogNode catalogNode)
        {
                return List.of();
        }

        @Override
        public List<DBNode> getChildrenOfSchema(DBSchemaNode schemaNode)
        {
                throw new UnsupportedOperationException("Redis 不支持获取模式(Schema)列表");
        }
}
