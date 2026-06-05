package valkyrie.driver.api;

import valkyrie.driver.dm.DMDriver;
import valkyrie.driver.mysql.MySQLDriver;
import valkyrie.driver.postgresql.PostgresqlDriver;
import valkyrie.driver.redis.RedisDataSource;
import valkyrie.driver.redis.RedisDriver;
import valkyrie.driver.sqlite.SQLiteDriver;

/**
 * @author Luo Tiansheng
 * @since 2026/4/20
 */
public class DriverFactory
{
        public static VkDataSource createDataSource(ConnectionConfig config)
        {
                return switch (config.getType()) {
                        case mysql, postgresql, dm, sqlite -> new PooledDataSource(config);
                        case redis -> new RedisDataSource(config);
                };
        }

        public static Driver create(ConnectionConfig config)
        {
                return switch (config.getType()) {
                        case mysql -> new MySQLDriver(createDataSource(config));
                        case postgresql -> new PostgresqlDriver(createDataSource(config));
                        case sqlite -> new SQLiteDriver(createDataSource(config));
                        case dm -> new DMDriver(createDataSource(config));
                        case redis -> new RedisDriver(createDataSource(config));
                };
        }
}
