package valkyrie.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import valkyrie.app.dialog.queryFile.QueryFileOverwriteDialog;
import valkyrie.app.dialog.queryFile.QueryFileRenameDialog;
import valkyrie.app.event.UpdateQueryFileEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.workbench.CloseWorkbenchTabEvent;
import valkyrie.app.event.workbench.OpenQueryEditorPaneEvent;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.core.model.QueryFile;
import valkyrie.core.repository.QueryFileRepository;
import valkyrie.driver.api.node.DBNode;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.utils.collection.Lists;

import java.util.List;

import static valkyrie.utils.string.StaticLibrary.streq;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class UIQueryDynamicNode extends UIDynamicNode
{
        static class QueryNodeWrapper extends DBNode
        {
                private QueryFile queryFile;

                public QueryNodeWrapper(QueryFile file)
                {
                        super(file.getName(), DBNodeKind.QUERY, null);
                        this.queryFile = file;
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

        public UIQueryDynamicNode(UIQueryContainerDynamicNode parent, QueryFile file)
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
                renameItem.setOnAction(e -> checkAndRename());

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

        public UIExplorerNode getPathNode()
        {
                /* 第一个父节点是 QueryContainer 容器节点 */
                return getExplorerParent().getExplorerParent();
        }

        public QueryFile getQueryFile()
        {
                return ((QueryNodeWrapper) dbNode).queryFile;
        }

        private void setQueryFile(QueryFile queryFile)
        {
                ((QueryNodeWrapper) dbNode).queryFile = queryFile;
        }

        private void checkAndRename()
        {
                QueryFile srcQueryFile = getQueryFile();
                String newFileName = QueryFileRenameDialog.showDialog(srcQueryFile);
                QueryFile dstQueryFile = new QueryFile(srcQueryFile.getParentFile(), newFileName);

                if (srcQueryFile.equals(dstQueryFile))
                        return;

                /* 当目标文件存在并且用户选择不覆盖时跳过 */
                if (dstQueryFile.exists()) {
                        if (!QueryFileOverwriteDialog.showDialog())
                                return;
                        dstQueryFile.forceDelete();

                        UIQueryContainerDynamicNode queryContainerDynamicNode =
                                (UIQueryContainerDynamicNode) getExplorerParent();

                        UIExplorerNode removeItem = null;

                        for (TreeItem<String> child : queryContainerDynamicNode.getChildren()) {
                                UIQueryDynamicNode queryDynamicChild = (UIQueryDynamicNode) child;
                                if (streq(queryDynamicChild.getLabel(), newFileName)) {
                                        removeItem = queryDynamicChild;
                                        break;
                                }
                        }

                        queryContainerDynamicNode.getChildren().remove(removeItem);
                }

                setQueryFile(dstQueryFile);
                QueryFileRepository.rename(srcQueryFile, newFileName);

                setLabel(newFileName);

                EventBus.publish(new UpdateQueryFileEvent(srcQueryFile, dstQueryFile, this));
        }
}
