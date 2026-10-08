package org.bingoscape.ui.components;

import org.bingoscape.ui.BingoTheme;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Rounded card with a vertical layout, the side panel counterpart of the overlay's board background.
 * Never grows taller than its content inside a BoxLayout.
 */
public class CardPanel extends JPanel {
    public CardPanel() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = BingoTheme.CORNER_RADIUS;
            g2.setColor(BingoTheme.CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
            g2.setColor(BingoTheme.CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
