package valkyrie.driver.suggestion;

import valkyrie.driver.api.Column;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 智能提示上下文引擎。
 * <p>
 * 在给定的 SQL 文本和光标位置下，解析出当前语句引用的表与别名，据此返回对应的
 * 提示项：
 * <ul>
 *   <li>{@code SELECT * FROM table_a WHERE |}：显示 table_a 的字段 + 关键字 + 表名</li>
 *   <li>{@code FROM table_a t0 JOIN table_b t1 ON t0.|}：只显示 table_a 的字段</li>
 * </ul>
 * 引擎构建（读取库表与字段元数据）需在后台线程完成，{@link #resolve} 为纯内存计算，
 * 可安全地在 JavaFX 线程中响应补全请求。
 *
 * @author Luo Tiansheng
 * @since 2026/9/11
 */
public class SuggestionEngine
{
        private static final Pattern TABLE_REF = Pattern.compile(
                "(?i)\\b(?:from|join)\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)"
                        + "(?:\\s+(?:as\\s+)?([`\"\\[]?[\\w]+[`\"\\]]?))?");

        private static final Pattern QUALIFIER = Pattern.compile(
                "([`\"\\[]?[\\w]+[`\"\\]]?)\\s*\\.\\s*[\\w`\"\\[\\]]*$");

        private static final Set<String> EXTRA_RESERVED = Set.of(
                "WHERE", "ON", "GROUP", "ORDER", "HAVING", "LIMIT", "OFFSET", "INNER",
                "LEFT", "RIGHT", "FULL", "CROSS", "JOIN", "AS", "USING", "SET", "VALUES",
                "UNION", "SELECT", "AND", "OR", "BY", "INTO", "UPDATE", "DELETE");

        private final List<Suggestion> keywords;
        private final List<Suggestion> tables;
        private final Map<String, List<Suggestion>> columnsByTable;
        private final Set<String> reserved;

        private SuggestionEngine(List<Suggestion> keywords,
                                 List<Suggestion> tables,
                                 Map<String, List<Suggestion>> columnsByTable,
                                 Set<String> reserved)
        {
                this.keywords = keywords;
                this.tables = tables;
                this.columnsByTable = columnsByTable;
                this.reserved = reserved;
        }

        /**
         * 构建引擎（会访问数据库元数据，请在后台线程调用）
         */
        public static SuggestionEngine of(Driver driver, Session session)
        {
                List<Suggestion> keywords = new ArrayList<>();
                List<Suggestion> tables = new ArrayList<>();

                for (Suggestion suggestion : driver.getSuggestions(session)) {
                        switch (suggestion.getKind()) {
                                case "Class" -> tables.add(suggestion);
                                case "Field" -> { /* 扁平字段忽略，改用按表字段 */ }
                                default -> keywords.add(suggestion);
                        }
                }

                Map<String, List<Suggestion>> columnsByTable = new HashMap<>();

                try {
                        driver.getTableColumns(session).forEach((table, columns) -> {
                                List<Suggestion> fields = new ArrayList<>();

                                for (Column column : columns)
                                        fields.add(Suggestion.ofField(
                                                column.getName(),
                                                column.getType() == null ? "" : column.getType()));

                                columnsByTable.put(table.toLowerCase(), fields);
                        });
                } catch (Exception e) {
                        /* 元数据读取失败时退化为仅关键字/表名提示 */
                }

                Set<String> reserved = new HashSet<>(EXTRA_RESERVED);
                keywords.forEach(s -> reserved.add(s.getLabel().toUpperCase()));

                return new SuggestionEngine(keywords, tables, columnsByTable, reserved);
        }

        /**
         * 根据光标上下文返回提示项（纯内存计算，可在 FX 线程执行）
         *
         * @param sql    编辑器完整文本
         * @param offset 光标在文本中的偏移
         */
        public List<Suggestion> resolve(String sql, int offset)
        {
                if (sql == null || sql.isEmpty())
                        return new ArrayList<>(keywords);

                offset = Math.max(0, Math.min(offset, sql.length()));

                String before = sql.substring(0, offset);

                int statementStart = before.lastIndexOf(';') + 1;
                int statementEnd = sql.indexOf(';', offset);
                if (statementEnd < 0)
                        statementEnd = sql.length();

                String statement = sql.substring(statementStart, statementEnd);

                Map<String, String> aliasToTable = new LinkedHashMap<>();
                List<String> referencedTables = new ArrayList<>();
                extractTables(statement, aliasToTable, referencedTables);

                /* 形如 t0. / table. 的限定名：只返回对应表的字段 */
                Matcher qualifier = QUALIFIER.matcher(before);
                if (qualifier.find()) {
                        String name = unquote(qualifier.group(1));
                        String table = aliasToTable.getOrDefault(name.toLowerCase(), name);
                        List<Suggestion> columns = columnsByTable.get(table.toLowerCase());
                        return columns == null ? new ArrayList<>() : columns;
                }

                List<Suggestion> result = new ArrayList<>();
                Set<String> added = new HashSet<>();

                for (String table : referencedTables) {
                        List<Suggestion> columns = columnsByTable.get(table.toLowerCase());

                        if (columns == null)
                                continue;

                        for (Suggestion column : columns) {
                                if (added.add(column.getLabel()))
                                        result.add(column);
                        }
                }

                result.addAll(keywords);
                result.addAll(tables);

                return result;
        }

        private void extractTables(String statement,
                                   Map<String, String> aliasToTable,
                                   List<String> referencedTables)
        {
                Matcher matcher = TABLE_REF.matcher(statement);

                while (matcher.find()) {
                        String table = unquote(matcher.group(1));
                        String key = table.contains(".")
                                ? table.substring(table.lastIndexOf('.') + 1)
                                : table;

                        if (!referencedTables.contains(key))
                                referencedTables.add(key);

                        aliasToTable.putIfAbsent(key.toLowerCase(), key);

                        String alias = matcher.group(2) == null ? null : unquote(matcher.group(2));

                        if (alias != null && !isReserved(alias))
                                aliasToTable.put(alias.toLowerCase(), key);
                }
        }

        private boolean isReserved(String value)
        {
                return reserved.contains(value.toUpperCase());
        }

        private static String unquote(String value)
        {
                if (value == null || value.length() < 2)
                        return value;

                char first = value.charAt(0);
                char last = value.charAt(value.length() - 1);

                if ((first == '`' && last == '`')
                        || (first == '"' && last == '"')
                        || (first == '[' && last == ']'))
                        return value.substring(1, value.length() - 1);

                return value;
        }
}
