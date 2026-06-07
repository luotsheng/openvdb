package valkyrie.core.model;

import valkyrie.utils.io.UFile;

import java.io.File;
import java.net.URI;

/**
 * 查询脚本信息
 *
 * @author Luo Tiansheng
 * @since 2026/3/31
 */
public class QueryFile extends UFile
{
        public QueryFile(File file)
        {
                this(file.getAbsolutePath());
        }

        public QueryFile(String pathname)
        {
                super(pathname);
        }

        public QueryFile(String parent, String child)
        {
                super(parent, child);
        }

        public QueryFile(File parent, String child)
        {
                super(parent, child);
        }

        public QueryFile(URI uri)
        {
                super(uri);
        }
}
