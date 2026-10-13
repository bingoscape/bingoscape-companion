package org.bingoscape.board;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The board kind's open and submit rules for one tile, for callers outside the overlay such as the legacy
 * board window. It builds the same {@link TileView} the overlay uses, so a blind tile, a tile of a locked
 * tier and an unstarted battleship board are gated identically everywhere. Meant for click handlers, not for
 * per-frame use: each call prepares the kind's board data.
 */
public final class TileGate {
    private TileGate() {
    }

    /**
     * Whether clicking the tile may open its details. False for blind and hidden tiles.
     */
    public static boolean canOpen(Bingo bingo, Tile tile) {
        TileView view = viewOf(bingo, tile);
        return view != null && BoardKind.forBingo(bingo).canOpen(view);
    }

    /**
     * Whether the tile may be submitted. False for blind tiles, tiles of locked tiers and before a
     * battleship event has started, besides the usual locked-board and already-completed rules.
     */
    public static boolean canSubmit(Bingo bingo, Tile tile) {
        TileView view = viewOf(bingo, tile);
        return view != null && BoardKind.forBingo(bingo).canSubmit(view, bingo);
    }

    /**
     * Label for the submit button, e.g. "Tier locked" or "Blind tile".
     */
    public static String submitLabel(Bingo bingo, Tile tile) {
        TileView view = viewOf(bingo, tile);
        return view == null ? "Take & Review Screenshot" : BoardKind.forBingo(bingo).primaryLabel(view, bingo);
    }

    /**
     * Why the tile cannot be used, or null when there is nothing to explain.
     */
    public static String notice(Bingo bingo, Tile tile) {
        TileView view = viewOf(bingo, tile);
        return view == null ? null : BoardKind.forBingo(bingo).detailsNotice(view);
    }

    private static TileView viewOf(Bingo bingo, Tile tile) {
        if (bingo == null || tile == null) {
            return null;
        }
        BoardKind kind = BoardKind.forBingo(bingo);
        List<Tile> tiles = bingo.getTiles() == null ? new ArrayList<Tile>() : new ArrayList<>(bingo.getTiles());
        tiles.removeIf(Objects::isNull);
        Object data = kind.prepare(bingo, tiles);
        // The index only matters for layout, which this never asks for
        return kind.view(bingo, tile, 0, data);
    }
}
