package valkyrie.app.dialog.connection;

import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import valkyrie.app.event.RefreshConnectionEvent;
import valkyrie.app.event.bus.EventBus;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.core.repository.ConnectionRepository;
import valkyrie.core.utils.JSONUtils;
import valkyrie.driver.api.DbType;
import valkyrie.driver.api.DriverFactory;
import valkyrie.driver.api.VkDataSource;
import valkyrie.utils.exception.Causes;

/**
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class CreateOrEditConnectionDialog extends Stage
{
        private TabPane tabPane;
        private HBox buttonBar;
        private final boolean isUpdate;
        private final DbType dbType;
        private final ConnectionPropertyModel oldProperty;
        private final ConnectionPropertyModel newProperty;
        private final Label status = new Label();

        private static final int WW = 700;
        private static final int WH = 500;

        public CreateOrEditConnectionDialog(DbType dbType)
        {
                this(dbType, null);
        }

        public CreateOrEditConnectionDialog(ConnectionPropertyModel propertyModel)
        {
                this(propertyModel.getDbType(), propertyModel);
        }

        public CreateOrEditConnectionDialog(DbType dbType, ConnectionPropertyModel newProperty)
        {
                this.isUpdate = newProperty != null;
                this.dbType = dbType;

                this.newProperty = isUpdate ? newProperty : switch (dbType) {
                        case mysql -> ConnectionPropertyModel.createMySQL();
                        case postgresql -> ConnectionPropertyModel.createPostgresql();
                        case sqlite -> ConnectionPropertyModel.createSQLite();
                        case dm -> ConnectionPropertyModel.createDM();
                        case redis -> ConnectionPropertyModel.createRedis();
                };

                this.oldProperty = isUpdate
                        ? JSONUtils.deepCopy(newProperty)
                        : null;

                if (!isUpdate)
                        this.newProperty.setType(dbType.name());

                setupTabPane();
                setupButtonBar();
                setupScene();
        }

        private void setupTabPane()
        {
                tabPane = new TabPane();

                Tab generalTab = new Tab("常规属性");
                generalTab.setClosable(false);
                generalTab.setContent(new ConnectionGeneralPane(newProperty));
                tabPane.getTabs().add(generalTab);

                switch (dbType) {
                        case mysql, postgresql, sqlite, dm -> {
                                Tab advanceTab = new Tab("高级属性");
                                advanceTab.setClosable(false);
                                advanceTab.setContent(new ConnectionAdvancedPane(newProperty));
                                tabPane.getTabs().add(advanceTab);
                        }
                        default -> {}
                }
        }

        private void setupButtonBar()
        {
                Button test = new Button("测试连接");
                test.setOnAction(event -> testConnection());

                Button save = new Button("保存");
                save.setOnAction(event -> saveConnection());

                Button cancel = new Button("取消");
                cancel.setOnAction(event -> close());

                buttonBar = new HBox(10, test, cancel, save);
                buttonBar.setAlignment(Pos.CENTER_RIGHT);
                buttonBar.setPadding(new Insets(10, 10, 10, 10));
        }

        private void setupScene()
        {
                setTitle(isUpdate ? "编辑连接" : "新增连接");

                HBox statusBar = new HBox(status);
                statusBar.setPadding(new Insets(20, 0, 0, 20));
                statusBar.setAlignment(Pos.BOTTOM_LEFT);

                VBox vbox = new VBox(10, tabPane, statusBar, buttonBar);
                VBox.setVgrow(tabPane, Priority.ALWAYS);

                Scene scene = new Scene(vbox, WW, WH);
                setScene(scene);
        }

        @SuppressWarnings({
                "unused"
        })
        public void testConnection()
        {
                var config = newProperty.toConnectionConfig();
                try (VkDataSource ds = DriverFactory.createDataSource(config)) {
                        status.setText("Connected successfully...");
                        status.setStyle("-fx-text-fill: #28a745;");
                } catch (Exception e) {
                        status.setText(Causes.message(e));
                        status.setStyle("-fx-text-fill: #b8312b;");
                }
        }

        private void saveConnection()
        {
                String content = JSONUtils.toJSONString(newProperty, SerializationFeature.INDENT_OUTPUT);

                if (isUpdate) {
                        ConnectionRepository.updateConnection(oldProperty.getName(), newProperty.getName(), content);
                } else {
                        ConnectionRepository.saveConnection(newProperty.getName(), content);
                }

                close();

                EventBus.publish(new RefreshConnectionEvent());
        }
}
