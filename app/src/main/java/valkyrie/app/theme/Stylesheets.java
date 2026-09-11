package valkyrie.app.theme;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.utils.io.UFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static valkyrie.utils.string.StrStaticImports.strrstr;

/**
 * 应用样式表加载与统一挂载。
 * <p>
 * 目录中不带 {@code .dark.css} 后缀的样式始终生效；带该后缀的仅在深色模式下
 * 追加，用于覆盖需要区分明暗的视觉效果（例如右键菜单的毛玻璃底色）。
 * <p>
 * 所有场景（主窗口、弹窗、弹出菜单）都应调用 {@link #apply(Scene)}，避免出现
 * 主窗口有皮肤、弹窗/菜单却是默认外观的不一致问题。
 *
 * @author Luo Tiansheng
 * @since 2026/9/11
 */
public final class Stylesheets
{
        private static final Logger LOG = LoggerFactory.getLogger(Stylesheets.class);

        private static final String DARK_SUFFIX = ".dark.css";

        private static final List<String> BASE_CSS = load(false);
        private static final List<String> DARK_CSS = load(true);

        private Stylesheets()
        {
        }

        public static List<String> current()
        {
                List<String> stylesheets = new ArrayList<>(BASE_CSS);

                if (ThemeManager.isDark())
                        stylesheets.addAll(DARK_CSS);

                return stylesheets;
        }

        public static void apply(Scene scene)
        {
                if (scene == null)
                        return;

                scene.getStylesheets().setAll(current());
        }

        private static List<String> load(boolean dark)
        {
                List<String> stylesheets = new ArrayList<>();

                try {
                        URL url = Stylesheets.class.getClassLoader().getResource("css");
                        UFile cssDir = new UFile(Objects.requireNonNull(url).toURI());
                        UFile[] files = cssDir.listFiles();

                        if (files == null)
                                return stylesheets;

                        Arrays.sort(files, Comparator.comparing(UFile::getName));

                        for (UFile file : files) {
                                if (file.getName().endsWith(DARK_SUFFIX) != dark)
                                        continue;

                                String path = file.getPath();
                                path = path.substring(strrstr(path, "/", 2));

                                URL css = Stylesheets.class.getResource(path);
                                if (css != null)
                                        stylesheets.add(css.toExternalForm());
                        }
                } catch (Exception e) {
                        /* 样式加载失败时退化为默认外观，不影响启动 */
                        LOG.warn("加载应用样式表失败", e);
                }

                return stylesheets;
        }
}
