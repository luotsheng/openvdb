package valkyrie.driver.postgresql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.driver.api.*;
import valkyrie.driver.api.exception.DriverException;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.driver.api.node.DBNodePath;
import valkyrie.driver.dm.DMSuggestions;
import valkyrie.driver.suggestion.Suggestion;
import valkyrie.driver.utils.JdbcUtils;
import valkyrie.utils.bean.BeanUtils;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.collection.Sets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static valkyrie.utils.collection.Lists.first;
import static valkyrie.utils.collection.Lists.second;
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

        private final String defaultKey = "__init_default__";
        private final Map<String, VkDataSource> dataSourceManager = new HashMap<>();

        public PostgresqlDriver(VkDataSource dataSource)
        {
                super(dataSource);
                dataSourceManager.put(defaultKey, dataSource);
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
        public DBNodePath getNodeHierarchyPath()
        {
                return new DBNodePath(DBNodeKind.CATALOG,
                        new DBNodePath(DBNodeKind.SCHEMA, null));
        }

        @Override
        protected Dialect createDialect()
        {
                return new PostgresqlDialect();
        }

        public Connection getConnection(Session session) throws SQLException
        {
                dataSource = dataSourceManager.get(defaultKey);

                if (session.catalog() != null)
                        dataSource = dataSourceManager.get(session.catalog());

                return super.getConnection(session);
        }

        @Override
        public String showCreateTable(Session session, String table)
        {
                return "";
        }

        @Override
        public List<Catalog> getCatalogs()
        {
                String sql = """
                        SELECT datname
                        FROM pg_database
                        WHERE has_database_privilege(datname, 'CONNECT')
                        AND NOT datistemplate
                        ORDER BY datname;
                        """;

                QueryResult rs = execute(new Session(), sql);

                List<Catalog> catalogs = rs.getRows().stream()
                        .map(t -> {
                                var name = first(t);
                                return Catalog.of(name, name);
                        })
                        .toList();

                VkDataSource ds = dataSourceManager.get(defaultKey);
                ConnectionConfig cnf = ds.getConnectionConfig();

                for (Catalog catalog : catalogs) {
                        ConnectionConfig cc =
                                BeanUtils.copyProperties(cnf, ConnectionConfig.class);
                        String jdbcUrl = JdbcUtils.updateDefaultDatabase(cc.getJdbcUrl(), catalog.getName());
                        cc.setJdbcUrl(jdbcUrl);
                        dataSourceManager.put(catalog.getName(), new PooledDataSource(cc));
                }

                return catalogs;
        }

        @Override
        public List<String> getSchemas(Session session)
        {
                return super.getSchemas(session);
        }

        @Override
        public List<Suggestion> getSuggestions(Session session)
        {
                Set<Suggestion> ret = Sets.newHashSet();

                ret.addAll(DMSuggestions.VALUES);

                /* 表信息 */
                List<Table> tables = getTables(session);
                ret.addAll(tables.stream()
                        .map(t -> Suggestion.ofClass(t.getName(), t.getComment()))
                        .toList());

                /* 字段信息 */
                QueryResult queryResult = execute(session, """
                        SELECT
                          c.column_name,
                          MAX(pd.description) AS comment
                        FROM
                          information_schema.columns c
                          LEFT JOIN pg_catalog.pg_class pc
                            ON pc.relname = c.table_name
                          LEFT JOIN pg_catalog.pg_namespace pn
                            ON pn.oid = pc.relnamespace
                            AND pn.nspname = c.table_schema
                          LEFT JOIN pg_catalog.pg_attribute pa
                            ON pa.attrelid = pc.oid
                            AND pa.attname = c.column_name
                          LEFT JOIN pg_catalog.pg_description pd
                            ON pd.objoid = pc.oid
                            AND pd.objsubid = pa.attnum
                        WHERE
                          c.table_schema = '%s'
                        GROUP BY
                          c.column_name
                        """, session.schema());

                ret.addAll(queryResult.getRows().stream()
                        .map(t -> Suggestion.ofField(first(t), second(t)))
                        .toList());

                return Lists.newArrayList(ret);
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
