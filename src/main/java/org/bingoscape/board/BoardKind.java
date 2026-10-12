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
 * How one type of board is laid out, painted and interacted with in the overlay. The overlay and the input
 * listener only talk to this interface, so adding a board type means adding an implementation and a case in
 * {@link #forType(BingoType)}. Implementations are stateless; everything is called on the render thread
 * except {@link #canOpen} and {@link #canSubmit}, which the input listener also calls on the AWT thread.
 */
interface BoardKind {
    /**
     * Picks the kind for a board type. Null and any type without its own kind get the standard board.
     */
    static BoardKind forType(BingoType type) {
        return StandardKind.INSTANCE;
    }

    static BoardKind forBingo(Bingo bingo) {
        return forType(bingo == null ? null : bingo.getBingoType());
    }

    BingoType type();

    /**
     * Rectangles for every tile in {@code model} plus the header controls. Tile order is the model's sorted
     * order; rectangles need not form a regular grid.
     */
    BoardLayout layout(BoardModel model, int viewportWidth, int viewportHeight, double scale, boolean hasCodephrase);

    /**
     * Precomputes the immutable render values of a tile; called once per tile when the model is built.
     */
    TileView view(Bingo bingo, Tile tile, int index);

    /**
     * Paints one tile into {@code r}; the graphics are already clipped to it.
     */
    void paintTile(Graphics2D g, Rectangle r, TileView view, boolean hovered, TilePainter painter);

    /**
     * Paints anything extra on top of the shared header, such as a legend or progress summary.
     */
    void paintHeaderExtras(Graphics2D g, BoardLayout layout, BoardModel model);

    /**
     * Whether clicking the tile opens its details.
     */
    boolean canOpen(TileView view);

    /**
     * Whether the details view enables its submit button for the tile.
     */
    boolean canSubmit(TileView view, Bingo bingo);

    /**
     * Hover tooltip for the tile, or null for none.
     */
    Tooltip tooltip(TileView view, IntFunction<BufferedImage> itemImages);
}
