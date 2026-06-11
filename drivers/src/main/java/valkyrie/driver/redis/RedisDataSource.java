package valkyrie.driver.redis;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.VkDataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static valkyrie.utils.string.StrStaticImports.strnempty;

/**
 * @author Luo Tiansheng
 * @since 2026/4/20
 */
public class RedisDataSource implements VkDataSource
{

        @Getter
        private final Jedis jedis;

        public RedisDataSource(ConnectionConfig config) {
                jedis = new Jedis(config.getHost(), Integer.parseInt(config.getPort()));

                if (strnempty(config.getPassword()))
                        jedis.auth(config.getPassword());

                jedis.ping();
        }

        @Override
        public void close() throws Exception {
                jedis.close();
        }

        /////////////////////////////////////////////////////////////////////////////
        ///                                JDBC                                   ///
        /////////////////////////////////////////////////////////////////////////////

        @Override
        public Connection getConnection() throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public int getLoginTimeout() throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
                throw new UnsupportedOperationException();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                throw new UnsupportedOperationException();
        }
}
