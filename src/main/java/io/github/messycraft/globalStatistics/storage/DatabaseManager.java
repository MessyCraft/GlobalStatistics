package io.github.messycraft.globalStatistics.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private HikariDataSource dataSource;

    public void initialize(FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        String host = config.getString("mysql.host", "127.0.0.1");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "global_statistics");
        String username = config.getString("mysql.username", "root");
        String password = config.getString("mysql.password", "password");
        String params = config.getString("mysql.params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + params;
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        hikariConfig.setPoolName(config.getString("hikari.pool-name", "GlobalStatisticsPool"));
        hikariConfig.setMinimumIdle(config.getInt("hikari.minimum-idle", 2));
        hikariConfig.setMaximumPoolSize(config.getInt("hikari.maximum-pool-size", 10));
        hikariConfig.setConnectionTimeout(config.getLong("hikari.connection-timeout-ms", 30000L));
        hikariConfig.setIdleTimeout(config.getLong("hikari.idle-timeout-ms", 600000L));
        hikariConfig.setMaxLifetime(config.getLong("hikari.max-lifetime-ms", 1800000L));

        dataSource = new HikariDataSource(hikariConfig);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void createTablesIfNeeded() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS gs_dataset ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "name VARCHAR(128) NOT NULL UNIQUE,"
                    + "created_at BIGINT NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.execute("CREATE TABLE IF NOT EXISTS gs_record ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "dataset_id BIGINT NOT NULL,"
                    + "content TEXT NOT NULL,"
                    + "ts_seconds BIGINT NOT NULL,"
                    + "CONSTRAINT fk_gs_record_dataset FOREIGN KEY (dataset_id) REFERENCES gs_dataset(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.execute("CREATE INDEX idx_gs_record_dataset_ts ON gs_record(dataset_id, ts_seconds)");
        } catch (SQLException indexCreateError) {
            if (!"42000".equals(indexCreateError.getSQLState()) && indexCreateError.getErrorCode() != 1061) {
                throw indexCreateError;
            }
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
