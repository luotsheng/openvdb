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
        private static @Getter UIExplorerNode selectedPathNode;

        private static final @Getter List<UIConnectionNode> connectionNodes = new ArrayList<>();

        public static synchronized void onSelectedEvent(UIExplorerNode explorerNode)
        {
                selectedPathNode = switch (explorerNode) {
                        case UIConnectionNode connectionNode -> connectionNode;
                        case UICatalogDynamicNode catalogDynamicNode -> catalogDynamicNode;
                        case UISchemaDynamicNode schemaDynamicNode -> schemaDynamicNode;
                        case UIQueryDynamicNode queryDynamicNode -> queryDynamicNode;
                        default -> selectedPathNode;
                };
        }
}
