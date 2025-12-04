package com.villagerreroller.ui;

import com.villagerreroller.VillagerReroller;
import com.villagerreroller.automation.RerollController;
import com.villagerreroller.config.ModConfig;
import com.villagerreroller.stats.StatisticsTracker;
import com.villagerreroller.trade.TradeEvaluator;
import com.villagerreroller.trade.TradeScanner;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public class OverlayRenderer {
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 6;
    private static final int PANEL_MARGIN = 4;
    private static final int TRADE_OFFSET_X = 180;
    private static final int TRADE_OFFSET_Y = 20;

    // Colors
    private static final int COLOR_BACKGROUND = 0xCC000000; // Semi-transparent black
    private static final int COLOR_BORDER = 0xFF4A90E2;     // Nice blue
    private static final int COLOR_ACCENT = 0xFF5CB85C;     // Green accent
    private static final int COLOR_WARNING = 0xFFF0AD4E;    // Orange warning
    private static final int COLOR_DANGER = 0xFFD9534F;     // Red danger
    private static final int COLOR_TEXT = 0xFFFFFFFF;       // White text
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;   // Dim gray text

    private final MinecraftClient client;
    private final TradeScanner tradeScanner;
    private final TradeEvaluator tradeEvaluator;

    // Animation
    private float animationTicks = 0;

    public OverlayRenderer() {
        this.client = MinecraftClient.getInstance();
        this.tradeScanner = new TradeScanner();
        this.tradeEvaluator = new TradeEvaluator(VillagerReroller.getInstance().getConfigManager().getConfig());

        // Register HUD render callback
        HudRenderCallback.EVENT.register(this::onRenderHud);
    }

    private void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        ModConfig config = VillagerReroller.getInstance().getConfigManager().getConfig();

        if (!config.isEnabled() || !config.isShowOverlay()) {
            return;
        }

        // Update animation
        animationTicks += 1.0f; // Simple increment per tick

        RerollController controller = VillagerReroller.getInstance().getRerollController();
        TextRenderer textRenderer = client.textRenderer;

        // Render main overlay (centered above hotbar)
        renderMainOverlay(context, textRenderer, config, controller);

        // Render trade quality indicators if in merchant screen
        if (config.isShowTradeQuality() && client.currentScreen instanceof MerchantScreen) {
            renderTradeQuality(context, textRenderer);
        }
    }

    private void renderMainOverlay(DrawContext context, TextRenderer textRenderer, ModConfig config, RerollController controller) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position above hotbar (hotbar is at bottom, ~22 pixels high, with some margin)
        int hotbarHeight = 22;
        int bottomMargin = 25; // Space between overlay and hotbar

        List<String> lines = new ArrayList<>();
        List<Integer> lineColors = new ArrayList<>();

        // Build content based on state
        if (controller.isRunning()) {
            // Title with icon
            lines.add("⚙ Trade Reroller Active");
            lineColors.add(COLOR_ACCENT);

            // Current attempts with progress
            lines.add(String.format("Attempt: %d/%d", controller.getCurrentAttempts(), config.getMaxRerollAttempts()));
            lineColors.add(COLOR_TEXT);

            // Current state
            String stateInfo = getStateDisplayText(controller);
            lines.add(stateInfo);
            lineColors.add(COLOR_TEXT_DIM);

            // Session stats if available
            StatisticsTracker stats = VillagerReroller.getInstance().getStatisticsTracker();
            if (stats.getSessionSuccesses() > 0) {
                lines.add(String.format("✓ Found: %d | Avg: %.1f tries",
                    stats.getSessionSuccesses(), stats.getSessionAverageAttempts()));
                lineColors.add(COLOR_ACCENT);
            }
        } else {
            // Idle state - show minimal info
            lines.add("⚙ Trade Reroller");
            lineColors.add(COLOR_TEXT_DIM);

            String modeText = config.getOperationMode().name().replace("_", " ");
            lines.add("Mode: " + modeText);
            lineColors.add(COLOR_TEXT_DIM);

            StatisticsTracker stats = VillagerReroller.getInstance().getStatisticsTracker();
            if (stats.getSessionSuccesses() > 0) {
                lines.add(String.format("Session: %d found", stats.getSessionSuccesses()));
                lineColors.add(COLOR_TEXT_DIM);
            }
        }

        // Calculate panel dimensions
        int maxWidth = 0;
        for (String line : lines) {
            int width = textRenderer.getWidth(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int panelWidth = maxWidth + PADDING * 2;
        int panelHeight = lines.size() * LINE_HEIGHT + PADDING * 2;

        // Add space for progress bar if active
        int progressBarHeight = 0;
        if (controller.isRunning() && config.isShowProgressBar()) {
            progressBarHeight = 8;
            panelHeight += progressBarHeight + 3;
        }

        // Center horizontally, position above hotbar
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = screenHeight - hotbarHeight - bottomMargin - panelHeight;

        // Draw panel background with border
        drawStyledPanel(context, panelX, panelY, panelWidth, panelHeight);

        // Draw content
        int textY = panelY + PADDING;
        for (int i = 0; i < lines.size(); i++) {
            int textX = panelX + PADDING + (panelWidth - PADDING * 2 - textRenderer.getWidth(lines.get(i))) / 2; // Center text
            context.drawText(textRenderer, lines.get(i), textX, textY, lineColors.get(i), true);
            textY += LINE_HEIGHT;
        }

        // Draw progress bar if active
        if (controller.isRunning() && config.isShowProgressBar()) {
            float progress = (float) controller.getCurrentAttempts() / config.getMaxRerollAttempts();
            int barWidth = panelWidth - PADDING * 2;
            int barX = panelX + PADDING;
            int barY = textY + 2;
            renderEnhancedProgressBar(context, barX, barY, barWidth, 6, progress);
        }
    }

    /**
     * Get human-readable text for current reroll state
     */
    private String getStateDisplayText(RerollController controller) {
        if (controller.getCurrentVillager() == null) {
            return "Status: Idle";
        }

        // Try to infer state from controller behavior
        // Since we don't have direct access to state, we'll show generic status
        int attempts = controller.getCurrentAttempts();

        if (attempts == 0) {
            return "Status: Initializing...";
        } else {
            // Animate dots for active status
            int dots = (int) (animationTicks / 10) % 4;
            String dotString = ".".repeat(dots);
            return "Status: Working" + dotString;
        }
    }

    /**
     * Draw a styled panel with background and glowing border
     */
    private void drawStyledPanel(DrawContext context, int x, int y, int width, int height) {
        // Draw shadow/glow effect
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x40000000);

        // Draw background
        context.fill(x, y, x + width, y + height, COLOR_BACKGROUND);

        // Draw border with slight glow
        drawGlowingBorder(context, x, y, width, height);
    }

    /**
     * Draw a glowing border effect
     */
    private void drawGlowingBorder(DrawContext context, int x, int y, int width, int height) {
        // Outer glow (dim)
        int glowColor = (COLOR_BORDER & 0x00FFFFFF) | 0x40000000;
        context.drawBorder(x - 1, y - 1, width + 2, height + 2, glowColor);

        // Main border
        context.drawBorder(x, y, width, height, COLOR_BORDER);
    }

    private void renderTradeQuality(DrawContext context, TextRenderer textRenderer) {
        List<TradeScanner.ScannedTrade> trades = tradeScanner.scanCurrentTrades();

        if (trades.isEmpty()) {
            return;
        }

        for (TradeScanner.ScannedTrade trade : trades) {
            TradeEvaluator.TradeScore score = tradeEvaluator.evaluateTrade(trade);

            // Calculate position based on trade slot
            int x = TRADE_OFFSET_X;
            int y = TRADE_OFFSET_Y + trade.getSlotIndex() * 20;

            // Render grade with color
            String grade = score.getGrade();
            int color = getGradeColor(grade);

            String text = "§l" + grade + " §r§7(" + score.getScore() + ")";
            context.drawText(textRenderer, text, x, y, color, true);
        }
    }

    /**
     * Render an enhanced progress bar with gradient and glow
     */
    private void renderEnhancedProgressBar(DrawContext context, int x, int y, int width, int height, float progress) {
        progress = Math.min(progress, 1.0f);

        // Background (dark)
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);

        // Progress fill with color gradient
        int progressWidth = (int) (width * progress);
        if (progressWidth > 0) {
            int color;
            if (progress < 0.5f) {
                color = COLOR_ACCENT; // Green when low
            } else if (progress < 0.8f) {
                color = COLOR_WARNING; // Orange when medium
            } else {
                color = COLOR_DANGER; // Red when high
            }

            // Draw progress with slight transparency
            context.fill(x, y, x + progressWidth, y + height, color);

            // Add highlight on top for 3D effect
            int highlightColor = (color & 0x00FFFFFF) | 0x40FFFFFF;
            context.fill(x, y, x + progressWidth, y + 1, highlightColor);
        }

        // Border
        context.drawBorder(x, y, width, height, COLOR_BORDER);
    }

    private int getGradeColor(String grade) {
        return switch (grade) {
            case "S" -> 0xFFD700; // Gold
            case "A" -> 0x00FF00; // Green
            case "B" -> 0x87CEEB; // Sky Blue
            case "C" -> 0xFFFFFF; // White
            default -> 0x808080;  // Gray
        };
    }
}
