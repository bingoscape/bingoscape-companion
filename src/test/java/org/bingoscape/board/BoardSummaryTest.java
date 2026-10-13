package org.bingoscape.board;

import org.bingoscape.ContractFixtures;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoardSummaryTest {
    private static void submit(Bingo bingo, int index, TileSubmissionType status) {
        TileSubmission submission = new TileSubmission();
        submission.setStatus(status);
        bingo.getTiles().get(index).setSubmission(submission);
    }

    @Test
    public void standardBoardCountsEveryVisibleTile() {
        Bingo bingo = ContractFixtures.bingo("standard");
        int visible = 0;
        int weight = 0;
        for (org.bingoscape.models.Tile tile : bingo.getTiles()) {
            if (!tile.isHidden()) {
                visible++;
                weight += tile.getWeight();
            }
        }

        BoardSummary summary = BoardSummary.of(bingo);

        assertEquals(BingoType.STANDARD, summary.type);
        assertEquals(visible, summary.tiles);
        assertEquals(weight, summary.xpTotal);
        assertTrue(summary.tiers.isEmpty());
        assertNull(summary.shipsLegend);
        assertEquals(0, summary.hits + summary.misses + summary.sunk);
    }

    @Test
    public void nullBingoIsAnEmptyStandardSummary() {
        BoardSummary summary = BoardSummary.of(null);

        assertEquals(BingoType.STANDARD, summary.type);
        assertEquals(0, summary.tiles);
    }

    @Test
    public void lockedStubTilesAreNotCounted() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        // Fixture: one tile in the unlocked tier 0 (5 XP), the rest are stubs of locked tier 1
        BoardSummary summary = BoardSummary.of(bingo);

        assertEquals(BingoType.PROGRESSION, summary.type);
        assertEquals(1, summary.tiles);
        assertEquals(5, summary.xpTotal);
    }

    @Test
    public void tiersListEveryTierWithWhatALockedOneStillNeeds() {
        BoardSummary summary = BoardSummary.of(ContractFixtures.bingo("progression-locked-stubs"));

        assertEquals(2, summary.tiers.size());
        BoardSummary.Tier first = summary.tiers.get(0);
        assertEquals(0, first.tier);
        assertTrue(first.unlocked);
        assertEquals(2, first.earnedXp);
        assertEquals(5, first.requiredXp);
        assertEquals(-1, first.xpNeeded);

        BoardSummary.Tier second = summary.tiers.get(1);
        assertEquals(1, second.tier);
        assertFalse(second.unlocked);
        assertEquals(3, second.xpNeeded);
    }

    @Test
    public void tileOfALockedTierIsSkippedEvenWithoutTheStubFlag() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        for (org.bingoscape.models.Tile tile : bingo.getTiles()) {
            tile.setTierLocked(false);
        }

        // tiers[] still says tier 1 is locked
        assertEquals(1, BoardSummary.of(bingo).tiles);
    }

    @Test
    public void progressionBoardWithoutTierListHasNoTierRows() {
        Bingo bingo = ContractFixtures.bingo("progression");
        bingo.getProgression().setTiers(null);

        assertTrue(BoardSummary.of(bingo).tiers.isEmpty());
    }

    @Test
    public void battleshipCountsMarksAndShipsRemaining() {
        BoardSummary summary = BoardSummary.of(ContractFixtures.bingo("battleship-started"));

        assertEquals(BingoType.BATTLESHIP, summary.type);
        assertEquals(3, summary.tiles);
        assertEquals(1, summary.hits);
        assertEquals(1, summary.misses);
        assertEquals(1, summary.sunk);
        assertEquals("Ships remaining: 2/3", summary.shipsLegend);
    }

    @Test
    public void blindTilesAreNotCountedAndNothingIsMarkedBeforeTheStart() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        long blind = bingo.getTiles().stream().filter(t -> t.isBlind()).count();

        BoardSummary summary = BoardSummary.of(bingo);

        assertEquals(bingo.getTiles().size() - blind, summary.tiles);
        assertEquals(0, summary.hits + summary.misses + summary.sunk);
        assertFalse(summary.shipsLegend.startsWith("Ships remaining"));
    }

    @Test
    public void markedBlindTilesAreNotCounted() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        bingo.getTiles().get(0).setBlind(true); // the hit tile

        BoardSummary summary = BoardSummary.of(bingo);

        assertEquals(2, summary.tiles);
        assertEquals(0, summary.hits);
        assertEquals(1, summary.misses);
        assertEquals(1, summary.sunk);
    }

    @Test
    public void submissionStatesAreCountedOnlyForCountedTiles() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        submit(bingo, 0, TileSubmissionType.ACCEPTED);
        submit(bingo, 1, TileSubmissionType.PENDING);
        submit(bingo, 2, TileSubmissionType.DECLINED);
        bingo.getTiles().get(2).setBlind(true);

        BoardSummary summary = BoardSummary.of(bingo);

        assertEquals(1, summary.completed);
        assertEquals(1, summary.xpEarned);
        assertEquals(1, summary.pending);
        assertEquals(0, summary.declined);
    }
}
