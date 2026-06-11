package valkyrie.driver.utils;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import valkyrie.driver.api.Column;
import valkyrie.driver.api.Dialect;
import valkyrie.utils.exception.SystemRuntimeException;

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
public class SQLUtils
{
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
                } catch (Exception e) {
                        throw new SystemRuntimeException(e);
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
