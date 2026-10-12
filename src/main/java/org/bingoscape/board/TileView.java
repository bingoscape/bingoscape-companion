package org.bingoscape.board;

import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.TileStatusStyle;

import java.util.UUID;

/**
 * Immutable per-tile values the painters and input handlers need, computed once when the {@link BoardModel}
 * is built so the render loop does no per-frame string building or null handling. {@link #tile} is the
 * shared model object; read-only use only.
 */
final class TileView {
    final Tile tile;
    /** Position in the sorted tile list, which is also the index into {@link BoardLayout#tiles}. */
    final int index;
    final UUID id;
    /** Never null: locked stub tiles have no title. */
    final String title;
    /** Precomputed "N XP". */
    final String xpLabel;
    /** Header image URL as sent by the server, or null when the tile has none. */
    final String imageUrl;
    final TileSubmissionType status;
    final boolean hidden;
    final boolean blind;
    final boolean tierLocked;

    private TileView(Tile tile, int index) {
        this.tile = tile;
        this.index = index;
        this.id = tile.getId();
        this.title = tile.getTitle() == null ? "" : tile.getTitle();
        this.xpLabel = tile.getWeight() + " XP";
        String url = tile.getHeaderImage();
        this.imageUrl = url == null || url.trim().isEmpty() ? null : url;
        this.status = TileStatusStyle.statusOf(tile);
        this.hidden = tile.isHidden();
        this.blind = tile.isBlind();
        this.tierLocked = tile.isTierLocked();
    }

    static TileView of(Tile tile, int index) {
        return new TileView(tile, index);
    }
}
