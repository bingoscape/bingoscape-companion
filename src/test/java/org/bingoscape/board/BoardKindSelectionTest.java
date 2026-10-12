package org.bingoscape.board;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BoardKindSelectionTest {
    private static Tile tile(int index, String title) {
        Tile tile = new Tile();
        tile.setId(UUID.randomUUID());
        tile.setIndex(index);
        tile.setTitle(title);
        tile.setWeight(10);
        return tile;
    }

    @Test
    public void nullAndStandardSelectStandardKind() {
        assertSame(StandardKind.INSTANCE, BoardKind.forType(null));
        assertSame(StandardKind.INSTANCE, BoardKind.forType(BingoType.STANDARD));
        assertEquals(BingoType.STANDARD, StandardKind.INSTANCE.type());
    }

    @Test
    public void missingOrUnknownBingoTypeSelectsStandardKind() {
        Bingo bingo = new Bingo();
        assertSame(StandardKind.INSTANCE, BoardKind.forBingo(bingo));
        assertSame(StandardKind.INSTANCE, BoardKind.forBingo(null));

        // Gson leaves the field null for a type string this plugin version does not know
        bingo.setBingoType(null);
        assertSame(StandardKind.INSTANCE, BoardKind.forBingo(bingo));
    }

    @Test
    public void everyTypeResolvesToAKind() {
        for (BingoType type : BingoType.values()) {
            assertTrue(BoardKind.forType(type) != null);
        }
    }

    @Test
    public void modelUsesTheKindOfTheBingo() {
        Bingo bingo = new Bingo();
        bingo.setBingoType(BingoType.STANDARD);

        assertSame(StandardKind.INSTANCE, BoardModel.of(bingo).kind);
    }

    @Test
    public void modelViewsFollowSortedTileOrder() {
        Tile second = tile(1, "second");
        Tile first = tile(0, "first");
        Bingo bingo = new Bingo();
        bingo.setTiles(Arrays.asList(second, first));

        BoardModel model = BoardModel.of(bingo);

        assertEquals(2, model.views.length);
        assertSame(first, model.views[0].tile);
        assertEquals(0, model.views[0].index);
        assertSame(second, model.views[1].tile);
        assertEquals(1, model.views[1].index);
        assertSame(model.views[1], model.viewById.get(second.getId()));
    }

    @Test
    public void viewPrecomputesRenderValues() {
        Tile tile = tile(0, "Kill Zulrah");
        tile.setHeaderImage("https://example.test/a.png");
        TileSubmission submission = new TileSubmission();
        submission.setStatus(TileSubmissionType.PENDING);
        tile.setSubmission(submission);

        TileView view = TileView.of(tile, 0);

        assertEquals("Kill Zulrah", view.title);
        assertEquals("10 XP", view.xpLabel);
        assertEquals("https://example.test/a.png", view.imageUrl);
        assertEquals(TileSubmissionType.PENDING, view.status);
        assertFalse(view.hidden);
        assertFalse(view.blind);
        assertFalse(view.tierLocked);
    }

    @Test
    public void lockedStubTilesGetNullSafeViews() {
        Tile stub = new Tile();
        stub.setId(UUID.randomUUID());
        stub.setTierLocked(true);
        stub.setHeaderImage("   ");

        TileView view = TileView.of(stub, 3);

        assertEquals("", view.title);
        assertEquals("0 XP", view.xpLabel);
        assertNull(view.imageUrl);
        assertNull(view.status);
        assertTrue(view.tierLocked);
    }

    @Test
    public void standardKindOpensAndSubmitsLikeTheOldOverlay() {
        Bingo bingo = new Bingo();
        BoardKind kind = StandardKind.INSTANCE;

        TileView normal = TileView.of(tile(0, "t"), 0);
        assertTrue(kind.canOpen(normal));
        assertTrue(kind.canSubmit(normal, bingo));

        Tile hiddenTile = tile(1, "h");
        hiddenTile.setHidden(true);
        assertFalse(kind.canOpen(TileView.of(hiddenTile, 1)));

        Tile noId = tile(2, "n");
        noId.setId(null);
        assertFalse(kind.canOpen(TileView.of(noId, 2)));

        Tile done = tile(3, "d");
        TileSubmission accepted = new TileSubmission();
        accepted.setStatus(TileSubmissionType.ACCEPTED);
        done.setSubmission(accepted);
        assertFalse(kind.canSubmit(TileView.of(done, 3), bingo));

        bingo.setLocked(true);
        assertFalse(kind.canSubmit(normal, bingo));
    }

    @Test
    public void standardLayoutMatchesPlainGridCompute() {
        Bingo bingo = new Bingo();
        bingo.setRows(4);
        bingo.setColumns(6);
        Tile[] tiles = new Tile[24];
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = tile(i, "t" + i);
        }
        bingo.setTiles(Arrays.asList(tiles));
        BoardModel model = BoardModel.of(bingo);

        BoardLayout viaKind = model.kind.layout(model, 1920, 1080, 1.0, true);
        BoardLayout direct = BoardLayout.compute(4, 6, 24, 1920, 1080, 1.0, true);

        assertEquals(direct.width, viaKind.width);
        assertEquals(direct.height, viaKind.height);
        assertEquals(direct.tileSize, viaKind.tileSize);
        assertEquals(direct.tiles, viaKind.tiles);
    }
}
