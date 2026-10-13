package org.bingoscape.board;

import org.bingoscape.models.Tile;

import java.awt.Point;

/**
 * Immutable snapshot of the board's clickable regions, published by the render thread and read by
 * the input listener. Coordinates are relative to the overlay origin.
 */
final class HitMap {
    /** Overlay position on the canvas when this frame was rendered. */
    final Point origin;
    final BoardLayout layout;
    final BoardKind kind;
    final TileView[] views;
    final Tile detailsTile;
    final TileDetailsView.DetailsHit details;

    HitMap(Point origin, BoardLayout layout, BoardKind kind, TileView[] views, Tile detailsTile,
           TileDetailsView.DetailsHit details) {
        this.origin = origin;
        this.layout = layout;
        this.kind = kind;
        this.views = views;
        this.detailsTile = detailsTile;
        this.details = details;
    }

    TileView viewAt(int x, int y) {
        int index = layout.tileIndexAt(x, y);
        return index >= 0 && index < views.length ? views[index] : null;
    }
}
