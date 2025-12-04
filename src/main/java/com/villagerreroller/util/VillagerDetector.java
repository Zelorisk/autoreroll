package com.villagerreroller.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class VillagerDetector {
    private static final double MAX_DETECTION_DISTANCE = 10.0;
    private static final double NEARBY_RADIUS = 5.0;

    private final MinecraftClient client;

    public VillagerDetector() {
        this.client = MinecraftClient.getInstance();
    }

    /**
     * Get the villager the player is currently looking at (crosshair targeted)
     */
    public Optional<VillagerEntity> getTargetedVillager() {
        if (client.player == null || client.world == null) {
            return Optional.empty();
        }

        HitResult hitResult = client.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof VillagerEntity villager) {
                return Optional.of(villager);
            }
        }

        return Optional.empty();
    }

    /**
     * Get the nearest villager to the player within detection range
     */
    public Optional<VillagerEntity> getNearestVillager() {
        if (client.player == null || client.world == null) {
            return Optional.empty();
        }

        World world = client.world;
        Box searchBox = client.player.getBoundingBox().expand(MAX_DETECTION_DISTANCE);

        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            villager -> villager.isAlive() && !villager.isBaby()
        );

        if (nearbyVillagers.isEmpty()) {
            return Optional.empty();
        }

        // Find the closest villager
        VillagerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (VillagerEntity villager : nearbyVillagers) {
            double distance = client.player.distanceTo(villager);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = villager;
            }
        }

        return Optional.ofNullable(closest);
    }

    /**
     * Get all villagers within a specific radius of the player
     */
    public List<VillagerEntity> getNearbyVillagers(double radius) {
        if (client.player == null || client.world == null) {
            return List.of();
        }

        World world = client.world;
        Box searchBox = client.player.getBoundingBox().expand(radius);

        return world.getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            villager -> villager.isAlive() && !villager.isBaby()
        );
    }

    /**
     * Get all nearby villagers (within default radius)
     */
    public List<VillagerEntity> getNearbyVillagers() {
        return getNearbyVillagers(NEARBY_RADIUS);
    }

    /**
     * Get the best villager to interact with - prefers targeted, then nearest
     */
    public Optional<VillagerEntity> getBestVillagerToReroll() {
        // First try to get the targeted villager
        Optional<VillagerEntity> targeted = getTargetedVillager();
        if (targeted.isPresent()) {
            return targeted;
        }

        // Fall back to nearest villager
        return getNearestVillager();
    }

    /**
     * Check if there's a villager within interaction range
     */
    public boolean hasVillagerInRange() {
        return getBestVillagerToReroll().isPresent();
    }
}
