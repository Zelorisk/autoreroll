package com.villagerreroller.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.villagerreroller.VillagerReroller;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public class StatisticsTracker {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private final Path statsFile;

    private Statistics allTimeStats;
    private Statistics sessionStats;

    public StatisticsTracker() {
        Path configDir = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(VillagerReroller.MOD_ID);
        this.statsFile = configDir.resolve("statistics.json");
        this.sessionStats = new Statistics();
        loadStatistics();
    }

    private void loadStatistics() {
        File file = statsFile.toFile();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                allTimeStats = GSON.fromJson(reader, Statistics.class);
                VillagerReroller.LOGGER.info(
                    "Loaded statistics from {}",
                    statsFile
                );
            } catch (IOException e) {
                VillagerReroller.LOGGER.error("Failed to load statistics", e);
                allTimeStats = new Statistics();
            }
        } else {
            allTimeStats = new Statistics();
        }
    }

    public void saveStatistics() {
        try (FileWriter writer = new FileWriter(statsFile.toFile())) {
            GSON.toJson(allTimeStats, writer);
            VillagerReroller.LOGGER.debug("Saved statistics to {}", statsFile);
        } catch (IOException e) {
            VillagerReroller.LOGGER.error("Failed to save statistics", e);
        }
    }

    public void recordSuccessfulReroll(int attempts) {
        sessionStats.totalRerolls++;
        sessionStats.successfulRerolls++;
        sessionStats.totalAttempts += attempts;
        sessionStats.recordBestTrade(attempts);

        allTimeStats.totalRerolls++;
        allTimeStats.successfulRerolls++;
        allTimeStats.totalAttempts += attempts;
        allTimeStats.recordBestTrade(attempts);

        saveStatistics();

        VillagerReroller.LOGGER.info(
            "Recorded successful reroll after {} attempts",
            attempts
        );
    }

    public void recordFailedReroll(int attempts) {
        sessionStats.totalRerolls++;
        sessionStats.failedRerolls++;
        sessionStats.totalAttempts += attempts;

        allTimeStats.totalRerolls++;
        allTimeStats.failedRerolls++;
        allTimeStats.totalAttempts += attempts;

        saveStatistics();

        VillagerReroller.LOGGER.info(
            "Recorded failed reroll after {} attempts",
            attempts
        );
    }

    public void recordEmeraldsSaved(int emeralds) {
        sessionStats.emeraldsSaved += emeralds;
        allTimeStats.emeraldsSaved += emeralds;
        saveStatistics();
    }

    public void resetSessionStats() {
        sessionStats = new Statistics();
        VillagerReroller.LOGGER.info("Reset session statistics");
    }

    public int getSessionRerolls() {
        return sessionStats.totalRerolls;
    }

    public int getSessionSuccesses() {
        return sessionStats.successfulRerolls;
    }

    public int getSessionFailures() {
        return sessionStats.failedRerolls;
    }

    public double getSessionAverageAttempts() {
        if (sessionStats.totalRerolls == 0) {
            return 0;
        }
        return (double) sessionStats.totalAttempts / sessionStats.totalRerolls;
    }

    public int getSessionEmeraldsSaved() {
        return sessionStats.emeraldsSaved;
    }

    public List<TradeRecord> getSessionBestTrades() {
        return new ArrayList<>(sessionStats.bestTrades);
    }

    public int getAllTimeRerolls() {
        return allTimeStats.totalRerolls;
    }

    public int getAllTimeSuccesses() {
        return allTimeStats.successfulRerolls;
    }

    public int getAllTimeFailures() {
        return allTimeStats.failedRerolls;
    }

    public double getAllTimeAverageAttempts() {
        if (allTimeStats.totalRerolls == 0) {
            return 0;
        }
        return (double) allTimeStats.totalAttempts / allTimeStats.totalRerolls;
    }

    public double getSuccessRate() {
        if (allTimeStats.totalRerolls == 0) {
            return 0;
        }
        return (
            ((double) allTimeStats.successfulRerolls /
                allTimeStats.totalRerolls) *
            100
        );
    }

    public int getAllTimeEmeraldsSaved() {
        return allTimeStats.emeraldsSaved;
    }

    public List<TradeRecord> getAllTimeBestTrades() {
        return new ArrayList<>(allTimeStats.bestTrades);
    }

    private static class Statistics {

        int totalRerolls = 0;
        int successfulRerolls = 0;
        int failedRerolls = 0;
        int totalAttempts = 0;
        int emeraldsSaved = 0;
        List<TradeRecord> bestTrades = new ArrayList<>();

        void recordBestTrade(int attempts) {
            TradeRecord record = new TradeRecord(
                System.currentTimeMillis(),
                attempts
            );
            bestTrades.add(record);

            bestTrades.sort((a, b) -> Integer.compare(a.attempts, b.attempts));
            if (bestTrades.size() > 10) {
                bestTrades = new ArrayList<>(bestTrades.subList(0, 10));
            }
        }
    }

    public static class TradeRecord {

        long timestamp;
        int attempts;
        String itemName;
        int emeraldCost;

        public TradeRecord(long timestamp, int attempts) {
            this.timestamp = timestamp;
            this.attempts = attempts;
        }

        public TradeRecord(
            long timestamp,
            int attempts,
            String itemName,
            int emeraldCost
        ) {
            this.timestamp = timestamp;
            this.attempts = attempts;
            this.itemName = itemName;
            this.emeraldCost = emeraldCost;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getAttempts() {
            return attempts;
        }

        public String getItemName() {
            return itemName;
        }

        public int getEmeraldCost() {
            return emeraldCost;
        }
    }

    public String exportStatistics() {
        StatisticsExport export = new StatisticsExport();
        export.sessionStats = sessionStats;
        export.allTimeStats = allTimeStats;
        export.exportTimestamp = System.currentTimeMillis();

        return GSON.toJson(export);
    }

    private static class StatisticsExport {

        Statistics sessionStats;
        Statistics allTimeStats;
        long exportTimestamp;
    }
}
