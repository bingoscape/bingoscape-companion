package org.bingoscape.board;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of a bingo board for rendering. Tiles are a sorted copy, so the shared
 * {@link Bingo#getTiles()} list is never mutated. {@link #views} holds the precomputed render values in the
 * same order and must not be modified.
 */
final class BoardModel {
    final Bingo bingo;
    final BoardKind kind;
    final List<Tile> tiles;
    final Map<UUID, Tile> byId;
    final TileView[] views;
    final Map<UUID, TileView> viewById;

    private BoardModel(Bingo bingo, BoardKind kind, List<Tile> tiles, Map<UUID, Tile> byId, TileView[] views,
                       Map<UUID, TileView> viewById) {
        this.bingo = bingo;
        this.kind = kind;
        this.tiles = tiles;
        this.byId = byId;
        this.views = views;
        this.viewById = viewById;
    }

    static BoardModel of(Bingo bingo) {
        return of(bingo, BoardKind.forBingo(bingo));
    }

    static BoardModel of(Bingo bingo, BoardKind kind) {
        List<Tile> tiles = bingo.getTiles() == null ? new ArrayList<>() : new ArrayList<>(bingo.getTiles());
        tiles.removeIf(Objects::isNull);
        tiles.sort(Comparator.comparingInt(Tile::getIndex));

        Map<UUID, Tile> byId = new HashMap<>();
        Map<UUID, TileView> viewById = new HashMap<>();
        TileView[] views = new TileView[tiles.size()];
        for (int i = 0; i < views.length; i++) {
            Tile tile = tiles.get(i);
            views[i] = kind.view(bingo, tile, i);
            if (tile.getId() != null) {
                byId.put(tile.getId(), tile);
                viewById.put(tile.getId(), views[i]);
            }
        }

        return new BoardModel(bingo, kind, Collections.unmodifiableList(tiles), Collections.unmodifiableMap(byId),
                views, Collections.unmodifiableMap(viewById));
    }
}
