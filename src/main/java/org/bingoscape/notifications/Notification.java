package org.bingoscape.notifications;

/**
 * A popup the overlay can show. Lower priority values are shown first.
 */
public interface Notification {
    int PRIORITY_COMPLETION = 0;
    int PRIORITY_DROP = 1;

    int priority();
}
