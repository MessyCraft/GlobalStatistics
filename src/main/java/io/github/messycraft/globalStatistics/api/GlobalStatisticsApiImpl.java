package io.github.messycraft.globalStatistics.api;

import io.github.messycraft.globalStatistics.service.StatisticsService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class GlobalStatisticsApiImpl implements GlobalStatisticsApi {

    private final StatisticsService service;

    public GlobalStatisticsApiImpl(StatisticsService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<Boolean> createDataset(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.createDataset(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteDataset(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.deleteDataset(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<File> exportDataset(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.exportDataset(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, File>> exportAllDatasets() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.exportAllDatasets();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> record(String key, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.record(key, content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<StatisticEntry>> queryAll(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.queryAll(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<StatisticEntry>> queryByRange(String key, long startInclusive, long endInclusive) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.queryByRange(key, startInclusive, endInclusive);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
