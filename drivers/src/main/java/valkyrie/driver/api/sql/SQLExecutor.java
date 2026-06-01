package valkyrie.driver.api.sql;

import valkyrie.driver.api.Dialect;
import valkyrie.driver.api.QueryResult;
import valkyrie.driver.api.Session;

/**
 * SQL 执行器（统一入口）
 * <p>
 * 用于执行所有类型 SQL 语句，并返回执行结果。
 * 仅负责 SQL 执行过程，不负责 SQL 解析、生成及方言处理。
 * <p>
 * 执行行为说明：
 * - jobId 用于标识一次独立执行任务（由调用方生成）
 * - SQL 为待执行的原始语句
 * - 执行过程由具体数据库驱动实现
 * <p>
 * 返回规则：
 * - 查询类语句（SELECT / SHOW / DESCRIBE 等）返回 DataGrid 结果集
 * - 非查询语句（INSERT / UPDATE / DELETE / DDL 等）返回 null
 * <p>
 * 取消行为：
 * - cancel(jobId) 用于终止对应执行任务
 * - 若任务已结束或不存在，则忽略或返回失败（实现决定）
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public interface SQLExecutor
{
        /**
         * 执行分页查询，返回指定范围内的数据网格。
         * <p>
         * 该方法接收完整的 SQL 查询语句，根据当前数据库方言自动添加分页子句（如 {@code LIMIT ... OFFSET ...} 或
         * {@code ROWNUM} 等），并执行查询。返回的 {@link QueryResult} 包含结果集数据、列元信息以及分页上下文。
         * <p>
         * <b>实现要求：</b>
         * <ul>
         *   <li>必须使用当前方言的 {@link Dialect#limit(String, int, int)} 方法生成带分页的 SQL</li>
         *   <li>应支持 {@code off} 从 0 开始（表示跳过前 off 条记录）</li>
         * </ul>
         * <p>
         * <b>使用示例：</b>
         * <pre>{@code
         * Session session = new Session("my_catalog", "my_schema");
         * String sql = "SELECT * FROM user ORDER BY id";
         * QueryResult queryResult = dialect.selectByPage(session, sql, 0, 20);
         * List<Map<String, Object>> rows = queryResult.getRows();
         * long total = queryResult.getTotal();  // 总记录数（如有）
         * }</pre>
         *
         * @param session 会话上下文，用于设置连接的 catalog 和 schema（不能为 {@code null}）
         * @param table   表名（不能为 {@code null} 或空白字符串）
         * @param off     起始偏移量（从 0 开始），表示跳过前 {@code off} 条记录
         * @param size    每页返回的最大记录数（必须大于 0）
         * @return 包含分页结果的数据网格，至少包含当前页的数据行；若结果集为空，返回的网格中行列表为空
         * @throws NullPointerException     如果 {@code session} 或 {@code sql} 为 {@code null}
         * @throws IllegalArgumentException 如果 {@code off < 0} 或 {@code size <= 0}，或 {@code sql} 为空白字符串
         * @see Dialect#limit(String, int, int)
         * @see QueryResult
         */
        QueryResult selectByPage(Session session, String table, int off, int size);

        /**
         * 执行 SQL 任务
         * <p>
         * 执行规则：
         * - SQL 为待执行的原始语句
         * - 执行过程由具体数据库驱动实现
         * <p>
         * 返回规则：
         * - 查询语句（SELECT / SHOW / DESCRIBE 等）返回 DataGrid
         * - 非查询语句（INSERT / UPDATE / DELETE / DDL 等）返回 null
         * <p>
         * 注意：
         * - 非查询语句的执行结果需通过执行状态或影响行数获取（实现层提供）
         * - 该方法可能为异步执行，结果返回不代表任务已完成（视实现而定）
         *
         * @param sql 待执行 SQL 任务
         * @return 查询结果集（非查询语句返回 null）
         */
        default QueryResult execute(Session session, SQL sql)
        {
                return execute(-1, session, sql);
        }

        /**
         * 执行 SQL 任务
         * <p>
         * 执行规则：
         * - SQL 为待执行的原始语句
         * - 执行过程由具体数据库驱动实现
         * <p>
         * 返回规则：
         * - 查询语句（SELECT / SHOW / DESCRIBE 等）返回 DataGrid
         * - 非查询语句（INSERT / UPDATE / DELETE / DDL 等）返回 null
         * <p>
         * 注意：
         * - 非查询语句的执行结果需通过执行状态或影响行数获取（实现层提供）
         * - 该方法可能为异步执行，结果返回不代表任务已完成（视实现而定）
         *
         * @param sqlfmt 字符串对象
         * @param args 格式化参数
         * @return 查询结果集（非查询语句返回 null）
         */
        default QueryResult execute(Session session, Object sqlfmt, Object... args) {
                return execute(session, new SQL(sqlfmt, args));
        }

        /**
         * 执行 SQL 任务
         * <p>
         * 执行由 jobId 标识的 SQL 任务，并返回查询结果。
         * <p>
         * 执行规则：
         * - jobId 由调用方提供，用于标识一次独立执行任务
         * - SQL 为待执行的原始语句
         * - 执行过程由具体数据库驱动实现
         * <p>
         * 返回规则：
         * - 查询语句（SELECT / SHOW / DESCRIBE 等）返回 DataGrid
         * - 非查询语句（INSERT / UPDATE / DELETE / DDL 等）返回 null
         * <p>
         * 注意：
         * - 非查询语句的执行结果需通过执行状态或影响行数获取（实现层提供）
         * - 该方法可能为异步执行，结果返回不代表任务已完成（视实现而定）
         *
         * @param jobId 执行任务唯一标识（由调用方生成）
         * @param sql   待执行 SQL 任务
         * @return 查询结果集（非查询语句返回 null）
         */
        QueryResult execute(long jobId, Session session, SQL sql);

        /**
         * 取消 SQL 执行任务
         * <p>
         * 根据 jobId 取消正在执行的 SQL 任务。
         * <p>
         * 行为说明：
         * - 若任务正在执行，则尝试中断执行
         * - 若任务已完成或不存在，则忽略或失败（由实现决定）
         *
         * @param jobId 执行任务唯一标识
         */
        void cancel(long jobId);
}