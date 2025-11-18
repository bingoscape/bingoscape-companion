package org.bingoscape.ui.components;

import org.bingoscape.utils.GoalTreeProgressCalculator.ProgressResult;

import javax.swing.*;
import java.awt.*;

/**
 * A compact progress bar component designed to overlay on bingo tiles.
 * Shows goal completion progress with percentage and color coding.
 */
public class TileProgressBar extends JPanel {

    private static final int PROGRESS_BAR_HEIGHT = 6;
    private static final int PADDING = 2;
    private static final Font PERCENTAGE_FONT = new Font("Arial", Font.BOLD, 9);

    // Color scheme matching the web app
    private static final Color COLOR_COMPLETE = new Color(16, 185, 129);     // Green #10b981
    private static final Color COLOR_PARTIAL = new Color(245, 158, 11);       // Amber #f59e0b
    private static final Color COLOR_NONE = new Color(107, 114, 128);         // Gray #6b7280
    private static final Color COLOR_BACKGROUND = new Color(31, 41, 55, 200); // Dark semi-transparent

    private final ProgressResult progress;

    /**
     * Create a new tile progress bar.
     *
     * @param progress The progress result to display
     */
    public TileProgressBar(ProgressResult progress) {
        this.progress = progress;
        setupComponent();
    }

    private void setupComponent() {
        setOpaque(false);
        setPreferredSize(new Dimension(0, PROGRESS_BAR_HEIGHT + (PADDING * 2)));
        setMinimumSize(new Dimension(0, PROGRESS_BAR_HEIGHT + (PADDING * 2)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (progress == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw background bar
        g2d.setColor(COLOR_BACKGROUND);
        g2d.fillRect(0, PADDING, width, PROGRESS_BAR_HEIGHT);

        // Calculate progress width
        int progressWidth = (int) (width * (progress.getPercentage() / 100.0));

        // Determine progress color
        Color progressColor;
        if (progress.isComplete()) {
            progressColor = COLOR_COMPLETE;
        } else if (progress.getCompletedCount() > 0) {
            progressColor = COLOR_PARTIAL;
        } else {
            progressColor = COLOR_NONE;
        }

        // Draw progress fill
        g2d.setColor(progressColor);
        g2d.fillRect(0, PADDING, progressWidth, PROGRESS_BAR_HEIGHT);

        // Draw percentage text
        String percentageText = String.format("%.0f%%", progress.getPercentage());
        g2d.setFont(PERCENTAGE_FONT);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(percentageText);
        int textHeight = fm.getAscent();

        int textX = (width - textWidth) / 2;
        int textY = PADDING + (PROGRESS_BAR_HEIGHT / 2) + (textHeight / 2) - 1;

        // Draw text shadow for better visibility
        g2d.setColor(Color.BLACK);
        g2d.drawString(percentageText, textX + 1, textY + 1);

        // Draw text
        g2d.setColor(Color.WHITE);
        g2d.drawString(percentageText, textX, textY);

        g2d.dispose();
    }

    /**
     * Create a progress bar panel if the progress is valid.
     *
     * @param progress The progress result, or null if no goals
     * @return A TileProgressBar component, or null if progress is null
     */
    public static TileProgressBar createProgressBar(ProgressResult progress) {
        if (progress == null) {
            return null;
        }
        return new TileProgressBar(progress);
    }
}
