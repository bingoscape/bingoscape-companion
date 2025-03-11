package org.bingoscape.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the status of a bingo tile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TileStatus {
    private String id;
    private TileSubmissionType status;
}
