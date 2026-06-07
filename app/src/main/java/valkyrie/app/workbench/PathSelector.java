package valkyrie.app.workbench;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;
import valkyrie.app.event.CatalogDynamicNodeInitializedEvent;
import valkyrie.app.event.ConnectedSuccessEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.explorer.*;
import valkyrie.app.widgets.VkComboBox;
import valkyrie.driver.api.Driver;
import valkyrie.driver.api.Session;
import valkyrie.driver.api.node.DBNodeKind;
import valkyrie.driver.api.node.DBNodePath;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author Luo Tiansheng
 * @since 2026/6/7
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class PathSelector implements EventListener
{
        private @Getter Driver driver;
        private final @Getter Session session = new Session();
        private DBNodePath dbNodePath;

        private UIConnectionNode selectedConnectionNode = null;
        private UICatalogDynamicNode selectedCatalogDynamicNode = null;
        private UISchemaDynamicNode selectedSchemaDynamicNode = null;

        // ComboBox
        private final @Getter VkComboBox<UIConnectionNode> connectionComboBox = new VkComboBox<>();
        private final @Getter VkComboBox<UICatalogDynamicNode> catalogComboBox = new VkComboBox<>();
        private final @Getter VkComboBox<UISchemaDynamicNode> schemaComboBox = new VkComboBox<>();

        public interface PathSelectorUpdateListener {
                void onUpdate(Driver driver, Session session);
        }

        private PathSelectorUpdateListener onSelectorUpdateListener;

        public PathSelector()
        {
                this(null);
        }

        public PathSelector(PathSelectorUpdateListener onSelectorUpdateListener)
        {
                if (onSelectorUpdateListener != null)
                        this.onSelectorUpdateListener = onSelectorUpdateListener;

                setupComboBox();

                // subscribe
                EventBus.subscribe(ConnectedSuccessEvent.class, this);
                EventBus.subscribe(CatalogDynamicNodeInitializedEvent.class, this);
        }

        public UIConnectionNode getSelectedConnection()
        {
                return connectionComboBox.getSelectionModel().getSelectedItem();
        }

        public UICatalogDynamicNode getSelectedCatalog()
        {
                return catalogComboBox.getSelectionModel().getSelectedItem();
        }

        public UISchemaDynamicNode getSelectedSchema()
        {
                return schemaComboBox.getSelectionModel().getSelectedItem();
        }

        public void useSelector(PathSelector selector)
        {
                restoreItems(connectionComboBox, selector.connectionComboBox.getItems());
                connectionComboBox.getSelectionModel().select(selector.getSelectedConnection());

                restoreItems(catalogComboBox, selector.catalogComboBox.getItems());
                catalogComboBox.getSelectionModel().select(selector.getSelectedCatalog());

                restoreItems(schemaComboBox, selector.schemaComboBox.getItems());
                schemaComboBox.getSelectionModel().select(selector.getSelectedSchema());
        }

        private static <T> void restoreItems(VkComboBox<T> comboBox, Collection<T> collection)
        {
                comboBox.getItems().clear();
                comboBox.getItems().addAll(collection);
        }

        //////////////////////////////////////////////////////////////////////
        ///                            CALLBACK                            ///
        //////////////////////////////////////////////////////////////////////

        private void updateDriver(Driver driver)
        {
                this.driver = driver;

                if (onSelectorUpdateListener != null && driver != null)
                        onSelectorUpdateListener.onUpdate(driver, session);
        }

        private void updateSessionCatalog(String catalog)
        {
                updateSession(catalog, session.schema());
        }

        private void updateSessionSchema(String schema)
        {
                updateSession(session.catalog(), schema);
        }

        private void updateSession(String catalog, String schema)
        {
                this.session.setCatalog(catalog);
                this.session.setSchema(schema);

                if (onSelectorUpdateListener != null)
                        onSelectorUpdateListener.onUpdate(driver, session);
        }

        //////////////////////////////////////////////////////////////////////
        ///                        ON SELECTED EVENT                       ///
        //////////////////////////////////////////////////////////////////////

        private void onSelectedConnectionNode(UIConnectionNode connectionNode)
        {
                this.selectedConnectionNode = connectionNode;

                if (!connectionNode.isConnect()) {
                        connectionNode.connect();
                } else {
                        updateConnectionNodeComboBox(connectionNode);
                }
        }

        private void onSelectedCatalogDynamicNode(UICatalogDynamicNode catalogDynamicNode)
        {
                this.selectedCatalogDynamicNode = catalogDynamicNode;
                updateSessionCatalog(catalogDynamicNode.getLabel());

                if (!catalogDynamicNode.isInitialized()) {
                        catalogDynamicNode.initialize();
                } else {
                        updateSchemaDynamicNodeComboBox(catalogDynamicNode);
                }
        }

        private void onSelectedSchemaDynamicNode(UISchemaDynamicNode schemaDynamicNode)
        {
                this.selectedSchemaDynamicNode = schemaDynamicNode;
                updateSessionSchema(schemaDynamicNode.getLabel());

                if (!schemaDynamicNode.isInitialized())
                        schemaDynamicNode.initialize();
        }

        //////////////////////////////////////////////////////////////////////
        ///                       UPDATE COMBO BOX                         ///
        //////////////////////////////////////////////////////////////////////

        private static <Node extends UIExplorerNode> void configureComboBox(VkComboBox<Node> comboBox)
        {
                comboBox.setButtonCell(new ListCell<>()
                {
                        @Override
                        protected void updateItem(Node item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getLabel());
                                setGraphic(item.createGraphic());
                        }
                });

                comboBox.setCellFactory(list -> new ListCell<>()
                {
                        @Override
                        protected void updateItem(Node item, boolean empty)
                        {
                                super.updateItem(item, empty);

                                if (empty || item == null)
                                        return;

                                setText(item.getLabel());
                                setGraphic(item.createGraphic());
                        }
                });
        }

        private <T> void registerSelectionListener(
                ComboBox<T> comboBox,
                Consumer<T> consumer)
        {
                comboBox.getSelectionModel().selectedItemProperty()
                        .addListener((obs, oldVal, newVal) -> {
                                if (newVal != null)
                                        consumer.accept(newVal);
                        });
        }

        private void setupComboBox()
        {
                schemaComboBox.setHidden(true);

                configureComboBox(connectionComboBox);
                configureComboBox(catalogComboBox);
                configureComboBox(schemaComboBox);

                // connection
                registerSelectionListener(connectionComboBox, this::onSelectedConnectionNode);
                registerSelectionListener(catalogComboBox, this::onSelectedCatalogDynamicNode);
                registerSelectionListener(schemaComboBox, this::onSelectedSchemaDynamicNode);

                // 初始化 ComboBox 数据
                initializeFromCurrentSelectedNode();
        }

        private void initializeFromCurrentSelectedNode()
        {
                for (UIConnectionNode connectionNode : GlobalDynamicNodeContext.getConnectionNodes())
                        connectionComboBox.getItems().add(connectionNode);

                UIExplorerNode pathNode =
                        GlobalDynamicNodeContext.getSelectedPathNode();

                if (pathNode == null)
                        return;

                switch (pathNode) {
                        case UIConnectionNode connectionNode ->
                                restoreConnection(connectionNode);

                        case UICatalogDynamicNode catalogDynamicNode ->
                                restoreCatalog(catalogDynamicNode);

                        case UISchemaDynamicNode schemaDynamicNode ->
                                restoreSchema(schemaDynamicNode);

                        case UIDynamicNode dynamicNode
                                -> restoreDynamicNode(dynamicNode);

                        default ->
                                throw new UnsupportedOperationException("不支持的路径节点类型：" + pathNode);
                }
        }

        private void restoreConnection(UIConnectionNode connectionNode)
        {
                connectionComboBox.getSelectionModel().select(connectionNode);
                updateConnectionNodeComboBox(connectionNode);
        }

        private void restoreCatalog(UICatalogDynamicNode catalogDynamicNode)
        {
                UIConnectionNode connectionNode =
                        (UIConnectionNode) catalogDynamicNode.getParent();

                restoreConnection(connectionNode);

                catalogComboBox.getSelectionModel()
                        .select(catalogDynamicNode);

                updateSchemaDynamicNodeComboBox(catalogDynamicNode);
        }

        private void restoreSchema(UISchemaDynamicNode schemaDynamicNode)
        {
                if (schemaDynamicNode.getParent() instanceof UIConnectionNode connectionNode)
                        restoreConnection(connectionNode);

                if (schemaDynamicNode.getParent() instanceof UICatalogDynamicNode catalogDynamicNode) {
                        restoreCatalog(catalogDynamicNode);
                }

                if (!schemaDynamicNode.isInitialized())
                        schemaDynamicNode.initialize();

                schemaComboBox.getSelectionModel()
                        .select(schemaDynamicNode);
        }

        private void restoreDynamicNode(UIDynamicNode dynamicNode)
        {
                UIExplorerNode parent = dynamicNode.getPathNode();

                if (parent instanceof UISchemaDynamicNode schemaDynamicNode)
                        restoreSchema(schemaDynamicNode);

                if (parent instanceof UICatalogDynamicNode catalogDynamicNode)
                        restoreCatalog(catalogDynamicNode);
        }

        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        private void updateConnectionNodeComboBox(UIConnectionNode connectionNode)
        {
                updateDriver(connectionNode.getDriver());

                if (driver == null)
                        return;

                dbNodePath = driver.getNodeHierarchyPath();

                catalogComboBox.setHidden(true);
                schemaComboBox.setHidden(true);

                // parent
                switch (dbNodePath.kind()) {
                        case CATALOG -> {
                                catalogComboBox.setHidden(false);
                                setCatalogComboBoxItem(connectionNode);
                        }
                        case SCHEMA -> {
                                schemaComboBox.setHidden(false);
                                setSchemaComboBoxItem(connectionNode);
                        }
                        default ->
                                throw new UnsupportedOperationException("查询编辑器 Parent 不支持类型：" + dbNodePath.kind());
                }

                // child
                DBNodePath child = dbNodePath.child();
                if (child != null) {
                        switch (child.kind()) {
                                case SCHEMA -> schemaComboBox.setHidden(false);
                                default ->
                                        throw new UnsupportedOperationException("查询 Child 编辑器不支持类型：" + dbNodePath.kind());
                        }
                }
        }

        private void updateSchemaDynamicNodeComboBox(UICatalogDynamicNode catalogDynamicNode)
        {
                DBNodePath child = dbNodePath.child();
                if (child != null && child.kind() == DBNodeKind.SCHEMA)
                        setSchemaComboBoxItem(catalogDynamicNode);
        }

        private void setCatalogComboBoxItem(UIExplorerNode parentNode)
        {
                ObservableList<TreeItem<String>> children = parentNode.getChildren();

                catalogComboBox.getItems().clear();
                for (TreeItem<String> child : children) {
                        UICatalogDynamicNode catalogDynamicNode = (UICatalogDynamicNode) child;
                        catalogComboBox.getItems().add(catalogDynamicNode);
                }
        }

        private void setSchemaComboBoxItem(UIExplorerNode parentNode)
        {
                ObservableList<TreeItem<String>> children = parentNode.getChildren();

                schemaComboBox.getItems().clear();
                for (TreeItem<String> child : children) {
                        UISchemaDynamicNode schemaDynamicNode = (UISchemaDynamicNode) child;
                        schemaComboBox.getItems().add(schemaDynamicNode);
                }

        }

        //////////////////////////////////////////////////////////////////////
        ///                              EVENT                             ///
        //////////////////////////////////////////////////////////////////////

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof ConnectedSuccessEvent connectedSuccessEvent) {
                        if (connectedSuccessEvent.getConnectionNode() == selectedConnectionNode)
                                updateConnectionNodeComboBox(selectedConnectionNode);
                }

                if (event instanceof CatalogDynamicNodeInitializedEvent catalogDynamicNodeInitializedEvent) {
                        if (catalogDynamicNodeInitializedEvent.getDynamicNode() == selectedCatalogDynamicNode)
                                updateSchemaDynamicNodeComboBox(selectedCatalogDynamicNode);
                }
        }
}
