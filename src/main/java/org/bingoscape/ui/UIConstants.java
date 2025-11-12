package org.bingoscape.ui;

import java.text.SimpleDateFormat;

/**
 * Centralized UI constants for the BingoScape plugin.
 * Provides consistent sizing, spacing, and formatting across all UI components.
 */
public final class UIConstants {

    // Layout Spacing
    public static final int BORDER_SPACING = 10;
    public static final int COMPONENT_SPACING = 12;
    public static final int SECTION_SPACING = 14;
    public static final int CARD_SPACING = 10;
    public static final int PINNED_SECTION_SPACING = 8;
    public static final int QUICK_ACTION_SPACING = 6;

    // Button Sizes
    public static final int BUTTON_SIZE = 24;
    public static final int QUICK_ACTION_BUTTON_SIZE = 28;
    public static final int MINI_TILE_SIZE = 50;
    public static final int PINNED_TILE_HEIGHT = 55;

    // Animation Constants
    public static final int FADE_STEP = 10;
    public static final int FADE_TIMER_DELAY = 50;
    public static final int MAX_ALPHA = 255;

    // Dialog and Window Sizes
    public static final int SCREENSHOT_DIALOG_WIDTH = 600;
    public static final int SCREENSHOT_DIALOG_HEIGHT = 500;
    public static final int SCREENSHOT_DIALOG_HEIGHT_WITH_TILE = 550;
    public static final int SCREENSHOT_SCROLL_WIDTH = 580;
    public static final int SCREENSHOT_SCROLL_HEIGHT = 400;

    // Dialog Padding and Borders
    public static final int DIALOG_PADDING = 10;
    public static final int DIALOG_BORDER_SIZE = 1;
    public static final int PANEL_BORDER_SIZE = 2;

    // Text Constants
    public static final String NO_EVENTS_TEXT = "No active events found";

    // Date Formatting
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    // Private constructor to prevent instantiation
    private UIConstants() {
        throw new AssertionError("UIConstants is a utility class and should not be instantiated");
    }
}
