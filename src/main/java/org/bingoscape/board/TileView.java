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
    /** Progression tier of the tile, 0 when the tile has none. */
    final int tier;
    /** Why the tile is locked ("Tier 2 locked: 3 more XP needed in Tier 1"), or null when it is not locked. */
    final String lockNote;

    private TileView(Tile tile, int index, boolean tierLocked, String lockNote) {
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
        this.tierLocked = tierLocked;
        this.tier = tile.getTier() == null ? 0 : tile.getTier();
        this.lockNote = lockNote;
    }

    static TileView of(Tile tile, int index) {
        return new TileView(tile, index, tile.isTierLocked(), null);
    }

    /**
     * A view whose lock state comes from the board's tier state instead of the tile flag alone.
     */
    static TileView of(Tile tile, int index, boolean tierLocked, String lockNote) {
        return new TileView(tile, index, tierLocked, lockNote);
    }
}
