package org.bingoscape.board;

import org.bingoscape.ContractFixtures;
import org.bingoscape.board.BoardDiff.Event;
import org.bingoscape.board.BoardDiff.Result;
import org.bingoscape.board.BoardDiff.ShipResult;
import org.bingoscape.board.BoardDiff.TierUnlocked;
import org.bingoscape.models.BattleshipState;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TierState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BoardDiffTest {
    private static final UUID BINGO = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID A = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    private static final UUID B = UUID.fromString("cccccccc-0000-4000-8000-000000000002");
    private static final UUID C = UUID.fromString("cccccccc-0000-4000-8000-000000000003");
    private static final UUID D = UUID.fromString("cccccccc-0000-4000-8000-000000000004");

    private static Tile tile(UUID id, int index, boolean blind) {
        Tile tile = new Tile();
        tile.setId(id);
        tile.setIndex(index);
        tile.setTitle("Tile " + index);
        tile.setBlind(blind);
        return tile;
    }

    private static Bingo ships(boolean started, UUID[] hit, UUID[] miss, UUID[] sunk, int shipsSunk) {
        BattleshipState.Marks marks = new BattleshipState.Marks();
        marks.setHitTileIds(Arrays.asList(hit));
        marks.setMissTileIds(Arrays.asList(miss));
        marks.setSunkTileIds(Arrays.asList(sunk));
        BattleshipState state = new BattleshipState();
        state.setStarted(started);
        state.setMarks(marks);
        state.setOpponentShipsSunk(shipsSunk);
        state.setOpponentShipsTotal(3);

        Bingo bingo = new Bingo();
        bingo.setId(BINGO);
        bingo.setBingoType(BingoType.BATTLESHIP);
        bingo.setTiles(new ArrayList<>(Arrays.asList(tile(A, 0, false), tile(B, 1, false), tile(C, 2, false), tile(D, 3, false))));
        bingo.setBattleship(state);
        return bingo;
    }

    private static UUID[] ids(UUID... ids) {
        return ids;
    }

    private static Bingo progression(boolean... unlocked) {
        List<TierState> tiers = new ArrayList<>();
        for (int i = 0; i < unlocked.length; i++) {
            TierState state = new TierState();
            state.setTier(i);
            state.setUnlocked(unlocked[i]);
            tiers.add(state);
        }
        ProgressionMetadata metadata = new ProgressionMetadata();
        metadata.setTiers(tiers);

        Bingo bingo = new Bingo();
        bingo.setId(BINGO);
        bingo.setBingoType(BingoType.PROGRESSION);
        bingo.setProgression(metadata);
        return bingo;
    }

    private static void assertShot(Event event, UUID tile, Result result) {
        assertTrue(event instanceof ShipResult);
        assertEquals(tile, ((ShipResult) event).getTileId());
        assertEquals(result, ((ShipResult) event).getResult());
    }

    // ---- Baselines ----

    @Test
    public void firstLoadReportsNothing() {
        assertTrue(BoardDiff.compute(null, ships(true, ids(A), ids(B), ids(), 0)).isEmpty());
        assertTrue(BoardDiff.compute(null, progression(true, true)).isEmpty());
        assertTrue(BoardDiff.compute(ships(true, ids(A), ids(), ids(), 0), null).isEmpty());
    }

    @Test
    public void anotherBingoReportsNothing() {
        Bingo old = ships(true, ids(), ids(), ids(), 0);
        Bingo other = ships(true, ids(A), ids(B), ids(), 0);
        other.setId(UUID.randomUUID());

        assertTrue(BoardDiff.compute(old, other).isEmpty());
    }

    @Test
    public void anotherBoardTypeReportsNothing() {
        Bingo old = progression(true, false);
        Bingo updated = progression(true, true);
        updated.setBingoType(BingoType.STANDARD);

        assertTrue(BoardDiff.compute(old, updated).isEmpty());
    }

    @Test
    public void standardBoardsReportNothing() {
        Bingo old = ContractFixtures.bingo("standard");
        assertTrue(BoardDiff.compute(old, ContractFixtures.bingo("standard")).isEmpty());
    }

    @Test
    public void teamSwitchReportsNothingButTheSameScopeDoes() {
        Bingo old = ships(true, ids(), ids(), ids(), 0);
        Bingo updated = ships(true, ids(A), ids(), ids(), 0);

        assertTrue(BoardDiff.compute(old, "event/Team A", updated, "event/Team B").isEmpty());
        assertTrue(BoardDiff.compute(old, "event/Team A", updated, null).isEmpty());
        assertTrue(BoardDiff.compute(old, null, updated, null).isEmpty());
        assertEquals(1, BoardDiff.compute(old, "event/Team A", updated, "event/Team A").size());
    }

    @Test
    public void repeatedRefreshProducesNoDuplicates() {
        Bingo before = ships(true, ids(), ids(), ids(), 0);
        Bingo after = ships(true, ids(A), ids(B), ids(), 0);

        assertEquals(2, BoardDiff.compute(before, after).size());
        // The next refresh returns the same state
        assertTrue(BoardDiff.compute(after, ships(true, ids(A), ids(B), ids(), 0)).isEmpty());
        assertTrue(BoardDiff.compute(after, after).isEmpty());

        Bingo tiers = progression(true, true, false);
        assertTrue(BoardDiff.compute(tiers, tiers).isEmpty());
    }

    // ---- Progression ----

    @Test
    public void newlyUnlockedTierIsReported() {
        List<Event> events = BoardDiff.compute(progression(true, false, false), progression(true, true, false));

        assertEquals(1, events.size());
        assertEquals(1, ((TierUnlocked) events.get(0)).getTier());
    }

    @Test
    public void cascadingUnlocksAreReportedLowestTierFirst() {
        List<Event> events = BoardDiff.compute(progression(true, false, false, false), progression(true, true, true, false));

        assertEquals(2, events.size());
        assertEquals(1, ((TierUnlocked) events.get(0)).getTier());
        assertEquals(2, ((TierUnlocked) events.get(1)).getTier());
    }

    @Test
    public void alreadyUnlockedAndStillLockedTiersAreSilent() {
        assertTrue(BoardDiff.compute(progression(true, true, false), progression(true, true, false)).isEmpty());
    }

    @Test
    public void tierMissingFromTheEarlierSnapshotIsNotReported() {
        Bingo old = progression(true);
        assertTrue(BoardDiff.compute(old, progression(true, true, true)).isEmpty());

        Bingo noTiers = progression();
        noTiers.getProgression().setTiers(null);
        assertTrue(BoardDiff.compute(noTiers, progression(true, true)).isEmpty());
        assertTrue(BoardDiff.compute(progression(true, false), noTiers).isEmpty());
    }

    @Test
    public void relockedTierIsSilent() {
        assertTrue(BoardDiff.compute(progression(true, true), progression(true, false)).isEmpty());
    }

    @Test
    public void progressionFixtureDiffsAsExpected() {
        Bingo locked = ContractFixtures.bingo("progression");
        // Pretend the earlier copy still had tier 1 locked
        Bingo earlier = ContractFixtures.bingo("progression");
        earlier.getProgression().stateOf(1).setUnlocked(false);

        List<Event> events = BoardDiff.compute(earlier, locked);
        assertEquals(1, events.size());
        assertEquals(1, ((TierUnlocked) events.get(0)).getTier());
    }

    // ---- Battleship ----

    @Test
    public void newMissAndHitAreReportedInBoardOrder() {
        Bingo old = ships(true, ids(), ids(), ids(), 0);
        Bingo updated = ships(true, ids(C), ids(B), ids(), 0);

        List<Event> events = BoardDiff.compute(old, updated);

        assertEquals(2, events.size());
        assertShot(events.get(0), B, Result.MISS);
        assertShot(events.get(1), C, Result.HIT);
    }

    @Test
    public void sinkingShotIsSunkNotAnExtraHit() {
        // A was hit earlier; the shot on B sinks the ship, so the server lists B as hit and sunk at once
        Bingo old = ships(true, ids(A), ids(), ids(), 0);
        Bingo updated = ships(true, ids(A, B), ids(), ids(A, B), 1);

        List<Event> events = BoardDiff.compute(old, updated);

        assertEquals(1, events.size());
        assertShot(events.get(0), B, Result.SUNK);
    }

    @Test
    public void earlierHitTurningSunkIsNotNews() {
        // The ship of A and B is now sunk; only the new tile B is announced, A just changes its mark
        Bingo old = ships(true, ids(A), ids(), ids(), 0);
        Bingo updated = ships(true, ids(), ids(), ids(A, B), 1);

        List<Event> events = BoardDiff.compute(old, updated);

        assertEquals(1, events.size());
        assertShot(events.get(0), B, Result.SUNK);
    }

    @Test
    public void singleTileShipSunkInOneShot() {
        List<Event> events = BoardDiff.compute(ships(true, ids(), ids(), ids(), 0), ships(true, ids(D), ids(), ids(D), 1));

        assertEquals(1, events.size());
        assertShot(events.get(0), D, Result.SUNK);
    }

    @Test
    public void shotsCarryTitleAndShipsRemaining() {
        List<Event> events = BoardDiff.compute(ships(true, ids(), ids(), ids(), 0), ships(true, ids(), ids(B), ids(), 1));

        ShipResult shot = (ShipResult) events.get(0);
        assertEquals("Tile 1", shot.getTileTitle());
        assertEquals(2, shot.getShipsRemaining());
    }

    @Test
    public void shipsRemainingIsUnknownWithoutAFleet() {
        Bingo updated = ships(true, ids(), ids(B), ids(), 0);
        updated.getBattleship().setOpponentShipsTotal(0);

        ShipResult shot = (ShipResult) BoardDiff.compute(ships(true, ids(), ids(), ids(), 0), updated).get(0);
        assertEquals(-1, shot.getShipsRemaining());
    }

    @Test
    public void nothingIsReportedBeforeTheStartOrForBlindTiles() {
        Bingo old = ships(false, ids(), ids(), ids(), 0);
        // Marks listed while the event has not started are ignored
        assertTrue(BoardDiff.compute(old, ships(false, ids(A), ids(B), ids(), 0)).isEmpty());

        Bingo blind = ships(true, ids(A), ids(B), ids(), 0);
        for (Tile tile : blind.getTiles()) {
            tile.setBlind(true);
        }
        assertTrue(BoardDiff.compute(ships(true, ids(), ids(), ids(), 0), blind).isEmpty());
    }

    @Test
    public void marksAppearingWhenTheEventStartsAreReported() {
        List<Event> events = BoardDiff.compute(ships(false, ids(), ids(), ids(), 0), ships(true, ids(A), ids(), ids(), 0));

        assertEquals(1, events.size());
        assertShot(events.get(0), A, Result.HIT);
    }

    @Test
    public void missingBattleshipStateOrTilesAreSafe() {
        Bingo old = ships(true, ids(), ids(), ids(), 0);
        Bingo updated = ships(true, ids(A), ids(), ids(), 0);

        Bingo noState = ships(true, ids(), ids(), ids(), 0);
        noState.setBattleship(null);
        assertTrue(BoardDiff.compute(old, noState).isEmpty());
        assertEquals(1, BoardDiff.compute(noState, updated).size());

        updated.setTiles(null);
        assertTrue(BoardDiff.compute(old, updated).isEmpty());
        updated.setTiles(Collections.<Tile>singletonList(null));
        assertTrue(BoardDiff.compute(old, updated).isEmpty());
    }

    @Test
    public void battleshipFixturesDiffAsExpected() {
        Bingo started = ContractFixtures.bingo("battleship-started");
        assertTrue(BoardDiff.compute(started, ContractFixtures.bingo("battleship-started")).isEmpty());
        assertTrue(BoardDiff.compute(ContractFixtures.bingo("battleship-before-start"),
                ContractFixtures.bingo("battleship-before-start")).isEmpty());

        List<Event> events = BoardDiff.compute(ContractFixtures.bingo("battleship-before-start"), started);
        assertEquals(3, events.size());
        assertShot(events.get(0), A, Result.HIT);
        assertShot(events.get(1), B, Result.MISS);
        assertShot(events.get(2), C, Result.SUNK);
    }
}
