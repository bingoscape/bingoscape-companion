package org.bingoscape.board;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure layout math for the board overlay. All rectangles are relative to the overlay origin.
 */
final class BoardLayout {
    static final int PAD = 10;
    static final int GAP = 4;
    static final int TITLE_HEIGHT = 24;
    static final int CODEPHRASE_HEIGHT = 14;
    static final int HEADER_SPACING = 6;
    static final int TILE_DEFAULT = 96;
    static final int TILE_MIN = 40;
    static final int DEFAULT_GRID_SIZE = 5;
    static final int BUTTON_SIZE = 20;
    static final int PIN_BUTTON_WIDTH = 44;
    static final int MIN_WIDTH = 240;
    static final double VIEWPORT_FILL = 0.9;

    enum Button {
        PIN, RELOAD, CLOSE, NONE
    }

    final int rows;
    final int cols;
    final int width;
    final int height;
    final int tileSize;
    final Rectangle title;
    final Rectangle codephrase;
    final Rectangle pinButton;
    final Rectangle reloadButton;
    final Rectangle closeButton;
    final Rectangle gridArea;
    final List<Rectangle> tiles;

    private BoardLayout(int rows, int cols, int width, int height, int tileSize, Rectangle title, Rectangle codephrase,
                        Rectangle pinButton, Rectangle reloadButton, Rectangle closeButton, Rectangle gridArea,
                        List<Rectangle> tiles) {
        this.rows = rows;
        this.cols = cols;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.title = title;
        this.codephrase = codephrase;
        this.pinButton = pinButton;
        this.reloadButton = reloadButton;
        this.closeButton = closeButton;
        this.gridArea = gridArea;
        this.tiles = tiles;
    }

    static BoardLayout compute(int rows, int cols, int tileCount, int viewportWidth, int viewportHeight,
                               double scale, boolean hasCodephrase) {
        int effectiveCols = cols <= 0 ? DEFAULT_GRID_SIZE : cols;
        int effectiveRows = rows <= 0 ? DEFAULT_GRID_SIZE : rows;
        // Like GridLayout, grow the grid when there are more tiles than cells
        effectiveRows = Math.max(effectiveRows, (tileCount + effectiveCols - 1) / effectiveCols);

        int headerHeight = TITLE_HEIGHT + (hasCodephrase ? CODEPHRASE_HEIGHT : 0) + HEADER_SPACING;
        int availableWidth = (int) (viewportWidth * VIEWPORT_FILL) - 2 * PAD;
        int availableHeight = (int) (viewportHeight * VIEWPORT_FILL) - 2 * PAD - headerHeight;
        int fitWidth = (availableWidth - (effectiveCols - 1) * GAP) / effectiveCols;
        int fitHeight = (availableHeight - (effectiveRows - 1) * GAP) / effectiveRows;
        int preferred = (int) Math.round(TILE_DEFAULT * scale);
        int tileSize = Math.max(TILE_MIN, Math.min(preferred, Math.min(fitWidth, fitHeight)));

        int gridWidth = effectiveCols * tileSize + (effectiveCols - 1) * GAP;
        int gridHeight = effectiveRows * tileSize + (effectiveRows - 1) * GAP;
        int width = Math.max(MIN_WIDTH, 2 * PAD + gridWidth);
        int height = 2 * PAD + headerHeight + gridHeight;

        int buttonY = PAD + (TITLE_HEIGHT - BUTTON_SIZE) / 2;
        Rectangle closeButton = new Rectangle(width - PAD - BUTTON_SIZE, buttonY, BUTTON_SIZE, BUTTON_SIZE);
        Rectangle reloadButton = new Rectangle(closeButton.x - GAP - BUTTON_SIZE, buttonY, BUTTON_SIZE, BUTTON_SIZE);
        Rectangle pinButton = new Rectangle(reloadButton.x - GAP - PIN_BUTTON_WIDTH, buttonY, PIN_BUTTON_WIDTH, BUTTON_SIZE);
        Rectangle title = new Rectangle(PAD, PAD, Math.max(0, pinButton.x - GAP - PAD), TITLE_HEIGHT);
        Rectangle codephrase = hasCodephrase
                ? new Rectangle(PAD, PAD + TITLE_HEIGHT, width - 2 * PAD, CODEPHRASE_HEIGHT)
                : null;

        int gridX = (width - gridWidth) / 2;
        int gridY = PAD + headerHeight;
        Rectangle gridArea = new Rectangle(gridX, gridY, gridWidth, gridHeight);

        List<Rectangle> tiles = new ArrayList<>(effectiveRows * effectiveCols);
        for (int i = 0; i < effectiveRows * effectiveCols; i++) {
            int x = gridX + (i % effectiveCols) * (tileSize + GAP);
            int y = gridY + (i / effectiveCols) * (tileSize + GAP);
            tiles.add(new Rectangle(x, y, tileSize, tileSize));
        }

        return new BoardLayout(effectiveRows, effectiveCols, width, height, tileSize, title, codephrase,
                pinButton, reloadButton, closeButton, gridArea, Collections.unmodifiableList(tiles));
    }

    /**
     * Returns the tile index under the point, or -1 for gaps and points outside the grid.
     */
    int tileIndexAt(int x, int y) {
        if (!gridArea.contains(x, y)) {
            return -1;
        }

        int step = tileSize + GAP;
        int dx = x - gridArea.x;
        int dy = y - gridArea.y;
        if (dx % step >= tileSize || dy % step >= tileSize) {
            return -1;
        }

        int col = dx / step;
        int row = dy / step;
        if (col >= cols || row >= rows) {
            return -1;
        }
        return row * cols + col;
    }

    Button buttonAt(int x, int y) {
        if (pinButton.contains(x, y)) {
            return Button.PIN;
        }
        if (reloadButton.contains(x, y)) {
            return Button.RELOAD;
        }
        if (closeButton.contains(x, y)) {
            return Button.CLOSE;
        }
        return Button.NONE;
    }

    boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
