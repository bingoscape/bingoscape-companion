package org.bingoscape.ui.components;

import org.bingoscape.ui.StyleConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Amber pill-shaped badge displaying XP/weight value with lightning icon.
 * Matches the design from the Bingoscape web app.
 */
public class XPBadge extends JPanel {

    private final int xpValue;

    public XPBadge(int xpValue) {
        this.xpValue = xpValue;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER, 4, 0));
        initComponents();
    }

    private void initComponents() {
        // Lightning icon label
        JLabel iconLabel = new JLabel(StyleConstants.ICON_LIGHTNING);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        iconLabel.setForeground(StyleConstants.AMBER_500);

        // XP value label
        JLabel valueLabel = new JLabel(String.valueOf(xpValue));
        valueLabel.setFont(StyleConstants.FONT_BADGE_SMALL);
        valueLabel.setForeground(StyleConstants.AMBER_500);

        add(iconLabel);
        add(valueLabel);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw pill-shaped background
        g2d.setColor(StyleConstants.AMBER_100);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(),
                          StyleConstants.BORDER_RADIUS_FULL,
                          StyleConstants.BORDER_RADIUS_FULL);

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // Add padding: 2px vertical, 8px horizontal (pill shape)
        return new Dimension(size.width + 16, size.height + 4);
    }
}
