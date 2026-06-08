package valkyrie.driver.api;

import lombok.Getter;
import lombok.Setter;

/**
 * 连接配置信息
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
@Getter
@Setter
public class ConnectionConfig
{
        private DbType type;
        private String host;
        private String port;
        private String username;
        private String password;
        private String defaultDatabase;
        private String jdbcUrl;
}
