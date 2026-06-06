package valkyrie.driver.api.node;

/**
 * @author Luo Tiansheng
 * @since 2026/6/6
 */
public record DBNodePath(DBNodeKind kind, DBNodePath child)
{
}
