package io.github.messycraft.globalStatistics;

import io.github.messycraft.globalStatistics.api.GlobalStatisticsApi;
import io.github.messycraft.globalStatistics.api.GlobalStatisticsApiImpl;
import io.github.messycraft.globalStatistics.command.GsCommand;
import io.github.messycraft.globalStatistics.service.StatisticsService;
import io.github.messycraft.globalStatistics.storage.DatabaseManager;
import io.github.messycraft.globalStatistics.storage.StatisticsRepository;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GlobalStatistics extends JavaPlugin {

    private final DatabaseManager databaseManager = new DatabaseManager();
    @Getter
    private GlobalStatisticsApi api;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            databaseManager.initialize(getConfig());
            databaseManager.createTablesIfNeeded();

            StatisticsRepository repository = new StatisticsRepository(databaseManager.getDataSource());
            StatisticsService service = new StatisticsService(repository, getDataFolder());
            this.api = new GlobalStatisticsApiImpl(service);

            GsCommand gsCommand = new GsCommand(this, service);
            PluginCommand command = getCommand("gs");
            if (command != null) {
                command.setExecutor(gsCommand);
                command.setTabCompleter(gsCommand);
            }
            getLogger().info("GlobalStatistics enabled.");
        } catch (Exception e) {
            getLogger().severe("Failed to enable plugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        databaseManager.shutdown();
        getLogger().info("GlobalStatistics disabled.");
    }
}
