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

                parse(type, raw);
        }

        /**
         * 拆分解析 SQL：
         * <p>
         * 优先整体交给 jsqlparser 解析；若整体解析失败（例如脚本中含 MySQL 的
         * {@code SET @var := ...}、{@code SELECT ... INTO @var}、{@code PREPARE}/{@code DEALLOCATE}
         * 等 jsqlparser 不支持的方言语句），则按分号将脚本逐条切分后容错解析，
         * 避免把整段脚本折叠为单条语句而导致命令类型误判、整批执行失败。
         */
        private void parse(SQLCommandType type, String raw)
        {
                try {
                        Statements parsed = CCJSqlParserUtil.parseStatements(raw);

                        for (Statement statement : parsed) {
                                SQLParsedStatement sqlParsedStatement = new SQLParsedStatement(statement);
                                if (type != null)
                                        sqlParsedStatement.setCommand(type);
                                this.statements.add(sqlParsedStatement);
                        }
                } catch (Exception e) {
                        for (String part : splitStatements(raw)) {
                                this.statements.add(parseOne(type, part));
                        }
                }
        }

        /**
         * 单条语句容错解析：jsqlparser 可解析则走结构化类型推导；
         * 解析失败则降级为纯文本，并按首关键字粗判命令类型。
         */
        private static SQLParsedStatement parseOne(SQLCommandType type, String part)
        {
                try {
                        Statement statement = CCJSqlParserUtil.parse(part);
                        SQLParsedStatement sqlParsedStatement = new SQLParsedStatement(statement);
                        if (type != null)
                                sqlParsedStatement.setCommand(type);
                        return sqlParsedStatement;
                } catch (Exception e) {
                        SQLCommandType command = type != null ? type : SQLParsedStatement.classify(part);
                        return new SQLParsedStatement(part, command);
                }
        }

        /**
         * 按分号将原始 SQL 切分为独立语句文本。
         * <p>
         * 切分过程会跳过字符串/引用标识符（含引号转义）以及行注释（--、#）与块注释（/* ... *{@code /}），
         * 避免语句内部出现分号或注释时被误切分。
         */
        private static List<String> splitStatements(String raw)
        {
                List<String> ret = Lists.newArrayList();

                StringBuilder part = new StringBuilder();
                int i = 0;
                int n = raw.length();
                char quote = 0;

                while (i < n) {
                        char c = raw.charAt(i);

                        /* 行注释 -- / # */
                        if (quote == 0 && c == '-' && i + 1 < n && raw.charAt(i + 1) == '-') {
                                i = skipToLineEnd(raw, i + 2);
                                continue;
                        }

                        if (quote == 0 && c == '#') {
                                i = skipToLineEnd(raw, i + 1);
                                continue;
                        }

                        /* 块注释 */
                        if (quote == 0 && c == '/' && i + 1 < n && raw.charAt(i + 1) == '*') {
                                int end = raw.indexOf("*/", i + 2);
                                i = end < 0 ? n : end + 2;
                                continue;
                        }

                        /* 进入字符串 / 引用标识符 */
                        if (quote == 0 && isQuote(c)) {
                                quote = c;
                                part.append(c);
                                i++;
                                continue;
                        }

                        if (quote != 0) {
                                part.append(c);

                                /* 反斜杠转义 */
                                if (c == '\\' && i + 1 < n) {
                                        part.append(raw.charAt(i + 1));
                                        i += 2;
                                        continue;
                                }

                                /* 引号结束（连续双引号视为转义，字符串继续） */
                                if (c == quote) {
                                        if (i + 1 < n && raw.charAt(i + 1) == quote) {
                                                part.append(quote);
                                                i += 2;
                                                continue;
                                        }
                                        quote = 0;
                                }

                                i++;
                                continue;
                        }

                        /* 语句结束 */
                        if (c == ';') {
                                collect(part, ret);
                                i++;
                                continue;
                        }

                        part.append(c);
                        i++;
                }

                collect(part, ret);

                return ret;
        }

        private static boolean isQuote(char c)
        {
                return c == '\'' || c == '"' || c == '`';
        }

        private static int skipToLineEnd(String raw, int from)
        {
                int end = raw.indexOf('\n', from);
                return end < 0 ? raw.length() : end + 1;
        }

        private static void collect(StringBuilder part, List<String> ret)
        {
                String sql = part.toString().trim();
                part.setLength(0);

                if (!sql.isEmpty())
                        ret.add(sql);
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
