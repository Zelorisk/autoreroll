package com.villagerreroller.util;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class VillagerDetector {

    private static final double MAX_DETECTION_DISTANCE = 10.0;
    private static final double NEARBY_RADIUS = 5.0;

    private final MinecraftClient client;

    public VillagerDetector() {
        this.client = MinecraftClient.getInstance();
    }

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

    public Optional<VillagerEntity> getNearestVillager() {
        if (client.player == null || client.world == null) {
            return Optional.empty();
        }

        World world = client.world;
        Box searchBox = client.player
            .getBoundingBox()
            .expand(MAX_DETECTION_DISTANCE);

        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            villager -> villager.isAlive() && !villager.isBaby()
        );

        if (nearbyVillagers.isEmpty()) {
            return Optional.empty();
        }

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

    public List<VillagerEntity> getNearbyVillagers() {
        return getNearbyVillagers(NEARBY_RADIUS);
    }

    public Optional<VillagerEntity> getBestVillagerToReroll() {
        Optional<VillagerEntity> targeted = getTargetedVillager();
        if (targeted.isPresent()) {
            return targeted;
        }

        return getNearestVillager();
    }

    public boolean hasVillagerInRange() {
        return getBestVillagerToReroll().isPresent();
    }
}
