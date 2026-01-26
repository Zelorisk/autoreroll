package com.villagerreroller.trade;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.config.ModConfig;
import java.util.ArrayList;
import java.util.List;

public class TradeFilter {

    private final ModConfig config;

    public TradeFilter(ModConfig config) {
        this.config = config;
    }

    public List<TradeScanner.ScannedTrade> filterTrades(
        List<TradeScanner.ScannedTrade> trades
    ) {
        List<TradeScanner.ScannedTrade> filtered = new ArrayList<>();

        for (TradeScanner.ScannedTrade trade : trades) {
            if (matchesCriteria(trade)) {
                filtered.add(trade);
            }
        }

        VillagerReroller.LOGGER.debug(
            "Filtered {} trades from {} total",
            filtered.size(),
            trades.size()
        );
        return filtered;
    }

    public boolean matchesCriteria(TradeScanner.ScannedTrade trade) {
        if (!trade.isEnchantedBook()) {
            return false;
        }

        if (!checkPriceThreshold(trade)) {
            return false;
        }

        return checkSelectedEnchantment(trade);
    }

    private boolean checkSelectedEnchantment(TradeScanner.ScannedTrade trade) {
        String targetEnchant = config.getSelectedEnchantment();
        int targetLevel = config.getSelectedEnchantmentLevel();

        for (String tradeEnchant : trade.getEnchantmentNames()) {
            String[] parts = tradeEnchant.split(":");
            if (parts.length >= 2) {
                String enchantId = parts[0] + ":" + parts[1];
                int level = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;

                if (enchantId.equals(targetEnchant) && level >= targetLevel) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPriceThreshold(TradeScanner.ScannedTrade trade) {
        int emeraldCost = trade.getEmeraldCost();
        return emeraldCost <= config.getMaxEmeraldsBooks();
    }

    public boolean hasAnyMatchingTrade(List<TradeScanner.ScannedTrade> trades) {
        return filterTrades(trades).size() > 0;
    }
}
