package valkyrie.test;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.junit.Test;

/**
 * @author Luo Tiansheng
 * @since 2026/6/12
 */
@SuppressWarnings("ALL")
public class JSqlParserTest
{
        /**
         * 如果列名包含关键字，会解析错误（JsqlParser issue: 2434）
         */
        @Test
        public void parseDDLFromSQLite() throws JSQLParserException
        {
                final String ddl = """
                        CREATE TABLE chatmsg(
                          Id INTEGER PRIMARY KEY AUTOINCREMENT,
                          MsgType INTEGER,
                          offset TEXT
                        )
                        """;

                var createTable = (CreateTable) CCJSqlParserUtil.parse(ddl);
        }

}
