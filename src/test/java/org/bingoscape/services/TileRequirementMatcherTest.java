package org.bingoscape.services;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.TierState;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import java.util.Arrays;

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

    private static Tile tierTile(Integer tier) {
        Tile tile = new Tile();
        tile.setTier(tier);
        return tile;
    }

    private static Bingo progression(boolean tierOneUnlocked) {
        TierState zero = new TierState();
        zero.setTier(0);
        zero.setUnlocked(true);
        TierState one = new TierState();
        one.setTier(1);
        one.setUnlocked(tierOneUnlocked);
        ProgressionMetadata progression = new ProgressionMetadata();
        progression.setTiers(Arrays.asList(zero, one));
        Bingo bingo = new Bingo();
        bingo.setProgression(progression);
        return bingo;
    }

    @Test
    public void lockedTierStubsAreSkipped() {
        Tile stub = tierTile(1);
        stub.setTierLocked(true);

        assertTrue(TileRequirementMatcher.isLockedOrBlind(new Bingo(), stub));
        assertTrue(TileRequirementMatcher.isLockedOrBlind(progression(true), stub));
    }

    @Test
    public void tileInATierThatTiersListCallsLockedIsSkipped() {
        assertTrue(TileRequirementMatcher.isLockedOrBlind(progression(false), tierTile(1)));
        assertFalse(TileRequirementMatcher.isLockedOrBlind(progression(false), tierTile(0)));
        assertFalse(TileRequirementMatcher.isLockedOrBlind(progression(true), tierTile(1)));
    }

    @Test
    public void blindTilesAreSkipped() {
        Tile blind = new Tile();
        blind.setBlind(true);

        assertTrue(TileRequirementMatcher.isLockedOrBlind(new Bingo(), blind));
    }

    @Test
    public void ordinaryTilesAreTracked() {
        assertFalse(TileRequirementMatcher.isLockedOrBlind(new Bingo(), new Tile()));
        assertFalse(TileRequirementMatcher.isLockedOrBlind(null, new Tile()));
        // No tier, no tier list, or a tier the list does not mention
        assertFalse(TileRequirementMatcher.isLockedOrBlind(progression(false), new Tile()));
        assertFalse(TileRequirementMatcher.isLockedOrBlind(progression(false), tierTile(7)));
        assertFalse(TileRequirementMatcher.isLockedOrBlind(new Bingo(), tierTile(1)));
    }
}
