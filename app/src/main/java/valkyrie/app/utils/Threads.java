package valkyrie.app.utils;

import javafx.application.Platform;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class Threads
{
        public static void start(Runnable runnable)
        {
                new Thread(runnable).start();
        }

        public static void runLater(Runnable runnable)
        {
                new Thread(() -> Platform.runLater(runnable)).start();
        }
}
