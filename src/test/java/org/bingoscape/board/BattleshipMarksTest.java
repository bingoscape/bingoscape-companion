package org.bingoscape.board;

import org.bingoscape.ContractFixtures;
import org.bingoscape.board.BattleshipMarks.Mark;
import org.bingoscape.models.BattleshipState;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BattleshipMarksTest {
    private static final UUID A = UUID.fromString("cccccccc-0000-4000-8000-000000000001");
    private static final UUID B = UUID.fromString("cccccccc-0000-4000-8000-000000000002");
    private static final UUID C = UUID.fromString("cccccccc-0000-4000-8000-000000000003");

    private static Tile tile(UUID id, boolean blind) {
        Tile tile = new Tile();
        tile.setId(id);
        tile.setBlind(blind);
        return tile;
    }

    private static BattleshipState state(boolean started, UUID[] hit, UUID[] miss, UUID[] sunk) {
        BattleshipState.Marks marks = new BattleshipState.Marks();
        marks.setHitTileIds(hit == null ? null : Arrays.asList(hit));
        marks.setMissTileIds(miss == null ? null : Arrays.asList(miss));
        marks.setSunkTileIds(sunk == null ? null : Arrays.asList(sunk));
        BattleshipState state = new BattleshipState();
        state.setStarted(started);
        state.setMarks(marks);
        state.setOpponentShipsTotal(3);
        return state;
    }

    @Test
    public void sunkBeatsHitBeatsMissWhenAnIdIsListedTwice() {
        BattleshipMarks marks = BattleshipMarks.of(state(true,
                new UUID[]{A, B}, new UUID[]{A, B, C}, new UUID[]{A}));

        assertEquals(Mark.SUNK, marks.markOf(tile(A, false)));
        assertEquals(Mark.HIT, marks.markOf(tile(B, false)));
        assertEquals(Mark.MISS, marks.markOf(tile(C, false)));
        assertEquals(Mark.NONE, marks.markOf(tile(UUID.randomUUID(), false)));
    }

    @Test
    public void priorityDoesNotDependOnListOrder() {
        BattleshipMarks marks = BattleshipMarks.of(state(true, new UUID[]{B}, new UUID[]{B}, null));

        assertEquals(Mark.HIT, marks.markOf(tile(B, false)));
        assertEquals(1, marks.markedCount());
    }

    @Test
    public void blindTilesNeverCarryAMark() {
        BattleshipMarks marks = BattleshipMarks.of(state(true, new UUID[]{A}, new UUID[]{B}, new UUID[]{C}));

        assertEquals(Mark.NONE, marks.markOf(tile(A, true)));
        assertEquals(Mark.NONE, marks.markOf(tile(B, true)));
        assertEquals(Mark.NONE, marks.markOf(tile(C, true)));
    }

    @Test
    public void nothingIsMarkedBeforeTheStart() {
        BattleshipMarks marks = BattleshipMarks.of(state(false, new UUID[]{A}, new UUID[]{B}, new UUID[]{C}));

        assertEquals(0, marks.markedCount());
        assertEquals(Mark.NONE, marks.markOf(tile(A, false)));
        assertTrue(marks.notStarted());
    }

    @Test
    public void missingStateIsNullSafe() {
        BattleshipMarks marks = BattleshipMarks.of(null);

        assertSame(BattleshipMarks.EMPTY, marks);
        assertEquals(Mark.NONE, marks.markOf(tile(A, false)));
        assertEquals(Mark.NONE, marks.markOf(null));
        assertEquals(Mark.NONE, marks.markOf(tile(null, false)));
        assertFalse(marks.notStarted());
        assertNull(marks.legend);
        assertEquals(0, marks.shipsRemaining());
        assertEquals(0, marks.shipsTotal());
    }

    @Test
    public void missingListsAndNullIdsAreSkipped() {
        BattleshipState state = state(true, null, new UUID[]{null, B}, null);
        assertEquals(Mark.MISS, BattleshipMarks.of(state).markOf(tile(B, false)));

        state.setMarks(null);
        assertEquals(0, BattleshipMarks.of(state).markedCount());
    }

    @Test
    public void bingoWithoutBattleshipBlockDoesNotCrashTheModel() {
        Bingo bingo = new Bingo();
        bingo.setBingoType(org.bingoscape.models.BingoType.BATTLESHIP);
        Tile tile = tile(A, false);
        tile.setTitle("Zulrah");
        bingo.setTiles(Arrays.asList(tile));

        BoardModel model = BoardModel.of(bingo);

        assertSame(BattleshipKind.SHOW_MARKERS, model.kind);
        assertEquals(Mark.NONE, model.views[0].mark);
        assertFalse(model.views[0].notStarted);
    }

    @Test
    public void shipsRemainingIsTotalMinusSunk() {
        BattleshipState state = state(true, null, null, null);
        state.setOpponentShipsTotal(5);
        state.setOpponentShipsSunk(2);

        BattleshipMarks marks = BattleshipMarks.of(state);

        assertEquals(3, marks.shipsRemaining());
        assertEquals(5, marks.shipsTotal());
        assertEquals("Ships remaining: 3/5", marks.legend);
    }

    @Test
    public void shipsRemainingNeverGoesNegative() {
        BattleshipState state = state(true, null, null, null);
        state.setOpponentShipsTotal(2);
        state.setOpponentShipsSunk(4);

        BattleshipMarks marks = BattleshipMarks.of(state);

        assertEquals(0, marks.shipsRemaining());
        assertEquals("Ships remaining: 0/2", marks.legend);
    }

    @Test
    public void startedWithoutShipsHasNoLegend() {
        BattleshipState state = state(true, null, null, null);
        state.setOpponentShipsTotal(0);

        assertNull(BattleshipMarks.of(state).legend);
    }

    @Test
    public void legendBeforeTheStartNamesTheStartDate() {
        BattleshipState state = state(false, null, null, null);
        assertEquals("Not started yet", BattleshipMarks.of(state).legend);

        state.setStartDate(new Date(1_792_000_000_000L));
        assertTrue(BattleshipMarks.of(state).legend.startsWith("Starts "));
    }

    @Test
    public void startedFixtureMarksEachTileOnce() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        BoardModel model = BoardModel.of(bingo);

        assertEquals(Mark.HIT, model.views[0].mark);
        assertEquals(Mark.MISS, model.views[1].mark);
        assertEquals(Mark.SUNK, model.views[2].mark);
        assertEquals("Ships remaining: 2/3", ((BattleshipMarks) model.data).legend);
    }

    @Test
    public void beforeStartFixtureHasBlindUnmarkedTiles() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        BoardModel model = BoardModel.of(bingo);

        for (TileView view : model.views) {
            assertTrue(view.blind);
            assertEquals(Mark.NONE, view.mark);
            assertTrue(view.notStarted);
        }
    }
}
