package org.bingoscape.board;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.TierState;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.TileStatusStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counts for the side panel's board card, computed once per board refresh. Hidden tiles, blind tiles and the
 * stub tiles of locked tiers are left out of every count, so a locked tier does not inflate the totals.
 * Immutable and pure; the panel only turns it into components.
 */
public final class BoardSummary {
    /** Progress of one progression tier, copied from {@code progression.tiers[]}. */
    public static final class Tier {
        public final int tier;
        public final boolean unlocked;
        public final long earnedXp;
        public final long requiredXp;
        /** For a locked tier the XP still missing in the previous tier, otherwise -1 (also when unknown). */
        public final long xpNeeded;

        Tier(int tier, boolean unlocked, long earnedXp, long requiredXp, long xpNeeded) {
            this.tier = tier;
            this.unlocked = unlocked;
            this.earnedXp = earnedXp;
            this.requiredXp = requiredXp;
            this.xpNeeded = xpNeeded;
        }
    }

    public final BingoType type;
    public final int tiles;
    public final int completed;
    public final int pending;
    public final int needsAction;
    public final int declined;
    public final int xpTotal;
    public final int xpEarned;
    /** Progression only: every tier of the board in ascending order, locked ones included. Never null. */
    public final List<Tier> tiers;
    /** Battleship only: the calling team's shots. Zero before the event starts and on other boards. */
    public final int hits;
    public final int misses;
    public final int sunk;
    /** Battleship only: "Ships remaining: X/Y" or the start notice, otherwise null. */
    public final String shipsLegend;

    private BoardSummary(BingoType type, int[] counts, List<Tier> tiers, int[] shots, String shipsLegend) {
        this.type = type;
        this.tiles = counts[0];
        this.completed = counts[1];
        this.pending = counts[2];
        this.needsAction = counts[3];
        this.declined = counts[4];
        this.xpTotal = counts[5];
        this.xpEarned = counts[6];
        this.tiers = tiers;
        this.hits = shots[0];
        this.misses = shots[1];
        this.sunk = shots[2];
        this.shipsLegend = shipsLegend;
    }

    public static BoardSummary of(Bingo bingo) {
        BingoType type = bingo == null ? BingoType.STANDARD : bingo.getBingoType();
        ProgressionMetadata progression = bingo == null ? null : bingo.getProgression();
        BattleshipMarks marks = type == BingoType.BATTLESHIP ? BattleshipMarks.of(bingo.getBattleship()) : BattleshipMarks.EMPTY;

        // tiles, completed, pending, needsAction, declined, xpTotal, xpEarned
        int[] counts = new int[7];
        // hits, misses, sunk
        int[] shots = new int[3];
        if (bingo != null && bingo.getTiles() != null) {
            for (Tile tile : bingo.getTiles()) {
                if (tile == null || tile.isHidden() || tile.isBlind() || isLocked(tile, progression)) {
                    continue;
                }
                counts[0]++;
                counts[5] += tile.getWeight();
                switch (marks.markOf(tile)) {
                    case HIT:
                        shots[0]++;
                        break;
                    case MISS:
                        shots[1]++;
                        break;
                    case SUNK:
                        shots[2]++;
                        break;
                    default:
                        break;
                }

                TileSubmissionType status = TileStatusStyle.statusOf(tile);
                if (status == null) {
                    continue;
                }
                switch (status) {
                    case ACCEPTED:
                        counts[1]++;
                        counts[6] += tile.getWeight();
                        break;
                    case PENDING:
                        counts[2]++;
                        break;
                    case REQUIRES_INTERACTION:
                        counts[3]++;
                        break;
                    case DECLINED:
                        counts[4]++;
                        break;
                    default:
                        break;
                }
            }
        }

        return new BoardSummary(type, counts, type == BingoType.PROGRESSION ? tiersOf(progression) : Collections.<Tier>emptyList(),
                shots, type == BingoType.BATTLESHIP ? marks.legend : null);
    }

    /**
     * A tile of a locked tier: flagged by the server, or its tier is listed as locked.
     */
    private static boolean isLocked(Tile tile, ProgressionMetadata progression) {
        if (tile.isTierLocked()) {
            return true;
        }
        if (progression == null) {
            return false;
        }
        TierState state = progression.stateOf(tile.getTier() == null ? 0 : tile.getTier());
        return state != null && !state.isUnlocked();
    }

    private static List<Tier> tiersOf(ProgressionMetadata progression) {
        if (progression == null || progression.getTiers() == null) {
            return Collections.emptyList();
        }
        List<Tier> tiers = new ArrayList<>();
        for (TierState state : progression.getTiers()) {
            if (state != null) {
                long needed = state.isUnlocked() ? -1 : TierLadder.xpNeeded(progression.getTiers(), state.getTier());
                tiers.add(new Tier(state.getTier(), state.isUnlocked(), state.getEarnedXp(), state.getXpRequired(), needed));
            }
        }
        tiers.sort((a, b) -> Integer.compare(a.tier, b.tier));
        return Collections.unmodifiableList(tiers);
    }
}
