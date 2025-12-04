package com.villagerreroller.util;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.automation.RerollController;
import com.villagerreroller.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.VillagerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class KeybindManager {
    private KeyBinding toggleModKey;
    private KeyBinding emergencyStopKey;
    private KeyBinding manualRerollKey;
    private KeyBinding cycleProfileKey;
    private VillagerDetector villagerDetector;

    public void register() {
        // Initialize villager detector
        villagerDetector = new VillagerDetector();

        // Register keybindings
        toggleModKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.villagerreroller.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.villagerreroller"
        ));

        emergencyStopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.villagerreroller.emergency_stop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_ESCAPE,
                "category.villagerreroller"
        ));

        manualRerollKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.villagerreroller.manual_reroll",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.villagerreroller"
        ));

        cycleProfileKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.villagerreroller.cycle_profile",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.villagerreroller"
        ));

        // Register tick event to handle key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyPresses();
        });

        VillagerReroller.LOGGER.info("Registered keybindings");
    }

    private void handleKeyPresses() {
        // Toggle mod
        while (toggleModKey.wasPressed()) {
            toggleMod();
        }

        // Emergency stop
        while (emergencyStopKey.wasPressed()) {
            emergencyStop();
        }

        // Manual reroll
        while (manualRerollKey.wasPressed()) {
            manualReroll();
        }

        // Cycle profile
        while (cycleProfileKey.wasPressed()) {
            cycleProfile();
        }
    }

    private void toggleMod() {
        ModConfig config = VillagerReroller.getInstance().getConfigManager().getConfig();
        boolean newState = !config.isEnabled();
        config.setEnabled(newState);
        VillagerReroller.getInstance().getConfigManager().save();

        NotificationHelper.sendMessage(newState ? "Mod enabled" : "Mod disabled");
        VillagerReroller.LOGGER.info("Mod toggled: {}", newState);
    }

    private void emergencyStop() {
        RerollController controller = VillagerReroller.getInstance().getRerollController();
        if (controller.isRunning()) {
            controller.emergencyStop();
        }
    }

    private void manualReroll() {
        ModConfig config = VillagerReroller.getInstance().getConfigManager().getConfig();
        RerollController controller = VillagerReroller.getInstance().getRerollController();

        if (!config.isEnabled()) {
            NotificationHelper.sendMessage("Mod is disabled! Press R to enable.");
            return;
        }

        // If already running, stop it
        if (controller.isRunning()) {
            controller.stopRerolling();
            NotificationHelper.sendMessage("Stopped rerolling");
            return;
        }

        // Find a villager to reroll
        Optional<VillagerEntity> villagerOpt = villagerDetector.getBestVillagerToReroll();

        if (villagerOpt.isEmpty()) {
            NotificationHelper.sendMessage("No villager found nearby! Look at or get closer to a villager.");
            VillagerReroller.LOGGER.warn("No villager found for manual reroll");
            return;
        }

        VillagerEntity villager = villagerOpt.get();

        // Check if the villager is a baby
        if (villager.isBaby()) {
            NotificationHelper.sendMessage("Cannot reroll baby villagers!");
            return;
        }

        // Start rerolling
        controller.startRerolling(villager);
        VillagerReroller.LOGGER.info("Manual reroll started for villager at {}", villager.getPos());
    }

    private void cycleProfile() {
        // TODO: Implement profile cycling
        NotificationHelper.sendMessage("Profile cycling not yet implemented");
        VillagerReroller.LOGGER.info("Profile cycle requested");
    }

    public KeyBinding getToggleModKey() {
        return toggleModKey;
    }

    public KeyBinding getEmergencyStopKey() {
        return emergencyStopKey;
    }

    public KeyBinding getManualRerollKey() {
        return manualRerollKey;
    }

    public KeyBinding getCycleProfileKey() {
        return cycleProfileKey;
    }
}
