package org.bingoscape.builders;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.models.Goal;
import org.bingoscape.ui.ColorPalette;
import org.bingoscape.constants.BingoTypeConstants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.imageio.ImageIO;

/**
 * Builder for standard grid-based bingo boards.
 *
 * Creates traditional rectangular grid layouts where tiles are arranged
 * in a fixed row/column pattern. Supports dynamic tile sizing based on
 * available space and handles hidden tiles with placeholder panels.
 *
 * @author BingoScape Development Team
 */
public class StandardBingoBoardBuilder extends BingoBoardBuilder {

    // Standard board specific constants
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;
    private static final int AVAILABLE_WIDTH_MARGIN = 40;
    private static final int AVAILABLE_HEIGHT_MARGIN = 100;

    // UI Constants
    private static final int SMALL_FONT_SIZE = 10;
    private static final int MEDIUM_FONT_SIZE = 12;
    private static final int LARGE_FONT_SIZE = 24;
    private static final int IMAGE_MARGIN = 10;
    private static final int IMAGE_TITLE_OFFSET = 20;
    private static final int SMALL_SPACING = 4;

    /**
     * Configuration class for standard board customization.
     */
    public static class StandardBoardConfiguration {
        private final boolean allowDynamicSizing;
        private final boolean showTierIndicators;

        public StandardBoardConfiguration() {
            this.allowDynamicSizing = true;
            this.showTierIndicators = true;
        }

        public StandardBoardConfiguration(boolean allowDynamicSizing, boolean showTierIndicators) {
            this.allowDynamicSizing = allowDynamicSizing;
            this.showTierIndicators = showTierIndicators;
        }

        public boolean isAllowDynamicSizing() { return allowDynamicSizing; }
        public boolean isShowTierIndicators() { return showTierIndicators; }
    }

    private final StandardBoardConfiguration configuration;

    /**
     * Creates a new standard board builder with default configuration.
     *
     * @param plugin The main plugin instance
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache
     */
    public StandardBingoBoardBuilder(BingoScapePlugin plugin, ExecutorService imageExecutor,
                                   Map<String, ImageIcon> imageCache) {
        this(plugin, imageExecutor, imageCache, new StandardBoardConfiguration());
    }

    /**
     * Creates a new standard board builder with custom configuration.
     *
     * @param plugin The main plugin instance
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache
     * @param configuration Custom configuration for this builder
     */
    public StandardBingoBoardBuilder(BingoScapePlugin plugin, ExecutorService imageExecutor,
                                   Map<String, ImageIcon> imageCache,
                                   StandardBoardConfiguration configuration) {
        super(plugin, imageExecutor, imageCache);
        this.configuration = configuration;
    }

    @Override
    protected void setupLayout(JPanel panel, Bingo bingo) {
        // Use traditional grid layout for standard bingos
        int rows = bingo.getRows() <= 0 ? DEFAULT_GRID_SIZE : bingo.getRows();
        int cols = bingo.getColumns() <= 0 ? DEFAULT_GRID_SIZE : bingo.getColumns();
        panel.setLayout(new GridLayout(rows, cols, SPACING, SPACING));
    }

    @Override
    protected void populateBoard(JPanel panel, Bingo bingo) {
        // Sort tiles by position for standard grid layout
        bingo.getTiles().sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        // Calculate tile size once for all tiles
        int tileSize = calculateTileSize(bingo, new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        // Create all tile panels
        for (Tile tile : bingo.getTiles()) {
            JPanel tilePanel;

            if (tile.isHidden()) {
                tilePanel = createHiddenTilePanel(tile, tileSize);
            } else {
                tilePanel = createTilePanel(tile, tileSize);
            }

            panel.add(tilePanel);
        }
    }

    @Override
    public BoardType getSupportedBoardType() {
        return BoardType.STANDARD;
    }

    @Override
    protected int calculateTileSize(Bingo bingo, Dimension availableSpace) {
        if (!configuration.allowDynamicSizing) {
            return 120; // Fixed size fallback
        }

        int rows = bingo.getRows() > 0 ? bingo.getRows() : DEFAULT_GRID_SIZE;
        int cols = bingo.getColumns() > 0 ? bingo.getColumns() : DEFAULT_GRID_SIZE;
        int availableWidth = (int) availableSpace.getWidth() - AVAILABLE_WIDTH_MARGIN;
        int availableHeight = (int) availableSpace.getHeight() - AVAILABLE_HEIGHT_MARGIN;

        return Math.min(availableWidth / cols, availableHeight / rows) - PADDING;
    }

    @Override
    protected JPanel createTilePanel(Tile tile, int tileSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));

        // Set background and border based on submission status using factory
        getTileFactory().applyTileAppearance(panel, tile.getSubmission());

        // Set tooltip with detailed information using shared factory
        panel.setToolTipText(getTileFactory().createDetailedTooltip(tile, getCurrentBingo()));

        // Add image or title content using factory
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            getTileFactory().loadTileImage(panel, tile, tileSize);
        } else {
            getTileFactory().addTileTitle(panel, tile);
        }

        // Add XP and tier indicators using factory
        getTileFactory().addTileIndicators(panel, tile, getCurrentBingo(), configuration.showTierIndicators);

        // Add status overlay if needed using factory
        getTileFactory().addStatusOverlay(panel, tile);

        // Add click behavior using factory
        getTileFactory().addTileInteractionListeners(panel, tile, tileClickCallback);

        return panel;
    }

    /**
     * Creates a hidden tile placeholder panel.
     *
     * @param tile The hidden tile data
     * @param tileSize The size for the panel
     * @return A panel representing the hidden tile
     */
    private JPanel createHiddenTilePanel(Tile tile, int tileSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));
        panel.setMaximumSize(new Dimension(tileSize, tileSize));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_COLOR, 1),
                new EmptyBorder(SMALL_SPACING, SMALL_SPACING, SMALL_SPACING, SMALL_SPACING)
        ));

        JLabel hiddenLabel = new JLabel("?", SwingConstants.CENTER);
        hiddenLabel.setForeground(Color.GRAY);
        hiddenLabel.setFont(new Font(hiddenLabel.getFont().getName(), Font.BOLD, LARGE_FONT_SIZE));
        panel.add(hiddenLabel, BorderLayout.CENTER);

        panel.setToolTipText("Hidden tile");
        return panel;
    }



    @Override
    protected StandardBoardConfiguration getBoardConfiguration() {
        return configuration;
    }
}
