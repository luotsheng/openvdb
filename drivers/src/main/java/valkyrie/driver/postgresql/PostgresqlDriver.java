package valkyrie.driver.postgresql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.driver.api.*;
import valkyrie.driver.suggestion.Suggestion;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

        public PostgresqlDriver(DataSource dataSource)
        {
                super(dataSource);
        }

        @Override
        public DbType getType()
        {
                return DbType.postgresql;
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
                return List.of();
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
