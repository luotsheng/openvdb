package valkyrie.app.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import valkyrie.core.model.ConnectionProfile;
import valkyrie.driver.api.ConnectionConfig;
import valkyrie.driver.api.DbType;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static valkyrie.utils.TypeConverter.atobool;

/**
 * 连接属性
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
@SuppressWarnings("unused")
public class ConnectionPropertyModel
{
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty sqlitePath = new SimpleStringProperty();
        private final StringProperty host = new SimpleStringProperty();
        private final StringProperty port = new SimpleStringProperty();
        private final StringProperty db = new SimpleStringProperty();
        private final StringProperty username = new SimpleStringProperty();
        private final StringProperty password = new SimpleStringProperty();
        private final BooleanProperty savePassword = new SimpleBooleanProperty();
        private final StringProperty jdbcUrl = new SimpleStringProperty();
        private final StringProperty timezone = new SimpleStringProperty();
        private final BooleanProperty useSSL = new SimpleBooleanProperty(true);
        private final BooleanProperty tinyint1isBit = new SimpleBooleanProperty(false);

        /* jdbc url 属性 */
        private final Map<String, String> jdbcQuery = new HashMap<>();

        public ConnectionPropertyModel()
        {
                setupListener();
        }

        public ConnectionPropertyModel(ConnectionProfile profile)
        {
                this.name.set(profile.getName());
                this.type.set(profile.getType());
                this.host.set(profile.getHost());
                this.port.set(profile.getPort());
                this.db.set(profile.getDb());
                this.username.set(profile.getUsername());
                this.password.set(profile.getPassword());
                this.savePassword.set(profile.getSavePassword());
                this.jdbcUrl.set(profile.getJdbcUrl());
                this.timezone.set(profile.getTimezone());
                this.useSSL.set(profile.getUseSSL());
                this.tinyint1isBit.set(profile.getTinyint1isBit());

                setupListener();
        }

        private void setupListener()
        {
                host.addListener(event -> update());
                port.addListener(event -> update());
                timezone.addListener(event -> update());
                useSSL.addListener(event -> update());
                tinyint1isBit.addListener(event -> update());
                sqlitePath.addListener(event -> update());

                jdbcUrl.addListener(event -> parse());
        }

        private void updateQuery(StringBuilder builder)
        {
                jdbcQuery.forEach((k, v) -> {

                        builder.append(k)
                                .append("=")
                                .append(v)
                                .append("&");

                });

                builder.deleteCharAt(builder.length() - 1);
        }

        private void parseQuery(String uriQuery)
        {
                if (uriQuery != null) {

                        String[] queries = uriQuery.split("&");

                        for (String query : queries) {
                                String[] entry = query.split("=");
                                jdbcQuery.put(entry[0], entry[1]);
                        }

                }

                String jqUseSSL = jdbcQuery.get("useSSL");
                useSSL.set(atobool(jqUseSSL));

                String jqTimezone = jdbcQuery.get("timezone");
                timezone.set(jqTimezone);

                String jqTinyInt1isBit = jdbcQuery.get("tinyint1isBit");
                tinyint1isBit.set(atobool(jqTinyInt1isBit));
        }

        private void update()
        {
                if (getDbType() != DbType.sqlite) {
                        StringBuilder builder = new StringBuilder(
                                String.format("jdbc:%s://%s:%s?", type.get(), host.get(), port.get())
                        );

                        jdbcQuery.put("timezone", timezone.get());
                        jdbcQuery.put("useSSL", String.valueOf(useSSL.get()));
                        jdbcQuery.put("tinyint1isBit", String.valueOf(tinyint1isBit.get()));

                        updateQuery(builder);

                        jdbcUrl.setValue(builder.toString());
                } else {
                        jdbcUrl.setValue("jdbc:sqlite:" + sqlitePath.get());
                }
        }

        private void parse()
        {
                // jdbc:mysql://localhost:3306/mydatabase?useSSL=false&serverTimezone=UTC

                String withUrl = jdbcUrl.get().substring(5);

                try {

                        URI uri = URI.create(withUrl);

                        String[] authority = uri.getAuthority().split(":");

                        type.set(uri.getScheme());
                        host.set(authority[0]);
                        port.set(String.valueOf(authority[1]));

                        String path = uri.getPath();
                        if (path != null && !path.isEmpty())
                                db.set(path.substring(1));

                        parseQuery(uri.getQuery());

                } catch (Exception e) {

                        /* ignore exception */

                }

        }

        public static ConnectionPropertyModel createMySQL()
        {
                ConnectionPropertyModel model = new ConnectionPropertyModel();
                model.name.set("MySQL");
                model.type.set("mysql");
                model.host.set("127.0.0.1");
                model.port.set("3306");
                model.username.set("root");
                return model;
        }

        public static ConnectionPropertyModel createSQLite()
        {
                ConnectionPropertyModel model = new ConnectionPropertyModel();
                model.name.set("SQLite");
                model.type.set("sqlite");
                return model;
        }

        public static ConnectionPropertyModel createDM()
        {
                ConnectionPropertyModel model = new ConnectionPropertyModel();
                model.name.set("达梦数据库");
                model.type.set("dm");
                model.host.set("127.0.0.1");
                model.port.set("5236");
                model.username.set("SYSDBA");
                return model;
        }

        public static ConnectionPropertyModel createRedis()
        {
                ConnectionPropertyModel model = new ConnectionPropertyModel();
                model.name.set("Redis");
                model.type.set("redis");
                model.host.set("127.0.0.1");
                model.port.set("6379");
                model.username.set("");
                return model;
        }

        /* property */
        public StringProperty nameProperty() { return name; }
        public StringProperty typeProperty() { return type; }
        public StringProperty sqlitePathProperty() { return sqlitePath; }
        public StringProperty hostProperty() { return host; }
        public StringProperty portProperty() { return port; }
        public StringProperty dbProperty() { return db; }
        public StringProperty usernameProperty() { return username; }
        public StringProperty passwordProperty() { return password; }
        public BooleanProperty savePasswordProperty() { return savePassword; }
        public StringProperty jdbcUrlProperty() { return jdbcUrl; }
        public StringProperty timezoneProperty() { return timezone; }
        public BooleanProperty useSSLProperty() { return useSSL; }
        public BooleanProperty tinyint1isBitProperty() { return tinyint1isBit; }

        /* get */
        public String getName() { return name.get();  }
        public String getType() { return type.get();  }
        public String getSQLitePath() { return sqlitePath.get();  }
        public String getHost() { return host.get();  }
        public String getPort() { return port.get();  }
        public String getDb() { return db.get();  }
        public String getUsername() { return username.get();  }
        public String getPassword() { return password.get();  }
        public Boolean getSavePassword() { return savePassword.get();  }
        public String getJdbcUrl() { return jdbcUrl.get();  }
        public String getTimezone() { return timezone.get();  }
        public Boolean getUseSSL() { return useSSL.get();  }
        public Boolean getTinyint1isBit() { return tinyint1isBit.get();  }

        /* get */
        public void setName(String name) { this.name.set(name);  }
        public void setType(String type) { this.type.set(type);  }
        public void setSqlitePath(String path) { this.sqlitePath.set(path);  }
        public void setHost(String host) { this.host.set(host);  }
        public void setPort(String port) { this.port.set(port);  }
        public void setDb(String db) { this.db.set(db);  }
        public void setUsername(String username) { this.username.set(username);  }
        public void setPassword(String password) { this.password.set(password);  }
        public void setSavePassword(Boolean savePassword) { this.savePassword.set(savePassword);  }
        public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl.set(jdbcUrl);  }
        public void setTimezone(String timezone) { this.timezone.set(timezone);  }
        public void setUseSSL(Boolean useSSL) { this.useSSL.set(useSSL);  }
        public void setTinyint1isBit(Boolean tinyint1isBit) { this.tinyint1isBit.set(tinyint1isBit);  }

        public ConnectionConfig toConnectionConfig()
        {
                ConnectionConfig config = new ConnectionConfig();

                config.setType(DbType.of(getType()));
                config.setHost(getHost());
                config.setPort(getPort());
                config.setUsername(getUsername());
                config.setPassword(getPassword());
                config.setJdbcUrl(getJdbcUrl());

                return config;
        }

        public DbType getDbType()
        {
                return DbType.of(getType());
        }
}