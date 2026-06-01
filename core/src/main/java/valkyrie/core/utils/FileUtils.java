package valkyrie.core.utils;

import valkyrie.core.exception.CoreException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
public class FileUtils
{
        public static boolean isDeepEmptyDirectory(File file)
        {
                return isDeepEmptyDirectory(file.toPath());
        }

        public static boolean isDeepEmptyDirectory(Path path)
        {
                try (Stream<Path> stream = Files.walk(path)) {
                        return stream
                                .filter(p -> !Files.isDirectory(p))
                                .findFirst()
                                .isEmpty();
                } catch (IOException e) {
                        throw new CoreException(e);
                }
        }
}
