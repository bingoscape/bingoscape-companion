package org.bingoscape.board;

import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.util.ColorUtil;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.Tile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.IntFunction;

/**
 * A battleship board: a regular grid whose tiles are blind "?" cells until the event starts. Afterwards the
 * calling team's shots are drawn on top of the tile faces, each with its own shape as well as colour: hit is
 * an X, miss a hollow dot on a dimmed face, sunk a ship in a ring. A legend line shows the opponent ships
 * remaining. Blind tiles can be neither opened nor submitted, and nothing can be submitted before the start.
 */
final class BattleshipKind implements BoardKind {
    static final BattleshipKind SHOW_MARKERS = new BattleshipKind(true);
    static final BattleshipKind HIDE_MARKERS = new BattleshipKind(false);

    static final String BLIND_NOTICE = "Blind tile: its content is revealed when the event starts.";
    static final String NOT_STARTED_NOTICE = "The event has not started yet. Tiles can be submitted once it begins.";
    static final String BLIND_LABEL = "Blind tile";
    static final String NOT_STARTED_LABEL = "Not started";

    private static final Color MISS_DIM = new Color(20, 24, 30, 150);
    private static final Color HIT_TINT = new Color(239, 68, 68, 70);
    private static final Color SUNK_TINT = new Color(56, 189, 248, 60);
    private static final BasicStroke SUNK_BORDER = new BasicStroke(2);
    private static final int MIN_TILE_GLYPH = 12;
    private static final int MAX_TILE_GLYPH = 48;
    private static final int LEGEND_GLYPH = 12;
    private static final int LEGEND_GAP = 10;
    private static final int LEGEND_ICON_GAP = 3;
    private static final String[] LEGEND_LABELS = {"Hit", "Miss", "Sunk"};

    private final boolean showMarkers;

    private BattleshipKind(boolean showMarkers) {
        this.showMarkers = showMarkers;
    }

    @Override
    public BingoType type() {
        return BingoType.BATTLESHIP;
    }

    @Override
    public BoardKind withOptions(BingoScapeConfig config) {
        return config.battleshipShowMarkers() ? SHOW_MARKERS : HIDE_MARKERS;
    }

    @Override
    public Object prepare(Bingo bingo, List<Tile> tiles) {
        return BattleshipMarks.of(bingo.getBattleship());
    }

    @Override
    public BoardLayout layout(BoardModel model, int viewportWidth, int viewportHeight, double scale, boolean hasCodephrase) {
        return BoardLayout.compute(model.bingo.getRows(), model.bingo.getColumns(), model.views.length,
                viewportWidth, viewportHeight, scale, hasCodephrase, true);
    }

    @Override
    public TileView view(Bingo bingo, Tile tile, int index, Object prepared) {
        if (!(prepared instanceof BattleshipMarks)) {
            return TileView.of(tile, index);
        }

        BattleshipMarks marks = (BattleshipMarks) prepared;
        BattleshipMarks.Mark mark = showMarkers ? marks.markOf(tile) : BattleshipMarks.Mark.NONE;
        return TileView.of(tile, index, mark, marks.notStarted());
    }

    @Override
    public void paintTile(Graphics2D g, Rectangle r, TileView view, boolean hovered, TilePainter painter) {
        if (view.hidden || view.blind) {
            painter.paintHidden(g, r);
            return;
        }

        painter.paintStandard(g, r, view, hovered);
        switch (view.mark) {
            case MISS:
                g.setColor(MISS_DIM);
                g.fillRect(r.x, r.y, r.width, r.height);
                drawCentered(g, r, Glyphs.miss(glyphSize(r)));
                break;
            case HIT:
                g.setColor(HIT_TINT);
                g.fillRect(r.x, r.y, r.width, r.height);
                drawCentered(g, r, Glyphs.hit(glyphSize(r)));
                break;
            case SUNK:
                g.setColor(SUNK_TINT);
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(Glyphs.SUNK_COLOR);
                g.setStroke(SUNK_BORDER);
                g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
                drawCentered(g, r, Glyphs.sunk(glyphSize(r)));
                break;
            default:
                break;
        }
    }

    private static int glyphSize(Rectangle r) {
        return Math.max(MIN_TILE_GLYPH, Math.min(MAX_TILE_GLYPH, Math.min(r.width, r.height) * 11 / 20));
    }

    private static void drawCentered(Graphics2D g, Rectangle r, BufferedImage glyph) {
        g.drawImage(glyph, r.x + (r.width - glyph.getWidth()) / 2, r.y + (r.height - glyph.getHeight()) / 2, null);
    }

    @Override
    public void paintHeaderExtras(Graphics2D g, BoardLayout layout, BoardModel model) {
        if (layout.legend == null || !(model.data instanceof BattleshipMarks)) {
            return;
        }

        Rectangle legend = layout.legend;
        g.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = g.getFontMetrics();
        String text = ((BattleshipMarks) model.data).legend;
        int baseline = legend.y + (legend.height - fm.getHeight()) / 2 + fm.getAscent();

        int keyWidth = showMarkers ? keyWidth(fm) : 0;
        int textWidth = text == null ? 0 : fm.stringWidth(text);
        // The key is secondary: it is dropped when the ships line would not fit next to it
        boolean drawKey = keyWidth > 0 && textWidth + LEGEND_GAP + keyWidth <= legend.width;
        if (text != null) {
            int room = drawKey ? legend.width - keyWidth - LEGEND_GAP : legend.width;
            BoardDraw.text(g, TextWrap.ellipsize(text, fm, room), legend.x, baseline, BoardDraw.FOREGROUND);
        }
        if (drawKey) {
            paintKey(g, fm, legend.x + legend.width - keyWidth, legend.y + (legend.height - LEGEND_GLYPH) / 2, baseline);
        }
    }

    private static int keyWidth(FontMetrics fm) {
        int width = 0;
        for (int i = 0; i < LEGEND_LABELS.length; i++) {
            width += LEGEND_GLYPH + LEGEND_ICON_GAP + fm.stringWidth(LEGEND_LABELS[i]);
            if (i > 0) {
                width += LEGEND_GAP;
            }
        }
        return width;
    }

    private static void paintKey(Graphics2D g, FontMetrics fm, int x, int y, int baseline) {
        for (int i = 0; i < LEGEND_LABELS.length; i++) {
            BufferedImage glyph = i == 0 ? Glyphs.hit(LEGEND_GLYPH) : i == 1 ? Glyphs.miss(LEGEND_GLYPH) : Glyphs.sunk(LEGEND_GLYPH);
            g.drawImage(glyph, x, y, null);
            x += LEGEND_GLYPH + LEGEND_ICON_GAP;
            BoardDraw.text(g, LEGEND_LABELS[i], x, baseline, BoardDraw.MUTED);
            x += fm.stringWidth(LEGEND_LABELS[i]) + LEGEND_GAP;
        }
    }

    @Override
    public boolean canOpen(TileView view) {
        return !view.hidden && !view.blind && view.id != null;
    }

    @Override
    public boolean canSubmit(TileView view, Bingo bingo) {
        return !view.blind && !view.notStarted && StandardKind.INSTANCE.canSubmit(view, bingo);
    }

    @Override
    public String primaryLabel(TileView view, Bingo bingo) {
        if (view.blind) {
            return BLIND_LABEL;
        }
        return view.notStarted ? NOT_STARTED_LABEL : BoardKind.super.primaryLabel(view, bingo);
    }

    @Override
    public String detailsNotice(TileView view) {
        if (view.blind) {
            return BLIND_NOTICE;
        }
        if (view.notStarted) {
            return NOT_STARTED_NOTICE;
        }
        return view.mark == BattleshipMarks.Mark.NONE ? null : "Shot result: " + view.mark.label;
    }

    @Override
    public Tooltip tooltip(TileView view, IntFunction<BufferedImage> itemImages) {
        String text = tooltipText(view);
        if (text != null) {
            return new Tooltip(text);
        }
        return new Tooltip(new TileHoverCard(view.tile, itemImages));
    }

    /**
     * Tooltip text for a blind or marked tile, or null when the tile gets the standard hover card.
     */
    static String tooltipText(TileView view) {
        if (view.hidden) {
            return BoardTooltipBuilder.build(view.tile);
        }
        if (view.blind) {
            return BoardTooltipBuilder.sanitize(view.title) + "</br>" + BLIND_LABEL;
        }
        if (view.mark == BattleshipMarks.Mark.NONE) {
            return null;
        }
        return BoardTooltipBuilder.build(view.tile) + "</br>" + ColorUtil.wrapWithColorTag(view.mark.label, markColor(view.mark));
    }

    private static Color markColor(BattleshipMarks.Mark mark) {
        return mark == BattleshipMarks.Mark.HIT ? Glyphs.HIT_COLOR
                : mark == BattleshipMarks.Mark.SUNK ? Glyphs.SUNK_COLOR : Glyphs.MISS_COLOR;
    }
}
