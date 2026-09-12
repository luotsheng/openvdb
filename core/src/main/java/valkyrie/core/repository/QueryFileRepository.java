package valkyrie.core.repository;

import valkyrie.core.Users;
import valkyrie.core.exception.CoreException;
import valkyrie.core.model.QueryFile;

import java.io.File;
import java.io.FileWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 脚本数据
 *
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class QueryFileRepository
{
        public static QueryFile write(String path, String content)
        {
                return write(getFile(path), content);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static QueryFile write(File file, String content)
        {
                try {
                        if (!file.exists()) {
                                file.getParentFile().mkdirs();
                                file.createNewFile();
                        }

                        try (FileWriter fw = new FileWriter(file)) {
                                fw.write(content);
                        }
                } catch (Exception e) {
                        throw new CoreException(e);
                }

                return new QueryFile(file);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static QueryFile rename(QueryFile src, String name)
        {
                var newFile = new File(src.getParentFile(), name);
                src.renameTo(newFile);
                return new QueryFile(newFile);
        }

        public static List<QueryFile> loadScriptFiles(String basePath)
        {
                List<QueryFile> models = new ArrayList<>();

                File sqlDir = getFile(basePath);

                File[] files = sqlDir.listFiles();
                if (files == null)
                        return models;

                for (File file : files)
                        models.add(new QueryFile(file));

                Collator collator = Collator.getInstance(Locale.CHINA);
                models.sort(Comparator.comparing(QueryFile::getName, collator));

                return models;
        }

        /**
         * 列出某个连接下所有数据库目录里的脚本（脚本对象页用）。
         * 目录结构：{@code <连接>/<数据库>/<脚本>.sql}
         */
        public static List<QueryFile> loadConnectionScripts(String connection)
        {
                List<QueryFile> models = new ArrayList<>();

                File[] catalogs = getFile(connection).listFiles();

                if (catalogs == null)
                        return models;

                for (File catalog : catalogs) {
                        File[] files = catalog.listFiles();

                        if (files == null)
                                continue;

                        for (File file : files) {
                                if (file.isFile())
                                        models.add(new QueryFile(file));
                        }
                }

                Collator collator = Collator.getInstance(Locale.CHINA);
                models.sort(Comparator.comparing(QueryFile::getName, collator));

                return models;
        }

        @SuppressWarnings("SameParameterValue")
        public static QueryFile getFile(String basePath)
        {
                return new QueryFile(Users.connectionDir + "/" + basePath);
        }

        public static boolean exists(String path)
        {
                return getFile(path).exists();
        }

        public static void removeIfExists(String path)
        {
                if (exists(path))
                        getFile(path).forceDelete();
        }
}
