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
}
