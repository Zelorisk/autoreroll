package com.villagerreroller.automation;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.config.ModConfig;
import com.villagerreroller.trade.TradeFilter;
import com.villagerreroller.trade.TradeScanner;
import com.villagerreroller.util.NotificationHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RerollController {
    private final MinecraftClient client;
    private final Map<UUID, VillagerState> villagerStates;
    private final JobSiteHandler jobSiteHandler;

    private boolean isRunning = false;
    private VillagerEntity currentVillager = null;
    private int currentAttempts = 0;
    private long lastRerollTime = 0;
    private boolean emergencyStop = false;

    // State machine for tick-based processing
    private enum RerollState {
        IDLE,
        INITIAL_PLACEMENT,
        WAITING_TO_BREAK,
        BREAKING_BLOCK,
        WAITING_FOR_DROP,
        PICKING_UP_ITEM,
        REPLACING_BLOCK,
        WAITING_FOR_VILLAGER,
        OPENING_TRADES,
        CHECKING_TRADES
    }

    private RerollState currentState = RerollState.IDLE;
    private long stateStartTime = 0;
    private BlockPos currentJobSite = null;
    private boolean stateActionStarted = false; // Flag to ensure one-time actions per state
    private int placementRetries = 0; // Track placement retry attempts

    public RerollController() {
        this.client = MinecraftClient.getInstance();
        this.villagerStates = new HashMap<>();
        this.jobSiteHandler = new JobSiteHandler();

        // Register tick event for state machine processing
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public void startRerolling(VillagerEntity villager) {
        if (isRunning) {
            VillagerReroller.LOGGER.warn("Reroll already in progress");
            return;
        }

        ModConfig config = VillagerReroller.getInstance().getConfigManager().getConfig();

        if (!config.isEnabled()) {
            NotificationHelper.sendMessage("Mod is disabled! Enable it in the config.");
            return;
        }

        if (config.getOperationMode() == ModConfig.OperationMode.MANUAL) {
            NotificationHelper.sendMessage("Manual mode - use highlights to identify good trades");
            return;
        }

        this.currentVillager = villager;
        this.currentAttempts = 0;
        this.isRunning = true;
        this.emergencyStop = false;
        this.lastRerollTime = System.currentTimeMillis();
        this.placementRetries = 0; // Reset placement retry counter

        VillagerReroller.LOGGER.info("=== Starting reroll for villager {} ===", villager.getUuid());
        NotificationHelper.sendMessage("Starting trade reroll...");

        // Initialize villager state
        VillagerState state = getOrCreateState(villager);
        state.lastAttemptTime = System.currentTimeMillis();

        // Check if villager already has a workstation nearby
        BlockPos existingJobSite = findJobSiteBlock();

        if (existingJobSite != null) {
            VillagerReroller.LOGGER.info("Found existing job site at {}, verifying villager has claimed it", existingJobSite);
            this.currentJobSite = existingJobSite;

            // IMPORTANT: Always verify villager has claimed job before checking trades
            // Go to WAITING_FOR_VILLAGER state to check profession first
            transitionToState(RerollState.WAITING_FOR_VILLAGER);
        } else {
            VillagerReroller.LOGGER.info("No job site found, will place one");
            transitionToState(RerollState.INITIAL_PLACEMENT);
        }
    }

    /**
     * Helper method to safely transition between states
     */
    private void transitionToState(RerollState newState) {
        VillagerReroller.LOGGER.info(">>> STATE TRANSITION: {} -> {}", currentState, newState);
        this.currentState = newState;
        this.stateStartTime = System.currentTimeMillis();
        this.stateActionStarted = false; // Reset action flag for new state
    }

    public void stopRerolling() {
        if (isRunning) {
            isRunning = false;

            // Stop any player movement
            stopPlayerMovement();

            VillagerReroller.LOGGER.info("Stopped rerolling");
            NotificationHelper.sendMessage("Reroll stopped. Attempts: " + currentAttempts);

            if (currentVillager != null) {
                VillagerState state = getOrCreateState(currentVillager);
                state.totalAttempts += currentAttempts;
            }

            currentVillager = null;
            currentAttempts = 0;
            currentState = RerollState.IDLE;
            currentJobSite = null;
        }
    }

    /**
     * Stop any automated player movement by clearing horizontal velocity and input
     */
    private void stopPlayerMovement() {
        if (client.player != null) {
            // Clear horizontal velocity only, keep vertical (for gravity/jumping)
            client.player.setVelocity(0, client.player.getVelocity().y, 0);
        }
        // Clear movement input to stop simulated key presses
        jobSiteHandler.clearMovementInput();
    }

    public void emergencyStop() {
        emergencyStop = true;
        stopRerolling();
        NotificationHelper.sendMessage("Emergency stop activated!");
        VillagerReroller.LOGGER.warn("Emergency stop activated");
    }

    /**
     * Main tick handler for the reroll state machine.
     *
     * CORRECT STATE SEQUENCE (must follow this order):
     * 1. INITIAL_PLACEMENT (if no workstation) OR WAITING_FOR_VILLAGER (if workstation exists)
     * 2. WAITING_FOR_VILLAGER - Wait for villager to claim job and get profession
     * 3. OPENING_TRADES - Right-click villager to open GUI
     * 4. CHECKING_TRADES - Scan trades and decide if match found
     * 5. WAITING_TO_BREAK - Wait for cooldown before breaking
     * 6. BREAKING_BLOCK - Break the workstation with axe
     * 7. WAITING_FOR_DROP - Wait for item to drop
     * 8. PICKING_UP_ITEM - Walk to and collect dropped item
     * 9. REPLACING_BLOCK - Place workstation back
     * 10. Loop back to step 2 (WAITING_FOR_VILLAGER)
     */
    private void onClientTick(MinecraftClient client) {
        if (!isRunning) {
            return;
        }

        if (emergencyStop) {
            stopRerolling();
            return;
        }

        if (currentVillager == null || !currentVillager.isAlive()) {
            VillagerReroller.LOGGER.warn("Villager is null or dead, stopping reroll");
            NotificationHelper.sendMessage("Villager lost! Stopping reroll.");
            stopRerolling();
            return;
        }

        ModConfig config = VillagerReroller.getInstance().getConfigManager().getConfig();
        long now = System.currentTimeMillis();
        long timeSinceStateStart = now - stateStartTime;

        VillagerReroller.LOGGER.debug("Tick: state={}, time={}ms, attempts={}/{}",
            currentState, timeSinceStateStart, currentAttempts, config.getMaxRerollAttempts());

        // State machine processing
        switch (currentState) {
            case INITIAL_PLACEMENT:
                // Place initial workstation if none exists
                if (!stateActionStarted) {
                    VillagerReroller.LOGGER.info("Attempting initial workstation placement...");

                    // Find a suitable position near the villager
                    BlockPos villagerPos = currentVillager.getBlockPos();
                    BlockPos placementPos = findSuitablePlacementPosition(villagerPos);

                    if (placementPos == null) {
                        VillagerReroller.LOGGER.error("No suitable placement position found!");
                        NotificationHelper.sendMessage("Cannot find place for workstation! Clear space near villager.");
                        stopRerolling();
                        return;
                    }

                    // Try to place the workstation
                    boolean placed = jobSiteHandler.placeInitialJobSite(placementPos);

                    if (!placed) {
                        VillagerReroller.LOGGER.error("Failed to place initial workstation!");
                        NotificationHelper.sendMessage("Failed to place workstation! Make sure you have one in inventory.");
                        stopRerolling();
                        return;
                    }

                    currentJobSite = placementPos;
                    VillagerReroller.LOGGER.info("Placed initial workstation at {}", placementPos);
                    stateActionStarted = true;
                }

                // Wait for placement to register
                if (timeSinceStateStart < 1000) {
                    return;
                }

                // Verify placement was successful
                if (!jobSiteHandler.isJobSiteBlock(currentJobSite)) {
                    VillagerReroller.LOGGER.error("Placement verification failed!");
                    NotificationHelper.sendMessage("Workstation placement failed!");
                    stopRerolling();
                    return;
                }

                VillagerReroller.LOGGER.info("Initial placement successful, waiting for villager to claim...");
                transitionToState(RerollState.WAITING_FOR_VILLAGER);
                break;

            case WAITING_TO_BREAK:
                // Check max attempts
                if (currentAttempts >= config.getMaxRerollAttempts()) {
                    NotificationHelper.sendMessage("Max attempts reached (" + currentAttempts + ")");
                    stopRerolling();
                    return;
                }

                // Check if inventory is full
                if (config.isPauseIfInventoryFull() && isInventoryFull()) {
                    NotificationHelper.sendMessage("Inventory full! Pausing reroll.");
                    stopRerolling();
                    return;
                }

                // Wait for cooldown
                long timeSinceLastReroll = now - lastRerollTime;
                if (timeSinceLastReroll < config.getRerollDelayMs()) {
                    return; // Still waiting
                }

                // Find job site
                VillagerReroller.LOGGER.info("Looking for job site block near villager...");
                currentJobSite = findJobSiteBlock();
                if (currentJobSite == null) {
                    VillagerReroller.LOGGER.warn("Could not find job site block for villager at {}", currentVillager.getPos());
                    NotificationHelper.sendMessage("Could not find job site block! Make sure villager has a workstation nearby.");
                    stopRerolling();
                    return;
                }
                VillagerReroller.LOGGER.info("Found job site at {}", currentJobSite);

                // Move to breaking state
                currentAttempts++;
                lastRerollTime = now;
                VillagerReroller.LOGGER.debug("Reroll attempt {} for villager", currentAttempts);

                transitionToState(RerollState.BREAKING_BLOCK);
                break;

            case BREAKING_BLOCK:
                // Start breaking on first entry to this state
                if (!stateActionStarted) {
                    VillagerReroller.LOGGER.info("Starting to break job site at {}", currentJobSite);
                    boolean started = jobSiteHandler.startBreakingJobSite(currentJobSite);

                    if (!started) {
                        VillagerReroller.LOGGER.error("Failed to start breaking job site block!");
                        NotificationHelper.sendMessage("Failed to break job site! Check if you have the right tools.");
                        stopRerolling();
                        return;
                    }
                    stateActionStarted = true;
                    return; // Wait for next tick to start breaking
                }

                // Continue breaking over multiple ticks
                boolean broken = jobSiteHandler.continueBreaking();

                if (broken) {
                    VillagerReroller.LOGGER.info("Job site broken successfully");

                    if (config.getOperationMode() == ModConfig.OperationMode.SEMI_AUTO) {
                        NotificationHelper.sendMessage("Job site broken. Replace manually to continue.");
                        stopRerolling();
                    } else {
                        // Move to waiting for drop
                        VillagerReroller.LOGGER.info("Waiting for item to drop...");
                        transitionToState(RerollState.WAITING_FOR_DROP);
                    }
                } else {
                    // Check timeout
                    if (timeSinceStateStart > 10000) {
                        VillagerReroller.LOGGER.error("Breaking timed out after 10 seconds!");
                        NotificationHelper.sendMessage("Failed to break job site! Timeout.");
                        jobSiteHandler.cancelBreaking();
                        stopRerolling();
                        return;
                    }
                    // Still breaking, continue on next tick
                }
                break;

            case WAITING_FOR_DROP:
                // Wait at least 500ms for item to spawn
                if (timeSinceStateStart < 500) {
                    return;
                }

                VillagerReroller.LOGGER.info("Looking for dropped item...");
                jobSiteHandler.resetPickupState(); // Reset pathfinding state for fresh start
                transitionToState(RerollState.PICKING_UP_ITEM);
                break;

            case PICKING_UP_ITEM:
                // Try to pick up the item - now with automatic player movement
                VillagerReroller.LOGGER.debug("PICKING_UP_ITEM: Attempting pickup ({}ms)...", timeSinceStateStart);
                int pickupResult = jobSiteHandler.tryPickupItem();

                if (pickupResult == 1) {
                    // Item successfully picked up - verify we have it before proceeding
                    VillagerReroller.LOGGER.info("✓ Item pickup reported success, verifying inventory...");

                    // Clear movement input immediately
                    jobSiteHandler.clearMovementInput();
                    stopPlayerMovement();

                    if (jobSiteHandler.hasWorkstationInInventory()) {
                        VillagerReroller.LOGGER.info("✓ Workstation confirmed in inventory. Proceeding to REPLACING_BLOCK.");
                        transitionToState(RerollState.REPLACING_BLOCK);
                    } else {
                        VillagerReroller.LOGGER.error("Item pickup success but NO workstation in inventory!");
                        NotificationHelper.sendMessage("§cOut of workstations! Stopping reroll.");
                        stopRerolling();
                        return;
                    }
                } else if (pickupResult == -1) {
                    // Fatal error
                    VillagerReroller.LOGGER.error("Fatal error during item pickup!");
                    NotificationHelper.sendMessage("Failed to pick up dropped item! Stopping.");
                    stopRerolling();
                    return;
                } else {
                    // Still trying (pickupResult == 0)
                    VillagerReroller.LOGGER.debug("Still trying to pick up item... ({}ms)", timeSinceStateStart);

                    // Show notification at 3 seconds to let user know bot is walking
                    if (timeSinceStateStart >= 3000 && timeSinceStateStart <= 3100) {
                        NotificationHelper.sendMessage("§eWalking to pick up item...");
                    }

                    // Extended timeout - 20 seconds to allow walking to far items
                    if (timeSinceStateStart > 20000) {
                        VillagerReroller.LOGGER.warn("Failed to pick up item after 20 seconds!");
                        jobSiteHandler.clearMovementInput();
                        stopPlayerMovement();

                        // Check if we still have a workstation available
                        if (jobSiteHandler.hasWorkstationInInventory()) {
                            VillagerReroller.LOGGER.info("Timeout but spare workstation found, proceeding to REPLACING_BLOCK");
                            NotificationHelper.sendMessage("§6Item pickup timeout, using spare workstation...");
                            transitionToState(RerollState.REPLACING_BLOCK);
                        } else {
                            VillagerReroller.LOGGER.error("Timeout AND no workstation in inventory!");
                            NotificationHelper.sendMessage("§cOut of workstations! Could not reach dropped item.");
                            stopRerolling();
                        }
                        return;
                    }
                    // Keep trying - movement is handled in tryPickupItem()
                }
                break;

            case REPLACING_BLOCK:
                // Verify we have a workstation before attempting placement
                if (!stateActionStarted) {
                    VillagerReroller.LOGGER.info("REPLACING_BLOCK state: Verifying inventory has workstation...");

                    if (!jobSiteHandler.hasWorkstationInInventory()) {
                        VillagerReroller.LOGGER.error("Cannot replace workstation - none in inventory!");
                        NotificationHelper.sendMessage("§cOut of workstations! Stopping reroll.");
                        stopRerolling();
                        return;
                    }

                    VillagerReroller.LOGGER.info("✓ Workstation found in inventory. Placing at {}... (attempt {})", currentJobSite, placementRetries + 1);

                    BlockPos originalAttemptPos = currentJobSite;
                    boolean replaced = jobSiteHandler.replaceJobSite(currentJobSite);

                    if (!replaced) {
                        VillagerReroller.LOGGER.error("Failed to place job site block at {}!", currentJobSite);
                        NotificationHelper.sendMessage("§cFailed to place job site! Check inventory for workstation.");
                        stopRerolling();
                        return;
                    }

                    // Find where the block was actually placed (might be alternative position)
                    BlockPos placedAt = findJobSiteBlockNear(originalAttemptPos, 2);
                    if (placedAt != null) {
                        currentJobSite = placedAt;
                        VillagerReroller.LOGGER.info("✓ Successfully placed workstation at {}", currentJobSite);
                    } else {
                        VillagerReroller.LOGGER.warn("Placement succeeded but couldn't locate block, using attempted position");
                    }

                    stateActionStarted = true;
                }

                // Wait longer for block placement to register on server
                if (timeSinceStateStart < 500) {
                    VillagerReroller.LOGGER.debug("Waiting for block placement to register... ({}ms)", timeSinceStateStart);
                    return;
                }

                // Verify the block was actually placed (with retry tolerance)
                if (currentJobSite != null && !jobSiteHandler.isJobSiteBlock(currentJobSite)) {
                    // Give it more time - server might be lagging (increased from 1.5s to 3s)
                    if (timeSinceStateStart < 3000) {
                        VillagerReroller.LOGGER.debug("Block not yet registered, waiting longer... ({}ms)", timeSinceStateStart);
                        return;
                    }

                    // Verification failed - try placing again up to 3 times
                    placementRetries++;
                    if (placementRetries < 3) {
                        VillagerReroller.LOGGER.warn("Block placement verification failed after 3s! Retrying... (attempt {}/3)", placementRetries + 1);
                        NotificationHelper.sendMessage("§6Placement verification failed, retrying... (" + (placementRetries + 1) + "/3)");

                        // Reset state to try placing again
                        stateActionStarted = false;
                        stateStartTime = System.currentTimeMillis();
                        return;
                    }

                    // All retries exhausted
                    VillagerReroller.LOGGER.error("Block placement failed after {} attempts! No job site at {}", placementRetries, currentJobSite);
                    NotificationHelper.sendMessage("§cBlock placement failed after " + placementRetries + " attempts! Server might be lagging.");
                    placementRetries = 0; // Reset for next reroll
                    stopRerolling();
                    return;
                }

                // Success! Reset retry counter
                placementRetries = 0;

                // Move to waiting for villager to claim it
                VillagerReroller.LOGGER.info("✓ Block placement verified. Waiting for villager to claim workstation...");
                transitionToState(RerollState.WAITING_FOR_VILLAGER);
                break;

            case WAITING_FOR_VILLAGER:
                // Wait for villager to claim the workstation and generate new trades
                // This can take 1-3 seconds depending on server lag
                // Villagers refresh their trades during work hours in-game

                // Minimum wait time before checking profession
                if (timeSinceStateStart < 2000) {
                    VillagerReroller.LOGGER.debug("Waiting for villager to claim workstation... ({}ms)", timeSinceStateStart);
                    return;
                }

                // Verify villager has claimed a profession
                boolean hasNoProfession = currentVillager.getVillagerData().profession().matchesKey(net.minecraft.village.VillagerProfession.NONE);
                VillagerReroller.LOGGER.debug("Checking villager profession: {}", currentVillager.getVillagerData().profession().getKey().orElse(null));

                if (hasNoProfession) {
                    // Villager hasn't claimed the workstation yet
                    if (timeSinceStateStart > 8000) {
                        VillagerReroller.LOGGER.error("Villager did not claim workstation after 8 seconds!");
                        NotificationHelper.sendMessage("§cVillager didn't claim workstation! Check placement and make sure villager is unemployed.");
                        stopRerolling();
                        return;
                    }
                    // Wait longer for villager to claim
                    VillagerReroller.LOGGER.debug("Villager still has no profession, waiting... ({}ms)", timeSinceStateStart);
                    return;
                }

                // Villager has a profession - ready to check trades
                VillagerReroller.LOGGER.info("✓ Villager has profession: {}. Ready to open trades.",
                    currentVillager.getVillagerData().profession().getKey().orElse(null));
                transitionToState(RerollState.OPENING_TRADES);
                break;

            case OPENING_TRADES:
                // CRITICAL: Verify villager has a profession before trying to open trades
                if (currentVillager.getVillagerData().profession().matchesKey(net.minecraft.village.VillagerProfession.NONE)) {
                    VillagerReroller.LOGGER.error("Villager lost profession before opening trades! Going back to wait state.");
                    NotificationHelper.sendMessage("§cVillager lost job! Waiting for villager to reclaim...");
                    transitionToState(RerollState.WAITING_FOR_VILLAGER);
                    return;
                }

                // Try to right-click the villager to open trading GUI
                VillagerReroller.LOGGER.debug("Attempting to open villager trades (attempt {}ms)", timeSinceStateStart);
                if (!openVillagerTrades()) {
                    if (timeSinceStateStart > 5000) {
                        VillagerReroller.LOGGER.error("Failed to open villager trades after 5 seconds!");
                        NotificationHelper.sendMessage("§cFailed to open villager GUI! Make sure you're close enough.");
                        stopRerolling();
                        return;
                    }
                    // Keep trying every tick until GUI opens or timeout
                    return;
                }

                VillagerReroller.LOGGER.info("✓ Trade GUI opened successfully, checking trades...");
                transitionToState(RerollState.CHECKING_TRADES);
                break;

            case CHECKING_TRADES:
                // Give GUI time to fully load and populate trades
                if (timeSinceStateStart < 200) {
                    return;
                }

                // Check if we're actually in the trading GUI
                if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.MerchantScreen)) {
                    if (timeSinceStateStart > 1500) {
                        VillagerReroller.LOGGER.warn("Not in merchant screen after 1.5 seconds, trying to open again...");
                        transitionToState(RerollState.OPENING_TRADES);
                        return;
                    }
                    // Wait a bit longer for GUI to open
                    return;
                }

                // Cast to MerchantScreen to access trade data
                net.minecraft.client.gui.screen.ingame.MerchantScreen merchantScreen =
                    (net.minecraft.client.gui.screen.ingame.MerchantScreen) client.currentScreen;

                // Check if trades are loaded
                if (merchantScreen.getScreenHandler().getRecipes() == null ||
                    merchantScreen.getScreenHandler().getRecipes().isEmpty()) {

                    if (timeSinceStateStart > 3000) {
                        VillagerReroller.LOGGER.warn("No trades available after 3 seconds, villager might not have refreshed yet");
                        // Close GUI and retry
                        if (client.player != null) {
                            client.player.closeHandledScreen();
                        }
                        transitionToState(RerollState.WAITING_FOR_VILLAGER);
                        return;
                    }
                    // Wait for trades to populate
                    VillagerReroller.LOGGER.debug("Waiting for trades to populate...");
                    return;
                }

                // Scan trades (one-time action)
                if (!stateActionStarted) {
                    VillagerReroller.LOGGER.info("Scanning trades...");
                    TradeScanner scanner = new TradeScanner();
                    List<TradeScanner.ScannedTrade> trades = scanner.scanCurrentTrades();

                    VillagerReroller.LOGGER.info("Found {} trades from villager", trades.size());

                    if (!trades.isEmpty()) {
                        // Log all trades for debugging
                        for (TradeScanner.ScannedTrade trade : trades) {
                            VillagerReroller.LOGGER.debug("  Trade {}: {}", trade.getSlotIndex(), trade.toString());
                        }

                        TradeFilter filter = new TradeFilter(config);

                        if (filter.hasAnyMatchingTrade(trades)) {
                            NotificationHelper.sendMessage("§aFound matching trade! Attempts: " + currentAttempts);
                            VillagerReroller.LOGGER.info("Found matching trade after {} attempts", currentAttempts);

                            // Record statistics
                            VillagerReroller.getInstance().getStatisticsTracker().recordSuccessfulReroll(currentAttempts);

                            // CRITICAL FIX: Close GUI immediately before stopping to prevent any further state transitions
                            if (client.player != null) {
                                client.player.closeHandledScreen();
                            }

                            stopRerolling();
                            return;
                        } else {
                            VillagerReroller.LOGGER.info("No matching trades found, continuing reroll");
                        }
                    } else {
                        VillagerReroller.LOGGER.warn("Scanner returned empty trade list!");
                    }

                    stateActionStarted = true;
                }

                // Wait a moment before closing GUI (only if we didn't find a match)
                if (timeSinceStateStart < 400) {
                    return;
                }

                // Close the GUI and continue rerolling
                if (client.player != null) {
                    client.player.closeHandledScreen();
                }

                VillagerReroller.LOGGER.info("Continuing to next reroll...");
                transitionToState(RerollState.WAITING_TO_BREAK);
                break;
        }
    }

    private BlockPos findJobSiteBlock() {
        if (currentVillager == null) {
            return null;
        }

        // Search for nearby job site blocks
        BlockPos villagerPos = currentVillager.getBlockPos();

        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = villagerPos.add(x, y, z);
                    if (jobSiteHandler.isJobSiteBlock(pos)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find a job site block near a specific position within given radius
     */
    private BlockPos findJobSiteBlockNear(BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (jobSiteHandler.isJobSiteBlock(pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isInventoryFull() {
        if (client.player == null) {
            return false;
        }

        return client.player.getInventory().getEmptySlot() == -1;
    }

    private boolean openVillagerTrades() {
        if (currentVillager == null || client.interactionManager == null || client.player == null) {
            return false;
        }

        try {
            // Check distance to villager
            double distance = client.player.distanceTo(currentVillager);
            if (distance > 4.5) {
                VillagerReroller.LOGGER.warn("Villager is too far away: {} blocks", String.format("%.2f", distance));
                return false;
            }

            // Make player look at the villager for proper interaction
            net.minecraft.util.math.Vec3d villagerPos = currentVillager.getPos();
            net.minecraft.util.math.Vec3d playerPos = client.player.getPos();
            net.minecraft.util.math.Vec3d eyePos = playerPos.add(0, client.player.getEyeHeight(client.player.getPose()), 0);

            // Calculate yaw and pitch to look at villager
            double deltaX = villagerPos.x - eyePos.x;
            double deltaY = villagerPos.y + currentVillager.getEyeHeight(currentVillager.getPose()) - eyePos.y;
            double deltaZ = villagerPos.z - eyePos.z;

            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) -(Math.atan2(deltaY, horizontalDistance) * (180.0 / Math.PI));

            // Set player rotation to face villager
            client.player.setYaw(yaw);
            client.player.setPitch(pitch);

            VillagerReroller.LOGGER.debug("Facing villager at distance {} with yaw={}, pitch={}",
                String.format("%.2f", distance), String.format("%.1f", yaw), String.format("%.1f", pitch));

            // Interact with the villager to open trading GUI
            client.interactionManager.interactEntity(
                client.player,
                currentVillager,
                net.minecraft.util.Hand.MAIN_HAND
            );

            // Check if GUI actually opened (may take a tick or two)
            // Don't use Thread.sleep as it blocks the client thread!
            if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.MerchantScreen) {
                VillagerReroller.LOGGER.info("Successfully opened villager trading GUI");
                return true;
            } else {
                VillagerReroller.LOGGER.debug("GUI not yet open, will retry next tick");
                return false;
            }

        } catch (Exception e) {
            VillagerReroller.LOGGER.error("Failed to open villager trades", e);
            return false;
        }
    }

    private VillagerState getOrCreateState(VillagerEntity villager) {
        UUID uuid = villager.getUuid();
        return villagerStates.computeIfAbsent(uuid, k -> new VillagerState());
    }

    /**
     * Find a suitable position to place a workstation near the villager
     */
    private BlockPos findSuitablePlacementPosition(BlockPos villagerPos) {
        if (client.world == null) {
            return null;
        }

        // Try positions in expanding circles around the villager
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only check perimeter of current radius
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    BlockPos testPos = villagerPos.add(x, 0, z);

                    // Check if this is a valid placement position
                    if (jobSiteHandler.isValidPlacementPosition(testPos)) {
                        return testPos;
                    }
                }
            }
        }

        return null;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getCurrentAttempts() {
        return currentAttempts;
    }

    public VillagerEntity getCurrentVillager() {
        return currentVillager;
    }

    private static class VillagerState {
        long lastAttemptTime = 0;
        int totalAttempts = 0;
        boolean locked = false;
    }
}
