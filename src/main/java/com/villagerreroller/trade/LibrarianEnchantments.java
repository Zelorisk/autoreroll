package com.villagerreroller.trade;

import java.util.ArrayList;
import java.util.List;

public class LibrarianEnchantments {

    public static final List<EnchantmentOption> ALL_ENCHANTMENTS = new ArrayList<>();

    static {
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:mending", "Mending", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:unbreaking", "Unbreaking", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:protection", "Protection", 4));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:fire_protection", "Fire Protection", 4));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:blast_protection", "Blast Protection", 4));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:projectile_protection", "Projectile Protection", 4));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:feather_falling", "Feather Falling", 4));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:thorns", "Thorns", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:respiration", "Respiration", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:aqua_affinity", "Aqua Affinity", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:depth_strider", "Depth Strider", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:frost_walker", "Frost Walker", 2));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:sharpness", "Sharpness", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:smite", "Smite", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:bane_of_arthropods", "Bane of Arthropods", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:knockback", "Knockback", 2));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:fire_aspect", "Fire Aspect", 2));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:looting", "Looting", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:sweeping_edge", "Sweeping Edge", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:efficiency", "Efficiency", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:silk_touch", "Silk Touch", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:fortune", "Fortune", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:power", "Power", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:punch", "Punch", 2));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:flame", "Flame", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:infinity", "Infinity", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:luck_of_the_sea", "Luck of the Sea", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:lure", "Lure", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:loyalty", "Loyalty", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:impaling", "Impaling", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:riptide", "Riptide", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:channeling", "Channeling", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:multishot", "Multishot", 1));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:quick_charge", "Quick Charge", 3));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:piercing", "Piercing", 4));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:density", "Density", 5));
        ALL_ENCHANTMENTS.add(new EnchantmentOption("minecraft:breach", "Breach", 4));
    }

    public static class EnchantmentOption {
        private final String id;
        private final String displayName;
        private final int maxLevel;

        public EnchantmentOption(String id, String displayName, int maxLevel) {
            this.id = id;
            this.displayName = displayName;
            this.maxLevel = maxLevel;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public String getFilterString(int level) {
            return id + ":" + level;
        }

        public String getDisplayString(int level) {
            if (maxLevel == 1) {
                return displayName;
            }
            return displayName + " " + toRoman(level);
        }

        private static String toRoman(int num) {
            return switch (num) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> String.valueOf(num);
            };
        }
    }

    public static int getIndex(String enchantmentId, int level) {
        for (int i = 0; i < ALL_ENCHANTMENTS.size(); i++) {
            EnchantmentOption opt = ALL_ENCHANTMENTS.get(i);
            if (opt.getId().equals(enchantmentId)) {
                return i;
            }
        }
        return 0;
    }

    public static EnchantmentOption getByIndex(int index) {
        if (index < 0 || index >= ALL_ENCHANTMENTS.size()) {
            return ALL_ENCHANTMENTS.get(0);
        }
        return ALL_ENCHANTMENTS.get(index);
    }
}
