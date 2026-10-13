package org.bingoscape.board;

import org.bingoscape.ContractFixtures;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TileGateTest {
    @Test
    public void standardTilesOpenAndSubmit() {
        Bingo bingo = ContractFixtures.bingo("standard");
        Tile tile = bingo.getTiles().get(0);
        tile.setSubmission(null); // the fixture tile is already accepted

        assertTrue(TileGate.canOpen(bingo, tile));
        assertTrue(TileGate.canSubmit(bingo, tile));
        assertNull(TileGate.notice(bingo, tile));
    }

    @Test
    public void lockedTierTileCannotBeSubmitted() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        Tile open = bingo.getTiles().get(0);
        Tile locked = bingo.getTiles().get(1);

        assertTrue(TileGate.canSubmit(bingo, open));
        assertFalse(TileGate.canSubmit(bingo, locked));
        assertEquals("Tier locked", TileGate.submitLabel(bingo, locked));
        assertEquals("Tier 1 locked: 3 more XP needed in Tier 0", TileGate.notice(bingo, locked));
    }

    @Test
    public void blindTileCanNeitherBeOpenedNorSubmitted() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        Tile blind = null;
        for (Tile tile : bingo.getTiles()) {
            if (tile.isBlind()) {
                blind = tile;
            }
        }

        assertFalse(TileGate.canOpen(bingo, blind));
        assertFalse(TileGate.canSubmit(bingo, blind));
        assertEquals(BattleshipKind.BLIND_LABEL, TileGate.submitLabel(bingo, blind));
    }

    @Test
    public void nothingIsSubmittableBeforeABattleshipStarts() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        for (Tile tile : bingo.getTiles()) {
            tile.setBlind(false);
        }

        assertFalse(TileGate.canSubmit(bingo, bingo.getTiles().get(0)));
        assertEquals(BattleshipKind.NOT_STARTED_LABEL, TileGate.submitLabel(bingo, bingo.getTiles().get(0)));
    }

    @Test
    public void startedBattleshipTilesSubmit() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");

        assertTrue(TileGate.canSubmit(bingo, bingo.getTiles().get(0)));
    }

    @Test
    public void nullArgumentsGateEverything() {
        assertFalse(TileGate.canOpen(null, null));
        assertFalse(TileGate.canSubmit(null, new Tile()));
    }
}
