package org.bingoscape.board;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Tile;
import org.bingoscape.services.TileImageCache;
import org.bingoscape.ui.TileStatusStyle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The standard tile faces (status background, XP, status label, image or wrapped title) that every
 * {@link BoardKind} builds on. Holds the per-board wrap memo, so there is one instance per overlay and it is
 * used on the render thread only.
 */
final class TilePainter {
    private static final int MIN_DETAIL_TILE_SIZE = 56;

    private final TileImageCache images;
    private final Map<String, List<String>> wrapMemo = new HashMap<>();
    private boolean showImages;
    private int lastTileSize = -1;

    TilePainter(TileImageCache images) {
        this.images = images;
    }

    /**
     * Call once per frame before painting tiles.
     */
    void beginFrame(boolean showImages, int tileSize) {
        this.showImages = showImages;
        // Wrapped titles depend on tile size; drop them when it changes so the memo can't grow on resize
        if (tileSize != lastTileSize) {
            wrapMemo.clear();
            lastTileSize = tileSize;
        }
    }

    /**
     * Drops cached title wrapping, e.g. when a different board is shown.
     */
    void reset() {
        wrapMemo.clear();
    }

    void paintHidden(Graphics2D g, Rectangle r) {
        g.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(ColorScheme.BORDER_COLOR);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FontManager.getDefaultBoldFont().deriveFont(Font.BOLD, 24f));
        BoardDraw.centeredText(g, "?", r, Color.GRAY);
    }

    void paintStandard(Graphics2D g, Rectangle r, TileView view, boolean hovered) {
        Color background = TileStatusStyle.background(view.status);
        g.setColor(hovered ? background.brighter() : background);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(TileStatusStyle.border(view.status));
        g.setStroke(new BasicStroke(2));
        g.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);

        Rectangle inner = new Rectangle(r.x + 4, r.y + 4, r.width - 8, r.height - 8);
        boolean showDetails = r.width >= MIN_DETAIL_TILE_SIZE;
        int top = inner.y;
        int bottom = inner.y + inner.height;

        if (showDetails) {
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fm = g.getFontMetrics();
            BoardDraw.text(g, view.xpLabel, inner.x + inner.width - fm.stringWidth(view.xpLabel),
                    inner.y + fm.getAscent(), BoardTooltipBuilder.GOLD);
            top += fm.getHeight();

            String label = TileStatusStyle.shortLabel(view.status);
            if (label != null) {
                g.setFont(FontManager.getRunescapeBoldFont());
                FontMetrics bold = g.getFontMetrics();
                String fitted = TextWrap.ellipsize(label, bold, inner.width);
                bottom -= bold.getHeight();
                BoardDraw.text(g, fitted, inner.x + (inner.width - bold.stringWidth(fitted)) / 2,
                        bottom + bold.getAscent(), TileStatusStyle.border(view.status));
            }
        }

        Rectangle content = new Rectangle(inner.x, top, inner.width, Math.max(0, bottom - top));
        paintContent(g, content, view);
    }

    /**
     * The tile image, or the wrapped title when images are off, missing or failed to load.
     */
    void paintContent(Graphics2D g, Rectangle box, TileView view) {
        String url = view.imageUrl;
        if (showImages && url != null && !images.hasFailed(url)) {
            BufferedImage image = images.getScaled(url, box.width, box.height);
            if (image != null) {
                g.drawImage(image, box.x + (box.width - image.getWidth()) / 2, box.y + (box.height - image.getHeight()) / 2, null);
                return;
            }

            // Scaled copy not ready yet (first frame or after a resize): draw the source scaled on the fly
            BufferedImage source = images.getSource(url);
            if (source != null) {
                Rectangle target = BoardDraw.fitted(source.getWidth(), source.getHeight(), box);
                Graphics2D ig = (Graphics2D) g.create();
                try {
                    ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    ig.drawImage(source, target.x, target.y, target.width, target.height, null);
                } finally {
                    ig.dispose();
                }
                return;
            }

            g.setFont(FontManager.getRunescapeSmallFont());
            BoardDraw.centeredText(g, "Loading...", box, Color.LIGHT_GRAY);
            return;
        }

        paintTitle(g, box, view);
    }

    void paintTitle(Graphics2D g, Rectangle box, TileView view) {
        String title = view.title;
        g.setFont(TextWrap.displayable(FontManager.getRunescapeFont(), title));
        FontMetrics fm = g.getFontMetrics();
        int maxLines = Math.max(1, box.height / fm.getHeight());
        // Keyed by text, not tile id: ids may be null, and equal titles wrap identically
        String key = title + "|" + box.width + "|" + maxLines;
        List<String> lines = wrapMemo.computeIfAbsent(key, k -> TextWrap.wrap(title, fm, box.width, maxLines));

        int y = box.y + (box.height - lines.size() * fm.getHeight()) / 2;
        for (String line : lines) {
            BoardDraw.text(g, line, box.x + (box.width - fm.stringWidth(line)) / 2, y + fm.getAscent(), Color.WHITE);
            y += fm.getHeight();
        }
    }
}
