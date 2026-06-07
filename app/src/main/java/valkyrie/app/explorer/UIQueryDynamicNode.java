package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import valkyrie.app.dialog.script.QueryFileRenameDialog;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.CloseWorkbenchTabEvent;
import valkyrie.app.event.workbench.OpenQueryEditorPaneEvent;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.core.model.ScriptFile;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.utils.collection.Lists;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class UIQueryDynamicNode extends UIDynamicNode
{
        @Getter
        static class QueryNodeWrapper extends DBNode
        {
                private final ScriptFile file;

                public QueryNodeWrapper(ScriptFile file)
                {
                        super(file.getName(), DBNodeKind.QUERY, null);
                        this.file = file;
                }

                @Override
                public boolean hasChildren()
                {
                        return false;
                }

                @Override
                public List<DBNode> getChildren()
                {
                        return Lists.of();
                }

        }
        public UIQueryDynamicNode(UIExplorerNode parent, ScriptFile file)
        {
                super(parent, new QueryNodeWrapper(file));
        }

        @Override
        public ContextMenu configureContextMenu()
        {
                VkContextMenu contextMenu = new VkContextMenu();

                MenuItem openItem = new MenuItem("打开查询");
                openItem.setOnAction(e -> onMouseDoubleClickEvent());

                MenuItem renameItem = new MenuItem("重命名");
                renameItem.setOnAction(e -> rename());

                contextMenu.getItems().addAll(
                        openItem,
                        renameItem
                );

                return contextMenu;
        }

        @Override
        public void onMouseDoubleClickEvent()
        {
                EventBus.publish(new OpenQueryEditorPaneEvent(this));
        }

        @Override
        public void onParentCloseEvent()
        {
                EventBus.publish(new CloseWorkbenchTabEvent(this));
        }

        public UIExplorerNode getDirectParent()
        {
                /* 第一个父节点是 TableContainer 容器节点 */
                return getExplorerParent().getExplorerParent();
        }

        public ScriptFile getScriptFile()
        {
                return ((QueryNodeWrapper) dbNode).getFile();
        }

        private void rename()
        {
                QueryFileRenameDialog.showDialog(getScriptFile());
        }
}
