package valkyrie.driver.api.sql;

import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.HashSet;
import java.util.Set;

import static valkyrie.utils.string.StrStaticImports.lowercase;

/**
 * @author Luo Tiansheng
 * @since 2026/4/02
 */
@Getter
public class SQLParsedStatement
{
        /**
         * SQL 语句
         */
        private final Statement statement;

        /**
         * 命令类型
         */
        @Setter
        private SQLCommandType command;

        /**
         * sql 脚本
         */
        private final String textValue;

        /**
         * SQL 语句中的所有表名称
         */
        private final Set<String> tables = new HashSet<>();

        /**
         * 如果 SQL 自动解析失败，则将 SQL 降级为纯字符串，去执行。
         */
        public SQLParsedStatement(String text, SQLCommandType command)
        {
                this.statement = null;
                this.command = command;
                this.textValue = text;
        }

        public SQLParsedStatement(Statement statement)
        {
                this.statement = statement;
                this.command = toType(statement);
                this.textValue = statement.toString();

                TablesNamesFinder<Void> finder = new TablesNamesFinder<>();

                try {
                        this.tables.addAll(finder.getTables(statement));
                } catch (Exception ignored) {
                        /* IGNORED */
                }
        }

        public boolean isSingleTable()
        {
                return tables.size() == 1;
        }

        public String getSingleTableName()
        {
                if (tables.size() != 1)
                        return null;
                return tables.iterator().next();
        }

        private static SQLCommandType toType(Statement statement)
        {
                return switch (statement) {
                        /* DDL */
                        case Alter ignored -> SQLCommandType.EXECUTE;
                        case CreateTable ignored -> SQLCommandType.EXECUTE;
                        case CreateView ignored -> SQLCommandType.EXECUTE;
                        case CreateIndex ignored -> SQLCommandType.EXECUTE;
                        case Drop ignored -> SQLCommandType.EXECUTE;
                        case Truncate ignored -> SQLCommandType.EXECUTE;
                        case RefreshMaterializedViewStatement ignored -> SQLCommandType.EXECUTE;

                        /* DML */
                        case Insert ignored -> SQLCommandType.EXECUTE_UPDATE;
                        case Update ignored -> SQLCommandType.EXECUTE_UPDATE;
                        case Delete ignored -> SQLCommandType.EXECUTE_UPDATE;
                        case Merge ignored -> SQLCommandType.EXECUTE_UPDATE;
                        case Upsert ignored -> SQLCommandType.EXECUTE_UPDATE;

                        /* DQL */
                        case Select ignored -> SQLCommandType.EXECUTE_QUERY;
                        case ShowStatement ignored -> SQLCommandType.EXECUTE_QUERY;

                        /* DCL */
                        case Grant ignored -> SQLCommandType.EXECUTE;

                        /* TCL */
                        case Commit ignored -> SQLCommandType.EXECUTE_UPDATE;
                        case RollbackStatement ignored -> SQLCommandType.EXECUTE_UPDATE;

                        /* 其他直接执行 */
                        case SetStatement ignored -> SQLCommandType.EXECUTE;
                        case Execute ignored -> SQLCommandType.EXECUTE;

                        /* 默认查询 */
                        default -> {
                                String sql = lowercase(statement);

                                if (sql.startsWith("create"))
                                        yield SQLCommandType.EXECUTE;

                                yield SQLCommandType.EXECUTE_QUERY;
                        }
                };
        }

        @Override
        public String toString()
        {
                return textValue;
        }
}
