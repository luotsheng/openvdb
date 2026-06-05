package valkyrie.driver.postgresql;

import valkyrie.driver.suggestion.SqlStandardSuggestions;
import valkyrie.driver.suggestion.Suggestion;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Luo Tiansheng
 * @since 2026/06/05
 */
public class PostgresqlSuggestions
{
        public static final Set<Suggestion> VALUES;

        static {
                Set<Suggestion> values = new LinkedHashSet<>();

                Collections.addAll(values,
                        // PG 特殊类型
                        Suggestion.ofKeyword("SERIAL"),
                        Suggestion.ofKeyword("BIGSERIAL"),
                        Suggestion.ofKeyword("SMALLSERIAL"),
                        Suggestion.ofKeyword("BOOLEAN"),
                        Suggestion.ofKeyword("BYTEA"),
                        Suggestion.ofKeyword("CHARACTER VARYING"),
                        Suggestion.ofKeyword("UUID"),
                        Suggestion.ofKeyword("JSON"),
                        Suggestion.ofKeyword("JSONB"),
                        Suggestion.ofKeyword("TEXT"),
                        Suggestion.ofKeyword("ARRAY"),
                        Suggestion.ofKeyword("TIMESTAMP WITH TIME ZONE"),
                        Suggestion.ofKeyword("TIMESTAMP WITHOUT TIME ZONE"),
                        Suggestion.ofKeyword("TIME WITH TIME ZONE"),
                        Suggestion.ofKeyword("TIME WITHOUT TIME ZONE"),
                        Suggestion.ofKeyword("INTERVAL"),
                        Suggestion.ofKeyword("MONEY"),
                        Suggestion.ofKeyword("INET"),
                        Suggestion.ofKeyword("CIDR"),
                        Suggestion.ofKeyword("MACADDR"),
                        Suggestion.ofKeyword("MACADDR8"),
                        Suggestion.ofKeyword("NUMERIC"),
                        Suggestion.ofKeyword("OID"),
                        Suggestion.ofKeyword("POINT"),
                        Suggestion.ofKeyword("LINE"),
                        Suggestion.ofKeyword("LSEG"),
                        Suggestion.ofKeyword("BOX"),
                        Suggestion.ofKeyword("PATH"),
                        Suggestion.ofKeyword("POLYGON"),
                        Suggestion.ofKeyword("CIRCLE"),
                        Suggestion.ofKeyword("INT2"),
                        Suggestion.ofKeyword("INT4"),
                        Suggestion.ofKeyword("INT8"),
                        Suggestion.ofKeyword("FLOAT4"),
                        Suggestion.ofKeyword("FLOAT8"),

                        // PG 特殊语法
                        Suggestion.ofKeyword("ILIKE"),
                        Suggestion.ofKeyword("RETURNING"),
                        Suggestion.ofKeyword("LATERAL"),
                        Suggestion.ofKeyword("WINDOW"),
                        Suggestion.ofKeyword("RECURSIVE"),
                        Suggestion.ofKeyword("DISTINCT ON"),
                        Suggestion.ofKeyword("CONCURRENTLY"),
                        Suggestion.ofKeyword("TABLESPACE"),
                        Suggestion.ofKeyword("TABLESAMPLE"),
                        Suggestion.ofKeyword("COLLATION"),
                        Suggestion.ofKeyword("INHERITS"),
                        Suggestion.ofKeyword("OWNER"),
                        Suggestion.ofKeyword("TRIGGER"),
                        Suggestion.ofKeyword("RULE"),
                        Suggestion.ofKeyword("SEQUENCE"),
                        Suggestion.ofKeyword("EXTENSION"),
                        Suggestion.ofKeyword("FOREIGN DATA WRAPPER"),
                        Suggestion.ofKeyword("SERVER"),
                        Suggestion.ofKeyword("MATERIALIZED VIEW"),
                        Suggestion.ofKeyword("UNLOGGED"),
                        Suggestion.ofKeyword("TEMP"),
                        Suggestion.ofKeyword("TEMPORARY"),
                        Suggestion.ofKeyword("LISTEN"),
                        Suggestion.ofKeyword("NOTIFY"),
                        Suggestion.ofKeyword("VACUUM"),
                        Suggestion.ofKeyword("ANALYZE"),
                        Suggestion.ofKeyword("EXPLAIN ANALYZE"),
                        Suggestion.ofKeyword("TRUNCATE"),
                        Suggestion.ofKeyword("CASCADE"),

                        // PG 索引类型
                        Suggestion.ofKeyword("USING"),
                        Suggestion.ofKeyword("GIST"),
                        Suggestion.ofKeyword("GIN"),
                        Suggestion.ofKeyword("BRIN"),
                        Suggestion.ofKeyword("HASH"),
                        Suggestion.ofKeyword("BTREE"),
                        Suggestion.ofKeyword("SPGIST"),

                        // PG 内置函数
                        Suggestion.ofFunction("NOW"),
                        Suggestion.ofFunction("CURRENT_TIMESTAMP"),
                        Suggestion.ofFunction("CURRENT_DATE"),
                        Suggestion.ofFunction("AGE"),
                        Suggestion.ofFunction("DATE_TRUNC"),
                        Suggestion.ofFunction("DATE_PART"),
                        Suggestion.ofFunction("EXTRACT"),
                        Suggestion.ofFunction("TO_CHAR"),
                        Suggestion.ofFunction("TO_DATE"),
                        Suggestion.ofFunction("TO_TIMESTAMP"),
                        Suggestion.ofFunction("GENERATE_SERIES"),
                        Suggestion.ofFunction("STRING_AGG"),
                        Suggestion.ofFunction("ARRAY_AGG"),
                        Suggestion.ofFunction("ARRAY_TO_STRING"),
                        Suggestion.ofFunction("STRING_TO_ARRAY"),
                        Suggestion.ofFunction("UNNEST"),
                        Suggestion.ofFunction("JSONB_BUILD_OBJECT"),
                        Suggestion.ofFunction("JSONB_AGG"),
                        Suggestion.ofFunction("JSON_EACH"),
                        Suggestion.ofFunction("JSONB_EACH"),
                        Suggestion.ofFunction("ROW_TO_JSON"),
                        Suggestion.ofFunction("ROW_NUMBER"),
                        Suggestion.ofFunction("RANK"),
                        Suggestion.ofFunction("DENSE_RANK"),
                        Suggestion.ofFunction("NTILE"),
                        Suggestion.ofFunction("LAG"),
                        Suggestion.ofFunction("LEAD"),
                        Suggestion.ofFunction("FIRST_VALUE"),
                        Suggestion.ofFunction("LAST_VALUE"),
                        Suggestion.ofFunction("NTH_VALUE"),
                        Suggestion.ofFunction("COALESCE"),
                        Suggestion.ofFunction("NULLIF"),
                        Suggestion.ofFunction("GREATEST"),
                        Suggestion.ofFunction("LEAST"),
                        Suggestion.ofFunction("REPLACE"),
                        Suggestion.ofFunction("CONCAT"),
                        Suggestion.ofFunction("SUBSTRING"),
                        Suggestion.ofFunction("UPPER"),
                        Suggestion.ofFunction("LOWER"),
                        Suggestion.ofFunction("LENGTH"),
                        Suggestion.ofFunction("TRIM"),
                        Suggestion.ofFunction("SPLIT_PART"),
                        Suggestion.ofFunction("REGEXP_REPLACE"),
                        Suggestion.ofFunction("REGEXP_MATCHES"),
                        Suggestion.ofFunction("PG_TYPE"),
                        Suggestion.ofFunction("PG_BACKEND_PID"),
                        Suggestion.ofFunction("PG_CANCEL_BACKEND"),
                        Suggestion.ofFunction("PG_TERMINATE_BACKEND"),
                        Suggestion.ofFunction("PG_SLEEP"),
                        Suggestion.ofFunction("PG_RELATION_SIZE"),
                        Suggestion.ofFunction("PG_INDEXES_SIZE"),
                        Suggestion.ofFunction("PG_TABLE_SIZE"),
                        Suggestion.ofFunction("PG_TOTAL_RELATION_SIZE"),
                        Suggestion.ofFunction("PG_SIZE_PRETTY"),

                        // CAST 语法
                        Suggestion.ofKeyword("::INTEGER"),
                        Suggestion.ofKeyword("::TEXT"),
                        Suggestion.ofKeyword("::BOOLEAN"),
                        Suggestion.ofKeyword("::NUMERIC"),
                        Suggestion.ofKeyword("::TIMESTAMP"),
                        Suggestion.ofKeyword("::DATE"),
                        Suggestion.ofKeyword("::JSONB")
                );

                values.addAll(SqlStandardSuggestions.VALUES);

                VALUES = Collections.unmodifiableSet(values);
        }

        private PostgresqlSuggestions()
        {
        }
}
