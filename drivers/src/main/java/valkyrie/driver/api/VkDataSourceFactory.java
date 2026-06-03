package valkyrie.driver.api;

import valkyrie.driver.redis.RedisDataSource;

/**
 * @author Luo Tiansheng
 * @since 2026/4/20
 */
public class VkDataSourceFactory
{
        public static VkDataSource create(ConnectionConfig config)
        {
                return switch (config.getType()) {
                        case mysql, sqlite, dm -> new PooledDataSource(config);
                        case redis -> new RedisDataSource(config);
                };
        }
}
