package valkyrie.driver.api.node;

import lombok.Getter;
import valkyrie.driver.api.Session;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@Getter
public abstract class DBSchemaNode extends DBNode
{
        private final Session session;

        public DBSchemaNode(DBNode parent, String label, DBMetadataProvider metadataProvider)
        {
                super(parent, label, DBNodeKind.SCHEMA, metadataProvider);
                this.session = Session.of(parent != null ? parent.getLabel() : null, label);
        }
}
