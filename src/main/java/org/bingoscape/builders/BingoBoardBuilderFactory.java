package org.bingoscape.builders;

import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Bingo;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Factory class for creating appropriate bingo board builders based on board type.
 * 
 * This factory implements the Factory Method pattern to provide type-safe creation
 * of board builders while maintaining extensibility for future board types.
 * 
 * The factory encapsulates the creation logic and ensures that the correct builder
 * is instantiated with proper dependencies and configuration.
 * 
 * @author BingoScape Development Team
 */
public class BingoBoardBuilderFactory {
    
    private final BingoScapePlugin plugin;
    private final ExecutorService imageExecutor;
    private final Map<String, ImageIcon> imageCache;
    
    /**
     * Creates a new builder factory with required dependencies.
     *
     * @param plugin The main plugin instance for callbacks and configuration
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache for performance
     */
    public BingoBoardBuilderFactory(BingoScapePlugin plugin, ExecutorService imageExecutor, 
                                  Map<String, ImageIcon> imageCache) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (imageExecutor == null) {
            throw new IllegalArgumentException("Image executor cannot be null");
        }
        if (imageCache == null) {
            throw new IllegalArgumentException("Image cache cannot be null");
        }
        
        this.plugin = plugin;
        this.imageExecutor = imageExecutor;
        this.imageCache = imageCache;
    }
    
    /**
     * Creates the appropriate board builder for the given bingo configuration.
     * 
     * This is the main factory method that analyzes the bingo configuration
     * and returns the appropriate builder instance with default configuration.
     *
     * @param bingo The bingo configuration to create a builder for
     * @return A builder instance capable of handling the bingo configuration
     * @throws IllegalArgumentException if bingo is null
     * @throws UnsupportedOperationException if the board type is not supported
     */
    public BingoBoardBuilder createBuilder(Bingo bingo) {
        if (bingo == null) {
            throw new IllegalArgumentException("Bingo configuration cannot be null");
        }
        
        BoardType boardType = BoardType.fromBingo(bingo);
        return createBuilderForType(boardType);
    }
    
    /**
     * Creates a board builder for the specified board type.
     * 
     * This method allows direct creation of builders by type, useful for
     * testing or when the board type is already known.
     *
     * @param boardType The type of board builder to create
     * @return A builder instance for the specified board type
     * @throws IllegalArgumentException if boardType is null
     * @throws UnsupportedOperationException if the board type is not supported
     */
    public BingoBoardBuilder createBuilderForType(BoardType boardType) {
        if (boardType == null) {
            throw new IllegalArgumentException("Board type cannot be null");
        }
        
        switch (boardType) {
            case STANDARD:
                return createStandardBuilder();
                
            case PROGRESSIVE:
                return createProgressiveBuilder();
                
            default:
                throw new UnsupportedOperationException("Unsupported board type: " + boardType);
        }
    }
    
    /**
     * Creates a standard board builder with custom configuration.
     * 
     * This method allows creation of standard builders with specific
     * configuration parameters for advanced use cases.
     *
     * @param configuration Custom configuration for the standard builder
     * @return A configured standard board builder
     */
    public StandardBingoBoardBuilder createStandardBuilder(
            StandardBingoBoardBuilder.StandardBoardConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        return new StandardBingoBoardBuilder(plugin, imageExecutor, imageCache, configuration);
    }
    
    /**
     * Creates a progressive board builder with custom configuration.
     * 
     * This method allows creation of progressive builders with specific
     * configuration parameters for advanced use cases.
     *
     * @param configuration Custom configuration for the progressive builder
     * @return A configured progressive board builder
     */
    public ProgressiveBingoBoardBuilder createProgressiveBuilder(
            ProgressiveBingoBoardBuilder.ProgressiveBoardConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        return new ProgressiveBingoBoardBuilder(plugin, imageExecutor, imageCache, configuration);
    }
    
    /**
     * Determines if a builder can be created for the given bingo configuration.
     * 
     * This method provides a way to check if the factory can handle a specific
     * bingo configuration without actually creating the builder.
     *
     * @param bingo The bingo configuration to check
     * @return true if a builder can be created for this configuration
     */
    public boolean canCreateBuilderFor(Bingo bingo) {
        if (bingo == null) {
            return false;
        }
        
        try {
            BoardType boardType = BoardType.fromBingo(bingo);
            return isBoardTypeSupported(boardType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Determines if the specified board type is supported by this factory.
     *
     * @param boardType The board type to check
     * @return true if the board type is supported
     */
    public boolean isBoardTypeSupported(BoardType boardType) {
        if (boardType == null) {
            return false;
        }
        
        switch (boardType) {
            case STANDARD:
            case PROGRESSIVE:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets all board types supported by this factory.
     *
     * @return Array of supported board types
     */
    public BoardType[] getSupportedBoardTypes() {
        return new BoardType[] { BoardType.STANDARD, BoardType.PROGRESSIVE };
    }
    
    /**
     * Creates a builder with automatic type detection and validation.
     * 
     * This method provides additional validation and error handling
     * compared to the basic createBuilder method.
     *
     * @param bingo The bingo configuration
     * @return A validated builder instance
     * @throws BuilderCreationException if builder creation fails
     */
    public BingoBoardBuilder createBuilderWithValidation(Bingo bingo) throws BuilderCreationException {
        try {
            // Validate input
            if (bingo == null) {
                throw new BuilderCreationException("Bingo configuration cannot be null");
            }
            
            // Check if we can create a builder
            if (!canCreateBuilderFor(bingo)) {
                throw new BuilderCreationException("Cannot create builder for bingo configuration: " + 
                    (bingo.getBingoType() != null ? bingo.getBingoType() : "unknown type"));
            }
            
            // Create the builder
            BingoBoardBuilder builder = createBuilder(bingo);
            
            // Additional validation
            if (!builder.canHandle(bingo)) {
                throw new BuilderCreationException("Created builder cannot handle the provided bingo configuration");
            }
            
            return builder;
            
        } catch (Exception e) {
            if (e instanceof BuilderCreationException) {
                throw e;
            }
            throw new BuilderCreationException("Failed to create builder: " + e.getMessage(), e);
        }
    }
    
    // Private helper methods
    
    private StandardBingoBoardBuilder createStandardBuilder() {
        return new StandardBingoBoardBuilder(plugin, imageExecutor, imageCache);
    }
    
    private ProgressiveBingoBoardBuilder createProgressiveBuilder() {
        return new ProgressiveBingoBoardBuilder(plugin, imageExecutor, imageCache);
    }
    
    /**
     * Exception thrown when builder creation fails.
     */
    public static class BuilderCreationException extends Exception {
        public BuilderCreationException(String message) {
            super(message);
        }
        
        public BuilderCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}