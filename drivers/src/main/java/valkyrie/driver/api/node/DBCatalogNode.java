package valkyrie.driver.api.node;

import valkyrie.driver.api.Catalog;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
public abstract class DBCatalogNode extends DBNode
{
        public DBCatalogNode(Catalog catalog, DBMetadataProvider metadataProvider)
        {
                super(catalog.getLabel(), DBNodeKind.CATALOG, metadataProvider);
        }
}
