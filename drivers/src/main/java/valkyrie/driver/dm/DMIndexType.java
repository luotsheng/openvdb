package valkyrie.driver.dm;

import static valkyrie.utils.string.StrStaticImports.uppercase;

/**
 * @author Luo Tiansheng
 * @since 2026/4/17
 */
public enum DMIndexType
{
        NORMAL,
        UNIQUE,
        BITMAP,
        ;

        public static DMIndexType of(String type)
        {
                return valueOf(uppercase(type));
        }

        public boolean equals(String name)
        {
                return of(name) == this;
        }

}
