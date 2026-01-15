package com.villagerreroller.util;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.config.ModConfig;
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
}
