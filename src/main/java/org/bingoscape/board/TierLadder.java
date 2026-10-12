package org.bingoscape.board;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TierState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The tiers of a progression board in ascending order, each with the indices of its tiles, its lock state
 * and the precomputed header texts. Built once per board by {@link ProgressionKind#prepare}, from the
 * server's {@code progression.tiers[]} (which also lists locked tiers) and the tiles' own tier fields.
 * Immutable; all math here is pure so it can be tested without a renderer.
 */
final class TierLadder {
    static final long UNKNOWN = -1;

    /**
     * One tier band. {@link #tiles} are indices into the model's sorted tile list, in tile order.
     */
    static final class Band {
        final int tier;
        final int[] tiles;
        final boolean locked;
        /** XP earned in this tier, or {@link #UNKNOWN} when the board has no tier state. */
        final long earnedXp;
        /** XP needed in this tier to unlock the next one (0 for the last tier), or {@link #UNKNOWN}. */
        final long requiredXp;
        /** For a locked band the XP still missing in the previous tier, otherwise {@link #UNKNOWN}. */
        final long xpNeeded;
        /** The tier whose XP unlocks this one, or -1 when there is none. */
        final int previousTier;
        /** "Tier N  earned/required XP", or "Tier N  Locked: X XP needed" for a locked band. */
        final String header;
        /** "Tier N locked: X more XP needed in Tier M" for a locked band, otherwise null. */
        final String lockNote;

        Band(int tier, int[] tiles, boolean locked, long earnedXp, long requiredXp, long xpNeeded, int previousTier) {
            this.tier = tier;
            this.tiles = tiles;
            this.locked = locked;
            this.earnedXp = earnedXp;
            this.requiredXp = requiredXp;
            this.xpNeeded = xpNeeded;
            this.previousTier = previousTier;
            this.header = locked ? lockedHeader(tier, xpNeeded) : unlockedHeader(tier, earnedXp, requiredXp);
            this.lockNote = locked ? lockNote(tier, xpNeeded, previousTier) : null;
        }

        /** Whether the tier is unlocked and its XP requirement is met, so the next tier opens. */
        boolean complete() {
            return !locked && requiredXp > 0 && earnedXp >= requiredXp;
        }
    }

    final Band[] bands;
    /** "Tiers unlocked: 2/3". */
    final String legend;

    private TierLadder(Band[] bands, String legend) {
        this.bands = bands;
        this.legend = legend;
    }

    static TierLadder of(Bingo bingo, List<Tile> sortedTiles) {
        ProgressionMetadata progression = bingo.getProgression();

        // Tiles keep the model's order inside a band; a missing tier counts as the first tier
        Map<Integer, List<Integer>> byTier = new TreeMap<>();
        Set<Integer> flaggedLocked = new HashSet<>();
        for (int i = 0; i < sortedTiles.size(); i++) {
            Tile tile = sortedTiles.get(i);
            int tier = tile.getTier() == null ? 0 : tile.getTier();
            byTier.computeIfAbsent(tier, k -> new ArrayList<>()).add(i);
            if (tile.isTierLocked()) {
                flaggedLocked.add(tier);
            }
        }

        Band[] bands = new Band[byTier.size()];
        int b = 0;
        int unlockedBands = 0;
        for (Map.Entry<Integer, List<Integer>> entry : byTier.entrySet()) {
            int tier = entry.getKey();
            List<Integer> indices = entry.getValue();
            int[] tiles = new int[indices.size()];
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = indices.get(i);
            }

            TierState state = progression == null ? null : progression.stateOf(tier);
            boolean locked = flaggedLocked.contains(tier) || (state != null && !state.isUnlocked());
            long earned = state == null ? UNKNOWN : state.getEarnedXp();
            long required = state == null ? UNKNOWN : state.getXpRequired();
            int previous = previousTier(progression == null ? null : progression.getTiers(), tier);
            long needed = locked ? xpNeeded(progression == null ? null : progression.getTiers(), tier) : UNKNOWN;
            bands[b++] = new Band(tier, tiles, locked, earned, required, needed, previous);
            if (!locked) {
                unlockedBands++;
            }
        }

        // The server lists every tier, including ones without tiles; fall back to the bands we have
        int total = bands.length;
        int unlocked = unlockedBands;
        if (progression != null && progression.getTiers() != null && !progression.getTiers().isEmpty()) {
            total = 0;
            unlocked = 0;
            for (TierState state : progression.getTiers()) {
                if (state == null) {
                    continue;
                }
                total++;
                if (state.isUnlocked() && !flaggedLocked.contains(state.getTier())) {
                    unlocked++;
                }
            }
        }
        return new TierLadder(bands, "Tiers unlocked: " + unlocked + "/" + total);
    }

    /**
     * The band of a tier, or null when the board has no tile in it.
     */
    Band bandOf(int tier) {
        for (Band band : bands) {
            if (band.tier == tier) {
                return band;
            }
        }
        return null;
    }

    /**
     * The tier that must be filled to unlock {@code tier}: the highest listed tier below it, or -1 when the
     * tier list is missing or has none.
     */
    static int previousTier(List<TierState> tiers, int tier) {
        int previous = -1;
        if (tiers == null) {
            return previous;
        }
        for (TierState state : tiers) {
            if (state != null && state.getTier() < tier && state.getTier() > previous) {
                previous = state.getTier();
            }
        }
        return previous;
    }

    /**
     * XP the team still has to earn in the previous tier to unlock {@code tier}: that tier's requirement minus
     * what it has earned, never negative. {@link #UNKNOWN} when there is no previous tier to read.
     */
    static long xpNeeded(List<TierState> tiers, int tier) {
        int previous = previousTier(tiers, tier);
        if (previous < 0) {
            return UNKNOWN;
        }
        for (TierState state : tiers) {
            if (state != null && state.getTier() == previous) {
                return Math.max(0, state.getXpRequired() - state.getEarnedXp());
            }
        }
        return UNKNOWN;
    }

    static String unlockedHeader(int tier, long earnedXp, long requiredXp) {
        if (earnedXp == UNKNOWN) {
            return "Tier " + tier;
        }
        // The last tier has nothing to unlock, so there is no requirement to show
        return requiredXp > 0
                ? "Tier " + tier + "  " + earnedXp + "/" + requiredXp + " XP"
                : "Tier " + tier + "  " + earnedXp + " XP";
    }

    static String lockedHeader(int tier, long xpNeeded) {
        return xpNeeded == UNKNOWN
                ? "Tier " + tier + "  Locked"
                : "Tier " + tier + "  Locked: " + xpNeeded + " XP needed";
    }

    static String lockNote(int tier, long xpNeeded, int previousTier) {
        return xpNeeded == UNKNOWN || previousTier < 0
                ? "Tier " + tier + " locked"
                : "Tier " + tier + " locked: " + xpNeeded + " more XP needed in Tier " + previousTier;
    }
}
