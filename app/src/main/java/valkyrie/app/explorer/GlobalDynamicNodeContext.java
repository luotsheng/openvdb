package valkyrie.app.explorer;

import lombok.Getter;

/**
 * 节点全局上下文
 *
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
public class GlobalDynamicNodeContext
{
        private static @Getter UIExplorerNode selectedExplorerNode;

        public static synchronized void onSelectedEvent(UIExplorerNode explorerNode)
        {
                selectedExplorerNode = explorerNode;
        }
}
