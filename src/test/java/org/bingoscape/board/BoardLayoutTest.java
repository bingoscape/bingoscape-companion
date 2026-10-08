package org.bingoscape.board;

import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoardLayoutTest {
    // Fixed-mode game viewport
    private static final int FIXED_WIDTH = 765;
    private static final int FIXED_HEIGHT = 503;

    @Test
    public void fiveByFiveFitsFixedViewport() {
        BoardLayout layout = BoardLayout.compute(5, 5, 25, FIXED_WIDTH, FIXED_HEIGHT, 1.0, true);

        assertTrue(layout.width <= FIXED_WIDTH * BoardLayout.VIEWPORT_FILL);
        assertTrue(layout.height <= FIXED_HEIGHT * BoardLayout.VIEWPORT_FILL);
        assertEquals(25, layout.tiles.size());
    }

    @Test
    public void largeViewportUsesScaledDefaultTileSize() {
        BoardLayout layout = BoardLayout.compute(5, 5, 25, 2560, 1440, 1.0, false);
        assertEquals(BoardLayout.TILE_DEFAULT, layout.tileSize);

        BoardLayout scaled = BoardLayout.compute(5, 5, 25, 2560, 1440, 1.5, false);
        assertEquals(144, scaled.tileSize);
    }

    @Test
    public void tileSizeNeverDropsBelowMinimum() {
        BoardLayout layout = BoardLayout.compute(7, 7, 49, FIXED_WIDTH, FIXED_HEIGHT, 1.0, true);
        assertTrue(layout.tileSize >= BoardLayout.TILE_MIN);

        BoardLayout tiny = BoardLayout.compute(10, 10, 100, 300, 200, 1.0, true);
        assertEquals(BoardLayout.TILE_MIN, tiny.tileSize);
    }

    @Test
    public void nonPositiveDimensionsDefaultToFive() {
        BoardLayout layout = BoardLayout.compute(0, -1, 25, 1920, 1080, 1.0, false);

        assertEquals(5, layout.rows);
        assertEquals(5, layout.cols);
    }

    @Test
    public void extraTilesAddRows() {
        BoardLayout layout = BoardLayout.compute(5, 5, 30, 1920, 1080, 1.0, false);

        assertEquals(6, layout.rows);
        assertEquals(30, layout.tiles.size());
    }

    @Test
    public void tileIndexAtMapsCellsAndRejectsGaps() {
        BoardLayout layout = BoardLayout.compute(3, 3, 9, 1920, 1080, 1.0, false);

        Rectangle first = layout.tiles.get(0);
        assertEquals(0, layout.tileIndexAt(first.x, first.y));

        Rectangle last = layout.tiles.get(8);
        assertEquals(8, layout.tileIndexAt(last.x + last.width - 1, last.y + last.height - 1));

        Rectangle middle = layout.tiles.get(4);
        assertEquals(4, layout.tileIndexAt(middle.x + middle.width / 2, middle.y + middle.height / 2));

        // The gap right of the first tile
        assertEquals(-1, layout.tileIndexAt(first.x + first.width + 1, first.y + 1));
        // Header area
        assertEquals(-1, layout.tileIndexAt(first.x, 0));
    }

    @Test
    public void headerButtonsDoNotOverlapTitle() {
        BoardLayout layout = BoardLayout.compute(3, 3, 9, FIXED_WIDTH, FIXED_HEIGHT, 1.0, false);

        assertTrue(layout.title.x + layout.title.width <= layout.pinButton.x);
        assertTrue(layout.pinButton.x + layout.pinButton.width <= layout.reloadButton.x);
        assertTrue(layout.reloadButton.x + layout.reloadButton.width <= layout.closeButton.x);
        assertTrue(layout.closeButton.x + layout.closeButton.width <= layout.width);
        assertEquals(BoardLayout.Button.CLOSE, layout.buttonAt(layout.closeButton.x + 1, layout.closeButton.y + 1));
        assertNull(layout.codephrase);
    }

    @Test
    public void narrowBoardsAreWidenedAndCentered() {
        BoardLayout layout = BoardLayout.compute(1, 1, 1, 1920, 1080, 1.0, false);

        assertEquals(BoardLayout.MIN_WIDTH, layout.width);
        Rectangle grid = layout.gridArea;
        assertEquals(layout.width - grid.x - grid.width, grid.x);
    }
}
