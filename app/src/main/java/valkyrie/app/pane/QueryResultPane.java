package valkyrie.app.pane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import valkyrie.app.assets.Assets;
import valkyrie.app.widgets.dialog.VkDialogHelper;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.QueryResult;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.Table;

/**
 * 打开表数据展示面板
 *
 * @author Luo Tiansheng
 * @since 2026/3/30
 */
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class QueryResultPane extends BorderPane
{
        private int start = 0;
        private int size = 1024;
        private Node oldGraphic;

        private final Tab owner;
        private final Driver driver;
        private final Session session;
        private final Table table;
        private final ResultViewPane resultViewPane;

        public QueryResultPane(Tab owner, Session session, Driver driver, Table table)
        {
                this.owner = owner;
                this.session = session;
                this.driver = driver;
                this.table = table;
                this.resultViewPane = new ResultViewPane(table.getName(), owner, true);

                resultViewPane.setReloadProgressListener(new ResultViewPane.ReloadProgressListener()
                {
                        @Override
                        public void start()
                        {
                                setLoadingIndicator();
                        }

                        @Override
                        public void end()
                        {
                                removeLoadingIndicator();
                        }
                });

                setCenter(resultViewPane);
        }

        private void setLoadingIndicator()
        {
                oldGraphic = owner.getGraphic();
                owner.setGraphic(Assets.newProgressIndicator());
        }

        private void removeLoadingIndicator()
        {
                owner.setGraphic(oldGraphic);
        }

        public void asyncUpdate()
        {
                setLoadingIndicator();

                new Thread(() -> {
                        try {
                                QueryResult rs = driver.selectByPage(session, table.getName(), start, size);
                                rs.setAddable(true);
                                Platform.runLater(() -> resultViewPane.reload(table.getName(), rs));
                        } catch (Exception e) {
                                VkDialogHelper.alert(e);
                        } finally {
                                Platform.runLater(this::removeLoadingIndicator);
                        }
                }).start();
        }
}
