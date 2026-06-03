package valkyrie.app.dialog.connection;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import valkyrie.app.model.ConnectionPropertyModel;
import valkyrie.app.pane.PropertyGridPane;
import valkyrie.app.widgets.VkComboBox;
import valkyrie.driver.api.DbType;

/**
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
class ConnectionAdvancedPane extends PropertyGridPane
{
        private final ConnectionPropertyModel propertyModel;

        private final TextField jdbcUrl = new TextField("jdbc:mysql://");
        private final VkComboBox<String> timezone = new VkComboBox<>();
        private final CheckBox useSSL = new CheckBox("使用 SSL");
        private final CheckBox tinyint1isBit = new CheckBox("TINYINT 转布尔类型");

        private static final String[] TIMEZONES = new String[]{
                "UTC",
                "Asia/Shanghai",
                "Asia/Hong_Kong",
                "Asia/Singapore",
                "Asia/Seoul",
                "Asia/Bangkok",
                "Asia/Dubai",
                "Europe/London",
                "Europe/Berlin",
                "Europe/Paris",
                "America/New_York",
                "America/Los_Angeles",
                "America/Chicago",
                "Australia/Sydney",
                "Asia/Tokyo",
        };

        public ConnectionAdvancedPane(ConnectionPropertyModel propertyModel)
        {
                super();
                this.propertyModel = propertyModel;
                bindModelProperty();
                setupTimezone();
                setupPaneLayout();
        }

        private void bindModelProperty()
        {
                jdbcUrl.textProperty().bindBidirectional(propertyModel.jdbcUrlProperty());
                timezone.valueProperty().bindBidirectional(propertyModel.timezoneProperty());
                useSSL.selectedProperty().bindBidirectional(propertyModel.useSSLProperty());
                tinyint1isBit.selectedProperty().bindBidirectional(propertyModel.tinyint1isBitProperty());
        }

        private void setupTimezone()
        {
                timezone.setMaxWidth(Double.MAX_VALUE);
                timezone.getItems().addAll(TIMEZONES);
                timezone.getSelectionModel().select(1);
        }

        private void setupPaneLayout()
        {
                addRow("JDBC URL：", jdbcUrl);

                if (propertyModel.getDbType() != DbType.sqlite) {
                        addRow("时区：", timezone);
                        addRow(null, useSSL);
                        addRow(null, tinyint1isBit);
                }
        }
}
