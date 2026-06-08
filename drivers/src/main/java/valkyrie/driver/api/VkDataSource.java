package valkyrie.driver.api;

import javax.sql.DataSource;

/**
 * @author Luo Tiansheng
 * @since 2026/4/20
 */
public interface VkDataSource extends DataSource, AutoCloseable
{
        default ConnectionConfig getConnectionConfig() { return null; }
}