package valkyrie.driver.api.node;

import valkyrie.driver.api.Table;
import valkyrie.utils.collection.Lists;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class DBTableNode extends DBNode
{
        private final Table table;

        public DBTableNode(DBNode parent, Table table)
        {
                super(parent, table.getName(), DBNodeKind.TABLE, null);
                this.table = table;
        }

        @Override
        public boolean hasChildren()
        {
                return false;
        }

        @Override
        public List<DBNode> getChildren()
        {
                return Lists.emptyList();
        }
}
