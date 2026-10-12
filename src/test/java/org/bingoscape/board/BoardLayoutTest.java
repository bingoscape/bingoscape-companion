package org.bingoscape.board;

import org.junit.Test;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void tileIndexAtMatchesRowMajorGridMath() {
        BoardLayout layout = BoardLayout.compute(4, 5, 20, FIXED_WIDTH, FIXED_HEIGHT, 1.0, true);
        Rectangle grid = layout.gridArea;
        int step = layout.tileSize + BoardLayout.GAP;

        // Every pixel of the grid and a margin around it must agree with the old arithmetic
        for (int y = grid.y - 3; y < grid.y + grid.height + 3; y++) {
            for (int x = grid.x - 3; x < grid.x + grid.width + 3; x++) {
                assertEquals("at " + x + "," + y, rowMajorIndex(layout, step, x, y), layout.tileIndexAt(x, y));
            }
        }
    }

    @Test
    public void raggedLastRowOnlyHitsExistingCells() {
        // Three columns, last row has two tiles: the free third cell is a gap, not a tile
        int size = 40;
        List<Rectangle> tiles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tiles.add(new Rectangle(20 + (i % 3) * (size + 4), 50 + (i / 3) * (size + 4), size, size));
        }
        BoardLayout layout = BoardLayout.withTiles(3, 3, 240, 200, size, false,
                new Rectangle(20, 50, 3 * size + 8, 3 * size + 8), tiles);

        assertEquals(7, layout.tileIndexAt(20 + (size + 4) + 1, 50 + 2 * (size + 4) + 1));
        assertEquals(-1, layout.tileIndexAt(20 + 2 * (size + 4) + 1, 50 + 2 * (size + 4) + 1));
        assertEquals(5, layout.tileIndexAt(20 + 2 * (size + 4) + 1, 50 + (size + 4) + 1));
    }

    @Test
    public void irregularRectanglesAreHitByPosition() {
        // Tiles of different sizes in a non-grid arrangement, as tiered boards will produce
        List<Rectangle> tiles = new ArrayList<>();
        tiles.add(new Rectangle(10, 40, 100, 30));
        tiles.add(new Rectangle(10, 80, 40, 60));
        tiles.add(new Rectangle(60, 80, 50, 25));
        tiles.add(new Rectangle(60, 115, 50, 25));
        BoardLayout layout = BoardLayout.withTiles(3, 2, 200, 160, 40, false, new Rectangle(10, 40, 100, 100), tiles);

        assertEquals(0, layout.tileIndexAt(109, 69));
        assertEquals(1, layout.tileIndexAt(10, 139));
        assertEquals(2, layout.tileIndexAt(60, 80));
        assertEquals(3, layout.tileIndexAt(109, 139));
        // Gaps between tiles and the area right of the narrow tile
        assertEquals(-1, layout.tileIndexAt(55, 100));
        assertEquals(-1, layout.tileIndexAt(80, 108));
        assertEquals(-1, layout.tileIndexAt(50, 75));
        // Inside the grid area but outside every rectangle, and outside the area entirely
        assertEquals(-1, layout.tileIndexAt(110, 100));
        assertEquals(-1, layout.tileIndexAt(5, 45));
        assertEquals(-1, layout.tileIndexAt(10, 140));
    }

    @Test
    public void withTilesPlacesHeaderControlsLikeCompute() {
        BoardLayout computed = BoardLayout.compute(3, 3, 9, 1920, 1080, 1.0, true);
        BoardLayout custom = BoardLayout.withTiles(3, 3, computed.width, computed.height, computed.tileSize, true,
                computed.gridArea, computed.tiles);

        assertEquals(computed.title, custom.title);
        assertEquals(computed.codephrase, custom.codephrase);
        assertEquals(computed.pinButton, custom.pinButton);
        assertEquals(computed.reloadButton, custom.reloadButton);
        assertEquals(computed.closeButton, custom.closeButton);
    }

    @Test
    public void withTilesKeepsLegendAndSectionHeaders() {
        List<Rectangle> tiles = new ArrayList<>();
        tiles.add(new Rectangle(10, 60, 40, 40));
        Rectangle legend = new Rectangle(10, 30, 100, 14);
        List<Rectangle> sections = new ArrayList<>();
        sections.add(new Rectangle(10, 44, 100, 16));
        BoardLayout layout = BoardLayout.withTiles(1, 1, 200, 160, 40, false, new Rectangle(10, 44, 100, 60), tiles,
                legend, sections);

        assertEquals(legend, layout.legend);
        assertEquals(sections, layout.sections);
        // Headers are inside the grid area but are not tiles
        assertEquals(-1, layout.tileIndexAt(20, 50));
        assertEquals(0, layout.tileIndexAt(20, 70));
    }

    @Test
    public void plainGridHasNoLegendOrSections() {
        BoardLayout layout = BoardLayout.compute(3, 3, 9, 1920, 1080, 1.0, false);

        assertNull(layout.legend);
        assertTrue(layout.sections.isEmpty());
    }

    private static int rowMajorIndex(BoardLayout layout, int step, int x, int y) {
        Rectangle grid = layout.gridArea;
        if (!grid.contains(x, y)) {
            return -1;
        }
        int dx = x - grid.x;
        int dy = y - grid.y;
        if (dx % step >= layout.tileSize || dy % step >= layout.tileSize) {
            return -1;
        }
        int col = dx / step;
        int row = dy / step;
        if (col >= layout.cols || row >= layout.rows) {
            return -1;
        }
        return row * layout.cols + col;
    }
}
