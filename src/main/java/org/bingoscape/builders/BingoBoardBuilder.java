package org.bingoscape.builders;

import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Abstract base class for building different types of bingo boards.
 * 
 * This class provides the common infrastructure and extension points for creating
 * various bingo board layouts while maintaining consistent behavior and styling.
 * 
 * Implementations should focus on their specific layout logic while leveraging
 * the shared functionality provided by this base class.
 * 
 * @author BingoScape Development Team
 */
public abstract class BingoBoardBuilder {
    
    // Common constants shared across all board types
    protected static final int DEFAULT_GRID_SIZE = 5;
    protected static final int SPACING = 4;
    protected static final int PADDING = 10;
    
    // Shared resources
    protected final BingoScapePlugin plugin;
    protected final ExecutorService imageExecutor;
    protected final Map<String, ImageIcon> imageCache;
    protected final TileComponentFactory tileFactory;
    
    // Callback for tile clicks
    protected TileClickCallback tileClickCallback;
    
    // Board state
    protected Bingo currentBingo;
    protected JPanel targetPanel;
    
    /**
     * Creates a new board builder with shared resources.
     *
     * @param plugin The main plugin instance for callbacks and configuration
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache for performance
     */
    protected BingoBoardBuilder(BingoScapePlugin plugin, ExecutorService imageExecutor, 
                              Map<String, ImageIcon> imageCache) {
        this.plugin = plugin;
        this.imageExecutor = imageExecutor;
        this.imageCache = imageCache;
        this.tileFactory = new TileComponentFactory(imageExecutor, imageCache);
    }
    
    /**
     * Builds and populates the bingo board in the target panel.
     * 
     * This is the main entry point that coordinates the board creation process.
     * Implementations should not override this method directly, but instead
     * implement the abstract methods called within.
     *
     * @param bingo The bingo data model to display
     * @param targetPanel The panel where the board should be rendered
     */
    public final void buildBoard(Bingo bingo, JPanel targetPanel) {
        this.currentBingo = bingo;
        this.targetPanel = targetPanel;
        
        // Validate inputs
        validateBuildInputs(bingo, targetPanel);
        
        // Clear existing content
        targetPanel.removeAll();
        
        // Set up the layout manager for this board type
        setupLayout(targetPanel, bingo);
        
        // Create and add board content
        populateBoard(targetPanel, bingo);
        
        // Final validation and cleanup
        finalizeBoard(targetPanel, bingo);
        
        // Refresh the UI
        SwingUtilities.invokeLater(() -> {
            targetPanel.revalidate();
            targetPanel.repaint();
        });
    }
    
    /**
     * Sets the callback for handling tile click events.
     *
     * @param callback The callback to invoke when tiles are clicked
     */
    public void setTileClickCallback(TileClickCallback callback) {
        this.tileClickCallback = callback;
    }
    
    /**
     * Sets up the appropriate layout manager for this board type.
     * 
     * Implementations should configure the target panel with the appropriate
     * LayoutManager and any necessary properties for their board type.
     *
     * @param panel The target panel to configure
     * @param bingo The bingo data model for sizing information
     */
    protected abstract void setupLayout(JPanel panel, Bingo bingo);
    
    /**
     * Creates and adds the board content to the target panel.
     * 
     * This is where the main board construction logic should be implemented.
     * Implementations should create tiles, organize them according to their
     * layout strategy, and add them to the target panel.
     *
     * @param panel The target panel to populate
     * @param bingo The bingo data model containing tiles and configuration
     */
    protected abstract void populateBoard(JPanel panel, Bingo bingo);
    
    /**
     * Gets the board type that this builder supports.
     *
     * @return The board type enum value
     */
    public abstract BoardType getSupportedBoardType();
    
    /**
     * Determines if this builder can handle the given bingo configuration.
     * 
     * Default implementation checks the board type, but can be overridden
     * for more complex validation logic.
     *
     * @param bingo The bingo configuration to check
     * @return true if this builder can handle the configuration
     */
    public boolean canHandle(Bingo bingo) {
        return getSupportedBoardType() == BoardType.fromBingo(bingo);
    }
    
    /**
     * Validates the inputs to the build process.
     * 
     * Override this method to add additional validation specific to your board type.
     *
     * @param bingo The bingo data model to validate
     * @param panel The target panel to validate
     * @throws IllegalArgumentException if inputs are invalid
     */
    protected void validateBuildInputs(Bingo bingo, JPanel panel) {
        if (bingo == null) {
            throw new IllegalArgumentException("Bingo cannot be null");
        }
        if (panel == null) {
            throw new IllegalArgumentException("Target panel cannot be null");
        }
        if (bingo.getTiles() == null) {
            throw new IllegalArgumentException("Bingo tiles cannot be null");
        }
    }
    
    /**
     * Performs final validation and cleanup after board construction.
     * 
     * Override this method to add post-construction validation or setup
     * specific to your board type.
     *
     * @param panel The populated target panel
     * @param bingo The bingo data model
     */
    protected void finalizeBoard(JPanel panel, Bingo bingo) {
        // Default implementation does nothing
        // Subclasses can override for specific finalization logic
    }
    
    /**
     * Creates a tile panel component for the given tile.
     * 
     * This method provides a hook for customizing tile creation per board type
     * while maintaining access to shared resources and utilities.
     *
     * @param tile The tile data to create a panel for
     * @param tileSize The size the tile should be displayed at
     * @return A JPanel representing the tile
     */
    protected abstract JPanel createTilePanel(Tile tile, int tileSize);
    
    /**
     * Gets configuration specific to this board type.
     * 
     * This method allows builders to define their own configuration
     * parameters while maintaining the common builder interface.
     *
     * @return Configuration object for this builder type
     */
    protected abstract Object getBoardConfiguration();
    
    /**
     * Calculates the appropriate tile size for this board type.
     * 
     * Different board types may have different tile sizing strategies.
     * This method allows each builder to implement its own sizing logic.
     *
     * @param bingo The bingo configuration
     * @param availableSpace The available space for the board
     * @return The calculated tile size in pixels
     */
    protected abstract int calculateTileSize(Bingo bingo, Dimension availableSpace);
    
    /**
     * Provides access to the current plugin instance.
     *
     * @return The plugin instance
     */
    protected final BingoScapePlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Provides access to the image executor service.
     *
     * @return The executor service for image loading
     */
    protected final ExecutorService getImageExecutor() {
        return imageExecutor;
    }
    
    /**
     * Provides access to the shared image cache.
     *
     * @return The image cache map
     */
    protected final Map<String, ImageIcon> getImageCache() {
        return imageCache;
    }
    
    /**
     * Gets the current bingo being built.
     *
     * @return The current bingo data model
     */
    protected final Bingo getCurrentBingo() {
        return currentBingo;
    }
    
    /**
     * Provides access to the shared tile component factory.
     *
     * @return The tile component factory
     */
    protected final TileComponentFactory getTileFactory() {
        return tileFactory;
    }
}