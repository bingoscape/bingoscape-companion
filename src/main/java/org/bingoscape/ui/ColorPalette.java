package org.bingoscape.ui;

import java.awt.Color;

/**
 * Centralized color palette for the BingoScape plugin UI.
 * Provides consistent colors across all UI components.
 */
public final class ColorPalette {

    // Primary Colors
    public static final Color GOLD = new Color(255, 215, 0);
    public static final Color SUCCESS = new Color(34, 197, 94);
    public static final Color ACCENT_BLUE = new Color(59, 130, 246);
    public static final Color WARNING_YELLOW = new Color(234, 179, 8);
    public static final Color ERROR_RED = new Color(239, 68, 68);

    // Background Colors
    public static final Color PINNED_TILE_BG = new Color(45, 55, 72);
    public static final Color CARD_BG = new Color(55, 65, 81);
    public static final Color HEADER_BG = new Color(31, 41, 55);

    // Border Colors
    public static final Color BORDER = new Color(75, 85, 99);

    // Status Background Colors
    public static final Color PENDING_BG = new Color(30, 64, 122);
    public static final Color ACCEPTED_BG = new Color(17, 99, 47);
    public static final Color REQUIRES_ACTION_BG = new Color(117, 89, 4);
    public static final Color DECLINED_BG = new Color(120, 34, 34);

    // Status Border Colors
    public static final Color PENDING_BORDER = new Color(59, 130, 246);
    public static final Color ACCEPTED_BORDER = new Color(34, 197, 94);
    public static final Color REQUIRES_ACTION_BORDER = new Color(234, 179, 8);
    public static final Color DECLINED_BORDER = new Color(239, 68, 68);

    // Status Text Colors
    public static final Color PENDING_TEXT = new Color(59, 130, 246);
    public static final Color ACCEPTED_TEXT = new Color(34, 197, 94);
    public static final Color REQUIRES_ACTION_TEXT = new Color(234, 179, 8);
    public static final Color DECLINED_TEXT = new Color(239, 68, 68);

    // Private constructor to prevent instantiation
    private ColorPalette() {
        throw new AssertionError("ColorPalette is a utility class and should not be instantiated");
    }
}
