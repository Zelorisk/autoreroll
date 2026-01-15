package com.villagerreroller;

import com.villagerreroller.automation.RerollController;
import com.villagerreroller.config.ConfigManager;
import com.villagerreroller.stats.StatisticsTracker;
import com.villagerreroller.ui.OverlayRenderer;
import com.villagerreroller.util.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerReroller implements ClientModInitializer {

    public static final String MOD_ID = "villagerreroller";
    public static final String MOD_NAME = "Villager Trade Reroller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static VillagerReroller instance;
    private ConfigManager configManager;
    private RerollController rerollController;
    private StatisticsTracker statisticsTracker;
    private KeybindManager keybindManager;
    private OverlayRenderer overlayRenderer;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initializing {} mod...", MOD_NAME);

        configManager = new ConfigManager();
        configManager.load();

        statisticsTracker = new StatisticsTracker();

        rerollController = new RerollController();

        keybindManager = new KeybindManager();
        keybindManager.register();

        overlayRenderer = new OverlayRenderer();

        LOGGER.info("{} mod initialized successfully!", MOD_NAME);
    }

    public static VillagerReroller getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RerollController getRerollController() {
        return rerollController;
    }

    public StatisticsTracker getStatisticsTracker() {
        return statisticsTracker;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }
}
