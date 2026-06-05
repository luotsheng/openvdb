package valkyrie.driver.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.commands.ProtocolCommand;
import valkyrie.driver.api.*;
import valkyrie.driver.api.exception.DriverException;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.sql.SQL;
import valkyrie.driver.suggestion.Suggestion;
import valkyrie.utils.collection.Lists;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static valkyrie.utils.TypeConverter.atos;
import static valkyrie.utils.string.StaticLibrary.fmt;
import static valkyrie.utils.string.StaticLibrary.strip;

/**
 * Redis 驱动层实现
 *
 * @author Luo Tiansheng
 * @since 2026/4/20
 */
public class RedisDriver extends Driver
{

        private final Jedis jedis;

        /**
         * 构造一个新的驱动实例。
         *
         * @param dataSource 数据源，用于获取数据库连接（不能为 {@code null}）
         * @throws NullPointerException 如果 {@code dataSource} 为 {@code null}
         */
        public RedisDriver(VkDataSource dataSource) {
                super(dataSource);
                this.jedis = ((RedisDataSource) dataSource).getJedis();
        }

        @Override
        public List<Catalog> getCatalogs() {
                List<Catalog> catalogs = Lists.newArrayList();
                int count = Integer.parseInt(jedis.configGet("databases").get("databases"));
                NumberFormat numberFormat = NumberFormat.getInstance();

                for (int i = 0; i < count; i++) {
                        jedis.select(i);
                        long dbSize = jedis.dbSize();
                        if (dbSize > 0) {
                                String index = String.valueOf(i);
                                String lab = fmt("DB%s (%s keys)", index, numberFormat.format(dbSize));
                                catalogs.add(Catalog.of(lab, index));
                        }
                }

                return catalogs;
        }

        @Override
        public QueryResult execute(long jobId, Session session, SQL sql, SQLExecuteCallback callback)
        {
                String currentCommandRef = null;

                try {
                        jedis.select(Integer.parseInt(session.catalog()));
                        currentCommandRef = sql.getRaw();
                        callback.execute(currentCommandRef);
                        String[] parts = strip(currentCommandRef).split("\\s+");
                        ProtocolCommand cmd = () -> parts[0].getBytes(StandardCharsets.UTF_8);
                        byte[][] args = new byte[parts.length - 1][];

                        for (int i = 1; i < parts.length; i++)
                                args[i - 1] = parts[i].getBytes(StandardCharsets.UTF_8);

                        // GET serviceCalendar|2026-04
                        Object result = jedis.sendCommand(cmd, args);

                        long startTime = System.currentTimeMillis();

                        var ret = switch (result) {
                                case null -> QueryResult.ofValue(session, null);
                                case byte[] b -> QueryResult.ofValue(session, atos(b));
                                case Long l -> QueryResult.ofValue(session, atos(l));
                                case List<?> list -> {
                                        List<?> mut = list;

                                        if (mut.isEmpty())
                                                yield QueryResult.ofList(session, List.of());

                                        List<String> values = new ArrayList<>();
                                        Object second = mut.get(1);

                                        if (second instanceof List<?> byteList)
                                                mut = byteList;

                                        for (Object v : mut)
                                                values.add(atos((byte[]) v));

                                        yield QueryResult.ofList(session, values);
                                }
                                default -> QueryResult.ofValue(session, result.toString());
                        };

                        long endTime = System.currentTimeMillis();
                        callback.cost(endTime - startTime);

                        return ret;
                } catch (Exception e) {
                        throw new DriverException(e);
                }
        }

        @Override
        public DbType getType()
        {
                return DbType.redis;
        }

        @Override
        public List<DBNode> getNodeHierarchy()
        {
                List<DBNode> ret = Lists.newArrayList();
                RedisMetadataProvider metadataProvider = new RedisMetadataProvider(this);

                List<Catalog> catalogs = getCatalogs();
                for (Catalog catalog : catalogs)
                        ret.add(new RedisCatalogNode(catalog, metadataProvider));

                return ret;
        }

        @Override
        protected Dialect createDialect()
        {
                return null;
        }

        @Override
        public String showCreateTable(Session session, String table)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public List<Suggestion> getSuggestion(Session session)
        {
                return Lists.newArrayList(RedisSuggestions.VALUES);
        }

        @Override
        public List<Table> getTables(Session session)
        {
                return Lists.newArrayList();
        }

        @Override
        public List<Index> getIndexes(Session session, String table)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> getIndexTypes()
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void dropTable(Session session, String table)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void dropColumns(Session session, String table, Collection<Column> columns)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void dropIndexKeys(Session session, String table, Collection<Index> selectionItems)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void dropPrimaryKey(Session session, String table)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void addPrimaryKey(Session session, String table, Collection<Column> primaryKeys)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void alterIndexKeys(Session session, String table, Collection<Index> indexes)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void alterChange(Session session, String table, Collection<Column> columns)
        {
                throw new UnsupportedOperationException();
        }

        @Override
        public void alterVisible(Session session, String table, Collection<Index> indexes)
        {
                throw new UnsupportedOperationException();
        }

}
