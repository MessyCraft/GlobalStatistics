package io.github.messycraft.globalStatistics.api;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface GlobalStatisticsApi {

    CompletableFuture<Boolean> createDataset(String key);

    CompletableFuture<Boolean> deleteDataset(String key);

    CompletableFuture<File> exportDataset(String key);

    CompletableFuture<Map<String, File>> exportAllDatasets();

    CompletableFuture<Boolean> record(String key, String content);

    CompletableFuture<List<StatisticEntry>> queryAll(String key);

    CompletableFuture<List<StatisticEntry>> queryByRange(String key, long startInclusive, long endInclusive);
}
