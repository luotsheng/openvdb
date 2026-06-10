package valkyrie.driver.api.node;

/**
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
public record DBNodePath(DBNodeKind kind, DBNodePath child)
{
        /**
         * 获取最后一个节点
         */
        public DBNodePath tail()
        {
                DBNodePath next = child;

                if (next == null)
                        return this;

                while (next.child != null)
                        next = next.child;

                return next;
        }
}
