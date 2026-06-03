package valkyrie.driver.api;

/**
 * 数据库方言接口。
 * <p>
 * 用于封装特定数据库的 SQL 语法差异，为上层的 SQL 构建和执行提供统一适配。
 * 不同数据库（MySQL、PostgreSQL、Oracle 等）应提供各自的 {@code Dialect} 实现，
 * 以生成符合该数据库语法的 SQL 语句。
 * <p>
 * <b>职责范围：</b>
 * <ul>
 *   <li>生成数据库特定的 DDL 语句（如 {@code SHOW CREATE TABLE}）</li>
 *   <li>生成分页查询的 {@code LIMIT} 或 {@code ROWNUM} 子句</li>
 *   <li>转义标识符（表名、列名等）以防止 SQL 注入或处理保留字</li>
 * </ul>
 * <p>
 * <b>实现注意事项：</b>
 * <ul>
 *   <li>实现类应为无状态（stateless）且线程安全</li>
 *   <li>每个数据库产品应有唯一对应的方言实现</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public interface Dialect
{
        /**
         * 生成数据库特定的分页子句。
         * <p>
         * 该方法接收原始 SQL 查询语句，并返回包装后的带分页限制的 SQL。
         * 不同数据库的分页实现方式不同：
         * <ul>
         *   <li>MySQL / PostgreSQL: {@code sql LIMIT size OFFSET off}</li>
         *   <li>SQL Server (老版本): 使用 {@code ROW_NUMBER()} 子查询</li>
         *   <li>Oracle (12c 之前): 使用 {@code ROWNUM} 嵌套查询</li>
         *   <li>Oracle (12c+): 使用 {@code OFFSET off ROWS FETCH NEXT size ROWS ONLY}</li>
         * </ul>
         *
         * @param sql  原始 SQL 查询语句（不能为 {@code null} 或空白）
         * @param off  起始偏移量（从 0 开始），表示跳过前 {@code off} 条记录
         * @param size 返回的最大记录条数（必须大于 0）
         * @return 添加分页子句后的完整 SQL 语句
         * @throws IllegalArgumentException 如果 {@code off < 0} 或 {@code size <= 0}，或原始 SQL 无效
         * @throws NullPointerException 如果 {@code sql} 为 {@code null}
         */
        default String limit(String sql, int off, int size)
        {
                return sql + " LIMIT " + size + " OFFSET " + off;
        }

        /**
         * 对 SQL 语句进行规范化处理。
         * <p>
         * 该方法用于将传入的 SQL 语句转换为当前方言所期望的标准形式，以消除不同书写风格或数据库兼容性差异带来的影响。
         * 典型处理包括但不限于：
         * <ul>
         *   <li>移除 SQL 中不同数据库的独有语法</li>
         *   <li>统一换行符和空白字符（将连续空白压缩为单个空格）</li>
         *   <li>将标识符（表名、列名）中的引号统一为当前方言的转义风格</li>
         * </ul>
         *
         * @param sql 原始 SQL 语句（不能为 {@code null}）
         * @return 规范化后的 SQL 语句
         * @throws NullPointerException 如果 {@code sql} 为 {@code null}
         */
        default String normalize(String sql) { return sql; }

        /**
         * 转义数据库标识符（如表名、列名）。
         * <p>
         * 根据数据库的引号规则，将标识符包裹在合适的引号中，以处理：
         * <ul>
         *   <li>包含特殊字符（如空格、连字符）的标识符</li>
         *   <li>与数据库保留字冲突的标识符</li>
         *   <li>大小写敏感或大小写折叠的需求</li>
         * </ul>
         * <b>示例：</b>
         * <ul>
         *   <li>MySQL: {@code quote("order")} 返回 {@code `order`}</li>
         *   <li>PostgreSQL / ANSI SQL: {@code quote("order")} 返回 {@code "order"}</li>
         *   <li>SQL Server: {@code quote("order")} 返回 {@code [order]}</li>
         * </ul>
         *
         * @param identifier 原始标识符（不能为 {@code null} 或空白）
         * @return 转义后的标识符，可直接拼接在 SQL 语句中使用
         * @throws IllegalArgumentException 如果 {@code identifier} 为 {@code null} 或仅含空白字符
         */
        String quote(String identifier);

        /**
         * 移除数据库标识符两端的引号。
         * <p>
         * 该方法用于去除由 {@link Dialect#quote(String)} 添加的数据库特定引号（如反引号、双引号或方括号），
         * 还原为原始标识符名称。若标识符未包含匹配的引号，则原样返回。
         * <p>
         * <b>处理规则：</b>
         * <ul>
         *   <li>MySQL：移除两端反引号（`）</li>
         *   <li>PostgreSQL / ANSI SQL：移除两端双引号（"）</li>
         *   <li>SQL Server：移除两端方括号（[ ]）</li>
         *   <li>仅当首尾字符均为相同类型的引号时执行移除，否则返回原字符串</li>
         * </ul>
         *
         * @param identifier 可能带有引号的标识符（可以为 {@code null} 或空白）
         * @return 移除引号后的标识符；若输入为 {@code null}，返回 {@code null}；若输入为空白字符串，返回原字符串
         * @see #quote(String)
         */
        String removeQuote(String identifier);
}
