package org.bingoscape.models;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced Tile model to include submission and goals
 */
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
    private TileSubmission submission;
    private List<Goal> goals;
}
