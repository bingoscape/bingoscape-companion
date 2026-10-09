package org.bingoscape.notifications;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * One drop popup: the item, what it is worth and which tile it counts for.
 */
@Value
@AllArgsConstructor
public class DropNotification implements Notification {
    int itemId;
    String itemName;
    int quantity;
    long stackValue;
    DropTier tier;
    /** Title of the first matching tile, or null. */
    String tileTitle;
    /** Further matching tiles beyond {@link #tileTitle}. */
    int extraTiles;
    /** Why the drop is not submitted automatically, or null when it is. */
    String warning;
    /** Teammate who received the drop, or null for your own drops. */
    String playerName;

    public DropNotification(int itemId, String itemName, int quantity, long stackValue, DropTier tier,
                            String tileTitle, int extraTiles, String warning) {
        this(itemId, itemName, quantity, stackValue, tier, tileTitle, extraTiles, warning, null);
    }

    @Override
    public int priority() {
        return PRIORITY_DROP;
    }
}
