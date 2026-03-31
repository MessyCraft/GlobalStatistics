package io.github.messycraft.globalStatistics.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

@SuppressWarnings("SqlNoDataSourceInspection")
public final class StatisticsRepository {

    private final DataSource dataSource;

    public StatisticsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean createDataset(String name) throws SQLException {
        if (datasetExists(name)) {
            return false;
        }

        String sql = "INSERT INTO gs_dataset(name, created_at) VALUES(?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setLong(2, System.currentTimeMillis() / 1000L);
            statement.executeUpdate();
            return true;
        }
    }

    public boolean deleteDataset(String name) throws SQLException {
        String sql = "DELETE FROM gs_dataset WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            return statement.executeUpdate() > 0;
        }
    }

    public List<String> listDatasets() throws SQLException {
        String sql = "SELECT name FROM gs_dataset ORDER BY name ASC";
        List<String> names = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(resultSet.getString("name"));
            }
        }
        return names;
    }

    public boolean record(String datasetName, String content, long timestampSeconds) throws SQLException {
        OptionalLong datasetId = findDatasetId(datasetName);
        if (datasetId.isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO gs_record(dataset_id, content, ts_seconds) VALUES(?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, datasetId.getAsLong());
            statement.setString(2, content);
            statement.setLong(3, timestampSeconds);
            statement.executeUpdate();
            return true;
        }
    }

    public List<RecordRow> queryAll(String datasetName) throws SQLException {
        OptionalLong datasetId = findDatasetId(datasetName);
        if (datasetId.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT ts_seconds, content FROM gs_record WHERE dataset_id = ? ORDER BY ts_seconds ASC, id ASC";
        return queryBySql(datasetId.getAsLong(), sql, Long.MIN_VALUE, Long.MAX_VALUE, false);
    }

    public List<RecordRow> queryByRange(String datasetName, long startInclusive, long endInclusive) throws SQLException {
        OptionalLong datasetId = findDatasetId(datasetName);
        if (datasetId.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT ts_seconds, content FROM gs_record WHERE dataset_id = ? AND ts_seconds >= ? AND ts_seconds <= ? ORDER BY ts_seconds ASC, id ASC";
        return queryBySql(datasetId.getAsLong(), sql, startInclusive, endInclusive, true);
    }

    public boolean datasetExists(String datasetName) throws SQLException {
        return findDatasetId(datasetName).isPresent();
    }

    private OptionalLong findDatasetId(String datasetName) throws SQLException {
        String sql = "SELECT id FROM gs_dataset WHERE name = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, datasetName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return OptionalLong.of(resultSet.getLong("id"));
                }
            }
        }
        return OptionalLong.empty();
    }

    private List<RecordRow> queryBySql(long datasetId, String sql, long startInclusive, long endInclusive, boolean withRange) throws SQLException {
        List<RecordRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, datasetId);
            if (withRange) {
                statement.setLong(2, startInclusive);
                statement.setLong(3, endInclusive);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new RecordRow(
                            resultSet.getLong("ts_seconds"),
                            resultSet.getString("content")
                    ));
                }
            }
        }
        return rows;
    }
}
