package org.bingoscape.board;

import org.bingoscape.models.BattleshipState;
import org.bingoscape.models.Tile;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The calling team's shots on a battleship board, resolved once per board from {@link BattleshipState}. Marks
 * only exist once the event has started, a tile that is sunk is never also shown as hit or miss, and blind
 * tiles never carry a mark. Immutable and null-safe: a missing or partial state yields no marks.
 */
final class BattleshipMarks {
    /** Result of a shot on a tile, weakest first. */
    enum Mark {
        NONE(null),
        MISS("Miss"),
        HIT("Hit"),
        SUNK("Ship sunk");

        final String label;

        Mark(String label) {
            this.label = label;
        }
    }

    /** No battleship state at all: no marks and nothing gated. */
    static final BattleshipMarks EMPTY = new BattleshipMarks(null, Collections.<UUID, Mark>emptyMap());

    private final BattleshipState state;
    private final Map<UUID, Mark> marks;
    /** "Ships remaining: X/Y" or the start notice, or null when the board has no state. */
    final String legend;

    private BattleshipMarks(BattleshipState state, Map<UUID, Mark> marks) {
        this.state = state;
        this.marks = marks;
        this.legend = legend(state);
    }

    /**
     * Resolves the marks. Where a tile id is listed more than once the strongest wins: sunk over hit over miss.
     * Nothing is marked before the event has started.
     */
    static BattleshipMarks of(BattleshipState state) {
        if (state == null) {
            return EMPTY;
        }
        if (!state.isStarted() || state.getMarks() == null) {
            return new BattleshipMarks(state, Collections.<UUID, Mark>emptyMap());
        }

        Map<UUID, Mark> marks = new HashMap<>();
        // Weakest first, so a later pass overwrites an earlier one
        put(marks, state.getMarks().getMissTileIds(), Mark.MISS);
        put(marks, state.getMarks().getHitTileIds(), Mark.HIT);
        put(marks, state.getMarks().getSunkTileIds(), Mark.SUNK);
        return new BattleshipMarks(state, Collections.unmodifiableMap(marks));
    }

    private static void put(Map<UUID, Mark> marks, List<UUID> ids, Mark mark) {
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            if (id != null) {
                marks.put(id, mark);
            }
        }
    }

    /**
     * The mark of a tile; {@link Mark#NONE} for blind tiles, tiles without an id and unmarked tiles.
     */
    Mark markOf(Tile tile) {
        if (tile == null || tile.isBlind() || tile.getId() == null) {
            return Mark.NONE;
        }
        Mark mark = marks.get(tile.getId());
        return mark == null ? Mark.NONE : mark;
    }

    /**
     * Whether the server says the event has not started yet. A board without state is not gated by this.
     */
    boolean notStarted() {
        return state != null && !state.isStarted();
    }

    int markedCount() {
        return marks.size();
    }

    /** Opponent ships not yet sunk, never negative. */
    int shipsRemaining() {
        return state == null ? 0 : Math.max(0, state.getOpponentShipsTotal() - state.getOpponentShipsSunk());
    }

    int shipsTotal() {
        return state == null ? 0 : Math.max(0, state.getOpponentShipsTotal());
    }

    private static String legend(BattleshipState state) {
        if (state == null) {
            return null;
        }
        if (!state.isStarted()) {
            return state.getStartDate() == null
                    ? "Not started yet"
                    : "Starts " + new SimpleDateFormat("MMM d, HH:mm").format(state.getStartDate());
        }
        int total = Math.max(0, state.getOpponentShipsTotal());
        if (total == 0) {
            return null;
        }
        int remaining = Math.max(0, total - state.getOpponentShipsSunk());
        return "Ships remaining: " + remaining + "/" + total;
    }
}
