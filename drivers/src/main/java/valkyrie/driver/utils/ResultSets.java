package valkyrie.driver.utils;

import valkyrie.driver.api.Column;
import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.GridRow;
import valkyrie.driver.api.QueryResult;
import valkyrie.driver.api.exception.DriverException;
import valkyrie.driver.api.sql.SQLParsedStatement;
import valkyrie.utils.collection.Lists;

import java.io.BufferedReader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 结果集工具
 *
 * @author Luo Tiansheng
 * @since 2026/3/30
 */
@SuppressWarnings("ALL")
public class ResultSets
{
        private static final String TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT_PATTERN);

        /**
         * 结果集转 QueryResultSet 对象
         */
        public static QueryResult toDataGrid(Connection connection,
                                             SQLParsedStatement ps,
                                             ResultSet rs,
                                             Dialect dialect,
                                             QueryResult queryResult)
                throws SQLException
        {
                SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_PATTERN);

                /* COL */
                setColumns(connection, ps, rs, dialect, queryResult);

                /* ROW */
                List<GridRow> rows = new ArrayList<>();

                while (rs.next()) {

                        GridRow row = new GridRow();

                        for (int i = 1; i <= queryResult.getColumns().size(); i++)
                                row.add(stringify(rs.getObject(i), sdf));

                        rows.add(row);
                }

                queryResult.setRows(rows);

                return queryResult;
        }

        private static void setColumns(Connection connection,
                                       SQLParsedStatement ps,
                                       ResultSet rs,
                                       Dialect dialect,
                                       QueryResult queryResult)
                throws SQLException
        {
                Map<String, Column> colMetas = new LinkedHashMap<>();
                ResultSetMetaData rsMeta = rs.getMetaData();

                for (int i = 1; i <= rsMeta.getColumnCount(); i++) {

                        Column c = new Column();

                        c.setIndex(i - 1);

                        c.setLabel(rsMeta.getColumnLabel(i));

                        c.setName(rsMeta.getColumnName(i));

                        c.setType(rsMeta.getColumnTypeName(i));

                        c.setNotNull(
                                rsMeta.isNullable(i) != ResultSetMetaData.columnNullable
                        );

                        colMetas.put(c.getName(), c);

                }

                boolean editable = false;

                if (ps.isSingleTable()) {
                        DatabaseMetaData dbMeta = connection.getMetaData();

                        Set<String> pks = new HashSet<>();
                        String singleTable = dialect.removeQuote(ps.getSingleTableName());

                        try (ResultSet pk = dbMeta.getPrimaryKeys(connection.getCatalog(), connection.getSchema(), singleTable)) {
                                while (pk.next())
                                        pks.add(pk.getString("COLUMN_NAME"));
                        }

                        pks.forEach(c -> {

                                Column meta = colMetas.get(c);
                                if (meta != null)
                                        meta.setPrimary(true);

                        });

                        Map<String, Map<String, Object>> columnInfo = new HashMap<>();

                        try (ResultSet col = dbMeta.getColumns(connection.getCatalog(), connection.getSchema(), singleTable, "%")) {

                                while (col.next()) {

                                        Map<String, Object> m = new HashMap<>();

                                        m.put("autoIncrement",
                                                "YES".equals(col.getString("IS_AUTOINCREMENT")));

                                        m.put("default",
                                                col.getString("COLUMN_DEF"));

                                        m.put("comment",
                                                col.getString("REMARKS"));

                                        columnInfo.put(col.getString("COLUMN_NAME"), m);

                                }

                        }

                        for (Column c : colMetas.values()) {
                                Map<String, Object> m = columnInfo.get(c.getName());

                                if (m == null)
                                        continue;

                                c.setAutoIncrement((Boolean) m.get("autoIncrement"));

                                c.setDefaultValue((String) m.get("default"));

                                c.setComment((String) m.get("comment"));
                        }

                        editable = colMetas
                                .values()
                                .stream()
                                .anyMatch(Column::isPrimary);
                }

                queryResult.setEditable(editable);
                queryResult.setColumns(Lists.newArrayList(colMetas.values()));
        }

        private static String stringify(Object val, SimpleDateFormat sdf)
        {
                if (val == null)
                        return null;

                return switch (val) {
                        case java.sql.Clob clob -> toString(clob);
                        case java.sql.Timestamp ts -> ts.toLocalDateTime().format(formatter);
                        case java.util.Date date -> sdf.format(date);
                        case LocalDateTime date -> date.format(formatter);
                        case byte[] bits -> toBInary(bits);
                        default -> val.toString();
                };
        }

        private static String toString(Clob clob)
        {
                StringBuilder builder = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(clob.getCharacterStream())) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                                builder.append(line);
                        }
                } catch (Exception e) {
                        throw new DriverException(e);
                }

                return builder.toString();
        }

        private static String toBInary(byte[] bytes)
        {
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                        sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF))
                                .replace(' ', '0'));
                }
                return sb.toString();
        }
}
