package com.villagerreroller.util;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.automation.RerollController;
import com.villagerreroller.config.ModConfig;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeybindManager {

    private static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("villagerreroller", "main"));

    private KeyBinding toggleModKey;
    private KeyBinding emergencyStopKey;
    private KeyBinding manualRerollKey;
    private KeyBinding cycleProfileKey;
    private VillagerDetector villagerDetector;

    public void register() {
        villagerDetector = new VillagerDetector();

        toggleModKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.villagerreroller.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
            )
        );

        emergencyStopKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.villagerreroller.emergency_stop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_ESCAPE,
                CATEGORY
            )
        );

        manualRerollKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.villagerreroller.manual_reroll",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
            )
        );

        cycleProfileKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.villagerreroller.cycle_profile",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                CATEGORY
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyPresses();
        });

        VillagerReroller.LOGGER.info("Registered keybindings");
    }

    private void handleKeyPresses() {
        while (toggleModKey.wasPressed()) {
            toggleMod();
        }

        while (emergencyStopKey.wasPressed()) {
            emergencyStop();
        }

        while (manualRerollKey.wasPressed()) {
            manualReroll();
        }

        while (cycleProfileKey.wasPressed()) {
            cycleProfile();
        }
    }

    private void toggleMod() {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();
        boolean newState = !config.isEnabled();
        config.setEnabled(newState);
        VillagerReroller.getInstance().getConfigManager().save();

        NotificationHelper.sendMessage(
            newState ? "Mod enabled" : "Mod disabled"
        );
        VillagerReroller.LOGGER.info("Mod toggled: {}", newState);
    }

    private void emergencyStop() {
        RerollController controller =
            VillagerReroller.getInstance().getRerollController();
        if (controller.isRunning()) {
            controller.emergencyStop();
        }
    }

    private void manualReroll() {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();
        RerollController controller =
            VillagerReroller.getInstance().getRerollController();

        if (!config.isEnabled()) {
            NotificationHelper.sendMessage(
                "Mod is disabled! Press R to enable."
            );
            return;
        }

        if (controller.isRunning()) {
            controller.stopRerolling();
            NotificationHelper.sendMessage("Stopped rerolling");
            return;
        }

        Optional<VillagerEntity> villagerOpt =
            villagerDetector.getBestVillagerToReroll();

        if (villagerOpt.isEmpty()) {
            NotificationHelper.sendMessage(
                "No villager found nearby! Look at or get closer to a villager."
            );
            VillagerReroller.LOGGER.warn("No villager found for manual reroll");
            return;
        }

        VillagerEntity villager = villagerOpt.get();

        if (villager.isBaby()) {
            NotificationHelper.sendMessage("Cannot reroll baby villagers!");
            return;
        }

        controller.startRerolling(villager);
        VillagerReroller.LOGGER.info(
            "Manual reroll started for villager at {}",
            villager.getBlockPos()
        );
    }

    private void cycleProfile() {
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
