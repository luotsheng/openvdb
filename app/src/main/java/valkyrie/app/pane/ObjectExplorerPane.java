package valkyrie.app.pane;

import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import valkyrie.app.assets.Assets;
import valkyrie.app.event.RefreshConnectionEvent;
import valkyrie.app.event.bus.Event;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.event.bus.EventListener;
import valkyrie.app.explorer.GlobalDynamicNodeContext;
import valkyrie.app.explorer.UIConnectionNode;
import valkyrie.app.explorer.UIExplorerNode;
import valkyrie.app.menu.ConnectionMenuBuilder;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.app.widgets.VkContextMenu;
import valkyrie.app.widgets.VkTextField;
import valkyrie.core.model.DiskSavedConnection;
import valkyrie.core.repository.ConnectionRepository;

import java.text.Collator;
import java.util.*;

import static valkyrie.utils.string.StrStaticImports.lowercase;

/**
 * 导航面板
 *
 * @author Luo Tiansheng
 * @since 2026/3/25
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class ObjectExplorerPane extends VBox implements EventListener
{
        private final TabPane tabPane;
        private final TreeView<String> treeView;
        private final VkContextMenu rootContextMenu;

        private final TreeItem<String> root = new TreeItem<>("我的连接", Assets.use("chain"));
        private final VkTextField search = new VkTextField();
        private final PauseTransition searchDelay = new PauseTransition(Duration.millis(100));
        private final Map<String, UIConnectionNode> connections = new HashMap<>();
        private final Map<TreeItem<?>, UIExplorerNode> filterSource = new HashMap<>();

        public ObjectExplorerPane()
        {
                this.tabPane = createTabPane();
                this.treeView = createTreeView();
                this.rootContextMenu = createRootContextMenu();

                EventBus.subscribe(this, RefreshConnectionEvent.class);

                setupContextMenu();
                setupSearchField();
                setupMouseClickListener();
                setupTreeNodeSelectedEvent();
                initializeLayout();
                refreshConnectionNode();

                treeView.getRoot().setExpanded(true);
        }

        @Override
        public void onEvent(Event event)
        {
                if (event instanceof RefreshConnectionEvent)
                        refreshConnectionNode();
        }

        private TabPane createTabPane()
        {
                TabPane tabPane = new TabPane();

                Tab navTab = new Tab("连接管理");
                navTab.setGraphic(Assets.use("navigation"));
                navTab.setClosable(false);

                tabPane.getTabs().addAll(navTab);

                return tabPane;
        }

        private void setupSearchField()
        {
                search.setPromptText("搜索...");

                search.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyEvent);

                searchDelay.setOnFinished(event -> applySearch());

                search.textProperty().addListener((observable, oldVal, newVal) -> searchDelay.playFromStart());
        }

        private void onKeyEvent(KeyEvent e)
        {
                if (e.isAltDown() && e.getCode() == KeyCode.BACK_SPACE)
                        search.clear();
        }

        /**
         * 应用当前搜索条件：命中节点按名称做“忽略大小写的子串匹配”（不做正则解析），
         * 过滤树中的节点为原节点的映射副本，保证结果仍可选择、双击打开与弹出右键菜单。
         */
        private void applySearch()
        {
                String text = search.getText();

                filterSource.clear();

                if (text == null || text.isBlank()) {
                        root.setExpanded(true);
                        treeView.setRoot(root);
                        return;
                }

                String keyword = lowercase(text).trim();

                TreeItem<String> filteredRoot = filterTree(root, keyword);

                filteredRoot.setExpanded(true);
                treeView.setRoot(filteredRoot);
        }

        private TreeItem<String> filterTree(TreeItem<String> parent, String keyword)
        {
                TreeItem<String> result = new TreeItem<>(parent.getValue(), parent.getGraphic());

                for (TreeItem<String> child : parent.getChildren()) {
                        TreeItem<String> filteredChild = filterTree(child, keyword);

                        boolean matched = matchLabel(child, keyword);

                        if (matched || !filteredChild.getChildren().isEmpty()) {
                                if (child instanceof UIExplorerNode explorerNode)
                                        filterSource.put(filteredChild, explorerNode);

                                result.setExpanded(true);
                                result.getChildren().add(filteredChild);
                        }
                }

                return result;
        }

        private static boolean matchLabel(TreeItem<String> item, String keyword)
        {
                String value = item.getValue();

                return value != null && lowercase(value).contains(keyword);
        }

        /**
         * 过滤树里的节点是普通副本，点击/双击/右键时先还原为真实的 UIExplorerNode 再派发。
         */
        private UIExplorerNode unwrap(TreeItem<?> item)
        {
                if (item instanceof UIExplorerNode explorerNode)
                        return explorerNode;

                return filterSource.get(item);
        }

        private TreeView<String> createTreeView()
        {
                TreeView<String> treeView = new TreeView<>(root);
                treeView.setShowRoot(true);

                return treeView;
        }

        private VkContextMenu createRootContextMenu()
        {
                VkContextMenu rootContextMenu = new VkContextMenu();

                Menu newConnectionMenu = ConnectionMenuBuilder.buildMenu();
                MenuItem refreshAllItem = new MenuItem("刷新连接");

                rootContextMenu.getItems().addAll(
                        newConnectionMenu,
                        new SeparatorMenuItem(),
                        refreshAllItem);

                /* 设置事件 */
                refreshAllItem.setOnAction(event -> refreshConnectionNode());

                return rootContextMenu;
        }

        private void setupContextMenu()
        {
                treeView.setOnContextMenuRequested(event -> {
                        Node node = event.getPickResult().getIntersectedNode();

                        double x = event.getScreenX();
                        double y = event.getScreenY();

                        while (node != null && !(node instanceof TreeCell<?>))
                                node = node.getParent();

                        if (node instanceof TreeCell<?> cell) {
                                TreeItem<?> item = cell.getTreeItem();

                                if (item == null)
                                        return;

                                if (item == treeView.getRoot()) {
                                        rootContextMenu.show(x, y);
                                        return;
                                }

                                UIExplorerNode explorerNode = unwrap(item);

                                if (explorerNode != null)
                                        explorerNode.showContextMenu(x, y);
                        }
                });
        }

        private void setupTreeNodeSelectedEvent()
        {
                treeView.getSelectionModel().selectedIndexProperty()
                        .addListener((observable, oldVal, newVal) -> {
                                TreeItem<String> treeItem = treeView.getTreeItem(newVal.intValue());

                                UIExplorerNode node = unwrap(treeItem);

                                if (node != null) {
                                        node.onSelectedEvent(node);
                                        GlobalDynamicNodeContext.onSelectedEvent(node);
                                }
                        });
        }

        private void setupMouseClickListener()
        {
                treeView.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                                Node node = event.getPickResult().getIntersectedNode();

                                while (node != null && !(node instanceof TreeCell<?>))
                                        node = node.getParent();

                                if (node instanceof TreeCell<?> cell) {
                                        TreeItem<?> item = cell.getTreeItem();

                                        UIExplorerNode vdbNode = unwrap(item);

                                        if (vdbNode == null)
                                                return;

                                        vdbNode.onMouseDoubleClickEvent();
                                }

                                event.consume();
                        }
                });
        }

        private void initializeLayout()
        {
                VBox vbox = new VBox(search, treeView);
                vbox.setSpacing(2);

                tabPane.getTabs().getFirst().setContent(vbox);
                VBox.setVgrow(treeView, Priority.ALWAYS);

                getChildren().add(tabPane);
                setVgrow(tabPane, Priority.ALWAYS);
        }

        private void refreshConnectionNode()
        {
                List<UIConnectionNode> removeList = new ArrayList<>();
                List<DiskSavedConnection> profiles = ConnectionRepository.loadConnections();

                connections.forEach((k, v) -> {
                        boolean isMatch = profiles.stream()
                                .anyMatch(e -> e.getName().equals(k));

                        if (!isMatch)
                                removeList.add(v);
                });

                ObservableList<TreeItem<String>> children = root.getChildren();

                if (!removeList.isEmpty()) {
                        for (UIConnectionNode connection : removeList) {
                                children.remove(connection);
                                connections.remove(connection.getLabel());
                        }
                }

                for (DiskSavedConnection profile : profiles) {
                        if (connections.containsKey(profile.getName()))
                                continue;

                        ConnectionPropertyModel propertyModel = new ConnectionPropertyModel(profile);

                        UIConnectionNode connection = new UIConnectionNode(treeView, propertyModel);
                        connections.put(profile.getName(), connection);
                        children.add(connection);

                        connection.setDeleteRequestListener(children::remove);
                }

                Collator collator = Collator.getInstance(Locale.CHINA);
                children.sort(Comparator.comparing(
                        node -> ((UIConnectionNode) node).getLabel(), collator));

                applySearch();
        }

}
