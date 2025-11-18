package org.bingoscape.ui;

import java.awt.*;

/**
 * Centralized style constants for UI components matching the Bingoscape web app design.
 */
public class StyleConstants {

    // Background and Foreground Colors
    public static final Color BACKGROUND = new Color(26, 29, 41);           // #1a1d29
    public static final Color FOREGROUND = new Color(255, 255, 255);        // #ffffff
    public static final Color MUTED_FOREGROUND = new Color(156, 163, 175);  // #9ca3af

    // Status Colors
    public static final Color GREEN_500 = new Color(16, 185, 129);          // #10b981 - Approved
    public static final Color GREEN_200 = new Color(167, 243, 208);         // #a7f3d0 - Light ring
    public static final Color GREEN_800 = new Color(6, 95, 70);             // #065f46 - Dark ring

    public static final Color YELLOW_500 = new Color(234, 179, 8);          // #eab308 - Needs Review
    public static final Color YELLOW_200 = new Color(254, 240, 138);        // #fef08a - Light ring
    public static final Color YELLOW_800 = new Color(133, 77, 14);          // #854d0e - Dark ring

    public static final Color BLUE_500 = new Color(59, 130, 246);           // #3b82f6 - Pending
    public static final Color BLUE_200 = new Color(191, 219, 254);          // #bfdbfe - Light ring
    public static final Color BLUE_800 = new Color(30, 64, 175);            // #1e40af - Dark ring

    public static final Color RED_500 = new Color(239, 68, 68);             // #ef4444 - Declined/Error
    public static final Color RED_200 = new Color(254, 202, 202);           // #fecaca - Light ring
    public static final Color RED_800 = new Color(153, 27, 27);             // #991b1b - Dark ring

    // Amber Colors (for XP badges)
    public static final Color AMBER_100 = new Color(254, 243, 199);         // #fef3c7
    public static final Color AMBER_500 = new Color(245, 158, 11);          // #f59e0b
    public static final Color AMBER_900_30 = new Color(120, 53, 15, 76);    // #78350f with 30% opacity

    // Secondary Colors
    public static final Color SECONDARY_BG = new Color(55, 65, 81, 76);     // #374151 with ~30% alpha
    public static final Color SECONDARY = new Color(107, 114, 128);         // #6b7280

    // Progress Bar Colors
    public static final Color PROGRESS_COMPLETE = GREEN_500;
    public static final Color PROGRESS_PARTIAL = AMBER_500;
    public static final Color PROGRESS_NONE = new Color(107, 114, 128);     // #6b7280 gray
    public static final Color PROGRESS_BG = new Color(31, 41, 55, 200);     // #1f2937 with alpha

    // Fonts
    public static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Arial", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Arial", Font.PLAIN, 12);
    public static final Font FONT_TINY = new Font("Arial", Font.PLAIN, 10);
    public static final Font FONT_BADGE = new Font("Arial", Font.BOLD, 12);
    public static final Font FONT_BADGE_SMALL = new Font("Arial", Font.BOLD, 10);

    // Spacing
    public static final int PADDING = 16;
    public static final int GAP_SECTIONS = 12;
    public static final int GAP_ITEMS = 8;
    public static final int GAP_SMALL = 4;
    public static final int BORDER_RADIUS = 8;
    public static final int BORDER_RADIUS_SMALL = 4;
    public static final int BORDER_RADIUS_FULL = 999;  // For pill shapes

    // Component Sizes
    public static final int HOVER_CARD_WIDTH = 320;
    public static final int STATUS_ICON_SIZE = 28;
    public static final int MINI_PROGRESS_BAR_HEIGHT = 4;
    public static final int TREE_INDENT = 8;

    // Timing (milliseconds)
    public static final int HOVER_SHOW_DELAY = 200;
    public static final int HOVER_HIDE_DELAY = 400;

    // Unicode Icons
    public static final String ICON_LIGHTNING = "⚡";      //  ⚡
    public static final String ICON_CHECK = "✓";          // ✓
    public static final String ICON_EXCLAMATION = "!";
    public static final String ICON_HOURGLASS = "⏳";      // ⏳
    public static final String ICON_CROSS = "✗";          // ✗
    public static final String ICON_CIRCLE = "●";         //  ●(filled)
    public static final String ICON_CIRCLE_EMPTY = "○";   //  ○(empty)
    public static final String ICON_HALF_CIRCLE = "◐";    //  ◐(half)
    public static final String ICON_FOLDER = "📁";   // 📁
    public static final String ICON_TARGET = "🎯";   // 🎯
    public static final String ICON_PACKAGE = "📦";  // 📦
    public static final String ICON_EYE_OFF = "🙈";  // 🙈

    // Helper method to create a color with custom alpha
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    // Helper method to create a rounded border
    public static javax.swing.border.Border createRoundedBorder(Color color, int thickness, int radius) {
        return new RoundedBorder(color, thickness, radius);
    }

    /**
     * Custom border class for rounded rectangles
     */
    public static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = thickness;
            return insets;
        }
    }
}
