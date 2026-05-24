package valkyrie.app.platform;

import valkyrie.utils.Captor;
import valkyrie.utils.system.OS;

import java.awt.*;
import java.io.File;

/**
 * @author Luo Tiansheng
 * @since 2026/5/24
 */
public class Explorer
{
        public static void browseFileDirectory(File file)
        {
                Captor.call(() -> {
                        switch (OS.getos())
                        {
                                case MACOS -> {
                                        Desktop desktop = Desktop.getDesktop();
                                        desktop.browseFileDirectory(file);
                                }

                                case WINDOWS -> {
                                        Runtime.getRuntime().exec(new String[] {
                                                "explorer.exe",
                                                "/select," + file.getAbsolutePath()
                                        });
                                }

                                default -> throw new UnsupportedOperationException();
                        }
                });
        }
}
