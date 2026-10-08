package org.bingoscape.board;

import org.bingoscape.models.Tile;

import java.awt.Point;
import java.util.List;

/**
 * Immutable snapshot of the board's clickable regions, published by the render thread and read by
 * the input listener. Coordinates are relative to the overlay origin.
 */
final class HitMap {
    /** Overlay position on the canvas when this frame was rendered. */
    final Point origin;
    final BoardLayout layout;
    final List<Tile> tiles;
    final Tile detailsTile;
    final TileDetailsView.DetailsHit details;

    HitMap(Point origin, BoardLayout layout, List<Tile> tiles, Tile detailsTile, TileDetailsView.DetailsHit details) {
        this.origin = origin;
        this.layout = layout;
        this.tiles = tiles;
        this.detailsTile = detailsTile;
        this.details = details;
    }

    Tile tileAt(int x, int y) {
        int index = layout.tileIndexAt(x, y);
        return index >= 0 && index < tiles.size() ? tiles.get(index) : null;
    }
}
