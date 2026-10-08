package org.bingoscape.board;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BoardModelAndTooltipTest {
    private static Tile tile(int index, String title) {
        Tile tile = new Tile();
        tile.setId(UUID.randomUUID());
        tile.setIndex(index);
        tile.setTitle(title);
        tile.setWeight(10);
        return tile;
    }

    @Test
    public void modelSortsACopyAndLeavesBingoTilesUntouched() {
        Tile second = tile(1, "second");
        Tile first = tile(0, "first");
        Bingo bingo = new Bingo();
        List<Tile> original = new ArrayList<>(Arrays.asList(second, first));
        bingo.setTiles(original);

        BoardModel model = BoardModel.of(bingo);

        assertSame(first, model.tiles.get(0));
        assertSame(second, model.tiles.get(1));
        assertSame(second, bingo.getTiles().get(0));
        assertSame(first, model.byId.get(first.getId()));
    }

    @Test
    public void modelHandlesMissingTiles() {
        assertTrue(BoardModel.of(new Bingo()).tiles.isEmpty());
    }

    @Test
    public void tooltipShowsTitleStatusAndEscapesTags() {
        Tile tile = tile(0, "Kill <b>Zulrah</b>");
        tile.setDescription("Get a drop");
        TileSubmission submission = new TileSubmission();
        submission.setStatus(TileSubmissionType.PENDING);
        submission.setSubmissionCount(2);
        tile.setSubmission(submission);

        String tooltip = BoardTooltipBuilder.build(tile);

        assertTrue(tooltip.contains("Kill (b)Zulrah(/b) (10 XP)"));
        assertTrue(tooltip.contains("Pending Review"));
        assertTrue(tooltip.contains("Submissions: 2"));
        assertFalse(tooltip.contains("<b>"));
    }

    @Test
    public void hiddenTileTooltip() {
        Tile tile = tile(0, "secret");
        tile.setHidden(true);

        assertEquals("Hidden tile", BoardTooltipBuilder.build(tile));
    }

    @Test
    public void primaryButtonStatesMatchWindow() {
        Tile tile = tile(0, "t");
        Bingo bingo = new Bingo();
        assertTrue(TileDetailsView.primaryEnabled(tile, bingo));
        assertEquals("Take & Review Screenshot", TileDetailsView.primaryLabel(tile, bingo));

        TileSubmission accepted = new TileSubmission();
        accepted.setStatus(TileSubmissionType.ACCEPTED);
        tile.setSubmission(accepted);
        assertFalse(TileDetailsView.primaryEnabled(tile, bingo));
        assertEquals("Already Completed", TileDetailsView.primaryLabel(tile, bingo));

        // Locked wins over completed, as in the old dialog
        bingo.setLocked(true);
        assertEquals("Submissions locked", TileDetailsView.primaryLabel(tile, bingo));
    }
}
