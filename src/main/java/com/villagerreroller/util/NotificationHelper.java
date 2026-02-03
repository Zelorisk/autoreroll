package com.villagerreroller.util;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.config.ModConfig;
import com.villagerreroller.trade.TradeScanner;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class NotificationHelper {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void sendMessage(String message) {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();

        switch (config.getNotificationStyle()) {
            case CHAT -> sendChatMessage(message);
            case ACTION_BAR -> sendActionBar(message);
            case OVERLAY -> sendOverlay(message);
        }
    }

    public static void sendChatMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("§6[VTR]§r " + message),
                false
            );
        }
    }

    public static void sendActionBar(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    public static void sendOverlay(String message) {
        sendActionBar(message);
    }

    public static void playSuccessSound() {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();

        if (config.isSoundNotifications() && client.player != null) {
            client.player.playSound(
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                1.0f,
                1.0f
            );
        }
    }

    public static void playErrorSound() {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();

        if (config.isSoundNotifications() && client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    public static void playClickSound() {
        if (client.player != null) {
            client.player.playSound(
                SoundEvents.UI_BUTTON_CLICK.value(),
                0.5f,
                1.0f
            );
        }
    }

    public static void sendTradeFoundNotification(
        String itemName,
        int emeraldCost,
        int attempts
    ) {
        String message = String.format(
            "§aFound: §f%s §7for §f%d emeralds §7(§f%d §7attempts)",
            itemName,
            emeraldCost,
            attempts
        );
        sendMessage(message);
        playSuccessSound();
    }

    public static void sendErrorNotification(String error) {
        String message = "§c" + error;
        sendMessage(message);
        playErrorSound();
    }

    public static void sendWarningNotification(String warning) {
        String message = "§e" + warning;
        sendMessage(message);
    }

    public static void sendInfoNotification(String info) {
        String message = "§7" + info;
        sendMessage(message);
    }

    public static void displayClientMessage(String message) {
        sendChatMessage(message);
    }

    public static void sendDebugAlert(String title, String... lines) {
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("§6§l[DEBUG] " + title),
                false
            );
            for (String line : lines) {
                client.player.sendMessage(Text.literal("  §7" + line), false);
            }
            playSuccessSound();
        }
    }

    public static void sendMatchFoundAlert(
        List<TradeScanner.ScannedTrade> matchingTrades,
        int attempts
    ) {
        if (client.player == null) return;

        client.player.sendMessage(Text.literal(""), false);
        client.player.sendMessage(
            Text.literal("§a§l═══════════════════════════"),
            false
        );
        client.player.sendMessage(Text.literal("§a§l    MATCH FOUND!"), false);
        client.player.sendMessage(
            Text.literal("§a§l═══════════════════════════"),
            false
        );
        client.player.sendMessage(
            Text.literal("§7Attempts: §f" + attempts),
            false
        );
        client.player.sendMessage(
            Text.literal("§7Matching trades: §f" + matchingTrades.size()),
            false
        );
        client.player.sendMessage(Text.literal(""), false);

        for (TradeScanner.ScannedTrade trade : matchingTrades) {
            client.player.sendMessage(
                Text.literal("§e► Trade Slot " + trade.getSlotIndex() + ":"),
                false
            );
            client.player.sendMessage(
                Text.literal("  §7Item: §f" + trade.getItemId()),
                false
            );
            client.player.sendMessage(
                Text.literal(
                    "  §7Cost: §a" + trade.getEmeraldCost() + " emeralds"
                ),
                false
            );

            if (!trade.getEnchantments().isEmpty()) {
                client.player.sendMessage(
                    Text.literal("  §7Enchantments:"),
                    false
                );
                for (String ench : trade.getEnchantmentNames()) {
                    String[] parts = ench.split(":");
                    String name = parts.length >= 2 ? parts[1] : ench;
                    int level =
                        parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                    client.player.sendMessage(
                        Text.literal("    §b" + name + " " + level),
                        false
                    );
                }
            }
            client.player.sendMessage(Text.literal(""), false);
        }

        client.player.sendMessage(
            Text.literal("§a§l═══════════════════════════"),
            false
        );
        client.player.sendMessage(Text.literal(""), false);

        playSuccessSound();
    }
}
