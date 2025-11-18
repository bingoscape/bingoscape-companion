package org.bingoscape.builders;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.models.*;
import org.bingoscape.ui.ColorPalette;
import org.bingoscape.ui.StatusConstants;
import org.bingoscape.ui.TileTooltipBuilder;
import org.bingoscape.ui.TileHoverCardManager;
import org.bingoscape.ui.UIStyleFactory;
import org.bingoscape.ui.components.TileProgressBar;
import org.bingoscape.utils.GoalTreeProgressCalculator;
import org.bingoscape.constants.BingoTypeConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.imageio.ImageIO;

/**
 * Factory class for creating common tile components and UI elements.
 * 
 * This factory encapsulates the shared logic for creating tile panels, tooltips,
 * status overlays, and other UI components that are common across different
 * board builders. This reduces code duplication and ensures consistent styling.
 * 
 * @author BingoScape Development Team
 */
public class TileComponentFactory {
    
    // Common UI Constants
    private static final int SMALL_FONT_SIZE = 10;
    private static final int MEDIUM_FONT_SIZE = 12;
    private static final int LARGE_FONT_SIZE = 24;
    private static final int SMALL_SPACING = 4;
    private static final int IMAGE_MARGIN = 10;
    private static final int IMAGE_TITLE_OFFSET = 20;

    private final ExecutorService imageExecutor;
    private final Map<String, ImageIcon> imageCache;
    private final TileTooltipBuilder tooltipBuilder;

    /**
     * Creates a new tile component factory with shared resources.
     *
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache for performance
     */
    public TileComponentFactory(ExecutorService imageExecutor, Map<String, ImageIcon> imageCache) {
        this.imageExecutor = imageExecutor;
        this.imageCache = imageCache;
        this.tooltipBuilder = new TileTooltipBuilder();
    }
    
    /**
     * Applies standard tile appearance (background and border) based on submission status.
     *
     * @param panel The tile panel to style
     * @param submission The tile's submission status
     */
    public void applyTileAppearance(JPanel panel, TileSubmission submission) {
        // Set background based on submission status
        Color backgroundColor = getTileBackgroundColor(submission);
        panel.setBackground(backgroundColor);

        // Set border based on submission status using UIStyleFactory
        panel.setBorder(UIStyleFactory.createStyledBorder(
            getTileBorderColor(submission), 2, SMALL_SPACING
        ));
    }
    
    /**
     * Creates a hidden tile placeholder panel.
     *
     * @param tileSize The size for the panel
     * @return A panel representing a hidden tile
     */
    public JPanel createHiddenTilePanel(int tileSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));
        panel.setMaximumSize(new Dimension(tileSize, tileSize));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(UIStyleFactory.createStyledBorder(
            ColorScheme.BORDER_COLOR, 1, SMALL_SPACING
        ));

        JLabel hiddenLabel = new JLabel("?", SwingConstants.CENTER);
        hiddenLabel.setForeground(Color.GRAY);
        hiddenLabel.setFont(new Font(hiddenLabel.getFont().getName(), Font.BOLD, LARGE_FONT_SIZE));
        panel.add(hiddenLabel, BorderLayout.CENTER);

        panel.setToolTipText("Hidden tile");
        return panel;
    }
    
    /**
     * Adds a title label to a tile panel.
     *
     * @param panel The tile panel
     * @param tile The tile data
     */
    public void addTileTitle(JPanel panel, Tile tile) {
        JLabel titleLabel = new JLabel("<html><center>" + tile.getTitle() + "</center></html>", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.CENTER);
    }
    
    /**
     * Adds an XP indicator to a tile panel.
     *
     * @param panel The tile panel
     * @param tile The tile data
     */
    public void addXpIndicator(JPanel panel, Tile tile) {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        JLabel xpLabel = new JLabel(String.valueOf(tile.getWeight()) + " XP");
        xpLabel.setForeground(ColorPalette.GOLD);
        xpLabel.setFont(new Font(xpLabel.getFont().getName(), Font.BOLD, SMALL_FONT_SIZE));
        xpLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bottomPanel.add(xpLabel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Adds XP and optional tier indicators to a tile panel.
     *
     * @param panel The tile panel
     * @param tile The tile data
     * @param currentBingo The current bingo for tier checking
     * @param showTierIndicator Whether to show tier indicators
     */
    public void addTileIndicators(JPanel panel, Tile tile, Bingo currentBingo, boolean showTierIndicator) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        // XP label on the right
        JLabel xpLabel = new JLabel(String.valueOf(tile.getWeight()) + " XP");
        xpLabel.setForeground(ColorPalette.GOLD);
        xpLabel.setFont(new Font(xpLabel.getFont().getName(), Font.BOLD, SMALL_FONT_SIZE));
        xpLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(xpLabel, BorderLayout.EAST);
        
        // Add tier indicator if enabled and applicable
        if (showTierIndicator && currentBingo != null &&
            BingoTypeConstants.isProgressive(currentBingo.getBingoType()) && tile.getTier() != null) {
            JLabel tierLabel = new JLabel("T" + tile.getTier());
            tierLabel.setForeground(ColorPalette.TIER_LABEL_BLUE);
            tierLabel.setFont(new Font(tierLabel.getFont().getName(), Font.BOLD, SMALL_FONT_SIZE));
            tierLabel.setHorizontalAlignment(SwingConstants.LEFT);
            topPanel.add(tierLabel, BorderLayout.WEST);
        }
        
        panel.add(topPanel, BorderLayout.NORTH);
    }
    
    /**
     * Adds a status overlay to a tile panel if the tile has a submission status.
     *
     * @param panel The tile panel
     * @param tile The tile data
     */
    public void addStatusOverlay(JPanel panel, Tile tile) {
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
                tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {

            JPanel overlayPanel = new JPanel(new BorderLayout());
            overlayPanel.setOpaque(false);

            JLabel statusLabel = new JLabel();
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.BOLD, MEDIUM_FONT_SIZE));

            statusLabel.setText(StatusConstants.getStatusText(tile.getSubmission().getStatus()).toUpperCase());
            statusLabel.setForeground(StatusConstants.getStatusColor(tile.getSubmission().getStatus()));

            overlayPanel.add(statusLabel, BorderLayout.SOUTH);
            panel.add(overlayPanel, BorderLayout.SOUTH);
        }
    }

    /**
     * Adds both status overlay and progress indicator to the bottom of a tile panel.
     * This method combines both components to avoid BorderLayout conflicts.
     *
     * @param panel The tile panel
     * @param tile The tile data
     */
    public void addBottomOverlays(JPanel panel, Tile tile) {
        // Create a container for bottom components using BoxLayout for vertical stacking
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        // Add status overlay if needed
        boolean hasStatus = tile.getSubmission() != null &&
                           tile.getSubmission().getStatus() != null &&
                           tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED;

        if (hasStatus) {
            JLabel statusLabel = new JLabel();
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.BOLD, MEDIUM_FONT_SIZE));
            statusLabel.setText(StatusConstants.getStatusText(tile.getSubmission().getStatus()).toUpperCase());
            statusLabel.setForeground(StatusConstants.getStatusColor(tile.getSubmission().getStatus()));
            bottomPanel.add(statusLabel);
        }

        // Add progress indicator if tile has goals
        if (tile.getGoalTree() != null && !tile.getGoalTree().isEmpty()) {
            GoalTreeProgressCalculator.ProgressResult progress =
                GoalTreeProgressCalculator.getProgressFromTile(tile.getGoalTree());

            if (progress != null) {
                TileProgressBar progressBar = TileProgressBar.createProgressBar(progress);
                if (progressBar != null) {
                    progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
                    bottomPanel.add(progressBar);
                }
            }
        }

        // Only add the bottom panel if it has content
        if (bottomPanel.getComponentCount() > 0) {
            panel.add(bottomPanel, BorderLayout.SOUTH);
        }
    }

    /**
     * Adds a progress indicator to a tile panel showing goal completion.
     * Only displays if the tile has goals defined in its goal tree.
     *
     * The progress bar is positioned at the bottom of the tile and shows:
     * - Overall completion percentage
     * - Visual progress bar with color coding (green=complete, yellow=partial, gray=none)
     * - Only evaluates root-level goals (not nested children)
     *
     * @param panel The tile panel
     * @param tile The tile data with goal tree
     * @deprecated Use addBottomOverlays() instead to avoid layout conflicts
     */
    @Deprecated
    public void addProgressIndicator(JPanel panel, Tile tile) {
        if (tile.getGoalTree() == null || tile.getGoalTree().isEmpty()) {
            return; // No goals to show progress for
        }

        // Calculate progress from root-level goal tree
        GoalTreeProgressCalculator.ProgressResult progress =
            GoalTreeProgressCalculator.getProgressFromTile(tile.getGoalTree());

        if (progress == null) {
            return; // Unable to calculate progress
        }

        // Create progress bar component
        TileProgressBar progressBar = TileProgressBar.createProgressBar(progress);
        if (progressBar == null) {
            return;
        }

        // Create a container panel for the progress bar
        // This allows proper positioning at the bottom without interfering with other components
        JPanel progressContainer = new JPanel(new BorderLayout());
        progressContainer.setOpaque(false);
        progressContainer.add(progressBar, BorderLayout.SOUTH);

        // Add to the main panel
        // Note: This will be layered with other SOUTH components via the existing panel structure
        panel.add(progressContainer, BorderLayout.PAGE_END);
    }
    
    /**
     * Adds hover and click behavior to a tile panel.
     *
     * @param panel The tile panel
     * @param tile The tile data
     * @param clickCallback Optional click callback (can be null)
     */
    public void addTileInteractionListeners(JPanel panel, Tile tile, TileClickCallback clickCallback) {
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            private final Color originalColor = panel.getBackground();
            private final Color hoverColor = UIStyleFactory.brighten(originalColor, 10);

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (clickCallback != null) {
                    clickCallback.onTileClicked(tile, evt);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                panel.setBackground(originalColor);
            }
        });
    }
    
    /**
     * Loads a tile image asynchronously and displays it in the panel.
     *
     * @param panel The tile panel
     * @param tile The tile data
     * @param tileSize The target size for the image
     */
    public void loadTileImage(JPanel panel, Tile tile, int tileSize) {
        String imageUrl = tile.getHeaderImage();
        
        // Create placeholder
        JLabel imageLabel = new JLabel("Loading...");
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        panel.add(imageLabel, BorderLayout.CENTER);
        
        // Load asynchronously
        imageExecutor.submit(() -> {
            try {
                // Check cache first
                if (imageCache.containsKey(imageUrl)) {
                    SwingUtilities.invokeLater(() -> {
                        updateImageLabel(imageLabel, imageCache.get(imageUrl));
                        panel.revalidate();
                    });
                    return;
                }
                
                // Load from network
                URL url = new URL(imageUrl);
                BufferedImage originalImage = ImageIO.read(url);
                
                if (originalImage != null) {
                    // Scale image
                    Image scaledImage = scaleImageWithAspectRatio(originalImage, 
                        tileSize - IMAGE_MARGIN, tileSize - IMAGE_TITLE_OFFSET);
                    ImageIcon icon = new ImageIcon(scaledImage);
                    
                    // Cache and display
                    imageCache.put(imageUrl, icon);
                    
                    SwingUtilities.invokeLater(() -> {
                        updateImageLabel(imageLabel, icon);
                        panel.revalidate();
                    });
                } else {
                    handleImageLoadError(imageLabel, tile);
                }
            } catch (IOException e) {
                handleImageLoadError(imageLabel, tile);
            }
        });
    }
    
    /**
     * Creates a detailed HTML tooltip for a tile matching the web design.
     * Delegates to TileTooltipBuilder for cleaner separation of concerns.
     *
     * @param tile The tile data
     * @param currentBingo The current bingo for context
     * @return HTML tooltip string
     * @deprecated Use attachHoverCard() instead for richer UI experience
     */
    @Deprecated
    public String createDetailedTooltip(Tile tile, Bingo currentBingo) {
        return tooltipBuilder.buildTooltip(tile, currentBingo);
    }

    /**
     * Attaches a rich hover card to a tile panel.
     * The hover card displays tile information in a custom popup matching the web app design.
     *
     * @param panel The tile panel to attach the hover card to
     * @param tile The tile data
     * @param bingo The current bingo for context
     * @param itemManager The item manager for loading item images
     */
    public void attachHoverCard(JPanel panel, Tile tile, Bingo bingo, net.runelite.client.game.ItemManager itemManager) {
        TileHoverCardManager.getInstance().attachHoverCard(panel, tile, bingo, itemManager);
    }
    
    // Private helper methods
    
    private void updateImageLabel(JLabel imageLabel, ImageIcon icon) {
        imageLabel.setText("");
        imageLabel.setIcon(icon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
    }
    
    private void handleImageLoadError(JLabel imageLabel, Tile tile) {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setText("<html><center>" + tile.getTitle() + "</center></html>");
            imageLabel.setIcon(null);
            imageLabel.setForeground(Color.WHITE);
        });
    }
    
    private Image scaleImageWithAspectRatio(BufferedImage img, int targetWidth, int targetHeight) {
        if (img == null) return null;
        
        double imgRatio = (double) img.getWidth() / img.getHeight();
        double targetRatio = (double) targetWidth / targetHeight;
        
        int scaledWidth, scaledHeight;
        if (imgRatio > targetRatio) {
            scaledWidth = targetWidth;
            scaledHeight = (int) (scaledWidth / imgRatio);
        } else {
            scaledHeight = targetHeight;
            scaledWidth = (int) (scaledHeight * imgRatio);
        }
        
        return img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }
    
    private Color getTileBackgroundColor(TileSubmission submission) {
        if (submission == null || submission.getStatus() == null ||
                submission.getStatus() == TileSubmissionType.NOT_SUBMITTED) {
            return ColorScheme.DARK_GRAY_COLOR;
        }

        Color bgColor = StatusConstants.getStatusBackgroundColor(submission.getStatus());
        return bgColor != null ? bgColor : ColorScheme.DARK_GRAY_COLOR;
    }

    private Color getTileBorderColor(TileSubmission submission) {
        if (submission == null || submission.getStatus() == null)
            return ColorScheme.BORDER_COLOR;

        Color borderColor = StatusConstants.getStatusBorderColor(submission.getStatus());
        return borderColor != null ? borderColor : ColorScheme.BORDER_COLOR;
    }

}