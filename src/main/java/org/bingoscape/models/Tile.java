package org.bingoscape.models;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Data
public class Tile {
    private UUID id;
    private UUID bingoId;
    private String headerImage;
    private String title;
    private String description;
    private int weight;
    private int index;
    private boolean isHidden;
    private Date createdAt;
    private Date updatedAt;
}
