package valkyrie.driver.api.node;

import lombok.Getter;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.utils.collection.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class DBTableContainerNode extends DBNode
{
        private final TableLoader tableLoader;
        private final @Getter List<Table> tables = new ArrayList<>();
        private final @Getter Session session;

        public interface TableLoader {
                List<Table> load();
        }

        public DBTableContainerNode(DBNode parent, TableLoader tableLoader)
        {
                super(parent, "数据表", DBNodeKind.TABLE, null);
                this.tableLoader = tableLoader;
                this.tables.addAll(tableLoader.load());

                session = switch (parent) {
                        case DBCatalogNode catalogNode -> catalogNode.getSession();
                        case DBSchemaNode schemaNode -> schemaNode.getSession();
                        default -> null;
                };
        }

        @Override
        public boolean hasChildren()
        {
                return true;
        }

        @Override
        public List<DBNode> getChildren()
        {
                tables.clear();
                tables.addAll(tableLoader.load());

                List<DBNode> children = Lists.newArrayList();
                for (Table table : tables)
                        children.add(new DBTableNode(this, table));

                return children;
        }
}
