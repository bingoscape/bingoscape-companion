package org.bingoscape.notifications;

import org.bingoscape.board.BoardDiff;

/**
 * Turns the board changes found by {@link BoardDiff} into popups.
 */
public final class BoardNotifications {
    private BoardNotifications() {
    }

    /**
     * The popup for a change, or null for a change this class does not know.
     */
    public static Notification toNotification(BoardDiff.Event event, String bingoTitle) {
        if (event instanceof BoardDiff.TierUnlocked) {
            return new TierUnlockedNotification(((BoardDiff.TierUnlocked) event).getTier(), bingoTitle);
        }
        if (event instanceof BoardDiff.ShipResult) {
            BoardDiff.ShipResult shot = (BoardDiff.ShipResult) event;
            String title = shot.getTileTitle() == null || shot.getTileTitle().trim().isEmpty() ? "Tile" : shot.getTileTitle();
            return new ShipResultNotification(title, shot.getResult(), shot.getShipsRemaining());
        }
        return null;
    }
}
