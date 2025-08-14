package org.bingoscape.builders;

import org.bingoscape.models.Bingo;

import javax.swing.*;
import java.awt.*;

/**
 * Manager class for creating and configuring board layout strategies.
 * 
 * This class encapsulates the layout creation logic for different board types,
 * providing a centralized place to manage layout configurations and allowing
 * for easy extension with new layout strategies in the future.
 * 
 * @author BingoScape Development Team
 */
public class BoardLayoutManager {
    
    // Layout configuration constants
    private static final int DEFAULT_GRID_SIZE = 5;
    private static final int SPACING = 4;
    
    /**
     * Layout strategy interface for different board types.
     */
    public interface LayoutStrategy {
        /**
         * Applies the layout to the given panel.
         *
         * @param panel The panel to configure
         * @param bingo The bingo configuration for sizing information
         */
        void applyLayout(JPanel panel, Bingo bingo);
        
        /**
         * Gets a description of this layout strategy.
         *
         * @return A human-readable description
         */
        String getDescription();
    }
    
    /**
     * Standard grid layout strategy for traditional bingo boards.
     */
    public static class StandardGridLayoutStrategy implements LayoutStrategy {
        @Override
        public void applyLayout(JPanel panel, Bingo bingo) {
            int rows = bingo.getRows() <= 0 ? DEFAULT_GRID_SIZE : bingo.getRows();
            int cols = bingo.getColumns() <= 0 ? DEFAULT_GRID_SIZE : bingo.getColumns();
            panel.setLayout(new GridLayout(rows, cols, SPACING, SPACING));
        }
        
        @Override
        public String getDescription() {
            return "Standard grid layout for traditional bingo boards";
        }
    }
    
    /**
     * Progressive vertical layout strategy for tier-based bingo boards.
     */
    public static class ProgressiveVerticalLayoutStrategy implements LayoutStrategy {
        @Override
        public void applyLayout(JPanel panel, Bingo bingo) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        }
        
        @Override
        public String getDescription() {
            return "Progressive vertical layout for tier-based bingo boards";
        }
    }
    
    /**
     * Creates the appropriate layout strategy for the given board type.
     *
     * @param boardType The type of board to create a layout for
     * @return The appropriate layout strategy
     * @throws IllegalArgumentException if the board type is not supported
     */
    public static LayoutStrategy createLayoutStrategy(BoardType boardType) {
        if (boardType == null) {
            throw new IllegalArgumentException("Board type cannot be null");
        }
        
        switch (boardType) {
            case STANDARD:
                return new StandardGridLayoutStrategy();
                
            case PROGRESSIVE:
                return new ProgressiveVerticalLayoutStrategy();
                
            default:
                throw new IllegalArgumentException("Unsupported board type: " + boardType);
        }
    }
    
    /**
     * Creates a layout strategy based on a bingo configuration.
     *
     * @param bingo The bingo configuration to analyze
     * @return The appropriate layout strategy
     */
    public static LayoutStrategy createLayoutStrategy(Bingo bingo) {
        BoardType boardType = BoardType.fromBingo(bingo);
        return createLayoutStrategy(boardType);
    }
    
    /**
     * Directly applies the appropriate layout to a panel based on board type.
     *
     * @param panel The panel to configure
     * @param boardType The type of board
     * @param bingo The bingo configuration
     */
    public static void applyLayout(JPanel panel, BoardType boardType, Bingo bingo) {
        LayoutStrategy strategy = createLayoutStrategy(boardType);
        strategy.applyLayout(panel, bingo);
    }
    
    /**
     * Directly applies the appropriate layout to a panel based on bingo configuration.
     *
     * @param panel The panel to configure
     * @param bingo The bingo configuration
     */
    public static void applyLayout(JPanel panel, Bingo bingo) {
        BoardType boardType = BoardType.fromBingo(bingo);
        applyLayout(panel, boardType, bingo);
    }
    
    /**
     * Determines if a board type supports custom layout configurations.
     *
     * @param boardType The board type to check
     * @return true if the board type supports layout customization
     */
    public static boolean supportsLayoutCustomization(BoardType boardType) {
        switch (boardType) {
            case STANDARD:
                return true; // Supports custom row/column configurations
            case PROGRESSIVE:
                return false; // Uses fixed vertical layout
            default:
                return false;
        }
    }
    
    /**
     * Gets layout information for a board type.
     *
     * @param boardType The board type to get information for
     * @return Layout information string
     */
    public static String getLayoutInfo(BoardType boardType) {
        LayoutStrategy strategy = createLayoutStrategy(boardType);
        return strategy.getDescription();
    }
    
    /**
     * Validates that a bingo configuration is compatible with its intended layout.
     *
     * @param bingo The bingo configuration to validate
     * @return true if the configuration is valid for the intended layout
     */
    public static boolean validateLayoutCompatibility(Bingo bingo) {
        if (bingo == null) {
            return false;
        }
        
        BoardType boardType = BoardType.fromBingo(bingo);
        
        switch (boardType) {
            case STANDARD:
                // Standard boards should have reasonable row/column values
                return bingo.getRows() >= 0 && bingo.getColumns() >= 0 &&
                       bingo.getRows() <= 20 && bingo.getColumns() <= 20;
                       
            case PROGRESSIVE:
                // Progressive boards should have progression metadata if they're truly progressive
                return true; // More lenient validation for now
                
            default:
                return false;
        }
    }
}