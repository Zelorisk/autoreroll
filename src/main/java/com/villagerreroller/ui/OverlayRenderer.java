package com.villagerreroller.ui;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.automation.RerollController;
import com.villagerreroller.config.ModConfig;
import com.villagerreroller.stats.StatisticsTracker;
import com.villagerreroller.trade.TradeEvaluator;
import com.villagerreroller.trade.TradeScanner;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.render.RenderTickCounter;

public class OverlayRenderer {

    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 6;
    private static final int PANEL_MARGIN = 4;
    private static final int TRADE_OFFSET_X = 180;
    private static final int TRADE_OFFSET_Y = 20;

    private static final int COLOR_BACKGROUND = 0xCC000000;
    private static final int COLOR_BORDER = 0xFF4A90E2;
    private static final int COLOR_ACCENT = 0xFF5CB85C;
    private static final int COLOR_WARNING = 0xFFF0AD4E;
    private static final int COLOR_DANGER = 0xFFD9534F;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;

    private final MinecraftClient client;
    private final TradeScanner tradeScanner;
    private final TradeEvaluator tradeEvaluator;

    private float animationTicks = 0;

    public OverlayRenderer() {
        this.client = MinecraftClient.getInstance();
        this.tradeScanner = new TradeScanner();
        this.tradeEvaluator = new TradeEvaluator(
            VillagerReroller.getInstance().getConfigManager().getConfig()
        );

        HudRenderCallback.EVENT.register(this::onRenderHud);
    }

    private void onRenderHud(
        DrawContext context,
        RenderTickCounter tickCounter
    ) {
        ModConfig config = VillagerReroller.getInstance()
            .getConfigManager()
            .getConfig();

        if (!config.isEnabled() || !config.isShowOverlay()) {
            return;
        }

        animationTicks += 1.0f;

        RerollController controller =
            VillagerReroller.getInstance().getRerollController();
        TextRenderer textRenderer = client.textRenderer;

        renderTradingStatusBar(context, textRenderer, controller);

        renderMainOverlay(context, textRenderer, config, controller);

        if (
            config.isShowTradeQuality() &&
            client.currentScreen instanceof MerchantScreen
        ) {
            renderTradeQuality(context, textRenderer);
        }
    }

    private void renderMainOverlay(
        DrawContext context,
        TextRenderer textRenderer,
        ModConfig config,
        RerollController controller
    ) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int hotbarHeight = 22;
        int bottomMargin = 25;

        List<String> lines = new ArrayList<>();
        List<Integer> lineColors = new ArrayList<>();

        if (controller.isRunning()) {
            lines.add("⚙ Trade Reroller Active");
            lineColors.add(COLOR_ACCENT);

            lines.add(
                String.format(
                    "Attempt: %d/%d",
                    controller.getCurrentAttempts(),
                    config.getMaxRerollAttempts()
                )
            );
            lineColors.add(COLOR_TEXT);

            String stateInfo = getStateDisplayText(controller);
            lines.add(stateInfo);
            lineColors.add(COLOR_TEXT_DIM);

            StatisticsTracker stats =
                VillagerReroller.getInstance().getStatisticsTracker();
            if (stats.getSessionSuccesses() > 0) {
                lines.add(
                    String.format(
                        "✓ Found: %d | Avg: %.1f tries",
                        stats.getSessionSuccesses(),
                        stats.getSessionAverageAttempts()
                    )
                );
                lineColors.add(COLOR_ACCENT);
            }
        } else {
            lines.add("⚙ Trade Reroller");
            lineColors.add(COLOR_TEXT_DIM);

            String modeText = config
                .getOperationMode()
                .name()
                .replace("_", " ");
            lines.add("Mode: " + modeText);
            lineColors.add(COLOR_TEXT_DIM);

            StatisticsTracker stats =
                VillagerReroller.getInstance().getStatisticsTracker();
            if (stats.getSessionSuccesses() > 0) {
                lines.add(
                    String.format(
                        "Session: %d found",
                        stats.getSessionSuccesses()
                    )
                );
                lineColors.add(COLOR_TEXT_DIM);
            }
        }

        int maxWidth = 0;
        for (String line : lines) {
            int width = textRenderer.getWidth(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int panelWidth = maxWidth + PADDING * 2;
        int panelHeight = lines.size() * LINE_HEIGHT + PADDING * 2;

        int progressBarHeight = 0;
        if (controller.isRunning() && config.isShowProgressBar()) {
            progressBarHeight = 8;
            panelHeight += progressBarHeight + 3;
        }

        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = screenHeight - hotbarHeight - bottomMargin - panelHeight;

        drawStyledPanel(context, panelX, panelY, panelWidth, panelHeight);

        int textY = panelY + PADDING;
        for (int i = 0; i < lines.size(); i++) {
            int textX =
                panelX +
                PADDING +
                (panelWidth -
                    PADDING * 2 -
                    textRenderer.getWidth(lines.get(i))) /
                2;
            context.drawText(
                textRenderer,
                lines.get(i),
                textX,
                textY,
                lineColors.get(i),
                true
            );
            textY += LINE_HEIGHT;
        }

        if (controller.isRunning() && config.isShowProgressBar()) {
            float progress =
                (float) controller.getCurrentAttempts() /
                config.getMaxRerollAttempts();
            int barWidth = panelWidth - PADDING * 2;
            int barX = panelX + PADDING;
            int barY = textY + 2;
            renderEnhancedProgressBar(
                context,
                barX,
                barY,
                barWidth,
                6,
                progress
            );
        }
    }

    private String getStateDisplayText(RerollController controller) {
        if (controller.getCurrentVillager() == null) {
            return "Status: Idle";
        }

        int attempts = controller.getCurrentAttempts();

        if (attempts == 0) {
            return "Status: Initializing...";
        } else {
            int dots = (int) (animationTicks / 10) % 4;
            String dotString = ".".repeat(dots);
            return "Status: Working" + dotString;
        }
    }

    private void drawStyledPanel(
        DrawContext context,
        int x,
        int y,
        int width,
        int height
    ) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x40000000);

        context.fill(x, y, x + width, y + height, COLOR_BACKGROUND);

        drawGlowingBorder(context, x, y, width, height);
    }

    private void drawGlowingBorder(
        DrawContext context,
        int x,
        int y,
        int width,
        int height
    ) {
        int glowColor = (COLOR_BORDER & 0x00FFFFFF) | 0x40000000;
        context.fill(x - 2, y - 2, x + width + 2, y - 1, glowColor);
        context.fill(
            x - 2,
            y + height + 1,
            x + width + 2,
            y + height + 2,
            glowColor
        );
        context.fill(x - 2, y - 1, x - 1, y + height + 1, glowColor);
        context.fill(
            x + width + 1,
            y - 1,
            x + width + 2,
            y + height + 1,
            glowColor
        );

        context.fill(x - 1, y - 1, x + width + 1, y, COLOR_BORDER);
        context.fill(
            x - 1,
            y + height,
            x + width + 1,
            y + height + 1,
            COLOR_BORDER
        );
        context.fill(x - 1, y, x, y + height, COLOR_BORDER);
        context.fill(x + width, y, x + width + 1, y + height, COLOR_BORDER);
    }

    private void renderTradeQuality(
        DrawContext context,
        TextRenderer textRenderer
    ) {
        List<TradeScanner.ScannedTrade> trades =
            tradeScanner.scanCurrentTrades();

        if (trades.isEmpty()) {
            return;
        }

        for (TradeScanner.ScannedTrade trade : trades) {
            TradeEvaluator.TradeScore score = tradeEvaluator.evaluateTrade(
                trade
            );

            int x = TRADE_OFFSET_X;
            int y = TRADE_OFFSET_Y + trade.getSlotIndex() * 20;

            String grade = score.getGrade();
            int color = getGradeColor(grade);

            String text = "§l" + grade + " §r§7(" + score.getScore() + ")";
            context.drawText(textRenderer, text, x, y, color, true);
        }
    }

    private void renderEnhancedProgressBar(
        DrawContext context,
        int x,
        int y,
        int width,
        int height,
        float progress
    ) {
        progress = Math.min(progress, 1.0f);

        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);

        int progressWidth = (int) (width * progress);
        if (progressWidth > 0) {
            int color;
            if (progress < 0.5f) {
                color = COLOR_ACCENT;
            } else if (progress < 0.8f) {
                color = COLOR_WARNING;
            } else {
                color = COLOR_DANGER;
            }

            context.fill(x, y, x + progressWidth, y + height, color);

            int highlightColor = (color & 0x00FFFFFF) | 0x40FFFFFF;
            context.fill(x, y, x + progressWidth, y + 1, highlightColor);
        }

        context.fill(x, y, x + width, y + 1, COLOR_BORDER);
        context.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        context.fill(x, y + 1, x + 1, y + height - 1, COLOR_BORDER);
        context.fill(
            x + width - 1,
            y + 1,
            x + width,
            y + height - 1,
            COLOR_BORDER
        );
    }

    private int getGradeColor(String grade) {
        return switch (grade) {
            case "S" -> 0xFFD700;
            case "A" -> 0x00FF00;
            case "B" -> 0x87CEEB;
            case "C" -> 0xFFFFFF;
            default -> 0x808080;
        };
    }

    private void renderTradingStatusBar(
        DrawContext context,
        TextRenderer textRenderer,
        RerollController controller
    ) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int hotbarY = screenHeight - 22;
        int statusBarY = hotbarY - 14;

        int statusBarWidth = 120;
        int statusBarHeight = 10;
        int statusBarX = (screenWidth - statusBarWidth) / 2;

        if (controller.isRunning()) {
            int pulseAlpha = (int) (Math.sin(animationTicks / 8.0) * 25 + 230);
            int glowColor = (COLOR_ACCENT & 0x00FFFFFF) | (pulseAlpha << 24);

            context.fill(
                statusBarX - 1,
                statusBarY - 1,
                statusBarX + statusBarWidth + 1,
                statusBarY + statusBarHeight + 1,
                glowColor
            );

            context.fill(
                statusBarX,
                statusBarY,
                statusBarX + statusBarWidth,
                statusBarY + statusBarHeight,
                0xE0000000
            );

            String statusText = "TRADING";
            int textX =
                statusBarX +
                (statusBarWidth - textRenderer.getWidth(statusText)) / 2;
            int textY = statusBarY + 1;

            context.drawText(
                textRenderer,
                statusText,
                textX,
                textY,
                COLOR_ACCENT,
                true
            );
        } else {
            context.fill(
                statusBarX,
                statusBarY,
                statusBarX + statusBarWidth,
                statusBarY + statusBarHeight,
                0x80000000
            );

            String statusText = "IDLE";
            int textX =
                statusBarX +
                (statusBarWidth - textRenderer.getWidth(statusText)) / 2;
            int textY = statusBarY + 1;

            context.drawText(
                textRenderer,
                statusText,
                textX,
                textY,
                COLOR_TEXT_DIM,
                true
            );
        }
    }
}
