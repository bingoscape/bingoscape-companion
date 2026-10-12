package org.bingoscape.models;

import lombok.Data;

import java.util.List;

/**
 * Progression metadata for progression-style bingo boards
 */
@Data
public class ProgressionMetadata {
    private List<TierXpRequirement> tierXpRequirements;
    private List<Integer> unlockedTiers;
    private List<TierProgress> tierProgress;
    private List<TierState> tiers; // Every tier of the board, including locked ones

    /**
     * The state of one tier, or null when {@code tiers} is missing or does not list it.
     */
    public TierState stateOf(int tier) {
        if (tiers == null) {
            return null;
        }
        for (TierState state : tiers) {
            if (state != null && state.getTier() == tier) {
                return state;
            }
        }
        return null;
    }
}
