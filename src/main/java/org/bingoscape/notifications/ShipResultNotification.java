package org.bingoscape.notifications;

import lombok.Value;
import org.bingoscape.board.BoardDiff;

/**
 * Popup for a shot of the team on a battleship tile: hit, miss or ship sunk.
 */
@Value
public class ShipResultNotification implements Notification {
    String tileTitle;
    BoardDiff.Result result;
    /** Opponent ships still afloat, or -1 when unknown. */
    int shipsRemaining;

    @Override
    public int priority() {
        return PRIORITY_COMPLETION;
    }

    @Override
    public double holdFactor() {
        return result == BoardDiff.Result.SUNK ? 1.5 : 1.0;
    }
}
