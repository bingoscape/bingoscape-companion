package org.bingoscape.board;

import org.bingoscape.ContractFixtures;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.TierState;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TierLockTest {
    private static TierState state(int tier, boolean unlocked, long earned, long required) {
        TierState state = new TierState();
        state.setTier(tier);
        state.setUnlocked(unlocked);
        state.setEarnedXp(earned);
        state.setXpRequired(required);
        return state;
    }

    private static Tile tile(int index, int tier) {
        Tile tile = new Tile();
        tile.setId(UUID.randomUUID());
        tile.setIndex(index);
        tile.setTier(tier);
        tile.setTitle("t" + index);
        tile.setWeight(5);
        return tile;
    }

    private static Tile stub(int index, int tier) {
        Tile stub = new Tile();
        stub.setId(UUID.randomUUID());
        stub.setIndex(index);
        stub.setTier(tier);
        stub.setTierLocked(true);
        return stub;
    }

    private static Bingo progression(List<Tile> tiles, TierState... states) {
        Bingo bingo = new Bingo();
        bingo.setBingoType(BingoType.PROGRESSION);
        bingo.setColumns(3);
        bingo.setTiles(tiles);
        ProgressionMetadata progression = new ProgressionMetadata();
        progression.setTiers(Arrays.asList(states));
        bingo.setProgression(progression);
        return bingo;
    }

    @Test
    public void xpNeededIsRemainingXpOfThePreviousTier() {
        List<TierState> tiers = Arrays.asList(state(0, true, 2, 5), state(1, false, 0, 8));

        assertEquals(3, TierLadder.xpNeeded(tiers, 1));
        // Tier 2 is behind tier 1, which has earned nothing yet
        assertEquals(8, TierLadder.xpNeeded(tiers, 2));
    }

    @Test
    public void xpNeededNeverGoesNegative() {
        List<TierState> tiers = Arrays.asList(state(0, true, 9, 5), state(1, false, 0, 5));
        assertEquals(0, TierLadder.xpNeeded(tiers, 1));
    }

    @Test
    public void xpNeededSkipsMissingTierNumbers() {
        List<TierState> tiers = Arrays.asList(state(0, true, 1, 5), state(2, false, 0, 5), state(5, false, 0, 7));

        assertEquals(4, TierLadder.xpNeeded(tiers, 2));
        assertEquals(5, TierLadder.xpNeeded(tiers, 5));
        assertEquals(2, TierLadder.previousTier(tiers, 5));
        assertEquals(0, TierLadder.previousTier(tiers, 1));
    }

    @Test
    public void xpNeededIsUnknownWithoutAPreviousTier() {
        assertEquals(TierLadder.UNKNOWN, TierLadder.xpNeeded(null, 1));
        assertEquals(TierLadder.UNKNOWN, TierLadder.xpNeeded(Collections.<TierState>emptyList(), 1));
        assertEquals(TierLadder.UNKNOWN, TierLadder.xpNeeded(Arrays.asList(state(1, true, 0, 5)), 1));
        assertEquals(TierLadder.UNKNOWN, TierLadder.xpNeeded(Arrays.asList(state(0, true, 0, 5), (TierState) null), 0));
    }

    @Test
    public void headerTexts() {
        assertEquals("Tier 1  2/5 XP", TierLadder.unlockedHeader(1, 2, 5));
        assertEquals("Tier 3  12 XP", TierLadder.unlockedHeader(3, 12, 0));
        assertEquals("Tier 2", TierLadder.unlockedHeader(2, TierLadder.UNKNOWN, TierLadder.UNKNOWN));
        assertEquals("Tier 2  Locked: 3 XP needed", TierLadder.lockedHeader(2, 3));
        assertEquals("Tier 2  Locked", TierLadder.lockedHeader(2, TierLadder.UNKNOWN));
        assertEquals("Tier 2 locked: 3 more XP needed in Tier 1", TierLadder.lockNote(2, 3, 1));
        assertEquals("Tier 2 locked", TierLadder.lockNote(2, TierLadder.UNKNOWN, -1));
    }

    @Test
    public void lockStateComesFromTheTierList() {
        // The tile carries no tierLocked flag, but tiers[] says its tier is locked
        Bingo bingo = progression(Arrays.asList(tile(0, 0), tile(1, 1)),
                state(0, true, 2, 5), state(1, false, 0, 5));

        BoardModel model = BoardModel.of(bingo);

        assertFalse(model.views[0].tierLocked);
        assertTrue(model.views[1].tierLocked);
        assertEquals("Tier 1 locked: 3 more XP needed in Tier 0", model.views[1].lockNote);
        assertNull(model.views[0].lockNote);
    }

    @Test
    public void tileFlagLocksEvenWithoutTierState() {
        Bingo bingo = progression(Arrays.asList(tile(0, 0), stub(1, 1)));
        bingo.getProgression().setTiers(null);

        BoardModel model = BoardModel.of(bingo);

        assertFalse(model.views[0].tierLocked);
        assertTrue(model.views[1].tierLocked);
        assertEquals("Tier 1 locked", model.views[1].lockNote);
    }

    @Test
    public void progressionKindGatesOpenAndSubmit() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        BoardModel model = BoardModel.of(bingo);
        BoardKind kind = model.kind;

        TileView open = model.views[0];
        TileView locked = model.views[1];

        assertTrue(kind.canOpen(open));
        assertTrue(kind.canSubmit(open, bingo));
        assertEquals("Take & Review Screenshot", kind.primaryLabel(open, bingo));
        assertNull(kind.detailsNotice(open));

        // Locked tiles open for details but never submit
        assertTrue(kind.canOpen(locked));
        assertFalse(kind.canSubmit(locked, bingo));
        assertEquals("Tier locked", kind.primaryLabel(locked, bingo));
        assertEquals("Tier 1 locked: 3 more XP needed in Tier 0", kind.detailsNotice(locked));
    }

    @Test
    public void lockedTileThatIsAlreadyAcceptedStillCannotSubmit() {
        Tile done = tile(0, 1);
        TileSubmission accepted = new TileSubmission();
        accepted.setStatus(TileSubmissionType.ACCEPTED);
        done.setSubmission(accepted);
        Bingo bingo = progression(Arrays.asList(done), state(0, true, 0, 5), state(1, false, 0, 5));

        BoardModel model = BoardModel.of(bingo);

        assertFalse(model.kind.canSubmit(model.views[0], bingo));
    }

    @Test
    public void lockedTileTooltipExplainsTheLock() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        BoardModel model = BoardModel.of(bingo);

        assertNotNull(model.kind.tooltip(model.views[1], id -> null));
        assertEquals("Tier 1 locked: 3 more XP needed in Tier 0", model.views[1].lockNote);
    }

    @Test
    public void stubTilesNeverNullPointer() {
        Tile bare = new Tile();
        bare.setTierLocked(true);
        bare.setTier(2);
        Bingo bingo = progression(new ArrayList<>(Arrays.asList(tile(0, 0), bare)), state(0, true, 0, 5));
        bingo.getProgression().setTierXpRequirements(null);

        BoardModel model = BoardModel.of(bingo);
        TileView view = model.views[1];

        assertEquals("", view.title);
        assertNull(view.imageUrl);
        assertTrue(view.tierLocked);
        assertFalse(model.kind.canOpen(view));  // no id
        assertFalse(model.kind.canSubmit(view, bingo));
        assertNotNull(model.kind.layout(model, 1920, 1080, 1.0, false));
    }

    @Test
    public void ladderGroupsTilesByAscendingTier() {
        // Tile order is by index, tiers interleaved
        Bingo bingo = progression(Arrays.asList(tile(0, 1), tile(1, 0), tile(2, 1), tile(3, 0)),
                state(0, true, 0, 5), state(1, true, 0, 5));

        TierLadder ladder = TierLadder.of(bingo, BoardModel.of(bingo).tiles);

        assertEquals(2, ladder.bands.length);
        assertEquals(0, ladder.bands[0].tier);
        assertEquals(Arrays.asList(1, 3), asList(ladder.bands[0].tiles));
        assertEquals(1, ladder.bands[1].tier);
        assertEquals(Arrays.asList(0, 2), asList(ladder.bands[1].tiles));
        assertSame(ladder.bands[1], ladder.bandOf(1));
        assertNull(ladder.bandOf(7));
    }

    @Test
    public void ladderHeadersAndLegendFollowTheTierList() {
        Bingo bingo = ContractFixtures.bingo("progression");
        TierLadder ladder = TierLadder.of(bingo, BoardModel.of(bingo).tiles);

        assertEquals("Tier 0  5/5 XP", ladder.bands[0].header);
        assertTrue(ladder.bands[0].complete());
        assertEquals("Tier 1  2/5 XP", ladder.bands[1].header);
        assertFalse(ladder.bands[1].complete());
        // Tier 2 has no tiles on the board but counts as a locked tier of the ladder
        assertEquals("Tiers unlocked: 2/3", ladder.legend);
    }

    @Test
    public void lockedBandHeaderShowsRemainingXp() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        TierLadder ladder = TierLadder.of(bingo, BoardModel.of(bingo).tiles);

        TierLadder.Band locked = ladder.bandOf(1);
        assertTrue(locked.locked);
        assertEquals(3, locked.xpNeeded);
        assertEquals(0, locked.previousTier);
        assertEquals("Tier 1  Locked: 3 XP needed", locked.header);
        assertEquals("Tiers unlocked: 1/2", ladder.legend);
    }

    private static List<Integer> asList(int[] values) {
        List<Integer> list = new ArrayList<>();
        for (int v : values) {
            list.add(v);
        }
        return list;
    }
}
