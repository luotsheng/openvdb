package valkyrie.driver.api.node;

import valkyrie.driver.api.Table;
import valkyrie.utils.collection.Lists;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class DBTableContainerNode extends DBNode
{
        private final List<Table> tables;

        public DBTableContainerNode(DBNode parent, List<Table> tables)
        {
                super(parent, "数据表", DBNodeKind.TABLE, null);
                this.tables = tables;
        }

        @Override
        public boolean hasChildren()
        {
                return true;
        }

        @Override
        public List<DBNode> getChildren()
        {
                List<DBNode> children = Lists.newArrayList();
                for (Table table : tables)
                        children.add(new DBTableNode(this, table));
                return children;
        }
}
