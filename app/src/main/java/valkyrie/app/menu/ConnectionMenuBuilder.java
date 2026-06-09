package valkyrie.app.menu;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import valkyrie.app.assets.Assets;
import valkyrie.app.dialog.connection.CreateOrEditConnectionDialog;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.driver.api.DbType;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class ConnectionMenuBuilder
{
        public static Menu buildMenu() {
                Menu newConnectionMenu = new Menu("新建连接");

                MenuItem mysqlItem = new MenuItem(DbType.mysql.getAlias());
                mysqlItem.setGraphic(Assets.use(DbType.mysql.getIcon()));
                mysqlItem.setOnAction(e -> openConnectionDialog(DbType.mysql));

                MenuItem postgresqlItem = new MenuItem(DbType.postgresql.getAlias());
                postgresqlItem.setGraphic(Assets.use(DbType.postgresql.getIcon()));
                postgresqlItem.setOnAction(e -> openConnectionDialog(DbType.postgresql));

                MenuItem sqliteItem = new MenuItem(DbType.sqlite.getAlias());
                sqliteItem.setGraphic(Assets.use(DbType.sqlite.getIcon()));
                sqliteItem.setOnAction(e -> openConnectionDialog(DbType.sqlite));

                MenuItem dmItem = new MenuItem(DbType.dm.getAlias());
                dmItem.setGraphic(Assets.use(DbType.dm.getIcon()));
                dmItem.setOnAction(e -> openConnectionDialog(DbType.dm));

                MenuItem redisItem = new MenuItem(DbType.redis.getAlias());
                redisItem.setGraphic(Assets.use(DbType.redis.getIcon()));
                redisItem.setOnAction(e -> openConnectionDialog(DbType.redis));

                newConnectionMenu.getItems().addAll(mysqlItem, postgresqlItem, sqliteItem, dmItem, redisItem);

                return newConnectionMenu;
        }

        public static VkContextMenu buildContextMenu() {
                VkContextMenu contextMenu = new VkContextMenu();
                contextMenu.getItems().addAll(buildMenu().getItems());
                return contextMenu;
        }

        @SuppressWarnings("unused")
        private static void openConnectionDialog(DbType dbType) {
                new CreateOrEditConnectionDialog(dbType).showAndWait();
        }
}
