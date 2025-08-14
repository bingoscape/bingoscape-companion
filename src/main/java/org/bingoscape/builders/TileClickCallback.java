package org.bingoscape.builders;

import org.bingoscape.models.Tile;
import java.awt.event.MouseEvent;

/**
 * Callback interface for handling tile click events.
 * 
 * This functional interface allows decoupling of tile click handling
 * from the board builders, enabling flexible response to user interactions.
 */
@FunctionalInterface
public interface TileClickCallback {
    /**
     * Called when a tile is clicked by the user.
     *
     * @param tile The tile that was clicked
     * @param event The mouse event that triggered the click
     */
    void onTileClicked(Tile tile, MouseEvent event);
    
    /**
     * Default method for backwards compatibility.
     * 
     * @param tile The tile that was clicked
     * @deprecated Use onTileClicked(Tile, MouseEvent) instead
     */
    @Deprecated
    default void onTileClicked(Tile tile) {
        onTileClicked(tile, null);
    }
}