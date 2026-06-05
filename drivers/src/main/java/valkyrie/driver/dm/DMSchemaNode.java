package valkyrie.driver.dm;

import valkyrie.driver.api.node.DBMetadataProvider;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBSchemaNode;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class DMSchemaNode extends DBSchemaNode
{
        public DMSchemaNode(String label, DBMetadataProvider metadataProvider)
        {
                super(null, label, metadataProvider);
        }

        @Override
        public boolean hasChildren()
        {
                return true;
        }

        @Override
        public List<DBNode> getChildren()
        {
                return getMetadataProvider().getChildrenOfSchema(this);
        }
}
