package valkyrie.core;

import valkyrie.utils.io.UFile;

/**
 * 用户数据
 *
 * @author Luo Tiansheng
 * @since 2026/3/31
 */
public class Users
{
        public static final String userHome = System.getProperty("user.home");
        public static final String META_INF = ".META-INF";
        public static final UFile baseDir = new UFile(userHome, ".valkyries");
        public static final UFile connectionDir = new UFile(baseDir, "C");
}
