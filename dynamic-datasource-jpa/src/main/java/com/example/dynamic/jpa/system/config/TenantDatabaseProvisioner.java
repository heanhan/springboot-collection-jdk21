package com.example.dynamic.jpa.system.config;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class TenantDatabaseProvisioner {

    private final TenantDataSourceProperties.Provisioner config;

    public TenantDatabaseProvisioner(TenantDataSourceProperties properties) {
        this.config = properties.getProvisioner();
    }

    public Connection connect() throws SQLException {
        if (config.getUsername() == null || config.getUsername().isBlank()
                || config.getPassword() == null || config.getPassword().isBlank()) {
            throw new IllegalStateException("租户数据库开通账号未配置");
        }
        return DriverManager.getConnection(serverUrl(), config.getUsername(), config.getPassword());
    }

    public String databaseUrl(String databaseName) {
        validateIdentifier(databaseName);
        return serverUrl(databaseName);
    }

    public void createDatabase(Connection connection, String databaseName) throws SQLException {
        validateIdentifier(databaseName);
        execute(connection, "CREATE DATABASE IF NOT EXISTS `" + databaseName
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
    }

    public void createRuntimeUser(Connection connection, String username, String password,
                                  String databaseName) throws SQLException {
        validateIdentifier(username);
        validateIdentifier(databaseName);
        if (!config.isCreateUser()) {
            return;
        }
        String account = quote(username) + "@" + quote(config.getUserHost());
        execute(connection, "CREATE USER IF NOT EXISTS " + account + " IDENTIFIED BY " + quote(password));
        execute(connection, "ALTER USER " + account + " IDENTIFIED BY " + quote(password));
        execute(connection, "GRANT ALL PRIVILEGES ON `" + databaseName + "`.* TO " + account);
    }

    public void dropDatabaseAndUser(Connection connection, String databaseName, String username) throws SQLException {
        validateIdentifier(databaseName);
        execute(connection, "DROP DATABASE IF EXISTS `" + databaseName + "`");
        if (config.isCreateUser() && username != null && !username.isBlank()) {
            validateIdentifier(username);
            execute(connection, "DROP USER IF EXISTS " + quote(username) + "@" + quote(config.getUserHost()));
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String serverUrl() {
        return serverUrl(null);
    }

    private String serverUrl(String databaseName) {
        return "jdbc:mysql://" + config.getHost() + ":" + config.getPort()
                + (databaseName == null ? "" : "/" + databaseName)
                + "?useUnicode=true&characterEncoding=utf8&connectTimeout=3000&socketTimeout=10000"
                + "&allowPublicKeyRetrieval=true&sslMode=" + config.getSslMode();
    }

    private void validateIdentifier(String value) {
        if (value == null || !value.matches("^[a-zA-Z0-9_]{1,64}$")) {
            throw new IllegalArgumentException("数据库标识符格式错误");
        }
    }

    private String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
