package org.bingoscape.models;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Represents a submission for a tile
 */
@Data
public class TileSubmission {
    private String id;
    private TileSubmissionType status;
    private Date lastUpdated;
    private int submissionCount;
    private SubmissionDetails latestSubmission;
}

