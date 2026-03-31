package io.github.messycraft.globalStatistics.service;

import io.github.messycraft.globalStatistics.api.StatisticEntry;
import io.github.messycraft.globalStatistics.storage.RecordRow;
import io.github.messycraft.globalStatistics.storage.StatisticsRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatisticsService {

    private final StatisticsRepository repository;
    private final File dataFolder;

    public StatisticsService(StatisticsRepository repository, File dataFolder) {
        this.repository = repository;
        this.dataFolder = dataFolder;
    }

    public boolean createDataset(String key) throws SQLException {
        return repository.createDataset(key);
    }

    public boolean deleteDataset(String key) throws SQLException {
        return repository.deleteDataset(key);
    }

    public List<String> listDatasets() throws SQLException {
        return repository.listDatasets();
    }

    public boolean record(String key, String content) throws SQLException {
        return repository.record(key, content, System.currentTimeMillis() / 1000L);
    }

    public List<StatisticEntry> queryAll(String key) throws SQLException {
        return toEntries(repository.queryAll(key));
    }

    public List<StatisticEntry> queryByDate(String key, int year, int month, int day) throws SQLException {
        LocalDate date = LocalDate.of(year, month, day);
        ZoneId zoneId = ZoneId.systemDefault();
        long start = date.atStartOfDay(zoneId).toEpochSecond();
        long end = date.plusDays(1).atStartOfDay(zoneId).toEpochSecond() - 1;
        return queryByRange(key, start, end);
    }

    public List<StatisticEntry> queryByRange(String key, long startInclusive, long endInclusive) throws SQLException {
        return toEntries(repository.queryByRange(key, startInclusive, endInclusive));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean datasetExists(String key) throws SQLException {
        return repository.datasetExists(key);
    }

    public File exportDataset(String key) throws Exception {
        if (!datasetExists(key)) {
            return null;
        }

        List<StatisticEntry> entries = queryAll(key);
        File targetDir = new File(new File(dataFolder, "export"), key);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IllegalStateException("Cannot create export directory: " + targetDir.getAbsolutePath());
        }

        long timestamp = System.currentTimeMillis() / 1000L;
        File output = new File(targetDir, timestamp + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(output), StandardCharsets.UTF_8))) {
            for (StatisticEntry entry : entries) {
                writer.write(csvField(String.valueOf(entry.timestamp())) + "," + csvField(entry.content()));
                writer.newLine();
            }
        }
        return output;
    }

    public Map<String, File> exportAllDatasets() throws Exception {
        Map<String, File> files = new HashMap<>();
        for (String dataset : listDatasets()) {
            files.put(dataset, exportDataset(dataset));
        }
        return files;
    }

    private List<StatisticEntry> toEntries(List<RecordRow> rows) {
        List<StatisticEntry> result = new ArrayList<>();
        for (RecordRow row : rows) {
            result.add(new StatisticEntry(row.timestamp(), row.content()));
        }
        return result;
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
