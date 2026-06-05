package valkyrie.driver.api.node;

import lombok.Getter;
import valkyrie.driver.api.Catalog;
import valkyrie.driver.api.Session;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public abstract class DBCatalogNode extends DBNode
{
        private final @Getter Session session;

        public DBCatalogNode(Catalog catalog, DBMetadataProvider metadataProvider)
        {
                super(catalog.getLabel(), DBNodeKind.CATALOG, metadataProvider);
                this.session = Session.ofCatalog(catalog.getName());
        }
}
