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
 * {@link Bingo#getTiles()} list is never mutated.
 */
final class BoardModel {
    final Bingo bingo;
    final List<Tile> tiles;
    final Map<UUID, Tile> byId;

    private BoardModel(Bingo bingo, List<Tile> tiles, Map<UUID, Tile> byId) {
        this.bingo = bingo;
        this.tiles = tiles;
        this.byId = byId;
    }

    static BoardModel of(Bingo bingo) {
        List<Tile> tiles = bingo.getTiles() == null ? new ArrayList<>() : new ArrayList<>(bingo.getTiles());
        tiles.removeIf(Objects::isNull);
        tiles.sort(Comparator.comparingInt(Tile::getIndex));

        Map<UUID, Tile> byId = new HashMap<>();
        for (Tile tile : tiles) {
            if (tile.getId() != null) {
                byId.put(tile.getId(), tile);
            }
        }

        return new BoardModel(bingo, Collections.unmodifiableList(tiles), Collections.unmodifiableMap(byId));
    }
}
