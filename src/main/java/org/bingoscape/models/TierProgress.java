package org.bingoscape.models;

import lombok.Data;

import java.util.Date;

/**
 * Progress information for a specific tier in progression bingo
 */
@Data
public class TierProgress {
    private Integer tier;
    private Boolean isUnlocked;
    private Date unlockedAt;
}