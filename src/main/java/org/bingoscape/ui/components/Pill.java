package org.bingoscape.ui.components;

import net.runelite.client.ui.FontManager;
import org.bingoscape.ui.BingoTheme;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Small rounded status badge, like the AND/OR badges in the overlay's goal tree.
 */
public class Pill extends JLabel {
    private Color accent = BingoTheme.MUTED;

    public Pill() {
        setFont(FontManager.getRunescapeSmallFont());
        setBorder(new EmptyBorder(1, 6, 1, 6));
        setOpaque(false);
    }

    public void setStatus(String text, Color accent) {
        this.accent = accent;
        setText(text);
        setForeground(accent);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BingoTheme.withAlpha(accent, 50));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(BingoTheme.withAlpha(accent, 140));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
