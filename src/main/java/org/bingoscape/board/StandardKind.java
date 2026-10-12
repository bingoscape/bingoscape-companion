package org.bingoscape.board;

import net.runelite.client.ui.overlay.tooltip.Tooltip;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.Tile;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.IntFunction;

/**
 * The plain rows x columns board: every tile painted by its submission status, hidden tiles as a gray "?".
 */
final class StandardKind implements BoardKind {
    static final StandardKind INSTANCE = new StandardKind();

    private StandardKind() {
    }

    @Override
    public BingoType type() {
        return BingoType.STANDARD;
    }

    @Override
    public BoardLayout layout(BoardModel model, int viewportWidth, int viewportHeight, double scale, boolean hasCodephrase) {
        return BoardLayout.compute(model.bingo.getRows(), model.bingo.getColumns(), model.views.length,
                viewportWidth, viewportHeight, scale, hasCodephrase);
    }

    @Override
    public TileView view(Bingo bingo, Tile tile, int index) {
        return TileView.of(tile, index);
    }

    @Override
    public void paintTile(Graphics2D g, Rectangle r, TileView view, boolean hovered, TilePainter painter) {
        if (view.hidden) {
            painter.paintHidden(g, r);
        } else {
            painter.paintStandard(g, r, view, hovered);
        }
    }

    @Override
    public void paintHeaderExtras(Graphics2D g, BoardLayout layout, BoardModel model) {
    }

    @Override
    public boolean canOpen(TileView view) {
        return !view.hidden && view.id != null;
    }

    @Override
    public boolean canSubmit(TileView view, Bingo bingo) {
        return TileDetailsView.primaryEnabled(view.tile, bingo);
    }

    @Override
    public Tooltip tooltip(TileView view, IntFunction<BufferedImage> itemImages) {
        return view.hidden
                ? new Tooltip(BoardTooltipBuilder.build(view.tile))
                : new Tooltip(new TileHoverCard(view.tile, itemImages));
    }
}
