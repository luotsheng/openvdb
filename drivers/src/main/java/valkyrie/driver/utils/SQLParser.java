package valkyrie.driver.utils;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import valkyrie.driver.api.Column;
import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.exception.ParserException;

import java.util.List;
import java.util.Map;

import static valkyrie.utils.string.StrStaticImports.strieq;
import static valkyrie.utils.string.StrStaticImports.uppercase;

/**
 * SQL 工具类
 *
 * @author Luo Tiansheng
 * @since 2026/4/7
 */
public class SQLParser
{
        /**
         * 去掉 SQL 里的注释（行注释 {@code --} / {@code #}、块注释 {@code /* ... *}{@code /}，可跨行），
         * 字符串与引用标识符里的注释符号保持原样。注释用一个空格替换，避免把前后 token 粘在一起。
         * <p>
         * 用于按关键字判断语句类型、抽取表名等场景：注释里出现 select / from 之类字样不应影响判断。
         */
        public static String stripComments(String sql)
        {
                if (sql == null || sql.isEmpty())
                        return sql == null ? null : "";

                StringBuilder out = new StringBuilder(sql.length());
                int i = 0;
                int n = sql.length();
                char quote = 0;

                while (i < n) {
                        char c = sql.charAt(i);

                        if (quote != 0) {
                                out.append(c);

                                if (c == '\\' && quote != '`' && i + 1 < n) {
                                        out.append(sql.charAt(i + 1));
                                        i += 2;
                                        continue;
                                }

                                if (c == quote) {
                                        /* 连续两个引号是转义，字符串继续 */
                                        if (i + 1 < n && sql.charAt(i + 1) == quote) {
                                                out.append(quote);
                                                i += 2;
                                                continue;
                                        }

                                        quote = 0;
                                }

                                i++;
                                continue;
                        }

                        if (c == '\'' || c == '"' || c == '`') {
                                quote = c;
                                out.append(c);
                                i++;
                                continue;
                        }

                        if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                                i = skipLine(sql, i + 2);
                                out.append(' ');
                                continue;
                        }

                        if (c == '#') {
                                i = skipLine(sql, i + 1);
                                out.append(' ');
                                continue;
                        }

                        if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                                int end = sql.indexOf("*/", i + 2);
                                i = end < 0 ? n : end + 2;
                                out.append(' ');
                                continue;
                        }

                        out.append(c);
                        i++;
                }

                return out.toString();
        }

        private static int skipLine(String sql, int from)
        {
                int end = sql.indexOf('\n', from);
                return end < 0 ? sql.length() : end + 1;
        }

        /**
         * 从 DDL 中解析字段权威类型和默认值
         */
        public static void parseColumnDefSpec(String ddl, Dialect dialect, Map<String, Column> metas)
        {
                try {
                        var createTable = (CreateTable) CCJSqlParserUtil.parse(ddl);

                        List<ColumnDefinition> definitions = createTable.getColumnDefinitions();

                        for (ColumnDefinition definition : definitions) {
                                Column columnMetaData = metas.get(toColumnName(dialect, definition));

                                if (columnMetaData == null)
                                        continue;

                                columnMetaData.setType(toDataType(definition));

                                boolean isDefault = false;

                                List<String> specs = definition.getColumnSpecs();

                                if (specs == null)
                                        continue;

                                for (int i = 0; i < specs.size(); i++) {
                                        String spec = specs.get(i);

                                        if (isDefault) {
                                                int next = i + 1;

                                                /* 针对处理带参数的默认函数值，例如：CURRENT_TIMESTAMP(3) */
                                                if (specs.size() > next && specs.get(next).startsWith("("))
                                                        spec = spec + specs.get(next);

                                                var columnDefaultSpec = newColumnDefaultSpec(definition, spec);
                                                columnMetaData.setDefaultValue(columnDefaultSpec.getDefaultValue());

                                                break;
                                        }

                                        if (strieq(spec, "DEFAULT"))
                                                isDefault = true;
                                }
                        }
                } catch (JSQLParserException e) {
                        throw new ParserException(e);
                }
        }

        private static String toColumnName(Dialect dialect, ColumnDefinition definition)
        {
                return dialect.removeQuote(definition.getColumnName());
        }

        private static String toDataType(ColumnDefinition definition)
        {
                return uppercase(definition.getColDataType().getDataType());
        }

        private static ColumnDefaultSpec newColumnDefaultSpec(ColumnDefinition definition, String spec)
        {
                String name = definition.getColumnName();

                if (name.startsWith("`") && name.endsWith("`")) {
                        name = name.substring(1);
                        name = name.substring(0, name.length() - 1);
                }

                var columnDefaultSpec = new ColumnDefaultSpec();
                columnDefaultSpec.setName(name);
                columnDefaultSpec.setDefaultValue(
                        strieq(spec, "null") ? null : spec
                );

                return columnDefaultSpec;
        }
}
