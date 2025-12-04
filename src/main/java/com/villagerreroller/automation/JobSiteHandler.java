package com.villagerreroller.automation;

import com.villagerreroller.VillagerReroller;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobSiteHandler {
    private final MinecraftClient client;
    private final Map<BlockPos, Block> rememberedBlocks;
    private BlockPos lastBrokenPos = null;
    private long lastBreakTime = 0;
    private ItemEntity droppedItem = null;

    // Block breaking state
    private BlockPos currentlyBreaking = null;
    private long breakStartTime = 0;
    private int breakingTickCount = 0;
    private int originalHotbarSlot = -1; // Track original slot before equipping axe

    // Item pickup pathfinding state
    private Vec3d lastPlayerPos = null;
    private long lastProgressTime = 0;
    private double lastDistanceToItem = Double.MAX_VALUE;
    private int stuckTicks = 0;

    // Villager job site blocks
    private static final Block[] JOB_SITE_BLOCKS = {
            Blocks.LECTERN,           // Librarian
            Blocks.BLAST_FURNACE,     // Armorer
            Blocks.SMOKER,            // Butcher
            Blocks.CARTOGRAPHY_TABLE, // Cartographer
            Blocks.BREWING_STAND,     // Cleric
            Blocks.COMPOSTER,         // Farmer
            Blocks.BARREL,            // Fisherman
            Blocks.FLETCHING_TABLE,   // Fletcher
            Blocks.CAULDRON,          // Leatherworker
            Blocks.STONECUTTER,       // Mason
            Blocks.LOOM,              // Shepherd
            Blocks.SMITHING_TABLE,    // Toolsmith
            Blocks.GRINDSTONE         // Weaponsmith
    };

    public JobSiteHandler() {
        this.client = MinecraftClient.getInstance();
        this.rememberedBlocks = new HashMap<>();
    }

    public boolean isJobSiteBlock(BlockPos pos) {
        World world = client.world;
        if (world == null) {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();

        for (Block jobSiteBlock : JOB_SITE_BLOCKS) {
            if (block == jobSiteBlock) {
                return true;
            }
        }

        return false;
    }

    /**
     * Start breaking a job site block. Returns true immediately after starting.
     * Call continueBreaking() to continue the breaking process over multiple ticks.
     */
    public boolean startBreakingJobSite(BlockPos pos) {
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (interactionManager == null || player == null || world == null) {
            VillagerReroller.LOGGER.warn("Cannot break job site - client not ready");
            return false;
        }

        // Remember the block type for replacement
        Block block = world.getBlockState(pos).getBlock();
        rememberedBlocks.put(pos, block);

        // Save original hotbar slot before switching
        originalHotbarSlot = player.getInventory().getSelectedSlot();

        // Find and equip an axe for faster breaking
        int axeSlot = findAxeInHotbar();

        if (axeSlot != -1) {
            player.getInventory().setSelectedSlot(axeSlot);
            VillagerReroller.LOGGER.info("Equipped axe from slot {} to break block faster (original: {})", axeSlot, originalHotbarSlot);
        } else {
            VillagerReroller.LOGGER.warn("No axe found in hotbar, breaking with current tool");
        }

        // Start breaking
        try {
            interactionManager.attackBlock(pos, Direction.UP);

            currentlyBreaking = pos;
            breakStartTime = System.currentTimeMillis();
            breakingTickCount = 0;

            VillagerReroller.LOGGER.info("Started breaking job site block at {}: {}", pos, block);
            return true;
        } catch (Exception e) {
            VillagerReroller.LOGGER.error("Failed to start breaking job site block", e);
            restoreOriginalHotbarSlot(); // Restore on error
            return false;
        }
    }

    /**
     * Continue breaking the current block. Returns true when block is broken, false while still breaking.
     */
    public boolean continueBreaking() {
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (currentlyBreaking == null || interactionManager == null || player == null || world == null) {
            return false;
        }

        BlockState state = world.getBlockState(currentlyBreaking);

        // Check if block is already broken
        if (state.isAir()) {
            VillagerReroller.LOGGER.info("Block broken successfully after {} ticks", breakingTickCount);
            lastBrokenPos = currentlyBreaking;
            lastBreakTime = System.currentTimeMillis();
            currentlyBreaking = null;
            breakingTickCount = 0;
            restoreOriginalHotbarSlot(); // Restore original slot after breaking
            return true;
        }

        // Continue breaking
        try {
            interactionManager.updateBlockBreakingProgress(currentlyBreaking, Direction.UP);
            breakingTickCount++;

            // Timeout after 5 seconds (100 ticks) to prevent infinite loop
            if (breakingTickCount > 100) {
                VillagerReroller.LOGGER.error("Breaking timed out after 100 ticks, forcing break");
                // Try to force break with a direct attack
                interactionManager.attackBlock(currentlyBreaking, Direction.UP);
                currentlyBreaking = null;
                restoreOriginalHotbarSlot(); // Restore slot on timeout
                return false;
            }

            return false; // Still breaking
        } catch (Exception e) {
            VillagerReroller.LOGGER.error("Failed to continue breaking block", e);
            currentlyBreaking = null;
            restoreOriginalHotbarSlot(); // Restore slot on exception
            return false;
        }
    }

    /**
     * Cancel current breaking operation
     */
    public void cancelBreaking() {
        if (currentlyBreaking != null) {
            ClientPlayerInteractionManager interactionManager = client.interactionManager;
            if (interactionManager != null) {
                interactionManager.cancelBlockBreaking();
            }
            currentlyBreaking = null;
            breakingTickCount = 0;
            restoreOriginalHotbarSlot(); // Restore original slot when cancelling
        }
    }

    /**
     * Restore the original hotbar slot that was saved before equipping the axe
     */
    private void restoreOriginalHotbarSlot() {
        if (originalHotbarSlot >= 0 && originalHotbarSlot < 9 && client.player != null) {
            client.player.getInventory().setSelectedSlot(originalHotbarSlot);
            VillagerReroller.LOGGER.debug("Restored original hotbar slot: {}", originalHotbarSlot);
            originalHotbarSlot = -1; // Reset
        }
    }

    public boolean isBreaking() {
        return currentlyBreaking != null;
    }

    private int findAxeInHotbar() {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return -1;
        }

        // Search hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }

        return -1;
    }

    public boolean replaceJobSite(BlockPos originalPos) {
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (interactionManager == null || player == null || world == null) {
            VillagerReroller.LOGGER.warn("Cannot replace job site - client not ready");
            return false;
        }

        // Get the remembered block type
        Block block = rememberedBlocks.get(originalPos);
        if (block == null) {
            VillagerReroller.LOGGER.warn("No remembered block type for position {}", originalPos);
            return false;
        }

        // Find the block in player's inventory
        int slot = findBlockInInventory(block);
        if (slot == -1) {
            VillagerReroller.LOGGER.warn("Block {} not found in inventory", block);
            return false;
        }

        // First, try to clear any old lecterns at alternative positions to avoid confusion
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = originalPos.add(x, 0, z);
                if (!checkPos.equals(originalPos) && isJobSiteBlock(checkPos)) {
                    VillagerReroller.LOGGER.info("Found old job site at {}, breaking it to avoid villager confusion", checkPos);
                    // Break the old lectern to clear it
                    if (interactionManager != null) {
                        interactionManager.attackBlock(checkPos, Direction.UP);
                        // Wait a moment for break
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                }
            }
        }

        // Try original position first, then nearby positions if blocked
        BlockPos[] positionsToTry = {
            originalPos,                          // Original position
            originalPos.add(1, 0, 0),            // East
            originalPos.add(-1, 0, 0),           // West
            originalPos.add(0, 0, 1),            // South
            originalPos.add(0, 0, -1)            // North
            // Removed diagonal positions to keep lecterns closer
        };

        // Save current slot
        int originalSlot = player.getInventory().getSelectedSlot();

        try {
            // Switch to the slot with the block (hotbar slots 0-8)
            if (slot >= 9) {
                // If item is not in hotbar, move it there
                VillagerReroller.LOGGER.info("Moving block from slot {} to hotbar", slot);
                player.getInventory().setSelectedSlot(0);
                interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, 0,
                    net.minecraft.screen.slot.SlotActionType.SWAP, player);
                slot = 0;
            }

            player.getInventory().setSelectedSlot(slot);
            VillagerReroller.LOGGER.debug("Selected hotbar slot {} with block {}", slot, block);

            // Try each position until one succeeds
            for (int i = 0; i < positionsToTry.length; i++) {
                BlockPos testPos = positionsToTry[i];

                // Check if position is valid
                if (!world.getBlockState(testPos).isAir()) {
                    if (i == 0) {
                        VillagerReroller.LOGGER.warn("Original position {} is not air, trying alternatives...", testPos);
                    }
                    continue; // Position blocked, try next
                }

                // Check for entities blocking this position
                Box checkBox = new Box(testPos);
                List<Entity> blockingEntities = world.getOtherEntities(null, checkBox);
                if (!blockingEntities.isEmpty()) {
                    VillagerReroller.LOGGER.debug("Position {} blocked by {} entities, trying next...", testPos, blockingEntities.size());
                    continue;
                }

                // Position looks good, try placing
                VillagerReroller.LOGGER.info("Attempting to place block at {} (position {})", testPos, i == 0 ? "original" : "alternative " + i);

                BlockHitResult hitResult = new BlockHitResult(
                        Vec3d.ofCenter(testPos),
                        Direction.UP,
                        testPos.down(),
                        false
                );

                interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);

                // Wait a tick for placement to register, then verify
                try {
                    Thread.sleep(50); // 50ms = ~1 tick
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Verify the block was actually placed
                if (isJobSiteBlock(testPos)) {
                    VillagerReroller.LOGGER.info("✓ Successfully placed job site block at {}: {}", testPos, block);

                    // Update remembered position if we used alternative
                    if (!testPos.equals(originalPos)) {
                        VillagerReroller.LOGGER.info("Updated job site position from {} to {}", originalPos, testPos);
                        rememberedBlocks.remove(originalPos);
                        rememberedBlocks.put(testPos, block);
                        lastBrokenPos = testPos; // Update for pickup tracking
                    }

                    // Restore original slot
                    player.getInventory().setSelectedSlot(originalSlot);
                    return true;
                } else {
                    VillagerReroller.LOGGER.warn("Placement at {} reported success but block not found, trying next position...", testPos);
                }
            }

            // All positions failed
            VillagerReroller.LOGGER.error("Failed to place block at any position (tried {} locations)", positionsToTry.length);
            player.getInventory().setSelectedSlot(originalSlot);
            return false;

        } catch (Exception e) {
            VillagerReroller.LOGGER.error("Exception during block placement", e);
            player.getInventory().setSelectedSlot(originalSlot);
            return false;
        }
    }



    private int findBlockInInventory(Block block) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return -1;
        }

        for (int i = 0; i < player.getInventory().size(); i++) {
            if (Block.getBlockFromItem(player.getInventory().getStack(i).getItem()) == block) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Place an initial workstation. Tries to find ANY workstation block in inventory.
     */
    public boolean placeInitialJobSite(BlockPos pos) {
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (interactionManager == null || player == null || world == null) {
            VillagerReroller.LOGGER.warn("Cannot place initial job site - client not ready");
            return false;
        }

        // Find any job site block in inventory
        int slot = -1;
        Block foundBlock = null;

        for (Block jobSiteBlock : JOB_SITE_BLOCKS) {
            slot = findBlockInInventory(jobSiteBlock);
            if (slot != -1) {
                foundBlock = jobSiteBlock;
                break;
            }
        }

        if (slot == -1 || foundBlock == null) {
            VillagerReroller.LOGGER.warn("No workstation blocks found in inventory");
            return false;
        }

        // Remember this block for later
        rememberedBlocks.put(pos, foundBlock);

        // Save current slot
        int originalSlot = player.getInventory().getSelectedSlot();

        try {
            // Move to hotbar if needed
            if (slot >= 9) {
                player.getInventory().setSelectedSlot(0);
                interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, 0,
                    net.minecraft.screen.slot.SlotActionType.SWAP, player);
                slot = 0;
            }

            player.getInventory().setSelectedSlot(slot);

            // Place the block
            BlockHitResult hitResult = new BlockHitResult(
                    Vec3d.ofCenter(pos),
                    Direction.UP,
                    pos.down(),
                    false
            );

            interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);

            VillagerReroller.LOGGER.info("Placed initial workstation at {}: {}", pos, foundBlock);

            // Restore original slot
            player.getInventory().setSelectedSlot(originalSlot);

            return true;

        } catch (Exception e) {
            VillagerReroller.LOGGER.error("Failed to place initial workstation", e);
            player.getInventory().setSelectedSlot(originalSlot);
            return false;
        }
    }

    /**
     * Try to pick up dropped item. Returns:
     * 1 = success (item in inventory)
     * 0 = still trying (item exists but not picked up yet)
     * -1 = error (no item found and not in inventory)
     */
    public int tryPickupItem() {
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (player == null || world == null || lastBrokenPos == null) {
            VillagerReroller.LOGGER.warn("Cannot check item pickup - missing client/world/position");
            resetPickupState();
            return -1;
        }

        Block expectedBlock = rememberedBlocks.get(lastBrokenPos);
        if (expectedBlock == null) {
            VillagerReroller.LOGGER.warn("No remembered block for position {}", lastBrokenPos);
            resetPickupState();
            return 1; // Assume picked up
        }

        // Check if player has the item in inventory (already picked up)
        if (findBlockInInventory(expectedBlock) != -1) {
            VillagerReroller.LOGGER.debug("Item confirmed in inventory - pickup successful");
            resetPickupState();
            return 1;
        }

        // Look for dropped item entities near the broken position
        Box searchBox = new Box(lastBrokenPos).expand(10.0); // Increased search range
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, searchBox,
            item -> Block.getBlockFromItem(item.getStack().getItem()) == expectedBlock);

        if (items.isEmpty()) {
            // No items on ground and not in inventory - might be creative mode or item disappeared
            VillagerReroller.LOGGER.debug("No dropped items found, assuming picked up or creative mode");
            resetPickupState();
            return 1;
        }

        // Find the closest item
        ItemEntity closestItem = null;
        double closestDist = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            double dist = player.squaredDistanceTo(item);
            if (dist < closestDist) {
                closestDist = dist;
                closestItem = item;
            }
        }

        if (closestItem != null) {
            double distance = Math.sqrt(closestDist);
            Vec3d itemPos = closestItem.getPos();
            Vec3d playerPos = player.getPos();

            // Initialize tracking on first attempt
            if (lastPlayerPos == null) {
                lastPlayerPos = playerPos;
                lastProgressTime = System.currentTimeMillis();
                lastDistanceToItem = distance;
                stuckTicks = 0;
            }

            // Check if player is making progress towards the item
            double playerMovement = playerPos.distanceTo(lastPlayerPos);
            boolean madeProgress = distance < lastDistanceToItem - 0.1; // Made progress if 0.1 blocks closer

            // Update progress tracking every 10 ticks (~0.5 seconds)
            if (System.currentTimeMillis() - lastProgressTime > 500) {
                if (!madeProgress && playerMovement < 0.1) {
                    // Player hasn't moved much and isn't getting closer - might be stuck
                    stuckTicks++;
                    VillagerReroller.LOGGER.debug("Player appears stuck (stuck ticks: {}), distance: {}", stuckTicks, String.format("%.2f", distance));
                } else {
                    // Made progress, reset stuck counter
                    stuckTicks = 0;
                }

                lastPlayerPos = playerPos;
                lastDistanceToItem = distance;
                lastProgressTime = System.currentTimeMillis();
            }

            // Make player face the item
            Vec3d eyePos = playerPos.add(0, player.getEyeHeight(player.getPose()), 0);

            double deltaX = itemPos.x - eyePos.x;
            double deltaY = itemPos.y - eyePos.y;
            double deltaZ = itemPos.z - eyePos.z;

            double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) -(Math.atan2(deltaY, horizontalDist) * (180.0 / Math.PI));

            player.setYaw(yaw);
            player.setPitch(pitch);

            // Items are automatically picked up within ~1.5 blocks
            if (distance <= 1.8) {
                // Still move closer if not super close
                if (distance > 0.5) {
                    VillagerReroller.LOGGER.debug("Item is {} blocks away, moving closer for pickup...", String.format("%.2f", distance));
                    movePlayerTowards(player, itemPos, 0.15, stuckTicks >= 2);
                } else {
                    VillagerReroller.LOGGER.debug("Item is {} blocks away, waiting for automatic pickup...", String.format("%.2f", distance));
                }
                return 0; // Wait for automatic pickup
            } else if (distance <= 10.0) {
                // Item is within reasonable range - move towards it
                VillagerReroller.LOGGER.debug("Item is {} blocks away, moving towards it... (stuck: {})", String.format("%.2f", distance), stuckTicks);

                // Move with jump if stuck
                movePlayerTowards(player, itemPos, 0.2, stuckTicks >= 2);

                // Reset stuck counter after jump
                if (stuckTicks >= 2) {
                    stuckTicks = 0;
                }

                return 0; // Still trying to reach it
            } else {
                // Item is too far away (> 10 blocks) - something went wrong
                VillagerReroller.LOGGER.warn("Item is {} blocks away - very far from expected position!", String.format("%.2f", distance));

                // Still try to move towards it slowly
                movePlayerTowards(player, itemPos, 0.15, stuckTicks >= 2);

                // Reset stuck counter after jump
                if (stuckTicks >= 2) {
                    stuckTicks = 0;
                }

                return 0; // Still trying
            }
        }

        resetPickupState();
        return 1; // No items found, assume success
    }

    /**
     * Reset item pickup pathfinding state (public method for controller)
     */
    public void resetPickupState() {
        lastPlayerPos = null;
        lastProgressTime = 0;
        lastDistanceToItem = Double.MAX_VALUE;
        stuckTicks = 0;
    }

    /**
     * Clear player movement - no-op for now since we're using velocity
     */
    public void clearMovementInput() {
        // Empty method - kept for API compatibility
    }

    /**
     * Move player towards a target position using velocity
     */
    private void movePlayerTowards(ClientPlayerEntity player, Vec3d targetPos, double speed, boolean shouldJump) {
        Vec3d playerPos = player.getPos();
        Vec3d direction = targetPos.subtract(playerPos).normalize();

        // Calculate velocity towards the item
        Vec3d targetVelocity = direction.multiply(speed, 0, speed);

        // Apply velocity with jump if stuck
        if (shouldJump && player.isOnGround()) {
            VillagerReroller.LOGGER.info("Player stuck, attempting to jump over obstacle");
            player.setVelocity(targetVelocity.x, 0.42, targetVelocity.z); // Jump velocity
        } else {
            // Normal movement - preserve Y velocity for gravity
            player.setVelocity(targetVelocity.x, player.getVelocity().y, targetVelocity.z);
        }

        // Also add a small direct position adjustment to ensure movement
        if (player.isOnGround() && !shouldJump) {
            // Add tiny position nudge to help overcome friction
            Vec3d nudge = direction.multiply(0.02, 0, 0.02);
            player.setPosition(player.getX() + nudge.x, player.getY(), player.getZ() + nudge.z);
        }
    }

    /**
     * Check if a position is valid for placing a workstation
     */
    public boolean isValidPlacementPosition(BlockPos pos) {
        World world = client.world;
        if (world == null) {
            return false;
        }

        // Check if the block below is solid
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return false;
        }

        // Check if the position itself is air
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        // Check if there are no entities blocking
        Box checkBox = new Box(pos);
        List<Entity> entities = world.getOtherEntities(null, checkBox);

        return entities.isEmpty();
    }

    /**
     * Check if player has ANY workstation block in inventory
     */
    public boolean hasWorkstationInInventory() {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return false;
        }

        for (Block jobSiteBlock : JOB_SITE_BLOCKS) {
            if (findBlockInInventory(jobSiteBlock) != -1) {
                return true;
            }
        }

        return false;
    }

    public Block getRememberedBlock(BlockPos pos) {
        return rememberedBlocks.get(pos);
    }

    public void clearRememberedBlocks() {
        rememberedBlocks.clear();
    }
}
