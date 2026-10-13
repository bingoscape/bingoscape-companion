package org.bingoscape.notifications;

import lombok.Value;

/**
 * Popup for a progression tier the team has unlocked.
 */
@Value
public class TierUnlockedNotification implements Notification {
    int tier;
    String bingoTitle;

    @Override
    public int priority() {
        return PRIORITY_COMPLETION;
    }

    @Override
    public double holdFactor() {
        return 1.5;
    }
}
