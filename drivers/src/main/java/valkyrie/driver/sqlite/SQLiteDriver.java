package valkyrie.driver.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.driver.api.*;
import valkyrie.driver.api.exception.DriverException;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.driver.api.node.DBNodePath;
import valkyrie.driver.api.sql.SQL;
import valkyrie.driver.suggestion.Suggestion;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.collection.Sets;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static valkyrie.utils.string.StaticLibrary.fmt;
import static valkyrie.utils.string.StaticLibrary.streq;

/**
 * SQLite 驱动层实现
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
@SuppressWarnings({
        "SqlNoDataSourceInspection",
        "DuplicatedCode",
        "SqlDialectInspection"
})
public class SQLiteDriver extends Driver
{
        private static final Logger LOG = LoggerFactory.getLogger(SQLiteDriver.class);

        public SQLiteDriver(VkDataSource dataSource)
        {
                super(dataSource);
        }

        @Override
        public DbType getType()
        {
                return DbType.sqlite;
        }

        @Override
        public List<DBNode> getNodeHierarchy()
        {
                return List.of();
        }

        @Override
        public DBNodePath getNodeHierarchyPath()
        {
                return new DBNodePath(DBNodeKind.CATALOG, null);
        }

        @Override
        protected Dialect createDialect()
        {
                return new SQLiteDialect();
        }

        @Override
        public List<Catalog> getCatalogs()
        {
                return Lists.of(Catalog.of("main", "main"));
        }

        @Override
        public String showCreateTable(Session session, String table)
        {
                String sql = "SELECT \"sql\" FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";

                try (Connection connection = getConnection();
                     Statement statement = connection.createStatement()) {
                        ResultSet rs = statement.executeQuery(sql);
                        return rs.getString("sql");
                } catch (SQLException e) {
                        throw new DriverException(e);
                }
        }

        @Override
        public List<Suggestion> getSuggestions(Session session)
        {
                return List.of();
        }

        @Override
        public List<Table> getTables(Session session)
        {
                String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";

                List<Table> tables = Lists.newArrayList();

                try (Connection connection = getConnection(session);
                     Statement statement = connection.createStatement()) {
                        try (var rs = statement.executeQuery(sql)) {
                                while (rs.next()) {
                                        tables.add(new Table(rs.getString("name")));
                                }
                        }
                } catch (SQLException e) {
                        throw new DriverException(e);
                }

                return tables;
        }

        @Override
        public List<Index> getIndexes(Session session, String table)
        {
                List<Index> indexes = Lists.newArrayList();
                Map<String, List<String>> indexColumns = new LinkedHashMap<>();

                // 获取索引列表
                String listSql = fmt("PRAGMA index_list(%s)", table);
                QueryResult indexList = execute(session, new SQL(listSql));

                for (int i = 0; i < indexList.size(); i++) {
                        String indexName = indexList.getRowValue(i, "name");
                        String type = streq(indexList.getRowValue(i, "unique"), "1") ? "UNIQUE" : "NORMAL";
                        boolean visible = true;

                        indexColumns.put(indexName, new ArrayList<>());

                        // 获取索引列信息
                        String infoSql = fmt("PRAGMA index_info(%s)", indexName);
                        QueryResult columnInfo = execute(session, new SQL(infoSql));

                        for (int j = 0; j < columnInfo.size(); j++) {
                                String colName = columnInfo.getRowValue(j, "name");
                                indexColumns.get(indexName).add(colName);
                        }

                        Index index = new Index();
                        index.setName(indexName);
                        index.setOriginalName(indexName);
                        index.setType(type);
                        index.setVisible(visible);
                        index.setOriginalVisible(visible);
                        index.finalIntegrityCode();

                        indexes.add(index);
                }

                // 生成列文本
                for (Index idx : indexes)
                        idx.generateColumnText(indexColumns.get(idx.getName()));

                return indexes;
        }

        @Override
        public Set<String> getIndexTypes()
        {
                return Sets.newLinkedHashSet(
                        "NORMAL",       // 普通 B-Tree 索引
                        "UNIQUE",       // 唯一约束索引
                        "PARTIAL",      // 部分索引（WHERE 条件过滤）- 3.8.0+
                        "EXPRESSION",   // 表达式/函数索引 - 3.9.0+
                        "FTS3",         // 全文索引 v3
                        "FTS4",         // 全文索引 v4
                        "FTS5",         // 全文索引 v5（推荐）
                        "RTREE"         // R-Tree 空间索引
                );
        }

        @Override
        public void dropTable(Session session, String table)
        {
                execute(session, fmt("DROP TABLE %s;", dialect.quote(table)));
        }

        @Override
        public void dropColumns(Session session, String table, Collection<Column> columns)
        {
                columns.forEach(column ->
                        execute(session, "ALTER TABLE \"%s\" DROP COLUMN \"%s\"", table, column.getOriginalName()));
        }

        @Override
        public void dropIndexKeys(Session session, String table, Collection<Index> indexes)
        {
                try {
                        for (Index index : indexes)
                                execute(session, "DROP INDEX IF EXISTS " + dialect.quote(index.getName()));
                } catch (Exception e) {
                        throw new UnsupportedOperationException("不支持删除 UNIQUE/PRIMARY KEY 约束索引，需要重建表");
                }
        }

        @Override
        public void alterIndexKeys(Session session, String table, Collection<Index> indexes)
        {
                throw new UnsupportedOperationException("不支持删除修改索引信息，需要重新建表");
        }

        @Override
        public void alterVisible(Session session, String table, Collection<Index> indexes)
        {
                throw new UnsupportedOperationException("不支持索引可见性(Invisible Index)");
        }

        @Override
        public void dropPrimaryKey(Session session, String table)
        {
                throw new UnsupportedOperationException("不支持直接删除主键，需要重建表");
        }

        @Override
        public void addPrimaryKey(Session session, String table, Collection<Column> primaryKeys)
        {
                throw new UnsupportedOperationException("不支持直接添加主键，需要重建表");
        }

        @Override
        public void alterChange(Session session, String table, Collection<Column> columns)
        {
                throw new UnsupportedOperationException("不支持直接修改列定义，需要重建表");
        }
}
