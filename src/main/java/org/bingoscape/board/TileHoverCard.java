package org.bingoscape.board;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.BingoTheme;
import org.bingoscape.ui.TileStatusStyle;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.IntFunction;

/**
 * Hover card shown next to a hovered board tile: title and XP pill, status pill, description and the
 * goal tree with progress. Rendered through RuneLite's tooltip manager, which positions it at the mouse.
 */
final class TileHoverCard implements LayoutableRenderableEntity {
    static final int WIDTH = 260;
    private static final int PADDING = 8;
    private static final int MAX_TITLE_LINES = 3;
    private static final int MAX_DESCRIPTION_LINES = 4;

    private final Tile tile;
    private final IntFunction<BufferedImage> itemImages;
    private final Rectangle bounds = new Rectangle();
    private Point location = new Point();

    TileHoverCard(Tile tile, IntFunction<BufferedImage> itemImages) {
        this.tile = tile;
        this.itemImages = itemImages;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        int contentWidth = WIDTH - 2 * PADDING;

        // Measure on a scratch graphics first: the background needs the height before the content is drawn
        int contentHeight;
        Graphics2D scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            contentHeight = paintContent(scratch, 0, 0, contentWidth);
        } finally {
            scratch.dispose();
        }

        Dimension size = new Dimension(WIDTH, contentHeight + 2 * PADDING);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(location.x, location.y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(ColorScheme.DARKER_GRAY_COLOR);
            g.fillRoundRect(0, 0, size.width, size.height, BingoTheme.CORNER_RADIUS, BingoTheme.CORNER_RADIUS);
            g.setColor(ColorScheme.BORDER_COLOR);
            g.drawRoundRect(0, 0, size.width - 1, size.height - 1, BingoTheme.CORNER_RADIUS, BingoTheme.CORNER_RADIUS);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            paintContent(g, PADDING, PADDING, contentWidth);
        } finally {
            g.dispose();
        }

        bounds.setBounds(location.x, location.y, size.width, size.height);
        return size;
    }

    /**
     * Paints the card content at (x, y) and returns the y below it.
     */
    private int paintContent(Graphics2D g, int x, int y, int width) {
        // Title with the XP pill on the right
        g.setFont(FontManager.getRunescapeSmallFont());
        String xp = tile.getWeight() + " XP";
        int xpWidth = g.getFontMetrics().stringWidth(xp) + 12;
        int pillHeight = BoardDraw.pillHeight(g);
        BoardDraw.pill(g, xp, x + width - xpWidth, y, BingoTheme.GOLD);

        g.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics bold = g.getFontMetrics();
        int titleEnd = y;
        for (String line : TextWrap.wrap(tile.getTitle(), bold, Math.max(0, width - xpWidth - 6), MAX_TITLE_LINES)) {
            BoardDraw.text(g, line, x, titleEnd + bold.getAscent(), Color.WHITE);
            titleEnd += bold.getHeight();
        }
        y = Math.max(titleEnd, y + pillHeight) + 4;

        // Status pill
        TileSubmissionType status = TileStatusStyle.statusOf(tile);
        if (TileStatusStyle.isSubmitted(status)) {
            g.setFont(FontManager.getRunescapeSmallFont());
            BoardDraw.pill(g, TileStatusStyle.displayText(status), x, y, TileStatusStyle.accent(status));
            y += pillHeight + 6;
        }

        // Description
        String description = tile.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fm = g.getFontMetrics();
            for (String line : TextWrap.wrap(description, fm, width, MAX_DESCRIPTION_LINES)) {
                BoardDraw.text(g, line, x, y + fm.getAscent(), BoardDraw.MUTED);
                y += fm.getHeight();
            }
            y += 6;
        }

        return TileDetailsView.paintGoals(g, tile, x, y, width, itemImages);
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public void setPreferredLocation(Point position) {
        this.location = position;
    }

    @Override
    public void setPreferredSize(Dimension dimension) {
        // Fixed width, height follows the content
    }
}
