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
}