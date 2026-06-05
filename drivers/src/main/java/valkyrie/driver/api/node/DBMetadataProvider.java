package valkyrie.driver.api.node;

import java.util.List;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public interface DBMetadataProvider
{
        List<DBNode> getChildrenOfCatalog(DBCatalogNode catalogNode);

        List<DBNode> getChildrenOfSchema(DBSchemaNode schemaNode);
}
