package valkyrie.driver.api.node;

import lombok.Getter;
import valkyrie.driver.api.Session;

/**
 * @author Luo Tiansheng
 * @since 2026/6/5
 */
@Getter
public abstract class DBCatalogNode extends DBNode
{
        private final Session session;

        public DBCatalogNode(String catalog, DBMetadataProvider metadataProvider)
        {
                super(catalog, DBNodeKind.CATALOG, metadataProvider);
                this.session = Session.ofCatalog(catalog);
        }
}
