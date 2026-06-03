package valkyrie.driver.dm;

import valkyrie.driver.api.Dialect;

import static valkyrie.utils.string.StaticLibrary.strcut;

/**
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public class DMDialect implements Dialect
{
        @Override
        public String normalize(String sql)
        {
                sql = sql.replaceAll("NOT\\s+CLUSTER\\s+PRIMARY\\s+KEY", "PRIMARY KEY");
                sql = sql.replaceAll("(?i)\\s+ENCRYPT\\s+WITH\\s+AES256_CBC\\s+AUTO\\s+BY\\s+WRAPPED\\s+'[^']*'", "");
                sql = sql.replaceAll("STORAGE\\s*\\([^)]*\\)", "");
                sql = sql.replaceAll("\\((\\d+)\\s+CHAR\\)", "");
                return sql;
        }

        @Override
        public String quote(String identifier)
        {
                if (identifier.startsWith("\"") && identifier.endsWith("\""))
                        return identifier;

                return "\"" + identifier + "\"";
        }

        @Override
        public String removeQuote(String identifier)
        {
                if (identifier.startsWith("\"")) {
                        identifier = strcut(identifier, 1, 0);
                        identifier = strcut(identifier, 0, -1);
                        return identifier;
                }

                return identifier;
        }
}
