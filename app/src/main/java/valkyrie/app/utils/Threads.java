package valkyrie.app.utils;

import javafx.application.Platform;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public class Threads
{
        public static void start(Runnable action)
        {
                new Thread(action).start();
        }

        public static void runLater(Runnable action)
        {
                new Thread(() -> Platform.runLater(action)).start();
        }
}
