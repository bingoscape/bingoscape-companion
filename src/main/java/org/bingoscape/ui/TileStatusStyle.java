package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;

import java.awt.Color;

/**
 * Colors and labels for tile submission states, shared by the board window and the board overlay.
 */
public final class TileStatusStyle {
    public static final Color PENDING = new Color(59, 130, 246);       // Blue
    public static final Color ACCEPTED = new Color(34, 197, 94);       // Green
    public static final Color NEEDS_ACTION = new Color(234, 179, 8);   // Yellow
    public static final Color DECLINED = new Color(239, 68, 68);       // Red

    private static final Color PENDING_BG = new Color(30, 64, 122);       // Darker blue
    private static final Color ACCEPTED_BG = new Color(17, 99, 47);       // Darker green
    private static final Color NEEDS_ACTION_BG = new Color(117, 89, 4);   // Darker yellow/gold
    private static final Color DECLINED_BG = new Color(120, 34, 34);      // Darker red

    private TileStatusStyle() {
    }

    public static TileSubmissionType statusOf(Tile tile) {
        if (tile == null || tile.getSubmission() == null) {
            return null;
        }
        return tile.getSubmission().getStatus();
    }

    public static boolean isSubmitted(TileSubmissionType status) {
        return status != null && status != TileSubmissionType.NOT_SUBMITTED;
    }

    public static Color background(TileSubmissionType status) {
        if (!isSubmitted(status)) {
            return ColorScheme.DARK_GRAY_COLOR;
        }

        switch (status) {
            case PENDING:
                return PENDING_BG;
            case ACCEPTED:
                return ACCEPTED_BG;
            case REQUIRES_INTERACTION:
                return NEEDS_ACTION_BG;
            case DECLINED:
                return DECLINED_BG;
            default:
                return ColorScheme.DARK_GRAY_COLOR;
        }
    }

    public static Color border(TileSubmissionType status) {
        if (status == null) {
            return ColorScheme.BORDER_COLOR;
        }

        switch (status) {
            case PENDING:
                return PENDING;
            case ACCEPTED:
                return ACCEPTED;
            case REQUIRES_INTERACTION:
                return NEEDS_ACTION;
            case DECLINED:
                return DECLINED;
            default:
                return ColorScheme.BORDER_COLOR;
        }
    }

    public static Color accent(TileSubmissionType status) {
        if (status == null) {
            return Color.LIGHT_GRAY;
        }

        switch (status) {
            case PENDING:
                return PENDING;
            case ACCEPTED:
                return ACCEPTED;
            case REQUIRES_INTERACTION:
                return NEEDS_ACTION;
            case DECLINED:
                return DECLINED;
            default:
                return Color.LIGHT_GRAY;
        }
    }

    public static String hex(TileSubmissionType status) {
        if (status == null) {
            return "#ffffff";
        }

        switch (status) {
            case PENDING:
                return "#3b82f6";
            case ACCEPTED:
                return "#22c55e";
            case REQUIRES_INTERACTION:
                return "#eab308";
            case DECLINED:
                return "#ef4444";
            default:
                return "#ffffff";
        }
    }

    /**
     * Short uppercase label shown on the tile itself, or null when nothing should be shown.
     */
    public static String shortLabel(TileSubmissionType status) {
        if (status == null) {
            return null;
        }

        switch (status) {
            case PENDING:
                return "PENDING";
            case ACCEPTED:
                return "COMPLETED";
            case REQUIRES_INTERACTION:
                return "NEEDS ACTION";
            case DECLINED:
                return "DECLINED";
            default:
                return null;
        }
    }

    public static String displayText(TileSubmissionType status) {
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
            default:
                return "Not Submitted";
        }
    }
}
