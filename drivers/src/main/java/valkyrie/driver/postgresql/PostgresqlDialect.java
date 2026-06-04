package valkyrie.driver.postgresql;

import valkyrie.driver.api.Dialect;

import static valkyrie.utils.string.StaticLibrary.strcut;

/**
 * @author Luo Tiansheng
 * @since 2026/6/04
 */
public class PostgresqlDialect implements Dialect
{
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
