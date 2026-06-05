package valkyrie.driver.api.node;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class DBQueryNode extends DBNode
{
        public DBQueryNode(DBNode parent)
        {
                super(parent, "查询脚本", DBNodeKind.QUERY, null);
        }

        @Override
        public boolean hasChildren()
        {
                return false;
        }

        @Override
        public List<DBNode> getChildren()
        {
                return null;
        }
}
