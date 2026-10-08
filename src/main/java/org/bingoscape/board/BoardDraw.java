package org.bingoscape.board;

import org.bingoscape.ui.BingoTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Small drawing primitives shared by the board overlay painters.
 */
final class BoardDraw {
    private static final Color SHADOW = Color.BLACK;
    private static final Color BUTTON_BG = BingoTheme.BUTTON_BACKGROUND;
    private static final Color BUTTON_HOVER = BingoTheme.BUTTON_HOVER;
    private static final Color BUTTON_BORDER = BingoTheme.BUTTON_BORDER;

    static final Color FOREGROUND = BingoTheme.FOREGROUND;
    static final Color MUTED = BingoTheme.MUTED;
    static final Color PROGRESS_COMPLETE = BingoTheme.PROGRESS_COMPLETE;
    static final Color PROGRESS_PARTIAL = BingoTheme.PROGRESS_PARTIAL;
    static final Color PROGRESS_BG = BingoTheme.PROGRESS_BACKGROUND;

    private BoardDraw() {
    }

    /**
     * Draws text with a 1px drop shadow, the usual RuneLite overlay style.
     */
    static void text(Graphics2D g, String text, int x, int baseline, Color color) {
        Font font = g.getFont();
        g.setFont(TextWrap.displayable(font, text));
        g.setColor(SHADOW);
        g.drawString(text, x + 1, baseline + 1);
        g.setColor(color);
        g.drawString(text, x, baseline);
        g.setFont(font);
    }

    static void centeredText(Graphics2D g, String text, Rectangle box, Color color) {
        FontMetrics fm = g.getFontMetrics();
        int x = box.x + (box.width - fm.stringWidth(text)) / 2;
        int baseline = box.y + (box.height - fm.getHeight()) / 2 + fm.getAscent();
        text(g, text, x, baseline, color);
    }

    static void button(Graphics2D g, Rectangle r, boolean hover, boolean enabled) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(enabled ? BUTTON_BG : withAlpha(BUTTON_BG, 120));
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            if (hover && enabled) {
                g2.setColor(BUTTON_HOVER);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            }
            g2.setColor(BUTTON_BORDER);
            g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 6, 6);
        } finally {
            g2.dispose();
        }
    }

    static int pillHeight(Graphics2D g) {
        return g.getFontMetrics().getHeight() + 2;
    }

    /**
     * Rounded status pill with the text in {@code accent}, like the AND/OR badges. Returns the pill width.
     */
    static int pill(Graphics2D g, String text, int x, int y, Color accent) {
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(text) + 12;
        int height = pillHeight(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(withAlpha(accent, 50));
            g2.fillRoundRect(x, y, width - 1, height - 1, height, height);
            g2.setColor(withAlpha(accent, 140));
            g2.drawRoundRect(x, y, width - 1, height - 1, height, height);
        } finally {
            g2.dispose();
        }
        text(g, text, x + 6, y + 1 + fm.getAscent(), accent);
        return width;
    }

    static void closeIcon(Graphics2D g, Rectangle r, boolean hover) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = Math.max(5, r.width * 3 / 10);
            int x1 = r.x + pad;
            int y1 = r.y + pad;
            int x2 = r.x + r.width - pad;
            int y2 = r.y + r.height - pad;
            g2.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawLine(x1 + 1, y1 + 1, x2 + 1, y2 + 1);
            g2.drawLine(x2 + 1, y1 + 1, x1 + 1, y2 + 1);
            g2.setColor(hover ? Color.WHITE : Color.LIGHT_GRAY);
            g2.drawLine(x1, y1, x2, y2);
            g2.drawLine(x2, y1, x1, y2);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Returns the largest rectangle with the image's aspect ratio that fits in {@code box}, centered.
     */
    static Rectangle fitted(int imageWidth, int imageHeight, Rectangle box) {
        if (imageWidth <= 0 || imageHeight <= 0 || box.width <= 0 || box.height <= 0) {
            return new Rectangle(box.x, box.y, 0, 0);
        }
        double ratio = Math.min((double) box.width / imageWidth, (double) box.height / imageHeight);
        int width = Math.max(1, (int) Math.round(imageWidth * ratio));
        int height = Math.max(1, (int) Math.round(imageHeight * ratio));
        return new Rectangle(box.x + (box.width - width) / 2, box.y + (box.height - height) / 2, width, height);
    }

    /**
     * Thin progress bar from the web app's compact goal tree: gray when nothing is done,
     * amber when partially done, green when complete.
     */
    static void progressBar(Graphics2D g, int x, int y, int width, int height, int current, int target, boolean complete) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(PROGRESS_BG);
            g2.fillRoundRect(x, y, width, height, 3, 3);
            if (target > 0) {
                int fill = (int) (width * Math.min(1.0, (double) current / target));
                g2.setColor(progressColor(current, complete));
                g2.fillRoundRect(x, y, fill, height, 3, 3);
            }
        } finally {
            g2.dispose();
        }
    }

    static Color progressColor(int current, boolean complete) {
        return BingoTheme.progressColor(current, complete);
    }

    static Color progressTextColor(int current, boolean complete, boolean hasProgress) {
        if (!hasProgress) {
            return MUTED;
        }
        if (complete) {
            return PROGRESS_COMPLETE;
        }
        return current > 0 ? PROGRESS_PARTIAL : MUTED;
    }

    static Color withAlpha(Color color, int alpha) {
        return BingoTheme.withAlpha(color, alpha);
    }
}
