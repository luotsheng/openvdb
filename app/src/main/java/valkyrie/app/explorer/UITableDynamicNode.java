package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import lombok.Getter;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.OpenTableDataPaneEvent;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBTableContainerNode;
import valkyrie.driver.api.node.DBTableNode;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class UITableDynamicNode extends UIDynamicNode
{
        private final @Getter Session session;

        public UITableDynamicNode(UIExplorerNode parent, DBNode dbNode)
        {
                super(parent, dbNode);
                this.session = ((DBTableContainerNode) dbNode.getParent()).getSession();
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();
                return contextMenu;
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                open();
        }

        public Table getTable()
        {
                return ((DBTableNode) dbNode).getTable();
        }

        public void open()
        {
                EventBus.publish(new OpenTableDataPaneEvent(this));
        }
}
