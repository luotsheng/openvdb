package valkyrie.driver.sqlite;

import valkyrie.driver.suggestion.SqlStandardSuggestions;
import valkyrie.driver.suggestion.Suggestion;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SQLite 关键字与函数
 *
 * @author Luo Tiansheng
 * @since 2026/9/13
 */
public class SQLiteSuggestions
{
        public static final Set<Suggestion> VALUES;

        static {
                Set<Suggestion> values = new LinkedHashSet<>();

                Collections.addAll(values,
                        // 语句与约束
                        Suggestion.ofKeyword("ABORT"),
                        Suggestion.ofKeyword("ACTION"),
                        Suggestion.ofKeyword("AFTER"),
                        Suggestion.ofKeyword("ATTACH"),
                        Suggestion.ofKeyword("AUTOINCREMENT"),
                        Suggestion.ofKeyword("BEFORE"),
                        Suggestion.ofKeyword("CONFLICT"),
                        Suggestion.ofKeyword("DEFERRABLE"),
                        Suggestion.ofKeyword("DEFERRED"),
                        Suggestion.ofKeyword("DETACH"),
                        Suggestion.ofKeyword("EXCLUSIVE"),
                        Suggestion.ofKeyword("EXPLAIN"),
                        Suggestion.ofKeyword("FAIL"),
                        Suggestion.ofKeyword("GENERATED"),
                        Suggestion.ofKeyword("IMMEDIATE"),
                        Suggestion.ofKeyword("INDEXED"),
                        Suggestion.ofKeyword("INITIALLY"),
                        Suggestion.ofKeyword("INSTEAD"),
                        Suggestion.ofKeyword("MATERIALIZED"),
                        Suggestion.ofKeyword("NOTHING"),
                        Suggestion.ofKeyword("NULLS"),
                        Suggestion.ofKeyword("PLAN"),
                        Suggestion.ofKeyword("PRAGMA"),
                        Suggestion.ofKeyword("QUERY"),
                        Suggestion.ofKeyword("RAISE"),
                        Suggestion.ofKeyword("REINDEX"),
                        Suggestion.ofKeyword("RELEASE"),
                        Suggestion.ofKeyword("RENAME"),
                        Suggestion.ofKeyword("RETURNING"),
                        Suggestion.ofKeyword("SAVEPOINT"),
                        Suggestion.ofKeyword("STRICT"),
                        Suggestion.ofKeyword("TEMP"),
                        Suggestion.ofKeyword("TEMPORARY"),
                        Suggestion.ofKeyword("TRIGGER"),
                        Suggestion.ofKeyword("UPSERT"),
                        Suggestion.ofKeyword("VACUUM"),
                        Suggestion.ofKeyword("VIEW"),
                        Suggestion.ofKeyword("VIRTUAL"),
                        Suggestion.ofKeyword("WITHOUT"),
                        // 查询与窗口
                        Suggestion.ofKeyword("EXCEPT"),
                        Suggestion.ofKeyword("GLOB"),
                        Suggestion.ofKeyword("GROUPS"),
                        Suggestion.ofKeyword("FOLLOWING"),
                        Suggestion.ofKeyword("OTHERS"),
                        Suggestion.ofKeyword("OVER"),
                        Suggestion.ofKeyword("PARTITION"),
                        Suggestion.ofKeyword("PRECEDING"),
                        Suggestion.ofKeyword("RANGE"),
                        Suggestion.ofKeyword("RECURSIVE"),
                        Suggestion.ofKeyword("TIES"),
                        Suggestion.ofKeyword("UNBOUNDED"),
                        Suggestion.ofKeyword("WINDOW"),
                        // 字面量
                        Suggestion.ofKeyword("TRUE"),
                        Suggestion.ofKeyword("FALSE"),
                        // 函数
                        Suggestion.ofFunction("CHANGES"),
                        Suggestion.ofFunction("COALESCE"),
                        Suggestion.ofFunction("DATE"),
                        Suggestion.ofFunction("DATETIME"),
                        Suggestion.ofFunction("HEX"),
                        Suggestion.ofFunction("IFNULL"),
                        Suggestion.ofFunction("IIF"),
                        Suggestion.ofFunction("INSTR"),
                        Suggestion.ofFunction("JULIANDAY"),
                        Suggestion.ofFunction("LAST_INSERT_ROWID"),
                        Suggestion.ofFunction("LIKELIHOOD"),
                        Suggestion.ofFunction("LIKELY"),
                        Suggestion.ofFunction("NULLIF"),
                        Suggestion.ofFunction("PRINTF"),
                        Suggestion.ofFunction("QUOTE"),
                        Suggestion.ofFunction("RANDOM"),
                        Suggestion.ofFunction("ROUND"),
                        Suggestion.ofFunction("SQLITE_VERSION"),
                        Suggestion.ofFunction("STRFTIME"),
                        Suggestion.ofFunction("SUBSTR"),
                        Suggestion.ofFunction("TIME"),
                        Suggestion.ofFunction("TOTAL"),
                        Suggestion.ofFunction("TYPEOF"),
                        Suggestion.ofFunction("UNHEX"),
                        Suggestion.ofFunction("UNICODE"),
                        Suggestion.ofFunction("UNIXEPOCH"),
                        Suggestion.ofFunction("UNLIKELY"),
                        // JSON1 扩展
                        Suggestion.ofFunction("JSON"),
                        Suggestion.ofFunction("JSON_ARRAY"),
                        Suggestion.ofFunction("JSON_ARRAY_LENGTH"),
                        Suggestion.ofFunction("JSON_EACH"),
                        Suggestion.ofFunction("JSON_EXTRACT"),
                        Suggestion.ofFunction("JSON_INSERT"),
                        Suggestion.ofFunction("JSON_OBJECT"),
                        Suggestion.ofFunction("JSON_PATCH"),
                        Suggestion.ofFunction("JSON_QUOTE"),
                        Suggestion.ofFunction("JSON_REMOVE"),
                        Suggestion.ofFunction("JSON_REPLACE"),
                        Suggestion.ofFunction("JSON_SET"),
                        Suggestion.ofFunction("JSON_TREE"),
                        Suggestion.ofFunction("JSON_TYPE"),
                        Suggestion.ofFunction("JSON_VALID")
                );

                values.addAll(SqlStandardSuggestions.VALUES);
                VALUES = Collections.unmodifiableSet(values);
        }

        private SQLiteSuggestions()
        {
                /* DO NOTHING... */
        }
}
