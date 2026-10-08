package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;

import java.awt.Color;

/**
 * Colors shared by the board overlay and the side panel.
 */
public final class BingoTheme {
    public static final Color GOLD = new Color(255, 215, 0);
    public static final Color FOREGROUND = Color.WHITE;
    public static final Color MUTED = new Color(156, 163, 175);

    public static final Color CARD_BACKGROUND = ColorScheme.DARKER_GRAY_COLOR;
    public static final Color CARD_BORDER = ColorScheme.BORDER_COLOR;
    public static final Color BUTTON_BACKGROUND = ColorScheme.MEDIUM_GRAY_COLOR;
    public static final Color BUTTON_HOVER = new Color(255, 255, 255, 40);
    public static final Color BUTTON_BORDER = ColorScheme.BORDER_COLOR;

    // Goal progress colors, matching the BingoScape web app
    public static final Color PROGRESS_COMPLETE = new Color(16, 185, 129);
    public static final Color PROGRESS_PARTIAL = new Color(245, 158, 11);
    public static final Color PROGRESS_NONE = new Color(107, 114, 128);
    public static final Color PROGRESS_BACKGROUND = new Color(31, 41, 55, 200);

    public static final int CORNER_RADIUS = 8;
    public static final int BUTTON_RADIUS = 6;

    private BingoTheme() {
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color progressColor(int current, boolean complete) {
        if (complete) {
            return PROGRESS_COMPLETE;
        }
        return current > 0 ? PROGRESS_PARTIAL : PROGRESS_NONE;
    }
}
