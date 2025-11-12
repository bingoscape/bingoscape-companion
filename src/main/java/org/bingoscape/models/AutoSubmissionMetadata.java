package org.bingoscape.models;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata for automatic tile submissions from the plugin.
 * Contains contextual information about where and how the submission was triggered.
 */
@Data
@Builder
public class AutoSubmissionMetadata {
    /**
     * The NPC ID that dropped the item (nullable for non-NPC sources).
     */
    private Integer npcId;

    /**
     * The name of the source (NPC name, activity name, etc.).
     */
    private String sourceName;

    /**
     * The OSRS item ID that triggered the automatic submission.
     */
    private Integer itemId;

    /**
     * The RuneScape account name of the logged-in player at time of submission.
     */
    private String accountName;

    /**
     * The type of source (e.g., "NPC loot", "Pickpocket", "Event reward").
     */
    private String sourceType;
}
