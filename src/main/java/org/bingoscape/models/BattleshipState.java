package org.bingoscape.models;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Battleship state of a board for the calling player's team. Opponent ship positions are never sent.
 */
@Data
public class BattleshipState {
    private boolean started;
    private Date startDate;
    private Date serverNow;
    private List<ShipRule> rules;
    private Marks marks;
    private int opponentShipsSunk;
    private int opponentShipsTotal;

    @Data
    public static class Marks {
        private List<UUID> hitTileIds;
        private List<UUID> missTileIds;
        private List<UUID> sunkTileIds;
    }
}
