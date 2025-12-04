package com.villagerreroller.ui;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.config.ModConfig;
import com.villagerreroller.config.ProfileManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;

public class ConfigScreen {
    public static Screen create(Screen parent) {
        ModConfig config = VillagerReroller.getInstance().getConfigManager().getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Villager Trade Reroller Config"))
                .setSavingRunnable(() -> {
                    VillagerReroller.getInstance().getConfigManager().save();
                    VillagerReroller.LOGGER.info("Configuration saved");
                });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General Settings Category
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled"), config.isEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Enable or disable the mod"))
                .setSaveConsumer(config::setEnabled)
                .build());

        general.addEntry(entryBuilder.startEnumSelector(Text.literal("Operation Mode"), ModConfig.OperationMode.class, config.getOperationMode())
                .setDefaultValue(ModConfig.OperationMode.MANUAL)
                .setTooltip(Text.literal("MANUAL: Highlight only, SEMI_AUTO: Breaks only, FULL_AUTO: Complete automation"))
                .setSaveConsumer(config::setOperationMode)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Text.literal("Reroll Delay (ms)"), config.getRerollDelayMs(), 100, 5000)
                .setDefaultValue(500)
                .setTooltip(Text.literal("Delay between reroll attempts in milliseconds"))
                .setSaveConsumer(config::setRerollDelayMs)
                .build());

        general.addEntry(entryBuilder.startIntField(Text.literal("Max Reroll Attempts"), config.getMaxRerollAttempts())
                .setDefaultValue(100)
                .setTooltip(Text.literal("Maximum number of reroll attempts per villager"))
                .setSaveConsumer(config::setMaxRerollAttempts)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Sound Notifications"), config.isSoundNotifications())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Play sounds when trades are found"))
                .setSaveConsumer(config::setSoundNotifications)
                .build());

        general.addEntry(entryBuilder.startEnumSelector(Text.literal("Notification Style"), ModConfig.NotificationStyle.class, config.getNotificationStyle())
                .setDefaultValue(ModConfig.NotificationStyle.ACTION_BAR)
                .setTooltip(Text.literal("Where to display notifications"))
                .setSaveConsumer(config::setNotificationStyle)
                .build());

        // Trade Filters Category
        ConfigCategory filters = builder.getOrCreateCategory(Text.literal("Trade Filters"));

        filters.addEntry(entryBuilder.startIntSlider(Text.literal("Max Emeralds (Books)"), config.getMaxEmeraldsBooks(), 1, 64)
                .setDefaultValue(10)
                .setTooltip(Text.literal("Maximum emeralds for enchanted books"))
                .setSaveConsumer(config::setMaxEmeraldsBooks)
                .build());

        filters.addEntry(entryBuilder.startIntSlider(Text.literal("Max Emeralds (Tools)"), config.getMaxEmeraldsTools(), 1, 64)
                .setDefaultValue(5)
                .setTooltip(Text.literal("Maximum emeralds for tools"))
                .setSaveConsumer(config::setMaxEmeraldsTools)
                .build());

        filters.addEntry(entryBuilder.startIntSlider(Text.literal("Max Emeralds (Armor)"), config.getMaxEmeraldsArmor(), 1, 64)
                .setDefaultValue(5)
                .setTooltip(Text.literal("Maximum emeralds for armor"))
                .setSaveConsumer(config::setMaxEmeraldsArmor)
                .build());

        filters.addEntry(entryBuilder.startIntSlider(Text.literal("Max Emeralds (Misc)"), config.getMaxEmeraldsMisc(), 1, 64)
                .setDefaultValue(10)
                .setTooltip(Text.literal("Maximum emeralds for miscellaneous items"))
                .setSaveConsumer(config::setMaxEmeraldsMisc)
                .build());

        filters.addEntry(entryBuilder.startEnumSelector(Text.literal("Filter Logic"), ModConfig.FilterLogic.class, config.getFilterLogic())
                .setDefaultValue(ModConfig.FilterLogic.AND)
                .setTooltip(Text.literal("AND: All conditions must match, OR: Any condition can match"))
                .setSaveConsumer(config::setFilterLogic)
                .build());

        filters.addEntry(entryBuilder.startStrList(Text.literal("Item Whitelist"), new ArrayList<>(config.getItemWhitelist()))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Click + to add items. Type full item IDs like:\nminecraft:enchanted_book\nminecraft:diamond_pickaxe\nminecraft:diamond\nLeave empty to accept all items"))
                .setSaveConsumer(list -> config.setItemWhitelist(new HashSet<>(list)))
                .build());

        filters.addEntry(entryBuilder.startStrList(Text.literal("Item Blacklist"), new ArrayList<>(config.getItemBlacklist()))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Click + to add items to blacklist. Type full item IDs like:\nminecraft:wheat\nminecraft:stick\nThese items will never be accepted"))
                .setSaveConsumer(list -> config.setItemBlacklist(new HashSet<>(list)))
                .build());

        filters.addEntry(entryBuilder.startStrList(Text.literal("Enchantment Filters"), config.getEnchantmentFilters())
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Click + to add enchantments. Format: enchantment:level\nminecraft:mending:1\nminecraft:sharpness:5\nminecraft:protection:4"))
                .setSaveConsumer(config::setEnchantmentFilters)
                .build());

        // Advanced Options Category
        ConfigCategory advanced = builder.getOrCreateCategory(Text.literal("Advanced"));

        advanced.addEntry(entryBuilder.startBooleanToggle(Text.literal("Require Max Enchantment Level"), config.isRequireMaxEnchantmentLevel())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Only accept enchantments at their maximum level"))
                .setSaveConsumer(config::setRequireMaxEnchantmentLevel)
                .build());

        advanced.addEntry(entryBuilder.startStrList(Text.literal("Combined Enchantments"), config.getCombinedEnchantments())
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Require multiple enchantments on the same book"))
                .setSaveConsumer(config::setCombinedEnchantments)
                .build());

        advanced.addEntry(entryBuilder.startStrList(Text.literal("Preferred First Slot Items"), new ArrayList<>(config.getPreferredFirstSlotItems()))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Prefer these items in the first trade slot"))
                .setSaveConsumer(list -> config.setPreferredFirstSlotItems(new HashSet<>(list)))
                .build());

        advanced.addEntry(entryBuilder.startStrList(Text.literal("Excluded Professions"), new ArrayList<>(config.getExcludedProfessions()))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Don't reroll these professions"))
                .setSaveConsumer(list -> config.setExcludedProfessions(new HashSet<>(list)))
                .build());

        // Safety Category
        ConfigCategory safety = builder.getOrCreateCategory(Text.literal("Safety"));

        safety.addEntry(entryBuilder.startIntSlider(Text.literal("Villager Cooldown (ms)"), config.getVillagerCooldownMs(), 0, 5000)
                .setDefaultValue(1000)
                .setTooltip(Text.literal("Cooldown between processing different villagers"))
                .setSaveConsumer(config::setVillagerCooldownMs)
                .build());

        safety.addEntry(entryBuilder.startBooleanToggle(Text.literal("Pause if Inventory Full"), config.isPauseIfInventoryFull())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Automatically pause rerolling when inventory is full"))
                .setSaveConsumer(config::setPauseIfInventoryFull)
                .build());

        safety.addEntry(entryBuilder.startBooleanToggle(Text.literal("Server Friendly Throttling"), config.isServerFriendlyThrottling())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Use longer delays to be respectful on multiplayer servers"))
                .setSaveConsumer(config::setServerFriendlyThrottling)
                .build());

        // HUD Category
        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD"));

        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Overlay"), config.isShowOverlay())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show HUD overlay with reroll status"))
                .setSaveConsumer(config::setShowOverlay)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Trade Quality"), config.isShowTradeQuality())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show quality indicators next to trades"))
                .setSaveConsumer(config::setShowTradeQuality)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Progress Bar"), config.isShowProgressBar())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show progress bar for reroll attempts"))
                .setSaveConsumer(config::setShowProgressBar)
                .build());

        // Profiles Category
        ConfigCategory profiles = builder.getOrCreateCategory(Text.literal("Profiles"));

        profiles.addEntry(entryBuilder.startTextField(Text.literal("Active Profile"), config.getActiveProfile())
                .setDefaultValue("default")
                .setTooltip(Text.literal("Currently active filter profile"))
                .setSaveConsumer(config::setActiveProfile)
                .build());

        return builder.build();
    }
}
