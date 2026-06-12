package valkyrie.driver.api;

import lombok.Getter;
import lombok.Setter;
import valkyrie.utils.security.Codec;

import static valkyrie.utils.string.StrStaticImports.strip;

/**
 * 数据库列元数据实体。
 * <p>
 * 表示数据库表中的一个列（字段），包含列名、数据类型、约束（是否可空、是否主键、是否自增）、
 * 默认值、注释等元信息。该类继承自 {@link Sealable}，支持完整性校验：通过
 * {@link #computeIntegrityCode()} 基于列的所有关键属性计算完整性码，并可封存后验证。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>从 {@link java.sql.ResultSetMetaData} 或 {@link java.sql.DatabaseMetaData} 中提取列信息</li>
 *   <li>作为 ORM 框架的字段映射描述对象</li>
 *   <li>在 SQL 构建器中表示 SELECT 子句的列或投影项</li>
 * </ul>
 * <p>
 * <b>属性说明：</b>
 * <ul>
 *   <li>{@code label} 与 {@code name} 的区别：{@code label} 用于 AS 别名（面向展示或查询结果），
 *       {@code name} 是数据库中的真实列名。若未显式设置别名，两者通常相同。</li>
 *   <li>{@code index} 为列在结果集或表结构中的位置索引，从 0 开始。</li>
 * </ul>
 *
 * @see Sealable
 * @see java.sql.ResultSetMetaData
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
@Getter
@Setter
public class Column extends Sealable
{
        /**
         * 列别名（通常对应 SQL 中的 {@code AS} 子句）。
         * <p>
         * 用于查询结果中的列标识，可能不同于数据库中的真实列名。
         * 若未设置别名，通常与 {@link #name} 相同。
         */
        private String label;

        /**
         * 数据库中的真实列名。
         * <p>
         * 对应表定义中的字段名称，用于生成 DDL 或 WHERE 条件中的标识符。
         */
        private String name;

        /**
         * 列在结果集或表结构中的位置序号。
         * <p>
         * 从 0 开始计数，表示该列在 SELECT 列表或表定义中的顺序。
         * 可为 {@code null} 表示未指定或未获取。
         */
        private Integer index;

        /**
         * 数据库字段类型名称。
         * <p>
         * 例如：{@code "VARCHAR"}、{@code "INT"}、{@code "TIMESTAMP"} 等。
         * 该值通常来自 {@link java.sql.Types} 对应的数据库类型名。
         */
        private String type;

        /**
         * 列是否不允许存储 {@code NULL} 值。
         * <p>
         * {@code true} 表示列可为空，{@code false} 表示列具有 {@code NOT NULL} 约束。
         */
        private boolean notNull;

        /**
         * 列是否为表的主键组成部分。
         * <p>
         * {@code true} 表示该列是主键的一部分（单列主键或复合主键之一）。
         */
        private boolean primary;

        /**
         * 列是否为自动递增（自增）列。
         * <p>
         * {@code true} 表示数据库会自动为该列生成唯一递增值（如 MySQL 的 {@code AUTO_INCREMENT}，
         * PostgreSQL 的 {@code SERIAL}）。
         */
        private boolean autoIncrement;

        // /**
        //  * 自增序列的起始值（种子值）。
        //  * <p>
        //  * 定义自增列或序列生成器的第一个值。当插入新记录且未显式指定自增列值时，
        //  * 数据库将从该起始值开始分配标识符。
        //  *
        //  * @see #inc
        //  */
        // private String seed = "0";
        //
        // /**
        //  * 自增序列的步长（增量值）。
        //  * <p>
        //  * 每次分配新值时，在上一个值的基础上增加的量。步长必须为正整数。
        //  * 例如，步长为 1 时生成连续整数（1,2,3...）；步长为 10 时生成 10,20,30...
        //  *
        //  * @see #seed
        //  */
        // private String inc = "1";

        /**
         * 列的默认值表达式（字符串形式）。
         * <p>
         * 可为 {@code null} 或空字符串表示无默认值。例如：{@code "CURRENT_TIMESTAMP"}、{@code "0"}。
         */
        private String defaultValue;

        /**
         * 列的注释/说明文本。
         * <p>
         * 存储在数据库中的列级注释，用于提供业务含义说明（如 MySQL 的 {@code COMMENT} 子句）。
         */
        private String comment;

        /**
         * 原始列名
         * <p>
         * 用于校验是否更新操作
         */
        private String originalName;

        /**
         * 计算当前列对象的完整性码。
         * <p>
         * 该方法将列的所有关键属性（name、index、type、nullable、primary、autoIncrement、
         * defaultValue、comment）通过 {@code strip} 处理后拼接为字符串，再转换为字节数组
         * 并进行十六进制编码作为完整性码。若某个属性为 {@code null}，{@code strip} 方法应
         * 返回空字符串或特定占位符，以确保状态敏感性。
         * <p>
         * <b>实现要求：</b>
         * <ul>
         *   <li>属性值的任何变化都会导致完整性码不同</li>
         *   <li>同一对象状态多次调用应返回相同结果（幂等）</li>
         * </ul>
         *
         * @return 十六进制字符串表示的完整性码
         */
        @Override
        public String computeIntegrityCode()
        {
                return Codec.toByteHex((strip(name) +
                        strip(index) +
                        strip(type) +
                        strip(notNull) +
                        strip(primary) +
                        strip(autoIncrement) +
                        strip(defaultValue) +
                        strip(comment)
                ).getBytes());
        }
}
