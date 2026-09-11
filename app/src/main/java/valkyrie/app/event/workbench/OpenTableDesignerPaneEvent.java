package valkyrie.app.event.workbench;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import valkyrie.app.assets.Assets;
import valkyrie.app.explorer.UITableDynamicNode;
import valkyrie.app.pane.TableDesignerPane;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;

import static valkyrie.utils.string.StrStaticImports.fmt;

/**
 * 打开设计表面板事件。
 * <p>
 * 既支持从资源树节点打开，也支持仅凭表名打开（例如在 SQL 编辑器里按住
 * Shortcut 键点击表名跳转），两者的 tabId 规则一致，可复用同一个标签页。
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class OpenTableDesignerPaneEvent extends OpenTabEvent
{
        private final Session session;
        private final Driver driver;
        private final Table table;
        private final String pathLabel;
        private final String connectionLabel;

        public OpenTableDesignerPaneEvent(UITableDynamicNode owner)
        {
                this(owner.getSession(), owner.getDriver(), owner.getTable(),
                        owner.getPathNode().getLabel(), owner.getRoot().getLabel(), owner);
        }

        public OpenTableDesignerPaneEvent(Session session,
                                          Driver driver,
                                          Table table,
                                          String pathLabel,
                                          String connectionLabel)
        {
                this(session, driver, table, pathLabel, connectionLabel, null);
        }

        private OpenTableDesignerPaneEvent(Session session,
                                           Driver driver,
                                           Table table,
                                           String pathLabel,
                                           String connectionLabel,
                                           Object owner)
        {
                super(owner);
                this.session = session;
                this.driver = driver;
                this.table = table;
                this.pathLabel = pathLabel == null ? "" : pathLabel;
                this.connectionLabel = connectionLabel == null ? "" : connectionLabel;
        }

        @Override
        public String tabId()
        {
                return fmt("D#%s@%s(%s)", pathLabel, table.getName(), connectionLabel);
        }

        @Override
        public Node createPane(Tab tab)
        {
                TableDesignerPane pane = new TableDesignerPane(tab, session, driver, table);
                tab.setGraphic(Assets.use("struct1"));
                return pane;
        }
}
