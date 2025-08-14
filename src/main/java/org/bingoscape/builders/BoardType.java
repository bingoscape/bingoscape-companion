package org.bingoscape.builders;

import org.bingoscape.models.Bingo;

/**
 * Enumeration defining the different types of bingo boards supported by the system.
 * This replaces string-based type detection with a type-safe approach.
 * 
 * @author BingoScape Development Team
 */
public enum BoardType {
    /**
     * Standard grid-based bingo boards with fixed row/column layout.
     * Uses traditional grid display with tiles arranged in a rectangular pattern.
     */
    STANDARD("standard"),
    
    /**
     * Progressive tier-based bingo boards with unlockable tiers.
     * Uses vertical tier layout with collapsible sections and progression mechanics.
     */
    PROGRESSIVE("progression");
    
    private final String apiValue;
    
    /**
     * Creates a BoardType with its corresponding API string value.
     *
     * @param apiValue The string value used in the API/data model
     */
    BoardType(String apiValue) {
        this.apiValue = apiValue;
    }
    
    /**
     * Gets the API string value for this board type.
     *
     * @return The string value used in API communications
     */
    public String getApiValue() {
        return apiValue;
    }
    
    /**
     * Determines the board type from a Bingo data model.
     * Provides safe type detection with fallback to STANDARD for unknown types.
     *
     * @param bingo The bingo data model to analyze
     * @return The detected board type, defaults to STANDARD if null or unknown
     */
    public static BoardType fromBingo(Bingo bingo) {
        if (bingo == null || bingo.getBingoType() == null) {
            return STANDARD;
        }
        
        String bingoType = bingo.getBingoType().toLowerCase().trim();
        
        for (BoardType type : BoardType.values()) {
            if (type.apiValue.equals(bingoType)) {
                return type;
            }
        }
        
        // Default to standard for unknown types to maintain backward compatibility
        return STANDARD;
    }
    
    /**
     * Determines if this board type supports progressive features like tiers and unlocking.
     *
     * @return true if this board type supports progression mechanics
     */
    public boolean isProgressionSupported() {
        return this == PROGRESSIVE;
    }
    
    /**
     * Determines if this board type uses a fixed grid layout.
     *
     * @return true if this board type uses a traditional grid layout
     */
    public boolean usesGridLayout() {
        return this == STANDARD;
    }
}