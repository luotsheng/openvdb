package valkyrie.app.menu;

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
                return buildMenu(true);
        }

        /**
         * @param withIcon 是否给菜单项设置数据库图标。macOS 原生菜单栏里图标会被放大，
         *                 顶部菜单应传 {@code false}。
         */
        public static Menu buildMenu(boolean withIcon) {
                Menu newConnectionMenu = new Menu("新建连接");

                for (DbType dbType : DbType.values()) {
                        MenuItem item = new MenuItem(dbType.getAlias());

                        if (withIcon)
                                item.setGraphic(Assets.use(dbType.getIcon()));

                        item.setOnAction(e -> openConnectionDialog(dbType));
                        newConnectionMenu.getItems().add(item);
                }

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
