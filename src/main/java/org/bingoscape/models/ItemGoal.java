package org.bingoscape.models;

import lombok.Data;

/**
 * Represents an item-based goal with OSRS item metadata.
 */
@Data
public class ItemGoal {
    private Integer itemId; // OSRS item ID
    private String baseName;
    private String exactVariant; // e.g., "Undamaged" for Barrows items
    private String imageUrl;
}
