package org.bingoscape.ui.components;

import org.bingoscape.ui.BingoTheme;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Thin rounded progress bar using the overlay's goal progress colors (gray, amber, green).
 */
public class ProgressBar extends JComponent {
    private static final int HEIGHT = 6;

    private int current;
    private int target;

    public ProgressBar() {
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public void setProgress(int current, int target) {
        this.current = current;
        this.target = target;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BingoTheme.PROGRESS_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
            if (target > 0) {
                int fill = (int) (getWidth() * Math.min(1.0, (double) current / target));
                g2.setColor(BingoTheme.progressColor(current, current >= target));
                g2.fillRoundRect(0, 0, fill, getHeight(), 3, 3);
            }
        } finally {
            g2.dispose();
        }
    }
}
