package valkyrie.driver.utils;

/**
 * @author Luo Tiansheng
 * @since 2026/6/8
 */
public class JdbcUtils
{
        public static String updateDefaultDatabase(String url, String value)
        {
                if (!url.startsWith("jdbc:"))
                        return url;

                int schemaEnd = url.indexOf("://");

                if (schemaEnd == -1)
                        return url;

                int dbIndex = url.indexOf("/", schemaEnd + 3);
                if (dbIndex == -1)
                        return url + "/" + value;

                int dbIndexEnd = url.indexOf("?", dbIndex);

                if (dbIndexEnd == -1)
                        return url.substring(0, dbIndex) + "/" + value;

                return url.substring(0, dbIndex) + "/" + value + "?" + url.substring(dbIndexEnd + 1);
        }
}
