package org.bingoscape.ui;

import org.bingoscape.models.TileSubmissionType;

import java.awt.Color;

/**
 * Centralized utility for tile submission status mappings.
 * Provides consistent status text, colors, and hex codes across all UI components.
 */
public final class StatusConstants {

    // Private constructor to prevent instantiation
    private StatusConstants() {
        throw new AssertionError("StatusConstants is a utility class and should not be instantiated");
    }

    /**
     * Gets the display text for a tile submission status.
     *
     * @param status The submission status
     * @return User-friendly status text
     */
    public static String getStatusText(TileSubmissionType status) {
        if (status == null) {
            return "Not Submitted";
        }

        switch (status) {
            case PENDING:
                return "Pending Review";
            case ACCEPTED:
                return "Completed";
            case REQUIRES_INTERACTION:
                return "Needs Action";
            case DECLINED:
                return "Declined";
            case NOT_SUBMITTED:
            default:
                return "Not Submitted";
        }
    }

    /**
     * Gets the color for displaying status text.
     *
     * @param status The submission status
     * @return The color to use for status text
     */
    public static Color getStatusColor(TileSubmissionType status) {
        if (status == null) {
            return Color.LIGHT_GRAY;
        }

        switch (status) {
            case PENDING:
                return ColorPalette.PENDING_TEXT;
            case ACCEPTED:
                return ColorPalette.ACCEPTED_TEXT;
            case REQUIRES_INTERACTION:
                return ColorPalette.REQUIRES_ACTION_TEXT;
            case DECLINED:
                return ColorPalette.DECLINED_TEXT;
            case NOT_SUBMITTED:
            default:
                return Color.LIGHT_GRAY;
        }
    }

    /**
     * Gets the hex color code for HTML/tooltip rendering.
     *
     * @param status The submission status
     * @return Hex color string (e.g., "#3b82f6")
     */
    public static String getStatusHexColor(TileSubmissionType status) {
        if (status == null) {
            return "#ffffff";
        }

        switch (status) {
            case PENDING:
                return "#3b82f6"; // Blue
            case ACCEPTED:
                return "#22c55e"; // Green
            case REQUIRES_INTERACTION:
                return "#eab308"; // Yellow
            case DECLINED:
                return "#ef4444"; // Red
            case NOT_SUBMITTED:
            default:
                return "#ffffff"; // White
        }
    }

    /**
     * Gets the background color for tiles with a given status.
     *
     * @param status The submission status
     * @return The background color to use
     */
    public static Color getStatusBackgroundColor(TileSubmissionType status) {
        if (status == null || status == TileSubmissionType.NOT_SUBMITTED) {
            return null; // Use default background
        }

        switch (status) {
            case PENDING:
                return ColorPalette.PENDING_BG;
            case ACCEPTED:
                return ColorPalette.ACCEPTED_BG;
            case REQUIRES_INTERACTION:
                return ColorPalette.REQUIRES_ACTION_BG;
            case DECLINED:
                return ColorPalette.DECLINED_BG;
            default:
                return null;
        }
    }

    /**
     * Gets the border color for tiles with a given status.
     *
     * @param status The submission status
     * @return The border color to use
     */
    public static Color getStatusBorderColor(TileSubmissionType status) {
        if (status == null) {
            return null; // Use default border
        }

        switch (status) {
            case PENDING:
                return ColorPalette.PENDING_BORDER;
            case ACCEPTED:
                return ColorPalette.ACCEPTED_BORDER;
            case REQUIRES_INTERACTION:
                return ColorPalette.REQUIRES_ACTION_BORDER;
            case DECLINED:
                return ColorPalette.DECLINED_BORDER;
            default:
                return null;
        }
    }
}
