package com.villagerreroller.trade;

import com.villagerreroller.VillagerReroller;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class TradeScanner {

    private final MinecraftClient client;
    private List<ScannedTrade> lastScannedTrades;
    private long lastScanTime;

    public TradeScanner() {
        this.client = MinecraftClient.getInstance();
        this.lastScannedTrades = new ArrayList<>();
        this.lastScanTime = 0;
    }

    public List<ScannedTrade> scanCurrentTrades() {
        List<ScannedTrade> trades = new ArrayList<>();

        if (client.currentScreen instanceof MerchantScreen merchantScreen) {
            try {
                TradeOfferList tradeOffers = merchantScreen
                    .getScreenHandler()
                    .getRecipes();

                for (int i = 0; i < tradeOffers.size(); i++) {
                    TradeOffer offer = tradeOffers.get(i);
                    ScannedTrade scannedTrade = new ScannedTrade(i, offer);
                    trades.add(scannedTrade);
                }

                lastScannedTrades = trades;
                lastScanTime = System.currentTimeMillis();

                VillagerReroller.LOGGER.debug(
                    "Scanned {} trades from villager",
                    trades.size()
                );
            } catch (Exception e) {
                VillagerReroller.LOGGER.error("Failed to scan trades", e);
            }
        }

        return trades;
    }

    public List<ScannedTrade> getLastScannedTrades() {
        return lastScannedTrades;
    }

    public long getLastScanTime() {
        return lastScanTime;
    }

    public static class ScannedTrade {

        private final int slotIndex;
        private final TradeOffer offer;
        private final ItemStack sellItem;
        private final ItemStack buyItem;
        private final ItemStack secondBuyItem;
        private final int emeraldCost;
        private final Map<RegistryEntry<Enchantment>, Integer> enchantments;
        private final String itemId;
        private final boolean isEnchantedBook;

        public ScannedTrade(int slotIndex, TradeOffer offer) {
            this.slotIndex = slotIndex;
            this.offer = offer;
            this.sellItem = offer.getSellItem().copy();
            this.buyItem = offer.getFirstBuyItem().itemStack().copy();
            this.secondBuyItem = offer
                .getSecondBuyItem()
                .map(tradedItem -> tradedItem.itemStack().copy())
                .orElse(ItemStack.EMPTY);
            this.emeraldCost = calculateEmeraldCost();
            this.enchantments = getEnchantmentsFromItem(sellItem);
            this.itemId = Registries.ITEM.getId(sellItem.getItem()).toString();
            this.isEnchantedBook = sellItem.getItem() == Items.ENCHANTED_BOOK;
        }

        private static Map<
            RegistryEntry<Enchantment>,
            Integer
        > getEnchantmentsFromItem(ItemStack stack) {
            ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
            );
            Map<RegistryEntry<Enchantment>, Integer> result = new HashMap<>();
            enchantments
                .getEnchantments()
                .forEach(entry -> {
                    result.put(entry, enchantments.getLevel(entry));
                });
            return result;
        }

        private int calculateEmeraldCost() {
            int cost = 0;

            if (buyItem.getItem() == Items.EMERALD) {
                cost += buyItem.getCount();
            }

            if (
                !secondBuyItem.isEmpty() &&
                secondBuyItem.getItem() == Items.EMERALD
            ) {
                cost += secondBuyItem.getCount();
            }

            return cost;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public TradeOffer getOffer() {
            return offer;
        }

        public ItemStack getSellItem() {
            return sellItem;
        }

        public ItemStack getBuyItem() {
            return buyItem;
        }

        public ItemStack getSecondBuyItem() {
            return secondBuyItem;
        }

        public int getEmeraldCost() {
            return emeraldCost;
        }

        public Map<RegistryEntry<Enchantment>, Integer> getEnchantments() {
            return enchantments;
        }

        public String getItemId() {
            return itemId;
        }

        public boolean isEnchantedBook() {
            return isEnchantedBook;
        }

        public boolean hasEnchantment(RegistryEntry<Enchantment> enchantment) {
            return enchantments.containsKey(enchantment);
        }

        public int getEnchantmentLevel(RegistryEntry<Enchantment> enchantment) {
            return enchantments.getOrDefault(enchantment, 0);
        }

        public List<String> getEnchantmentNames() {
            List<String> names = new ArrayList<>();
            for (Map.Entry<
                RegistryEntry<Enchantment>,
                Integer
            > entry : enchantments.entrySet()) {
                RegistryEntry<Enchantment> enchantEntry = entry.getKey();
                enchantEntry
                    .getKey()
                    .ifPresent(key -> {
                        String enchantId = key.getValue().toString();
                        int level = entry.getValue();
                        names.add(enchantId + ":" + level);
                    });
            }
            return names;
        }

        @Override
        public String toString() {
            return String.format(
                "Trade[slot=%d, item=%s, emeralds=%d, enchants=%s]",
                slotIndex,
                itemId,
                emeraldCost,
                getEnchantmentNames()
            );
        }
    }
}
