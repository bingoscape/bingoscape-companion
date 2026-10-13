package org.bingoscape.models;

import com.google.gson.Gson;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Deserializes the API contract fixtures with a plain Gson, as the plugin does at runtime.
 */
public class ModelDeserializationTest {
    private static final Gson GSON = new Gson();

    private static Bingo load(String name) throws IOException {
        try (InputStream in = ModelDeserializationTest.class.getResourceAsStream("/contracts/" + name + ".json")) {
            assertNotNull("missing fixture " + name, in);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, Bingo.class);
            }
        }
    }

    private static UUID uuid(String s) {
        return UUID.fromString(s);
    }

    @Test
    public void standardBoardKeepsExistingFieldsAndDefaultsNewOnes() throws IOException {
        Bingo bingo = load("standard");
        assertEquals(BingoType.STANDARD, bingo.getBingoType());
        assertNull(bingo.getProgression());
        assertNull(bingo.getBattleship());
        assertEquals(2, bingo.getRows());
        assertEquals("abc123", bingo.getCodephrase());
        assertEquals(2, bingo.getTiles().size());

        Tile first = bingo.getTiles().get(0);
        assertEquals("Abyssal whip", first.getTitle());
        assertEquals(Integer.valueOf(0), first.getTier());
        assertFalse(first.isBlind());
        assertFalse(first.isTierLocked());
        assertEquals(TileSubmissionType.ACCEPTED, first.getSubmission().getStatus());
        assertTrue(first.getGoalTree().get(0).getProgress().isComplete());

        Tile hidden = bingo.getTiles().get(1);
        assertTrue(hidden.isHidden());
        assertNull(hidden.getTier());
    }

    @Test
    public void missingOrUnknownBingoTypeIsStandard() {
        assertEquals(BingoType.STANDARD, GSON.fromJson("{\"title\":\"x\"}", Bingo.class).getBingoType());
        assertEquals(BingoType.STANDARD, GSON.fromJson("{\"bingoType\":null}", Bingo.class).getBingoType());
        assertEquals(BingoType.STANDARD, GSON.fromJson("{\"bingoType\":\"something-new\"}", Bingo.class).getBingoType());
        assertEquals(BingoType.PROGRESSION, GSON.fromJson("{\"bingoType\":\"progression\"}", Bingo.class).getBingoType());
        assertEquals(BingoType.BATTLESHIP, GSON.fromJson("{\"bingoType\":\"battleship\"}", Bingo.class).getBingoType());
    }

    @Test
    public void progressionBoardExposesTiersAndLegacyFields() throws IOException {
        Bingo bingo = load("progression");
        assertEquals(BingoType.PROGRESSION, bingo.getBingoType());
        assertNull(bingo.getBattleship());

        ProgressionMetadata progression = bingo.getProgression();
        assertEquals(3, progression.getTierXpRequirements().size());
        assertEquals(Long.valueOf(5), progression.getTierXpRequirements().get(0).getXpRequired());
        assertEquals(2, progression.getUnlockedTiers().size());
        assertEquals(Boolean.TRUE, progression.getTierProgress().get(0).getIsUnlocked());

        assertEquals(3, progression.getTiers().size());
        TierState t0 = progression.getTiers().get(0);
        assertEquals(0, t0.getTier());
        assertTrue(t0.isUnlocked());
        assertNotNull(t0.getUnlockedAt());
        assertEquals(5, t0.getEarnedXp());
        assertEquals(5, t0.getXpRequired());

        TierState t1 = progression.getTiers().get(1);
        assertEquals(2, t1.getEarnedXp());

        TierState t2 = progression.getTiers().get(2);
        assertEquals(2, t2.getTier());
        assertFalse(t2.isUnlocked());
        assertNull(t2.getUnlockedAt());
        assertEquals(0, t2.getEarnedXp());
        assertEquals(5, t2.getXpRequired());
    }

    @Test
    public void lockedTierTilesAreStubs() throws IOException {
        Bingo bingo = load("progression-locked-stubs");
        Tile open = bingo.getTiles().get(0);
        assertFalse(open.isTierLocked());
        assertEquals("Tier 0 tile", open.getTitle());

        for (int i = 1; i < 3; i++) {
            Tile stub = bingo.getTiles().get(i);
            assertTrue(stub.isTierLocked());
            assertNotNull(stub.getId());
            assertEquals(Integer.valueOf(1), stub.getTier());
            assertEquals(i + 1, stub.getIndex());
            assertNull(stub.getTitle());
            assertNull(stub.getDescription());
            assertNull(stub.getHeaderImage());
            assertNull(stub.getGoals());
            assertNull(stub.getGoalTree());
            assertEquals(0, stub.getWeight());
        }
        assertFalse(bingo.getProgression().getTiers().get(1).isUnlocked());
    }

    @Test
    public void battleshipBeforeStartIsBlindWithEmptyMarks() throws IOException {
        Bingo bingo = load("battleship-before-start");
        assertEquals(BingoType.BATTLESHIP, bingo.getBingoType());
        assertNull(bingo.getProgression());
        for (Tile tile : bingo.getTiles()) {
            assertTrue(tile.isBlind());
        }
        assertEquals("Tile 1", bingo.getTiles().get(0).getTitle());

        BattleshipState state = bingo.getBattleship();
        assertFalse(state.isStarted());
        assertNotNull(state.getStartDate());
        assertTrue(state.getStartDate().after(state.getServerNow()));
        assertEquals(2, state.getRules().size());
        assertEquals(3, state.getRules().get(0).getLength());
        assertEquals(2, state.getRules().get(0).getCount());
        assertNotNull(state.getMarks());
        assertTrue(state.getMarks().getHitTileIds().isEmpty());
        assertTrue(state.getMarks().getMissTileIds().isEmpty());
        assertTrue(state.getMarks().getSunkTileIds().isEmpty());
        assertEquals(0, state.getOpponentShipsSunk());
        assertEquals(3, state.getOpponentShipsTotal());
    }

    @Test
    public void battleshipStartedCarriesMarks() throws IOException {
        Bingo bingo = load("battleship-started");
        for (Tile tile : bingo.getTiles()) {
            assertFalse(tile.isBlind());
        }

        BattleshipState state = bingo.getBattleship();
        assertTrue(state.isStarted());
        Date start = state.getStartDate();
        assertTrue(start.before(state.getServerNow()));
        assertEquals(1, state.getRules().size());
        assertEquals(1, state.getMarks().getHitTileIds().size());
        assertEquals(uuid("cccccccc-0000-4000-8000-000000000001"), state.getMarks().getHitTileIds().get(0));
        assertEquals(uuid("cccccccc-0000-4000-8000-000000000002"), state.getMarks().getMissTileIds().get(0));
        assertEquals(uuid("cccccccc-0000-4000-8000-000000000003"), state.getMarks().getSunkTileIds().get(0));
        assertEquals(1, state.getOpponentShipsSunk());
        assertEquals(3, state.getOpponentShipsTotal());
    }

    @Test
    public void teamCompletionTierIsInt() {
        String json = "{\"completions\":[{\"id\":\"1\",\"tile\":{\"points\":10,\"tier\":2}},"
                + "{\"id\":\"2\",\"tile\":{\"points\":4}}]}";
        TeamCompletionsResponse r = GSON.fromJson(json, TeamCompletionsResponse.class);
        assertEquals(2, r.getCompletions().get(0).getTile().getTier());
        assertEquals(0, r.getCompletions().get(1).getTile().getTier());
    }
}
