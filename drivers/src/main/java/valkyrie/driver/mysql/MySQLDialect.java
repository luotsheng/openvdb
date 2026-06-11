package valkyrie.driver.mysql;

import valkyrie.driver.api.Dialect;

import static valkyrie.utils.string.StrStaticImports.strcut;

/**
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public class MySQLDialect implements Dialect
{
        @Override
        public String quote(String identifier)
        {
                if (identifier.startsWith("`") && identifier.endsWith("`"))
                        return identifier;

                return "`" + identifier + "`";
        }

        @Override
        public String removeQuote(String identifier)
        {
                if (identifier.startsWith("`")) {
                        identifier = strcut(identifier, 1, 0);
                        identifier = strcut(identifier, 0, -1);
                        return identifier;
                }

                return identifier;
        }
}
