package valkyrie.app.theme;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.core.Users;
import valkyrie.utils.io.UFile;
import valkyrie.utils.system.OS;

import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 主题管理：负责浅色/深色/跟随系统三种模式的切换与持久化。
 * <p>
 * 基础控件外观由 AtlantaFX 的 Cupertino 主题提供，通过替换
 * {@code userAgentStylesheet} 实现整体换肤；应用自身的 CSS 统一引用
 * {@code -color-*} 变量，从而自动适配明暗两种模式。
 *
 * @author Luo Tiansheng
 * @since 2026/9/11
 */
public final class ThemeManager
{
        private static final Logger LOG = LoggerFactory.getLogger(ThemeManager.class);

        private static final String KEY = "ui.theme";

        private static ThemeMode mode = ThemeMode.SYSTEM;
        private static final List<Runnable> listeners = new ArrayList<>();

        private ThemeManager()
        {
        }

        public static void initialize()
        {
                mode = loadMode();
                apply();
        }

        public static ThemeMode mode()
        {
                return mode;
        }

        public static void setMode(ThemeMode value)
        {
                if (value == null || value == mode)
                        return;

                mode = value;
                saveMode(value);
                apply();
        }

        public static void toggle()
        {
                setMode(isDark() ? ThemeMode.LIGHT : ThemeMode.DARK);
        }

        public static boolean isDark()
        {
                return switch (mode) {
                        case LIGHT -> false;
                        case DARK -> true;
                        case SYSTEM -> systemPrefersDark();
                };
        }

        public static void addListener(Runnable listener)
        {
                listeners.add(listener);
        }

        private static void apply()
        {
                String stylesheet = isDark()
                        ? new CupertinoDark().getUserAgentStylesheet()
                        : new CupertinoLight().getUserAgentStylesheet();

                javafx.application.Application.setUserAgentStylesheet(stylesheet);

                listeners.forEach(Runnable::run);
        }

        /**
         * macOS 下通过 {@code defaults read -g AppleInterfaceStyle} 读取系统是否处于深色模式。
         * 其他平台暂不支持自动探测，默认浅色。
         */
        private static boolean systemPrefersDark()
        {
                if (!OS.isMacOS())
                        return false;

                try {
                        Process process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                                .redirectErrorStream(true)
                                .start();

                        String output = new String(process.getInputStream().readAllBytes()).trim();
                        process.waitFor();

                        return "Dark".equalsIgnoreCase(output);
                } catch (Exception e) {
                        return false;
                }
        }

        private static UFile settingsFile()
        {
                return new UFile(Users.baseDir, "settings.properties");
        }

        private static ThemeMode loadMode()
        {
                UFile file = settingsFile();

                if (!file.exists())
                        return ThemeMode.SYSTEM;

                return ThemeMode.from(file.loadProperties().getProperty(KEY));
        }

        private static void saveMode(ThemeMode value)
        {
                UFile file = settingsFile();

                try {
                        UFile parent = file.getParentFile();
                        if (parent != null && !parent.exists())
                                parent.mkdirs();

                        Properties properties = file.exists() ? file.loadProperties() : new Properties();
                        properties.setProperty(KEY, value.name());

                        try (OutputStream out = Files.newOutputStream(file.toPath())) {
                                properties.store(out, "Valkyrie user settings");
                        }
                } catch (Exception e) {
                        /* 持久化失败不影响本次切换 */
                        LOG.warn("保存主题设置失败", e);
                }
        }
}
