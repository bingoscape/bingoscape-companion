package org.bingoscape.board;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.LockedTileDisplay;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.Tile;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

/**
 * A progression board: tiles are grouped into tier bands, lowest tier on top, each with a header showing the
 * tier's XP or what is still needed to unlock it. Tiles of locked tiers are server-side stubs without any
 * content; they are shown dimmed with a lock (or not at all) and can never be submitted.
 */
final class ProgressionKind implements BoardKind {
    /** Locked tiles stay visible, dimmed with a lock. */
    static final ProgressionKind SHOW_LOCKED = new ProgressionKind(false);
    /** Locked tiles are not drawn; their band shrinks to its header. */
    static final ProgressionKind HIDE_LOCKED = new ProgressionKind(true);

    static final int BAND_HEADER_HEIGHT = 16;
    static final int BAND_GAP = 6;
    static final int LEGEND_HEIGHT = 14;
    static final int LEGEND_SPACING = 4;

    private static final Color LOCKED_BACKGROUND = new Color(38, 38, 38);
    private static final Color LOCKED_HOVER = new Color(52, 52, 52);
    private static final Color LOCKED_DIM = new Color(38, 38, 38, 190);
    private static final int HEADER_GLYPH_SIZE = 12;
    private static final int MIN_TILE_GLYPH = 12;
    private static final int MAX_TILE_GLYPH = 32;

    private final boolean hideLocked;

    private ProgressionKind(boolean hideLocked) {
        this.hideLocked = hideLocked;
    }

    @Override
    public BingoType type() {
        return BingoType.PROGRESSION;
    }

    @Override
    public BoardKind withOptions(BingoScapeConfig config) {
        return config.progressionShowLockedTiles() == LockedTileDisplay.HIDE ? HIDE_LOCKED : SHOW_LOCKED;
    }

    @Override
    public Object prepare(Bingo bingo, List<Tile> tiles) {
        return TierLadder.of(bingo, tiles);
    }

    @Override
    public BoardLayout layout(BoardModel model, int viewportWidth, int viewportHeight, double scale, boolean hasCodephrase) {
        TierLadder ladder = ladder(model);
        if (ladder == null || ladder.bands.length == 0) {
            return BoardLayout.compute(model.bingo.getRows(), model.bingo.getColumns(), model.views.length,
                    viewportWidth, viewportHeight, scale, hasCodephrase);
        }
        return layoutBands(ladder, model.views.length, model.bingo.getColumns(), hideLocked, viewportWidth,
                viewportHeight, scale, hasCodephrase);
    }

    /**
     * Stacks the tier bands top to bottom, each a header row followed by the band's tiles in rows of at most
     * {@code columns}. Tile rectangles are returned in model tile order. Locked bands collapse to their header
     * when {@code hideLocked}; their tiles get empty rectangles that no point hits.
     */
    static BoardLayout layoutBands(TierLadder ladder, int tileCount, int columns, boolean hideLocked, int viewportWidth,
                                   int viewportHeight, double scale, boolean hasCodephrase) {
        int cols = columns <= 0 ? BoardLayout.DEFAULT_GRID_SIZE : columns;
        int bandCount = ladder.bands.length;

        int usedCols = 1;
        int totalRows = 0;
        int[] rowsOfBand = new int[bandCount];
        for (int b = 0; b < bandCount; b++) {
            TierLadder.Band band = ladder.bands[b];
            int count = hideLocked && band.locked ? 0 : band.tiles.length;
            rowsOfBand[b] = (count + cols - 1) / cols;
            totalRows += rowsOfBand[b];
            usedCols = Math.max(usedCols, Math.min(cols, count));
        }

        int headerHeight = BoardLayout.TITLE_HEIGHT + (hasCodephrase ? BoardLayout.CODEPHRASE_HEIGHT : 0)
                + BoardLayout.HEADER_SPACING;
        int legendBlock = LEGEND_HEIGHT + LEGEND_SPACING;
        int availableWidth = (int) (viewportWidth * BoardLayout.VIEWPORT_FILL) - 2 * BoardLayout.PAD;
        int availableHeight = (int) (viewportHeight * BoardLayout.VIEWPORT_FILL) - 2 * BoardLayout.PAD
                - headerHeight - legendBlock;
        // Everything in the grid column that is not a tile: band headers, gaps between bands and between rows
        int chrome = bandCount * BAND_HEADER_HEIGHT + (bandCount - 1) * BAND_GAP
                + Math.max(0, totalRows - countNonEmpty(rowsOfBand)) * BoardLayout.GAP;
        int fitWidth = (availableWidth - (usedCols - 1) * BoardLayout.GAP) / usedCols;
        int fitHeight = totalRows == 0 ? Integer.MAX_VALUE : (availableHeight - chrome) / totalRows;
        int preferred = (int) Math.round(BoardLayout.TILE_DEFAULT * scale);
        int tileSize = Math.max(BoardLayout.TILE_MIN, Math.min(preferred, Math.min(fitWidth, fitHeight)));

        int gridWidth = usedCols * tileSize + (usedCols - 1) * BoardLayout.GAP;
        int gridHeight = chrome + totalRows * tileSize;
        int width = Math.max(BoardLayout.MIN_WIDTH, 2 * BoardLayout.PAD + gridWidth);
        int height = 2 * BoardLayout.PAD + headerHeight + legendBlock + gridHeight;

        int gridX = (width - gridWidth) / 2;
        int gridY = BoardLayout.PAD + headerHeight + legendBlock;
        Rectangle gridArea = new Rectangle(gridX, gridY, gridWidth, gridHeight);
        Rectangle legend = new Rectangle(BoardLayout.PAD, BoardLayout.PAD + headerHeight,
                width - 2 * BoardLayout.PAD, LEGEND_HEIGHT);

        Rectangle[] tiles = new Rectangle[tileCount];
        Rectangle[] sections = new Rectangle[bandCount];
        int y = gridY;
        for (int b = 0; b < bandCount; b++) {
            TierLadder.Band band = ladder.bands[b];
            sections[b] = new Rectangle(gridX, y, gridWidth, BAND_HEADER_HEIGHT);
            y += BAND_HEADER_HEIGHT;

            boolean collapsed = hideLocked && band.locked;
            for (int i = 0; i < band.tiles.length; i++) {
                if (collapsed) {
                    // A zero-size rectangle contains no point, so the tile can be neither hovered nor clicked
                    tiles[band.tiles[i]] = new Rectangle(gridX, y, 0, 0);
                } else {
                    tiles[band.tiles[i]] = new Rectangle(gridX + (i % cols) * (tileSize + BoardLayout.GAP),
                            y + (i / cols) * (tileSize + BoardLayout.GAP), tileSize, tileSize);
                }
            }
            if (rowsOfBand[b] > 0) {
                y += rowsOfBand[b] * tileSize + (rowsOfBand[b] - 1) * BoardLayout.GAP;
            }
            if (b < bandCount - 1) {
                y += BAND_GAP;
            }
        }

        return BoardLayout.withTiles(totalRows, usedCols, width, height, tileSize, hasCodephrase, gridArea,
                Arrays.asList(tiles), legend, Arrays.asList(sections));
    }

    private static int countNonEmpty(int[] rows) {
        int n = 0;
        for (int r : rows) {
            if (r > 0) {
                n++;
            }
        }
        return n;
    }

    @Override
    public TileView view(Bingo bingo, Tile tile, int index, Object prepared) {
        if (!(prepared instanceof TierLadder)) {
            return TileView.of(tile, index);
        }

        TierLadder.Band band = ((TierLadder) prepared).bandOf(tile.getTier() == null ? 0 : tile.getTier());
        // The tile flag alone also counts: the server marks stubs even if tiers[] were missing
        boolean locked = tile.isTierLocked() || (band != null && band.locked);
        String note = null;
        if (locked) {
            note = band != null && band.lockNote != null ? band.lockNote : "Tier locked";
        }
        return TileView.of(tile, index, locked, note);
    }

    @Override
    public void paintTile(Graphics2D g, Rectangle r, TileView view, boolean hovered, TilePainter painter) {
        if (view.tierLocked) {
            if (!hideLocked && r.width > 0) {
                paintLocked(g, r, view, hovered, painter);
            }
        } else if (view.hidden) {
            painter.paintHidden(g, r);
        } else {
            painter.paintStandard(g, r, view, hovered);
        }
    }

    /**
     * Dark face with a lock. A locked tile normally has no content; if one does carry a title or image it is
     * shown under a gray veil, and null content is never drawn.
     */
    private static void paintLocked(Graphics2D g, Rectangle r, TileView view, boolean hovered, TilePainter painter) {
        g.setColor(hovered ? LOCKED_HOVER : LOCKED_BACKGROUND);
        g.fillRect(r.x, r.y, r.width, r.height);

        if (!view.title.isEmpty() || view.imageUrl != null) {
            painter.paintContent(g, new Rectangle(r.x + 4, r.y + 4, r.width - 8, r.height - 8), view);
            g.setColor(LOCKED_DIM);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        g.setColor(ColorScheme.BORDER_COLOR);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);

        int size = Math.max(MIN_TILE_GLYPH, Math.min(MAX_TILE_GLYPH, Math.min(r.width, r.height) * 2 / 5));
        BufferedImage lock = Glyphs.lock(size);
        g.drawImage(lock, r.x + (r.width - size) / 2, r.y + (r.height - size) / 2, null);
    }

    @Override
    public void paintHeaderExtras(Graphics2D g, BoardLayout layout, BoardModel model) {
        TierLadder ladder = ladder(model);
        if (ladder == null || layout.legend == null) {
            return;
        }

        g.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = g.getFontMetrics();
        BoardDraw.text(g, TextWrap.ellipsize(ladder.legend, fm, layout.legend.width), layout.legend.x,
                layout.legend.y + fm.getAscent(), BoardDraw.MUTED);

        for (int b = 0; b < ladder.bands.length && b < layout.sections.size(); b++) {
            paintBandHeader(g, fm, layout.sections.get(b), ladder.bands[b]);
        }
    }

    private static void paintBandHeader(Graphics2D g, FontMetrics fm, Rectangle r, TierLadder.Band band) {
        int x = r.x + 2;
        int textWidth = r.width - 4;
        if (band.locked) {
            g.drawImage(Glyphs.lock(HEADER_GLYPH_SIZE), x, r.y + (r.height - HEADER_GLYPH_SIZE) / 2, null);
            x += HEADER_GLYPH_SIZE + 3;
            textWidth -= HEADER_GLYPH_SIZE + 3;
        }

        Color color = band.locked ? BoardDraw.MUTED : band.complete() ? BoardDraw.PROGRESS_COMPLETE : BoardDraw.FOREGROUND;
        int baseline = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        BoardDraw.text(g, TextWrap.ellipsize(band.header, fm, textWidth), x, baseline, color);

        // Thin XP bar along the bottom edge of an unlocked band
        if (!band.locked && band.requiredXp > 0 && band.earnedXp >= 0) {
            BoardDraw.progressBar(g, r.x, r.y + r.height - 3, r.width, 3, clamp(band.earnedXp), clamp(band.requiredXp),
                    band.complete());
        }
    }

    private static int clamp(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    @Override
    public boolean canOpen(TileView view) {
        // Locked tiles open too, to explain why they are locked; only hidden-by-config ones cannot be reached
        return !view.hidden && view.id != null && !(hideLocked && view.tierLocked);
    }

    @Override
    public boolean canSubmit(TileView view, Bingo bingo) {
        return !view.tierLocked && StandardKind.INSTANCE.canSubmit(view, bingo);
    }

    @Override
    public String primaryLabel(TileView view, Bingo bingo) {
        return view.tierLocked ? "Tier locked" : BoardKind.super.primaryLabel(view, bingo);
    }

    @Override
    public String detailsNotice(TileView view) {
        return view.tierLocked ? view.lockNote : null;
    }

    @Override
    public Tooltip tooltip(TileView view, IntFunction<BufferedImage> itemImages) {
        if (view.tierLocked) {
            return view.lockNote == null ? null : new Tooltip(BoardTooltipBuilder.sanitize(view.lockNote));
        }
        return StandardKind.INSTANCE.tooltip(view, itemImages);
    }

    private static TierLadder ladder(BoardModel model) {
        return model.data instanceof TierLadder ? (TierLadder) model.data : null;
    }
}
