package com.villagerreroller.trade;

import com.villagerreroller.config.ModConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.HashMap;
import java.util.Map;

public class TradeEvaluator {
    private final ModConfig config;
    private final Map<String, Integer> enchantmentPriority;

    public TradeEvaluator(ModConfig config) {
        this.config = config;
        this.enchantmentPriority = initializeEnchantmentPriority();
    }

    private Map<String, Integer> initializeEnchantmentPriority() {
        Map<String, Integer> priority = new HashMap<>();

        // High priority enchantments
        priority.put("minecraft:mending", 100);
        priority.put("minecraft:unbreaking", 90);
        priority.put("minecraft:fortune", 95);
        priority.put("minecraft:silk_touch", 90);
        priority.put("minecraft:efficiency", 85);
        priority.put("minecraft:sharpness", 85);
        priority.put("minecraft:looting", 90);
        priority.put("minecraft:protection", 85);

        // Medium priority enchantments
        priority.put("minecraft:feather_falling", 70);
        priority.put("minecraft:respiration", 65);
        priority.put("minecraft:aqua_affinity", 65);
        priority.put("minecraft:depth_strider", 70);
        priority.put("minecraft:frost_walker", 60);
        priority.put("minecraft:swift_sneak", 75);
        priority.put("minecraft:soul_speed", 75);

        // Weapon enchantments
        priority.put("minecraft:fire_aspect", 60);
        priority.put("minecraft:knockback", 50);
        priority.put("minecraft:sweeping", 55);

        // Bow enchantments
        priority.put("minecraft:power", 70);
        priority.put("minecraft:punch", 60);
        priority.put("minecraft:flame", 65);
        priority.put("minecraft:infinity", 80);

        // Other useful enchantments
        priority.put("minecraft:channeling", 70);
        priority.put("minecraft:riptide", 70);
        priority.put("minecraft:loyalty", 65);
        priority.put("minecraft:impaling", 60);

        return priority;
    }

    public TradeScore evaluateTrade(TradeScanner.ScannedTrade trade) {
        int score = 0;
        StringBuilder reason = new StringBuilder();

        // Base score: lower emerald cost is better
        int maxCost = getMaxCostForItem(trade);
        int emeraldCost = trade.getEmeraldCost();

        if (emeraldCost <= maxCost) {
            int costScore = (maxCost - emeraldCost) * 10;
            score += costScore;
            reason.append(String.format("Cost: %d emeralds (+%d), ", emeraldCost, costScore));
        } else {
            reason.append(String.format("Cost: %d emeralds (too expensive), ", emeraldCost));
        }

        // Enchantment score
        if (!trade.getEnchantments().isEmpty()) {
            int enchantScore = calculateEnchantmentScore(trade);
            score += enchantScore;
            reason.append(String.format("Enchantments (+%d), ", enchantScore));
        }

        // First slot bonus
        if (trade.getSlotIndex() == 0 && config.getPreferredFirstSlotItems().contains(trade.getItemId())) {
            score += 20;
            reason.append("First slot bonus (+20), ");
        }

        // Whitelist bonus
        if (config.getItemWhitelist().contains(trade.getItemId())) {
            score += 30;
            reason.append("Whitelisted item (+30), ");
        }

        // Remove trailing comma
        if (reason.length() > 0) {
            reason.setLength(reason.length() - 2);
        }

        return new TradeScore(trade, score, reason.toString());
    }

    private int calculateEnchantmentScore(TradeScanner.ScannedTrade trade) {
        int[] scoreRef = {0}; // Use array to modify from lambda

        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : trade.getEnchantments().entrySet()) {
            RegistryEntry<Enchantment> enchantEntry = entry.getKey();
            int level = entry.getValue();

            enchantEntry.getKey().ifPresent(key -> {
                String enchantId = key.getValue().toString();

                // Get base priority
                int priority = enchantmentPriority.getOrDefault(enchantId, 50);

                // Multiply by level
                int enchantScore = priority * level;

                // Bonus for max level
                Enchantment enchantment = enchantEntry.value();
                if (level == enchantment.getMaxLevel()) {
                    enchantScore += 20;
                }

                scoreRef[0] += enchantScore;
            });
        }

        return scoreRef[0];
    }

    private int getMaxCostForItem(TradeScanner.ScannedTrade trade) {
        if (trade.isEnchantedBook()) {
            return config.getMaxEmeraldsBooks();
        }

        String itemId = trade.getItemId();

        if (isToolItem(itemId)) {
            return config.getMaxEmeraldsTools();
        }

        if (isArmorItem(itemId)) {
            return config.getMaxEmeraldsArmor();
        }

        return config.getMaxEmeraldsMisc();
    }

    private boolean isToolItem(String itemId) {
        return itemId.contains("_pickaxe") ||
                itemId.contains("_axe") ||
                itemId.contains("_shovel") ||
                itemId.contains("_hoe") ||
                itemId.contains("_sword");
    }

    private boolean isArmorItem(String itemId) {
        return itemId.contains("_helmet") ||
                itemId.contains("_chestplate") ||
                itemId.contains("_leggings") ||
                itemId.contains("_boots");
    }

    public static class TradeScore implements Comparable<TradeScore> {
        private final TradeScanner.ScannedTrade trade;
        private final int score;
        private final String reason;

        public TradeScore(TradeScanner.ScannedTrade trade, int score, String reason) {
            this.trade = trade;
            this.score = score;
            this.reason = reason;
        }

        public TradeScanner.ScannedTrade getTrade() {
            return trade;
        }

        public int getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }

        public String getGrade() {
            if (score >= 200) return "S";
            if (score >= 150) return "A";
            if (score >= 100) return "B";
            if (score >= 50) return "C";
            return "D";
        }

        @Override
        public int compareTo(TradeScore other) {
            return Integer.compare(other.score, this.score); // Descending order
        }

        @Override
        public String toString() {
            return String.format("Score: %d [%s] - %s", score, getGrade(), reason);
        }
    }
}
