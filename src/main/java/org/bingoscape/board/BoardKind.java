package org.bingoscape.board;

import net.runelite.client.ui.overlay.tooltip.Tooltip;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.Tile;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
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
        if (type == BingoType.PROGRESSION) {
            return ProgressionKind.SHOW_LOCKED;
        }
        return StandardKind.INSTANCE;
    }

    static BoardKind forBingo(Bingo bingo) {
        return forType(bingo == null ? null : bingo.getBingoType());
    }

    BingoType type();

    /**
     * The same kind adjusted to the user's config, e.g. how locked tiles are shown. Kinds are singletons, so
     * the overlay can compare the result by identity to notice a config change and rebuild the model.
     */
    default BoardKind withOptions(BingoScapeConfig config) {
        return this;
    }

    /**
     * Board-wide data computed once when the model is built from the sorted tiles, handed to {@link #view} and
     * kept in {@link BoardModel#data}. Null when the kind needs none.
     */
    default Object prepare(Bingo bingo, List<Tile> tiles) {
        return null;
    }

    /**
     * Rectangles for every tile in {@code model} plus the header controls. Tile order is the model's sorted
     * order; rectangles need not form a regular grid.
     */
    BoardLayout layout(BoardModel model, int viewportWidth, int viewportHeight, double scale, boolean hasCodephrase);

    /**
     * Precomputes the immutable render values of a tile; called once per tile when the model is built, with
     * the result of {@link #prepare}.
     */
    TileView view(Bingo bingo, Tile tile, int index, Object prepared);

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
     * Text of the details view's primary button; it is only clickable when {@link #canSubmit} allows it.
     */
    default String primaryLabel(TileView view, Bingo bingo) {
        return TileDetailsView.primaryLabel(view.tile, bingo);
    }

    /**
     * Extra line shown under the title in the details view, e.g. why a tile is locked, or null for none.
     */
    default String detailsNotice(TileView view) {
        return null;
    }

    /**
     * Hover tooltip for the tile, or null for none.
     */
    Tooltip tooltip(TileView view, IntFunction<BufferedImage> itemImages);
}
