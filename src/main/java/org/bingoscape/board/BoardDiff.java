package org.bingoscape.board;

import lombok.Value;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TierState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * What changed between two loads of the same board that deserves a popup: a progression tier that became
 * unlocked and battleship tiles that were shot. Pure and free of I/O; the plugin calls it on every board
 * swap. Only changes between two snapshots of the same bingo are reported, so a first load and a switch to
 * another bingo report nothing, and comparing a snapshot with itself reports nothing.
 */
public final class BoardDiff {
    /** Outcome of a shot on a battleship tile. */
    public enum Result {
        HIT, MISS, SUNK
    }

    /** A change worth a popup. */
    public interface Event {
    }

    /** A progression tier that was locked and is now unlocked. */
    @Value
    public static class TierUnlocked implements Event {
        int tier;
    }

    /** A battleship tile that was newly shot, with the resolved outcome (sunk beats hit beats miss). */
    @Value
    public static class ShipResult implements Event {
        UUID tileId;
        String tileTitle;
        Result result;
        /** Opponent ships still afloat after this shot, or -1 when the board does not say. */
        int shipsRemaining;
    }

    private BoardDiff() {
    }

    /**
     * As {@link #compute(Bingo, Bingo)}, for snapshots that were loaded for a team scope (event and team):
     * nothing is reported unless both snapshots carry the same non-null scope, so switching teams or events
     * re-baselines instead of announcing the other team's shots.
     */
    public static List<Event> compute(Bingo old, String oldScope, Bingo updated, String updatedScope) {
        if (oldScope == null || !oldScope.equals(updatedScope)) {
            return Collections.emptyList();
        }
        return compute(old, updated);
    }

    /**
     * The events between two snapshots of the board, tier unlocks first (lowest tier first), then shots in
     * board order. Empty when {@code old} is null (first load), when the bingo differs, or when the board
     * is not of the same progression or battleship type. The caller is responsible for passing a null
     * {@code old} when the team changed.
     */
    public static List<Event> compute(Bingo old, Bingo updated) {
        if (old == null || updated == null || old.getId() == null || !old.getId().equals(updated.getId())) {
            return Collections.emptyList();
        }
        if (old.getBingoType() != updated.getBingoType()) {
            return Collections.emptyList();
        }

        List<Event> events = new ArrayList<>();
        if (updated.getBingoType() == BingoType.PROGRESSION) {
            tierUnlocks(old.getProgression(), updated.getProgression(), events);
        } else if (updated.getBingoType() == BingoType.BATTLESHIP) {
            shots(old, updated, events);
        }
        return events;
    }

    private static void tierUnlocks(ProgressionMetadata before, ProgressionMetadata after, List<Event> events) {
        if (before == null || after == null || after.getTiers() == null) {
            return;
        }
        List<TierState> tiers = new ArrayList<>();
        for (TierState state : after.getTiers()) {
            if (state != null && state.isUnlocked()) {
                tiers.add(state);
            }
        }
        Collections.sort(tiers, Comparator.comparingInt(TierState::getTier));
        for (TierState state : tiers) {
            // A tier the earlier snapshot did not list cannot be told apart from a first look, so it is not reported
            TierState earlier = before.stateOf(state.getTier());
            if (earlier != null && !earlier.isUnlocked()) {
                events.add(new TierUnlocked(state.getTier()));
            }
        }
    }

    /**
     * Compares resolved marks, not the raw id lists: a ship that sinks turns the HIT marks of its other
     * tiles into SUNK, which is not news. Only a tile that had no mark before is reported, with the mark it
     * has now, so the shot that sinks a ship shows as SUNK and never as an extra HIT.
     */
    private static void shots(Bingo old, Bingo updated, List<Event> events) {
        if (updated.getTiles() == null) {
            return;
        }
        BattleshipMarks before = BattleshipMarks.of(old.getBattleship());
        BattleshipMarks after = BattleshipMarks.of(updated.getBattleship());
        int remaining = after.shipsTotal() > 0 ? after.shipsRemaining() : -1;
        List<Tile> tiles = new ArrayList<>();
        for (Tile tile : updated.getTiles()) {
            if (tile != null) {
                tiles.add(tile);
            }
        }
        Collections.sort(tiles, Comparator.comparingInt(Tile::getIndex));
        for (Tile tile : tiles) {
            if (before.markOf(tile) != BattleshipMarks.Mark.NONE) {
                continue;
            }
            Result result = result(after.markOf(tile));
            if (result != null) {
                events.add(new ShipResult(tile.getId(), tile.getTitle(), result, remaining));
            }
        }
    }

    private static Result result(BattleshipMarks.Mark mark) {
        switch (mark) {
            case SUNK:
                return Result.SUNK;
            case HIT:
                return Result.HIT;
            case MISS:
                return Result.MISS;
            default:
                return null;
        }
    }
}
