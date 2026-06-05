package valkyrie.driver.api.node;

import lombok.Getter;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public enum DBNodeKind
{
        CATALOG("database1"),
        SCHEMA("schema"),
        TABLE("table"),
        QUERY("sql"),
        ;

        private final @Getter String icon;

        DBNodeKind(String icon) { this.icon = icon; }

}
