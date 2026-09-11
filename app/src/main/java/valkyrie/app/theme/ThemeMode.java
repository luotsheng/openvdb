package valkyrie.app.theme;

/**
 * 界面主题模式
 *
 * @author Luo Tiansheng
 * @since 2026/9/11
 */
public enum ThemeMode
{
        SYSTEM("跟随系统"),
        LIGHT("浅色"),
        DARK("深色");

        private final String label;

        ThemeMode(String label)
        {
                this.label = label;
        }

        public String label()
        {
                return label;
        }

        public static ThemeMode from(String name)
        {
                if (name == null || name.isBlank())
                        return SYSTEM;

                for (ThemeMode mode : values()) {
                        if (mode.name().equalsIgnoreCase(name.trim()))
                                return mode;
                }

                return SYSTEM;
        }
}
