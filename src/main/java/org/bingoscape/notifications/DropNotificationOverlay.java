package org.bingoscape.notifications;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.ui.BingoTheme;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Drop popup at the top center of the game view. Colored by the value tier of the drop, slides in,
 * holds, fades out; further drops wait in a queue.
 */
@Singleton
public class DropNotificationOverlay extends Overlay {
    static final long SLIDE_MS = 450;
    static final long FADE_MS = 400;
    private static final int MAX_QUEUE = 10;
    private static final double QUEUED_HOLD_FACTOR = 0.6;

    private static final int BASE_WIDTH = 300;
    private static final int PADDING = 8;
    private static final int ICON_BOX = 40;
    private static final int ROW_GAP = 1;
    private static final int RADIUS = BingoTheme.CORNER_RADIUS;
    private static final Color WARNING = BingoTheme.PROGRESS_PARTIAL;

    private final BingoScapeConfig config;
    private final ItemManager itemManager;
    private final Queue<DropNotification> queue = new ConcurrentLinkedQueue<>();

    // Render-thread only
    private DropNotification current;
    private long startedAt;
    private boolean hurry;
    private int cachedScale = -1;
    private Font regular;
    private Font bold;

    // Text of the current popup fitted to the card; reset when the popup or the scale changes
    private String fittedName;
    private String valueText;
    private String fittedTile;
    private String fittedWarning;

    @Inject
    public DropNotificationOverlay(BingoScapeConfig config, ItemManager itemManager) {
        this.config = config;
        this.itemManager = itemManager;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
        setMovable(false);
    }

    public void enqueue(DropNotification notification) {
        if (queue.size() < MAX_QUEUE) {
            queue.offer(notification);
        }
    }

    public void clear() {
        queue.clear();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        long now = System.currentTimeMillis();
        if (current != null && now - startedAt >= totalMs(holdMs(), hurry)) {
            current = null;
        }
        if (current == null) {
            current = queue.poll();
            if (current != null) {
                startedAt = now;
                // Decided once per popup so the fade timing cannot jump when more drops arrive
                hurry = !queue.isEmpty();
                resetFittedText();
            }
        }
        if (current == null) {
            return null;
        }

        applyScale();
        double scale = config.notificationScale() / 100.0;
        int pad = (int) Math.round(PADDING * scale);
        FontMetrics small = graphics.getFontMetrics(regular);
        FontMetrics big = graphics.getFontMetrics(bold);
        Layout layout = Layout.of(small.getHeight(), big.getHeight(), pad, current.getWarning() != null);
        int width = (int) Math.round(BASE_WIDTH * scale);

        long elapsed = now - startedAt;
        float alpha = alpha(elapsed, holdMs(), hurry);
        int slideOffset = (int) Math.round(-layout.height * (1 - slideProgress(elapsed)));

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            // Slide inside the overlay bounds so nothing is drawn above the game view
            g.clipRect(0, 0, width, layout.height);
            g.translate(0, slideOffset);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            paintCard(g, current, width, layout, scale, pad);
        } finally {
            g.dispose();
        }
        return new Dimension(width, layout.height);
    }

    /**
     * Vertical layout derived from the font heights, so the rows never overlap at any scale.
     */
    static final class Layout {
        final int pillHeight;
        final int captionY;
        final int nameY;
        final int statY;
        final int contentBottom;
        final int warningY;
        final int height;

        private Layout(int pillHeight, int captionY, int nameY, int statY, int contentBottom, int warningY, int height) {
            this.pillHeight = pillHeight;
            this.captionY = captionY;
            this.nameY = nameY;
            this.statY = statY;
            this.contentBottom = contentBottom;
            this.warningY = warningY;
            this.height = height;
        }

        static Layout of(int smallHeight, int boldHeight, int pad, boolean warning) {
            int pillHeight = smallHeight + 2;
            int captionY = pad;
            int nameY = captionY + pillHeight + ROW_GAP;
            int statY = nameY + boldHeight;
            int contentBottom = statY + smallHeight + pad;
            int warningHeight = warning ? smallHeight + 4 : 0;
            return new Layout(pillHeight, captionY, nameY, statY, contentBottom, contentBottom, contentBottom + warningHeight);
        }
    }

    private void paintCard(Graphics2D g, DropNotification drop, int width, Layout layout, double scale, int pad) {
        Color accent = drop.getTier().color();
        int arc = (int) Math.round(RADIUS * scale);
        int height = layout.height;

        // Card with a soft tier-colored glow under the border
        g.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g.fillRoundRect(0, 0, width, height, arc, arc);
        g.setColor(BingoTheme.withAlpha(accent, 30));
        g.fillRoundRect(0, 0, width, height, arc, arc);
        g.setStroke(new BasicStroke(3f));
        g.setColor(BingoTheme.withAlpha(accent, 60));
        g.drawRoundRect(1, 1, width - 3, height - 3, arc, arc);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(accent);
        g.drawRoundRect(1, 1, width - 3, height - 3, arc, arc);

        // Item icon on a tinted square, centered in the content area
        int box = (int) Math.round(ICON_BOX * scale);
        int boxY = Math.max(pad, (layout.contentBottom - box) / 2);
        g.setColor(BingoTheme.withAlpha(accent, 50));
        g.fillRoundRect(pad, boxY, box, box, arc, arc);
        g.setColor(BingoTheme.withAlpha(accent, 140));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(pad, boxY, box - 1, box - 1, arc, arc);
        paintIcon(g, drop, pad, boxY, box, accent);

        int textX = pad + box + pad;
        int textRight = width - pad;

        // Caption row: "Bingo Item" left, tier pill (and +N queued) right
        g.setFont(regular);
        FontMetrics small = g.getFontMetrics();
        shadowed(g, "Bingo Item", textX, layout.captionY + small.getAscent(), BingoTheme.MUTED);

        String tierLabel = drop.getTier().label();
        int pillWidth = small.stringWidth(tierLabel) + 12;
        int pillX = textRight - pillWidth;
        g.setColor(BingoTheme.withAlpha(accent, 50));
        g.fillRoundRect(pillX, layout.captionY, pillWidth - 1, layout.pillHeight - 1, layout.pillHeight, layout.pillHeight);
        g.setColor(BingoTheme.withAlpha(accent, 140));
        g.drawRoundRect(pillX, layout.captionY, pillWidth - 1, layout.pillHeight - 1, layout.pillHeight, layout.pillHeight);
        shadowed(g, tierLabel, pillX + 6, layout.captionY + 1 + small.getAscent(), accent);

        int queued = queue.size();
        if (queued > 0) {
            String more = "+" + queued;
            shadowed(g, more, pillX - 4 - small.stringWidth(more), layout.captionY + small.getAscent(), BingoTheme.MUTED);
        }

        // Item name in the tier color
        g.setFont(bold);
        FontMetrics big = g.getFontMetrics();
        if (fittedName == null) {
            String name = (drop.getQuantity() > 1 ? drop.getQuantity() + " x " : "") + drop.getItemName();
            fittedName = ellipsize(name, big, textRight - textX);
        }
        shadowed(g, fittedName, textX, layout.nameY + big.getAscent(), accent);

        // Value left, tile right
        g.setFont(regular);
        if (valueText == null) {
            valueText = drop.getStackValue() > 0 ? DropTierResolver.formatGp(drop.getStackValue()) + " gp" : "No value";
            if (drop.getTileTitle() != null) {
                String tile = drop.getTileTitle() + (drop.getExtraTiles() > 0 ? " +" + drop.getExtraTiles() : "");
                fittedTile = ellipsize(tile, small, textRight - (textX + small.stringWidth(valueText) + pad));
            }
        }
        shadowed(g, valueText, textX, layout.statY + small.getAscent(), Color.WHITE);
        if (fittedTile != null) {
            shadowed(g, fittedTile, textRight - small.stringWidth(fittedTile), layout.statY + small.getAscent(), BingoTheme.MUTED);
        }

        if (drop.getWarning() != null) {
            if (fittedWarning == null) {
                fittedWarning = ellipsize(drop.getWarning(), small, width - 2 * pad);
            }
            g.setColor(BingoTheme.withAlpha(WARNING, 40));
            g.fillRect(2, layout.warningY, width - 4, height - layout.warningY - 2);
            int textY = layout.warningY + (height - 2 - layout.warningY - small.getHeight()) / 2 + small.getAscent();
            shadowed(g, fittedWarning, pad, textY, WARNING);
        }
    }

    private void paintIcon(Graphics2D g, DropNotification drop, int x, int y, int box, Color accent) {
        // The stack size is part of the name ("N x ..."), so no count on the sprite
        BufferedImage icon = itemManager.getImage(drop.getItemId(), drop.getQuantity(), false);
        if (icon == null) {
            // Sprite not loaded: first letter on the tinted square
            g.setFont(bold);
            String letter = drop.getItemName().isEmpty() ? "?" : drop.getItemName().substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            shadowed(g, letter, x + (box - fm.stringWidth(letter)) / 2,
                    y + (box - fm.getHeight()) / 2 + fm.getAscent(), accent);
            return;
        }

        // Fit the sprite into the box, keep it crisp
        double ratio = Math.min((box - 4.0) / icon.getWidth(), (box - 4.0) / icon.getHeight());
        int w = Math.max(1, (int) Math.round(icon.getWidth() * ratio));
        int h = Math.max(1, (int) Math.round(icon.getHeight() * ratio));
        Object previous = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(icon, x + (box - w) / 2, y + (box - h) / 2, w, h, null);
        if (previous != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);
        }
    }

    private void applyScale() {
        int scale = config.notificationScale();
        if (scale == cachedScale) {
            return;
        }
        cachedScale = scale;
        Font small = FontManager.getRunescapeSmallFont();
        Font large = FontManager.getRunescapeBoldFont();
        regular = small.deriveFont(small.getSize2D() * scale / 100f);
        bold = large.deriveFont(large.getSize2D() * scale / 100f);
        resetFittedText();
    }

    private void resetFittedText() {
        fittedName = null;
        valueText = null;
        fittedTile = null;
        fittedWarning = null;
    }

    private long holdMs() {
        return config.notificationSeconds() * 1000L;
    }

    private static void shadowed(Graphics2D g, String text, int x, int baseline, Color color) {
        g.setColor(Color.BLACK);
        g.drawString(text, x + 1, baseline + 1);
        g.setColor(color);
        g.drawString(text, x, baseline);
    }

    private static String ellipsize(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    /**
     * Visible time of one popup: slide in, hold, fade out. The hold shrinks when more popups wait.
     */
    static long totalMs(long holdMs, boolean hurry) {
        return SLIDE_MS + effectiveHold(holdMs, hurry) + FADE_MS;
    }

    private static long effectiveHold(long holdMs, boolean hurry) {
        return hurry ? Math.round(holdMs * QUEUED_HOLD_FACTOR) : holdMs;
    }

    /**
     * Overall opacity at {@code elapsedMs}: fades in with the slide, full during the hold, fades out at the end.
     */
    static float alpha(long elapsedMs, long holdMs, boolean hurry) {
        long fadeStart = SLIDE_MS + effectiveHold(holdMs, hurry);
        if (elapsedMs < SLIDE_MS) {
            return clamp(elapsedMs / (float) SLIDE_MS);
        }
        if (elapsedMs < fadeStart) {
            return 1f;
        }
        return clamp(1f - (elapsedMs - fadeStart) / (float) FADE_MS);
    }

    /**
     * Ease-out-cubic slide progress, 0 at start and 1 once the popup has arrived.
     */
    static float slideProgress(long elapsedMs) {
        float t = clamp(elapsedMs / (float) SLIDE_MS);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
