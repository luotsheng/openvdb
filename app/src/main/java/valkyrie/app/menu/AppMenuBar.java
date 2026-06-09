package valkyrie.app.menu;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import valkyrie.utils.system.OS;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class AppMenuBar extends MenuBar
{
        public AppMenuBar()
        {
                if (OS.isMacOS()) {
                        setUseSystemMenuBar(true);
                        setMinHeight(0);
                        setMaxHeight(0);
                        setMouseTransparent(true);
                }

                // 文件菜单
                Menu fileMenu = new Menu("文件");

                MenuItem newScriptEditorItem = new MenuItem("新建查询");

                MenuItem importItem = new MenuItem("导入");
                MenuItem exportItem = new MenuItem("导出");

                MenuItem exitItem = new MenuItem("退出");
                fileMenu.getItems().addAll(
                        ConnectionMenuBuilder.buildMenu(),
                        newScriptEditorItem,
                        new SeparatorMenuItem(),
                        importItem,
                        exportItem,
                        new SeparatorMenuItem(),
                        exitItem);

                // 编辑菜单
                Menu editMenu = new Menu("编辑");
                MenuItem copyItem = new MenuItem("复制");
                MenuItem pasteItem = new MenuItem("粘贴");
                editMenu.getItems().addAll(copyItem, pasteItem);

                // 代码菜单
                Menu codeMenu = new Menu("代码");

                // 运行菜单
                Menu runMenu = new Menu("运行");

                // 帮助菜单
                Menu helpMenu = new Menu("帮助");
                MenuItem aboutItem = new MenuItem("关于");
                helpMenu.getItems().add(aboutItem);

                getMenus().addAll(
                        fileMenu,
                        editMenu,
                        codeMenu,
                        runMenu,
                        helpMenu
                );
        }
}
