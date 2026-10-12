package org.bingoscape.models;

import lombok.Data;

import java.util.Date;

/**
 * State of a single tier of a progression board, including locked tiers.
 * xpRequired is the XP needed in this tier to unlock the next one.
 */
@Data
public class TierState {
    private int tier;
    private boolean isUnlocked;
    private Date unlockedAt;
    private long earnedXp;
    private long xpRequired;
}
