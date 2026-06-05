package valkyrie.driver.api;

import lombok.Getter;
import valkyrie.driver.dm.DMDriver;
import valkyrie.driver.mysql.MySQLDriver;
import valkyrie.driver.postgresql.PostgresqlDriver;
import valkyrie.driver.redis.RedisDriver;
import valkyrie.driver.sqlite.SQLiteDriver;

import javax.sql.DataSource;

import static valkyrie.utils.string.StaticLibrary.lowercase;

/**
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
@Getter
public enum DbType
{
        mysql("MySQL", "mysql", "com.mysql.cj.jdbc.Driver", true),
        postgresql("Postgresql", "postgresql", "org.postgresql.Driver", true),
        sqlite("SQLite", "sqlite", "org.sqlite.JDBC", true),
        dm("达梦数据库", "dm2", "dm.jdbc.driver.DmDriver", true),
        redis("Redis", "redis", null, false),
        ;

        private final String alias;
        private final String icon;
        private final String driverClass;
        private final boolean supportedProductMetaData;

        DbType(String alias, String icon, String driverClass, boolean supportedProductMetaData)
        {
                this.alias = alias;
                this.icon = icon;
                this.driverClass = driverClass;
                this.supportedProductMetaData = supportedProductMetaData;
        }

        public static DbType of(String type)
        {
                return valueOf(lowercase(type));
        }

        public Driver createDriver(VkDataSource dataSource)
        {
                return switch (this) {
                        case mysql -> new MySQLDriver(dataSource);
                        case postgresql -> new PostgresqlDriver(dataSource);
                        case sqlite -> new SQLiteDriver(dataSource);
                        case dm -> new DMDriver(dataSource);
                        case redis -> new RedisDriver(dataSource);
                };
        }
}
