package valkyrie.test;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.junit.Test;

/**
 * @author Luo Tiansheng
 * @since 2026/6/12
 */
public class JSqlParserTest
{
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
