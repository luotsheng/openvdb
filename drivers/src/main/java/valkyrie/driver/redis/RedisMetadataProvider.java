package valkyrie.driver.redis;

import valkyrie.driver.api.Driver;
import valkyrie.driver.api.node.*;
import valkyrie.utils.collection.Lists;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
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
                return Lists.of(new DBQueryNode(catalogNode));
        }

        @Override
        public List<DBNode> getChildrenOfSchema(DBSchemaNode schemaNode)
        {
                throw new UnsupportedOperationException("Redis 不支持获取模式(Schema)列表");
        }
}
