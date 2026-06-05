package valkyrie.driver.api.node;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@Getter
public abstract class DBNode
{
        private final String label;

        private final DBNode parent;

        private final DBNodeKind kind;

        private final DBMetadataProvider metadataProvider;

        public DBNode(String label, DBNodeKind kind, DBMetadataProvider metadataProvider)
        {
                this(null, label, kind, metadataProvider);
        }

        public DBNode(DBNode parent, String label, DBNodeKind kind, DBMetadataProvider metadataProvider)
        {
                this.parent = parent;
                this.label = label;
                this.kind = kind;
                this.metadataProvider = metadataProvider;
        }

        public abstract boolean hasChildren();

        public abstract List<DBNode> getChildren();
}
