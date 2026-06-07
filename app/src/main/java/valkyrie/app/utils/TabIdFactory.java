package valkyrie.app.utils;

import valkyrie.app.explorer.UIQueryDynamicNode;

import static valkyrie.utils.string.StaticLibrary.fmt;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
public class TabIdFactory
{
        private static int queryTabCount = 0;

        public static String buildQueryTabId(UIQueryDynamicNode queryDynamicNode)
        {
                if (queryDynamicNode == null)
                        return "Q#新建查询脚本_" + (queryTabCount++);

                return fmt("Q#%s@%s(%s)",
                        queryDynamicNode.getPathNode().getLabel(),
                        queryDynamicNode.getLabel(),
                        queryDynamicNode.getRoot().getLabel());
        }
}
