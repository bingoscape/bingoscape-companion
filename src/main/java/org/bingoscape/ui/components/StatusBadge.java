package org.bingoscape.ui.components;

import org.bingoscape.ui.StyleConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Status badge component showing submission status with colored circular icon and text.
 * Matches the design from the Bingoscape web app.
 */
public class StatusBadge extends JPanel {

    private final String status;
    private final Color iconColor;
    private final Color ringColor;
    private final String iconText;
    private final String displayText;

    public StatusBadge(String status) {
        this.status = status != null ? status.toLowerCase() : "";
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, StyleConstants.GAP_ITEMS, 0));

        // Determine colors and icons based on status
        switch (this.status) {
            case "approved":
            case "completed":
                iconColor = StyleConstants.GREEN_500;
                ringColor = StyleConstants.GREEN_200;
                iconText = StyleConstants.ICON_CHECK;
                displayText = "Approved";
                break;
            case "pending":
            case "pending_review":
                iconColor = StyleConstants.BLUE_500;
                ringColor = StyleConstants.BLUE_200;
                iconText = StyleConstants.ICON_HOURGLASS;
                displayText = "Pending Review";
                break;
            case "needs_review":
            case "needs_action":
                iconColor = StyleConstants.YELLOW_500;
                ringColor = StyleConstants.YELLOW_200;
                iconText = StyleConstants.ICON_EXCLAMATION;
                displayText = "Needs Review";
                break;
            case "declined":
            case "rejected":
                iconColor = StyleConstants.RED_500;
                ringColor = StyleConstants.RED_200;
                iconText = StyleConstants.ICON_CROSS;
                displayText = "Declined";
                break;
            default:
                iconColor = StyleConstants.SECONDARY;
                ringColor = StyleConstants.SECONDARY_BG;
                iconText = "?";
                displayText = capitalize(this.status);
                break;
        }

        initComponents();
    }

    private void initComponents() {
        // Container for the status badge with background
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, StyleConstants.GAP_ITEMS, 0));
        container.setBackground(StyleConstants.SECONDARY_BG);
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Circular status icon
        StatusIcon icon = new StatusIcon(iconText, iconColor, ringColor);
        container.add(icon);

        // Status text label
        JLabel textLabel = new JLabel(displayText);
        textLabel.setFont(StyleConstants.FONT_SMALL);
        textLabel.setForeground(StyleConstants.FOREGROUND);
        container.add(textLabel);

        add(container);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw rounded background
        g2d.setColor(StyleConstants.SECONDARY_BG);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(),
                          StyleConstants.BORDER_RADIUS,
                          StyleConstants.BORDER_RADIUS);

        g2d.dispose();
    }

    /**
     * Circular status icon with ring border effect
     */
    private static class StatusIcon extends JPanel {
        private final String iconText;
        private final Color iconColor;
        private final Color ringColor;

        public StatusIcon(String iconText, Color iconColor, Color ringColor) {
            this.iconText = iconText;
            this.iconColor = iconColor;
            this.ringColor = ringColor;
            setOpaque(false);
            setPreferredSize(new Dimension(
                StyleConstants.STATUS_ICON_SIZE,
                StyleConstants.STATUS_ICON_SIZE
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size = StyleConstants.STATUS_ICON_SIZE;

            // Draw ring border (2px)
            g2d.setColor(ringColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(1, 1, size - 2, size - 2);

            // Draw filled circle background
            g2d.setColor(iconColor);
            g2d.fillOval(3, 3, size - 6, size - 6);

            // Draw icon text centered
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(iconText);
            int textHeight = fm.getAscent();
            int x = (size - textWidth) / 2;
            int y = (size + textHeight) / 2 - 2;
            g2d.drawString(iconText, x, y);

            g2d.dispose();
        }
    }
}
