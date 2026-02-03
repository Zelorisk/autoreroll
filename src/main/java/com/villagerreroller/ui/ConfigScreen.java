package com.villagerreroller.ui;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.config.ModConfig;
import com.villagerreroller.config.ProfileManager;
import com.villagerreroller.trade.LibrarianEnchantments;
import com.villagerreroller.trade.LibrarianEnchantments.EnchantmentOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {

    public static Screen create(Screen parent) {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("Villager Trade Reroller Config"))
            .setSavingRunnable(() -> {
                VillagerReroller.getInstance().getConfigManager().save();
                VillagerReroller.LOGGER.info("Configuration saved");
            });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
            Text.literal("General")
        );

        general.addEntry(
            entryBuilder
                .startBooleanToggle(Text.literal("Enabled"), config.isEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Enable or disable the mod"))
                .setSaveConsumer(config::setEnabled)
                .build()
        );

        general.addEntry(
            entryBuilder
                .startEnumSelector(
                    Text.literal("Operation Mode"),
                    ModConfig.OperationMode.class,
                    config.getOperationMode()
                )
                .setDefaultValue(ModConfig.OperationMode.FULL_AUTO)
                .setTooltip(
                    Text.literal(
                        "MANUAL: Highlight only, SEMI_AUTO: Breaks only, FULL_AUTO: Complete automation"
                    )
                )
                .setSaveConsumer(config::setOperationMode)
                .build()
        );

        general.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Reroll Delay (ms)"),
                    config.getRerollDelayMs(),
                    100,
                    5000
                )
                .setDefaultValue(500)
                .setTooltip(
                    Text.literal(
                        "Delay between reroll attempts in milliseconds"
                    )
                )
                .setSaveConsumer(config::setRerollDelayMs)
                .build()
        );

        general.addEntry(
            entryBuilder
                .startIntField(
                    Text.literal("Max Reroll Attempts"),
                    config.getMaxRerollAttempts()
                )
                .setDefaultValue(100)
                .setTooltip(
                    Text.literal(
                        "Maximum number of reroll attempts per villager"
                    )
                )
                .setSaveConsumer(config::setMaxRerollAttempts)
                .build()
        );

        general.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Sound Notifications"),
                    config.isSoundNotifications()
                )
                .setDefaultValue(true)
                .setTooltip(Text.literal("Play sounds when trades are found"))
                .setSaveConsumer(config::setSoundNotifications)
                .build()
        );

        general.addEntry(
            entryBuilder
                .startEnumSelector(
                    Text.literal("Notification Style"),
                    ModConfig.NotificationStyle.class,
                    config.getNotificationStyle()
                )
                .setDefaultValue(ModConfig.NotificationStyle.ACTION_BAR)
                .setTooltip(Text.literal("Where to display notifications"))
                .setSaveConsumer(config::setNotificationStyle)
                .build()
        );

        general.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Open GUI Only When Matched"),
                    config.isOpenGuiOnlyWhenMatched()
                )
                .setDefaultValue(false)
                .setTooltip(
                    Text.literal(
                        "Only open the trading GUI when a matching trade is found.\nWhen disabled, GUI opens every attempt to check trades."
                    )
                )
                .setSaveConsumer(config::setOpenGuiOnlyWhenMatched)
                .build()
        );

        ConfigCategory filters = builder.getOrCreateCategory(
            Text.literal("Trade Filters")
        );

        filters.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Max Emeralds (Books)"),
                    config.getMaxEmeraldsBooks(),
                    1,
                    64
                )
                .setDefaultValue(10)
                .setTooltip(
                    Text.literal("Maximum emeralds for enchanted books")
                )
                .setSaveConsumer(config::setMaxEmeraldsBooks)
                .build()
        );

        filters.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Max Emeralds (Tools)"),
                    config.getMaxEmeraldsTools(),
                    1,
                    64
                )
                .setDefaultValue(5)
                .setTooltip(Text.literal("Maximum emeralds for tools"))
                .setSaveConsumer(config::setMaxEmeraldsTools)
                .build()
        );

        filters.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Max Emeralds (Armor)"),
                    config.getMaxEmeraldsArmor(),
                    1,
                    64
                )
                .setDefaultValue(5)
                .setTooltip(Text.literal("Maximum emeralds for armor"))
                .setSaveConsumer(config::setMaxEmeraldsArmor)
                .build()
        );

        filters.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Max Emeralds (Misc)"),
                    config.getMaxEmeraldsMisc(),
                    1,
                    64
                )
                .setDefaultValue(10)
                .setTooltip(
                    Text.literal("Maximum emeralds for miscellaneous items")
                )
                .setSaveConsumer(config::setMaxEmeraldsMisc)
                .build()
        );

        filters.addEntry(
            entryBuilder
                .startEnumSelector(
                    Text.literal("Filter Logic"),
                    ModConfig.FilterLogic.class,
                    config.getFilterLogic()
                )
                .setDefaultValue(ModConfig.FilterLogic.AND)
                .setTooltip(
                    Text.literal(
                        "AND: All conditions must match, OR: Any condition can match"
                    )
                )
                .setSaveConsumer(config::setFilterLogic)
                .build()
        );

        List<EnchantmentOption> enchantments =
            LibrarianEnchantments.ALL_ENCHANTMENTS;
        int currentEnchantIndex = LibrarianEnchantments.getIndex(
            config.getSelectedEnchantment(),
            config.getSelectedEnchantmentLevel()
        );

        filters.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Target Enchantment"),
                    currentEnchantIndex,
                    0,
                    enchantments.size() - 1
                )
                .setDefaultValue(0)
                .setTooltip(
                    Text.literal(
                        "Select the enchanted book you want to find.\nUse the slider to cycle through all available enchantments."
                    )
                )
                .setTextGetter(value -> {
                    EnchantmentOption opt = LibrarianEnchantments.getByIndex(
                        value
                    );
                    int level = Math.min(
                        config.getSelectedEnchantmentLevel(),
                        opt.getMaxLevel()
                    );
                    return Text.literal(opt.getDisplayString(level));
                })
                .setSaveConsumer(value -> {
                    EnchantmentOption opt = LibrarianEnchantments.getByIndex(
                        value
                    );
                    config.setSelectedEnchantment(opt.getId());
                    if (
                        config.getSelectedEnchantmentLevel() > opt.getMaxLevel()
                    ) {
                        config.setSelectedEnchantmentLevel(opt.getMaxLevel());
                    }
                })
                .build()
        );

        EnchantmentOption currentEnchant = LibrarianEnchantments.getByIndex(
            currentEnchantIndex
        );
        filters.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Enchantment Level"),
                    config.getSelectedEnchantmentLevel(),
                    1,
                    currentEnchant.getMaxLevel()
                )
                .setDefaultValue(currentEnchant.getMaxLevel())
                .setTooltip(
                    Text.literal(
                        "Select the minimum level for the enchantment.\nHigher levels are rarer and more expensive."
                    )
                )
                .setTextGetter(value -> {
                    EnchantmentOption opt = LibrarianEnchantments.getByIndex(
                        LibrarianEnchantments.getIndex(
                            config.getSelectedEnchantment(),
                            1
                        )
                    );
                    return Text.literal(opt.getDisplayString(value));
                })
                .setSaveConsumer(config::setSelectedEnchantmentLevel)
                .build()
        );

        ConfigCategory advanced = builder.getOrCreateCategory(
            Text.literal("Advanced")
        );

        advanced.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Require Max Enchantment Level"),
                    config.isRequireMaxEnchantmentLevel()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Text.literal(
                        "Only accept enchantments at their maximum level"
                    )
                )
                .setSaveConsumer(config::setRequireMaxEnchantmentLevel)
                .build()
        );

        advanced.addEntry(
            entryBuilder
                .startStrList(
                    Text.literal("Combined Enchantments"),
                    config.getCombinedEnchantments()
                )
                .setDefaultValue(new ArrayList<>())
                .setTooltip(
                    Text.literal(
                        "Require multiple enchantments on the same book"
                    )
                )
                .setSaveConsumer(config::setCombinedEnchantments)
                .build()
        );

        advanced.addEntry(
            entryBuilder
                .startStrList(
                    Text.literal("Preferred First Slot Items"),
                    new ArrayList<>(config.getPreferredFirstSlotItems())
                )
                .setDefaultValue(new ArrayList<>())
                .setTooltip(
                    Text.literal("Prefer these items in the first trade slot")
                )
                .setSaveConsumer(list ->
                    config.setPreferredFirstSlotItems(new HashSet<>(list))
                )
                .build()
        );

        advanced.addEntry(
            entryBuilder
                .startStrList(
                    Text.literal("Excluded Professions"),
                    new ArrayList<>(config.getExcludedProfessions())
                )
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.literal("Don't reroll these professions"))
                .setSaveConsumer(list ->
                    config.setExcludedProfessions(new HashSet<>(list))
                )
                .build()
        );

        ConfigCategory safety = builder.getOrCreateCategory(
            Text.literal("Safety")
        );

        safety.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Villager Cooldown (ms)"),
                    config.getVillagerCooldownMs(),
                    0,
                    5000
                )
                .setDefaultValue(1000)
                .setTooltip(
                    Text.literal(
                        "Cooldown between processing different villagers"
                    )
                )
                .setSaveConsumer(config::setVillagerCooldownMs)
                .build()
        );

        safety.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Pause if Inventory Full"),
                    config.isPauseIfInventoryFull()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Text.literal(
                        "Automatically pause rerolling when inventory is full"
                    )
                )
                .setSaveConsumer(config::setPauseIfInventoryFull)
                .build()
        );

        safety.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Server Friendly Throttling"),
                    config.isServerFriendlyThrottling()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Text.literal(
                        "Use longer delays to be respectful on multiplayer servers"
                    )
                )
                .setSaveConsumer(config::setServerFriendlyThrottling)
                .build()
        );

        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD"));

        hud.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Show Overlay"),
                    config.isShowOverlay()
                )
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show HUD overlay with reroll status"))
                .setSaveConsumer(config::setShowOverlay)
                .build()
        );

        hud.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Show Trade Quality"),
                    config.isShowTradeQuality()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Text.literal("Show quality indicators next to trades")
                )
                .setSaveConsumer(config::setShowTradeQuality)
                .build()
        );

        hud.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Show Progress Bar"),
                    config.isShowProgressBar()
                )
                .setDefaultValue(true)
                .setTooltip(
                    Text.literal("Show progress bar for reroll attempts")
                )
                .setSaveConsumer(config::setShowProgressBar)
                .build()
        );

        ConfigCategory reachPlacement = builder.getOrCreateCategory(
            Text.literal("Reach & Placement")
        );

        reachPlacement.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Interaction Reach"),
                    (int) (config.getInteractionReach() * 10),
                    30,
                    100
                )
                .setDefaultValue(45)
                .setTooltip(
                    Text.literal(
                        "How far you can interact with villagers (blocks * 10)\nDefault: 4.5 blocks\nIncrease if villager is slightly too far away"
                    )
                )
                .setSaveConsumer(value ->
                    config.setInteractionReach(value / 10.0)
                )
                .setTextGetter(value ->
                    Text.literal(String.format("%.1f blocks", value / 10.0))
                )
                .build()
        );

        reachPlacement.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Placement Reach"),
                    config.getPlacementReach(),
                    3,
                    15
                )
                .setDefaultValue(8)
                .setTooltip(
                    Text.literal(
                        "How far to search for placement locations\nDefault: 8 blocks\nIncrease if placements are failing due to distance"
                    )
                )
                .setSaveConsumer(config::setPlacementReach)
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .build()
        );

        reachPlacement.addEntry(
            entryBuilder
                .startIntSlider(
                    Text.literal("Job Site Search Reach"),
                    config.getJobSiteSearchReach(),
                    3,
                    20
                )
                .setDefaultValue(8)
                .setTooltip(
                    Text.literal(
                        "How far to search for existing job sites\nDefault: 8 blocks\nIncrease if mod can't find villager's workstation"
                    )
                )
                .setSaveConsumer(config::setJobSiteSearchReach)
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .build()
        );

        reachPlacement.addEntry(
            entryBuilder
                .startBooleanToggle(
                    Text.literal("Use Fixed Placement Block"),
                    config.isUseFixedPlacementBlock()
                )
                .setDefaultValue(false)
                .setTooltip(
                    Text.literal(
                        "Always place lectern on the block you're looking at when toggling mod\nWhen enabled, look at target block before starting reroll\nMod will automatically walk out of the way before placing"
                    )
                )
                .setSaveConsumer(config::setUseFixedPlacementBlock)
                .build()
        );

        ConfigCategory profiles = builder.getOrCreateCategory(
            Text.literal("Profiles")
        );

        ProfileManager profileManager = VillagerReroller.getInstance()
            .getConfigManager()
            .getProfileManager();

        profiles.addEntry(
            entryBuilder
                .startTextField(
                    Text.literal("Active Profile"),
                    config.getActiveProfile()
                )
                .setDefaultValue("default")
                .setTooltip(Text.literal("Currently active filter profile"))
                .setSaveConsumer(config::setActiveProfile)
                .build()
        );

        profiles.addEntry(
            entryBuilder
                .startTextDescription(
                    Text.literal(
                        "§7Available profiles: " +
                            String.join(
                                ", ",
                                profileManager.getAllProfiles().keySet()
                            )
                    )
                )
                .build()
        );

        profiles.addEntry(
            entryBuilder
                .startTextDescription(
                    Text.literal(
                        "§eCreate new profile: Save current settings as a profile"
                    )
                )
                .build()
        );

        profiles.addEntry(
            entryBuilder
                .startTextDescription(
                    Text.literal(
                        "§aLoad profile: Type profile name in 'Active Profile' and save config"
                    )
                )
                .build()
        );

        profiles.addEntry(
            entryBuilder
                .startTextDescription(
                    Text.literal(
                        "§6Profiles are stored in: config/villager-reroller/profiles/"
                    )
                )
                .build()
        );

        return builder.build();
    }
}
