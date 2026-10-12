package org.bingoscape.models;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class Bingo {
    private UUID id;
    private UUID eventId;
    private String title;
    private String description;
    private int rows;
    private int columns;
    private String codephrase;
    private Date createdAt;
    private Date updatedAt;
    private boolean locked;
    private boolean visible;
    private List<Tile> tiles;
    private BingoType bingoType;
    private ProgressionMetadata progression; // Only present for progression boards
    private BattleshipState battleship; // Only present for battleship boards

    /**
     * Missing or unknown bingoType values are treated as a standard board.
     */
    public BingoType getBingoType() {
        return bingoType == null ? BingoType.STANDARD : bingoType;
    }
}
