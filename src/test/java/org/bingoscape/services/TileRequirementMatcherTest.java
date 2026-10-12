package org.bingoscape.services;

import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TileRequirementMatcherTest {
    private static Tile tileWith(TileSubmissionType status) {
        Tile tile = new Tile();
        TileSubmission submission = new TileSubmission();
        submission.setStatus(status);
        tile.setSubmission(submission);
        return tile;
    }

    @Test
    public void acceptedSubmissionCountsAsApproved() {
        assertTrue(TileRequirementMatcher.isTileApproved(tileWith(TileSubmissionType.ACCEPTED)));
    }

    @Test
    public void otherStatusesAreNotApproved() {
        assertFalse(TileRequirementMatcher.isTileApproved(tileWith(TileSubmissionType.PENDING)));
        assertFalse(TileRequirementMatcher.isTileApproved(tileWith(TileSubmissionType.DECLINED)));
        assertFalse(TileRequirementMatcher.isTileApproved(tileWith(TileSubmissionType.REQUIRES_INTERACTION)));
        assertFalse(TileRequirementMatcher.isTileApproved(tileWith(TileSubmissionType.NOT_SUBMITTED)));
        assertFalse(TileRequirementMatcher.isTileApproved(tileWith(null)));
    }

    @Test
    public void tileWithoutSubmissionIsNotApproved() {
        assertFalse(TileRequirementMatcher.isTileApproved(new Tile()));
    }
}
