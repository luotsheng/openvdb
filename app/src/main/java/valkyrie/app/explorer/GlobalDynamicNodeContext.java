package valkyrie.app.explorer;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点全局上下文
 *
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
public class GlobalDynamicNodeContext
{
        private static @Getter UIExplorerNode selectedExplorerNode;

        private static final @Getter List<UIConnectionNode> connectionNodes = new ArrayList<>();

        public static synchronized void onSelectedEvent(UIExplorerNode explorerNode)
        {
                selectedExplorerNode = explorerNode;
        }
}
