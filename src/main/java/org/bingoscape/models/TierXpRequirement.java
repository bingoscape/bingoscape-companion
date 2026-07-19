package org.bingoscape.models;

import lombok.Data;

/**
 * XP requirement for unlocking a tier in progression bingo
 */
@Data
public class TierXpRequirement {
    private Integer tier;
    private Long xpRequired;
}