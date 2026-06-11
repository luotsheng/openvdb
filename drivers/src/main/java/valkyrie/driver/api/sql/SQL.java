package valkyrie.driver.api.sql;

import lombok.Getter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.utils.collection.Lists;

import java.util.Iterator;
import java.util.List;

import static valkyrie.utils.collection.Lists.last;
import static valkyrie.utils.string.StrStaticImports.fmt;
import static valkyrie.utils.string.StrStaticImports.lowercase;

/**
 * SQL 执行单元
 * <p>
 * 表示一次用户提交的 SQL 内容，支持包含多条语句。
 * 每条语句可能属于不同类型（SELECT / DDL / DML / DCL / TCL / 扩展语句）。
 * <p>
 * 执行特性：
 * - SQL 内容可能包含多条语句（按分隔符拆分后执行）
 * - 执行顺序严格按照语句顺序
 * - 每条语句独立产生执行结果
 * <p>
 * 使用场景：
 * - 编辑器执行选中 SQL
 * - 脚本批量执行
 * - 控制台命令执行
 * <p>
 * 该接口由各自驱动独立实现，其中 SQL 方言解析等内容，由子类自由实现。
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public class SQL implements Iterable<SQLParsedStatement>
{
        private static final Logger LOG = LoggerFactory.getLogger(SQL.class);

        @Getter
        private final String raw;

        private final List<SQLParsedStatement> statements = Lists.newArrayList();

        public SQL(Object sqlfmt, Object... args)
        {
                this(null, fmt(sqlfmt, args));

        }

        public SQL(SQLCommandType type, String raw)
        {
                this.raw = raw;

                try {
                        Statements statements = CCJSqlParserUtil.parseStatements(raw);

                        for (Statement statement : statements) {
                                SQLParsedStatement sqlParsedStatement = new SQLParsedStatement(statement);
                                if (type != null)
                                        sqlParsedStatement.setCommand(type);
                                this.statements.add(sqlParsedStatement);
                        }
                } catch (Exception e) {
                        this.statements.clear();
                        type = checkType(raw);
                        this.statements.add(new SQLParsedStatement(raw, type != null ? type : SQLCommandType.EXECUTE));
                }
        }

        private static SQLCommandType checkType(String raw)
        {
                if (lowercase(raw).startsWith("pragma"))
                        return SQLCommandType.EXECUTE_QUERY;
                return null;
        }

        public SQLParsedStatement getLast()
        {
                return last(statements);
        }

        @Override
        public Iterator<SQLParsedStatement> iterator()
        {
                return statements.iterator();
        }

        public String getSingleTableName()
        {
                return last(statements).getSingleTableName();
        }
}
