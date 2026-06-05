package valkyrie.driver.postgresql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.driver.api.*;
import valkyrie.driver.api.exception.DriverException;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.suggestion.Suggestion;
import valkyrie.utils.collection.Lists;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * Postgresql 驱动层实现
 *
 * @author Luo Tiansheng
 * @since 2026/6/04
 */
@SuppressWarnings({"SqlSourceToSinkFlow", "DuplicatedCode"})
public class PostgresqlDriver extends Driver
{
        private static final Logger LOG = LoggerFactory.getLogger(PostgresqlDriver.class);

        public PostgresqlDriver(VkDataSource dataSource)
        {
                super(dataSource);
        }

        @Override
        public DbType getType()
        {
                return DbType.postgresql;
        }

        @Override
        public List<DBNode> getNodeHierarchy()
        {
                List<DBNode> catalogNodes = Lists.newArrayList();
                PostgresqlMetadataProvider metadataProvider = new PostgresqlMetadataProvider(this);

                List<Catalog> catalogs = getCatalogs();
                for (Catalog catalog : catalogs)
                        catalogNodes.add(new PostgresqlCatalogNode(catalog, metadataProvider));

                return catalogNodes;
        }

        @Override
        protected Dialect createDialect()
        {
                return new PostgresqlDialect();
        }

        @Override
        public String showCreateTable(Session session, String table)
        {
                return "";
        }

        @Override
        public List<Suggestion> getSuggestion(Session session)
        {
                return List.of();
        }

        @Override
        public List<Table> getTables(Session session)
        {
                List<Table> ret = Lists.newArrayList();

                String sql = fmt("""
                        SELECT
                          c.relname AS name,
                          NULL::timestamp AS create_time,
                          NULL::timestamp AS update_time,
                          'PostgreSQL' AS engine,
                          pg_total_relation_size(c.oid)/1024.0 AS size,
                          c.reltuples::bigint AS rows,
                          obj_description(c.oid, 'pg_class') AS comment
                        FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        JOIN pg_tables t ON c.relname = t.tablename
                        WHERE c.relkind = 'r'
                          AND n.nspname = '%s'
                        ORDER BY c.relname;
                        """, session.schema());

                try (Connection connection = getConnection(session);
                     Statement statement = connection.createStatement()) {
                        ResultSet r = statement.executeQuery(sql);
                        while (r.next()) {
                                ret.add(new Table(
                                        r.getString("name"),
                                        r.getDate("create_time"),
                                        r.getDate("update_time"),
                                        r.getString("engine"),
                                        r.getFloat("size"),
                                        r.getInt("rows"),
                                        r.getString("comment")
                                ));
                        }
                } catch (Exception e) {
                        throw new DriverException(e);
                }

                return ret;
        }

        @Override
        public List<Index> getIndexes(Session session, String table)
        {
                return List.of();
        }

        @Override
        public Set<String> getIndexTypes()
        {
                return Set.of();
        }

        @Override
        public void dropTable(Session session, String table)
        {

        }

        @Override
        public void dropColumns(Session session, String table, Collection<Column> columns)
        {

        }

        @Override
        public void dropIndexKeys(Session session, String table, Collection<Index> selectionItems)
        {

        }

        @Override
        public void dropPrimaryKey(Session session, String table)
        {

        }

        @Override
        public void addPrimaryKey(Session session, String table, Collection<Column> primaryKeys)
        {

        }

        @Override
        public void alterIndexKeys(Session session, String table, Collection<Index> indexes)
        {

        }

        @Override
        public void alterChange(Session session, String table, Collection<Column> columns)
        {

        }

        @Override
        public void alterVisible(Session session, String table, Collection<Index> indexes)
        {

        }
}
