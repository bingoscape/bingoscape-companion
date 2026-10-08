package org.bingoscape.notifications;

import lombok.Value;

/**
 * One drop popup: the item, what it is worth and which tile it counts for.
 */
@Value
public class DropNotification {
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
}
