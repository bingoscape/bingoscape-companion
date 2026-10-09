package org.bingoscape.notifications;

import lombok.Value;

/**
 * Popup for a bingo tile the team has completed.
 */
@Value
public class TileCompletedNotification implements Notification {
    String tileTitle;
    int points;
    String teamName;

    @Override
    public int priority() {
        return PRIORITY_COMPLETION;
    }
}
