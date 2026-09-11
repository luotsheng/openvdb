package valkyrie.driver.api;

import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.update.Update;
import valkyrie.driver.api.exception.DriverException;
import valkyrie.driver.api.sql.SQL;
import valkyrie.utils.Optional;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.collection.Maps;

import java.util.*;

/**
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
@Getter
@Setter
public class QueryResult
{
        @Getter
        private List<Column> columns;

        private final Map<String, Integer> columnIndices = Maps.newHashMap();

        private List<Column> pks;

        @Setter
        @Getter
        private List<GridRow> rows = Lists.newArrayList();

        @Setter
        @Getter
        private boolean editable = false;

        @Setter
        @Getter
        private boolean addable = false;

        private final Session session;
        private final Driver driver;

        private final Map<Integer, GridRow> updateRowBuffer = new HashMap<>();

        private final SQL sql;

        public interface UpdateListener
        {
                void update(GridRow row);
        }

        @Setter
        private UpdateListener updateListener;

        /**
         * 构造器
         */
        public QueryResult(Session session, Driver driver, SQL sql)
        {
                this.session = session;
                this.driver = driver;
                this.sql = sql;
        }
        
        public static QueryResult ofValue(Session session, String value)
        {
                QueryResult queryResult = new QueryResult(session, null, null);
                
                Column col = new Column();
                col.setLabel("Value");
                col.setName("Value");
                col.setType("Object");

                queryResult.setColumns(Lists.of(col));
                queryResult.addEmptyRow();
                queryResult.getRows().getFirst().set(0, value);

                return queryResult;
        }

        public static QueryResult ofList(Session session, List<String> list)
        {
                QueryResult queryResult = new QueryResult(session, null, null);

                Column col = new Column();
                col.setLabel("Value");
                col.setName("Value");
                col.setType("ANY");

                queryResult.setColumns(Lists.of(col));

                list.forEach(e -> {
                        GridRow row = new GridRow();
                        row.add(e);
                        queryResult.rows.add(row);
                });

                return queryResult;
        }

        public int size()
        {
                return rows.size();
        }

        public void setColumns(List<Column> columns)
        {
                this.columns = columns;

                pks = columns.stream()
                        .filter(Column::isPrimary)
                        .toList();

                for (int i = 0; i < columns.size(); i++)
                        columnIndices.put(columns.get(i).getLabel(), i);
        }


        public void reload()
        {
                if (driver != null && session != null && sql != null) {

                        QueryResult queryResult = driver.execute(session, sql);

                        columns = queryResult.columns;
                        rows = queryResult.rows;

                        clearUpdateBuffer();

                }
        }

        public void addEmptyRow()
        {
                rows.addLast(new GridRow(columns.size()));
        }

        public void remove(List<Integer> indices)
        {
                if (indices == null || indices.isEmpty())
                        return;

                SQL sql = toDeleteSQL(indices);
                driver.execute(session, sql);
        }

        public void addUpdateRow(int colIndex, int rowIndex, String newValue)
        {
                if (rowIndex < 0)
                        return;

                GridRow row = new GridRow();

                if (updateRowBuffer.containsKey(rowIndex)) {
                        row.addAll(updateRowBuffer.get(rowIndex));
                } else {
                        row.addAll(rows.get(rowIndex));
                }

                row.set(colIndex, newValue);

                updateRowBuffer.put(rowIndex, row);

                if (updateListener != null)
                        updateListener.update(row);

        }

        public boolean isUpdatable()
        {
                return !updateRowBuffer.isEmpty();
        }

        public void clearUpdateBuffer()
        {
                updateRowBuffer.clear();
        }

        /**
         * 刷新行更新缓冲区
         */
        public void update()
        {
                if (!isUpdatable())
                        return;

                SQL sql = toUpdateSQL();

                int[] affected = { 0 };

                driver.execute(-1, session, sql, new SQLExecuteCallback()
                {
                        @Override
                        public void row(int value)
                        {
                                affected[0] += value;
                        }
                });

                /*
                 * 影响行数为 0 说明 WHERE 没有匹配到原始数据行（数据可能已被其他
                 * 会话修改，或无主键表的定位条件不精确）。此时必须报错，而不是
                 * 静默重载旧数据，否则用户会看到"提交了但数据没更新"。
                 */
                if (affected[0] <= 0)
                        throw new DriverException("没有匹配到需要更新的数据行，修改可能未生效");

                reload();
                updateRowBuffer.clear();
        }

        private SQL toDeleteSQL(List<Integer> indices)
        {
                List<Delete> deletes = new ArrayList<>();

                indices.forEach(index -> {
                        var delete = new Delete();
                        List<Expression> equals = new ArrayList<>();

                        var table = new Table(driver.getDialect().removeQuote(sql.getSingleTableName()));
                        delete.setTable(table);

                        List<Column> whereColumns = columns;

                        if (!pks.isEmpty())
                                whereColumns = pks;

                        whereColumns.forEach(col -> {

                                var w = equalsOrNull(col.getName(), rows.get(index).get(col.getIndex()));

                                equals.add(w);

                        });

                        Expression exp = equals.getFirst();

                        for (int i = 1; i < equals.size(); i++)
                                exp = new AndExpression(exp, equals.get(i));

                        delete.setWhere(exp);

                        if (pks.isEmpty()) {
                                Limit limit = new Limit();
                                limit.setRowCount(new LongValue(1));
                                delete.setLimit(limit);
                        }

                        deletes.add(delete);
                });

                StringBuilder builder = new StringBuilder();

                for (Delete delete : deletes)
                        builder.append(delete.toString()).append(";");

                return new SQL(builder.toString());
        }

        private SQL toUpdateSQL()
        {
                List<Update> updates = new ArrayList<>();

                for (Map.Entry<Integer, GridRow> entry : updateRowBuffer.entrySet()) {

                        var update = new Update();
                        var row = entry.getValue();

                        var table = new Table(driver.getDialect().removeQuote(sql.getSingleTableName()));
                        update.setTable(table);

                        for (int i = 0; i < row.size(); i++) {

                                String v = row.get(i);

                                if (!Objects.equals(v, rows.get(entry.getKey()).get(i))) {

                                        var c = new net.sf.jsqlparser.schema.Column(columns.get(i).getName());

                                        Expression exp;

                                        if (v != null) {
                                                exp = new StringValue(escape(v));
                                        } else {
                                                exp = new NullValue();
                                        }

                                        update.addUpdateSet(c, exp);

                                }

                        }

                        List<Column> whereColumns = columns;

                        if (!pks.isEmpty())
                                whereColumns = pks;

                        Expression whereExpression = null;

                        for (Column col : whereColumns) {

                                var r = rows.get(entry.getKey());
                                var w = equalsOrNull(col.getName(), r.get(col.getIndex()));

                                // 组合 WHERE 条件
                                if (whereExpression == null) {
                                        whereExpression = w;
                                } else {
                                        whereExpression = new AndExpression(whereExpression, w);
                                }

                        }

                        if (whereExpression != null)
                                update.setWhere(whereExpression);

                        /* 如果没有主键只修改一条 */
                        if (pks.isEmpty()) {
                                Limit limit = new Limit();
                                limit.setRowCount(new LongValue(1));
                                update.setLimit(limit);
                        }

                        updates.add(update);
                }

                StringBuilder builder = new StringBuilder();

                for (Update update : updates)
                        builder.append(update.toString()).append(";");

                return new SQL(builder.toString());
        }

        /**
         * 构造 {@code column = value} 定位条件；原值为 NULL 时使用
         * {@code column IS NULL}，避免生成恒不匹配的 {@code column = NULL}。
         */
        private static Expression equalsOrNull(String columnName, String value)
        {
                var column = new net.sf.jsqlparser.schema.Column(columnName);

                if (value == null)
                        return new IsNullExpression(column);

                var equals = new EqualsTo();
                equals.setLeftExpression(column);
                equals.setRightExpression(new StringValue(escape(value)));

                return equals;
        }

        /**
         * 转义字符串字面量中的单引号，防止生成的 SQL 语法错误。
         */
        private static String escape(String value)
        {
                return value.replace("'", "''");
        }

        /**
         * 根据列名获取指定行数据
         *
         * @param index 行索引
         * @param columnName 字段名
         * @return 对应行列值
         */
        public String getRowValue(int index, String columnName)
        {
                return getRowValue(index, columnIndices.get(columnName));
        }

        /**
         * 根据列名获取指定行数据
         *
         * @param index 行索引
         * @param col 列索引
         * @return 对应行列值
         */
        public String getRowValue(int index, int col)
        {
                return Optional.ifError(() -> rows.get(index).get(col), null);
        }
}
