package valkyrie.driver.api;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据库会话上下文记录。
 * <p>
 * 封装当前数据库连接的会话级别作用域信息，包括当前目录（catalog）和当前模式（schema）。
 * 该类用于在调用 JDBC 操作时传递上下文，以便在获取连接后正确设置
 * {@link java.sql.Connection#setCatalog(String)} 和 {@link java.sql.Connection#setSchema(String)}。
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * Session session = new Session("my_catalog", "my_schema");
 * try (Connection conn = dataSource.getConnection()) {
 *     if (session.catalog() != null) conn.setCatalog(session.catalog());
 *     if (session.schema() != null) conn.setSchema(session.schema());
 *     // 执行 SQL...
 * }
 * }</pre>
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 * @see java.sql.Connection#setCatalog(String)
 * @see java.sql.Connection#setSchema(String)
 */
@Setter
public class Session
{
        private String catalog;
        private String schema;

        public Session()
        {
                this(null, null);
        }

        public Session(String catalog, String schema)
        {
                this.catalog = catalog;
                this.schema = schema;
        }

        public String catalog()
        {
                return catalog;
        }

        public String schema()
        {
                return schema;
        }

        public static Session of(String catalog, String schema)
        {
                return new Session(catalog, schema);
        }

        public static Session ofCatalog(String catalog)
        {
                return new Session(catalog, null);
        }
}
