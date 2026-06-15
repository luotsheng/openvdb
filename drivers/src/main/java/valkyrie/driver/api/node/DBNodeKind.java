package valkyrie.driver.api.node;

import lombok.Getter;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@Getter
public enum DBNodeKind
{
        CATALOG("database1"),
        SCHEMA("schema"),
        TABLE("table"),
        QUERY("sql"),
        ;

        private final String icon;

        DBNodeKind(String icon) { this.icon = icon; }

}
