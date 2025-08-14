package org.bingoscape.builders;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.models.*;

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
    
    // Color Constants
    private static final Color GOLD_COLOR = new Color(255, 215, 0);
    
    // Status Background Colors
    private static final Color PENDING_BG_COLOR = new Color(30, 64, 122);
    private static final Color ACCEPTED_BG_COLOR = new Color(17, 99, 47);
    private static final Color REQUIRES_ACTION_BG_COLOR = new Color(117, 89, 4);
    private static final Color DECLINED_BG_COLOR = new Color(120, 34, 34);
    
    // Status Border Colors
    private static final Color PENDING_BORDER_COLOR = new Color(59, 130, 246);
    private static final Color ACCEPTED_BORDER_COLOR = new Color(34, 197, 94);
    private static final Color REQUIRES_ACTION_BORDER_COLOR = new Color(234, 179, 8);
    private static final Color DECLINED_BORDER_COLOR = new Color(239, 68, 68);
    
    // Status Text Colors
    private static final Color PENDING_TEXT_COLOR = new Color(59, 130, 246);
    private static final Color ACCEPTED_TEXT_COLOR = new Color(34, 197, 94);
    private static final Color REQUIRES_ACTION_TEXT_COLOR = new Color(234, 179, 8);
    private static final Color DECLINED_TEXT_COLOR = new Color(239, 68, 68);
    
    private final ExecutorService imageExecutor;
    private final Map<String, ImageIcon> imageCache;
    
    /**
     * Creates a new tile component factory with shared resources.
     *
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache for performance
     */
    public TileComponentFactory(ExecutorService imageExecutor, Map<String, ImageIcon> imageCache) {
        this.imageExecutor = imageExecutor;
        this.imageCache = imageCache;
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
        
        // Set border based on submission status
        panel.setBorder(new CompoundBorder(
                new LineBorder(getTileBorderColor(submission), 2),
                new EmptyBorder(SMALL_SPACING, SMALL_SPACING, SMALL_SPACING, SMALL_SPACING)
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
        xpLabel.setForeground(GOLD_COLOR);
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
        xpLabel.setForeground(GOLD_COLOR);
        xpLabel.setFont(new Font(xpLabel.getFont().getName(), Font.BOLD, SMALL_FONT_SIZE));
        xpLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(xpLabel, BorderLayout.EAST);
        
        // Add tier indicator if enabled and applicable
        if (showTierIndicator && currentBingo != null && 
            "progression".equals(currentBingo.getBingoType()) && tile.getTier() != null) {
            JLabel tierLabel = new JLabel("T" + tile.getTier());
            tierLabel.setForeground(new Color(200, 200, 255));
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
            
            switch (tile.getSubmission().getStatus()) {
                case PENDING:
                    statusLabel.setText("PENDING");
                    statusLabel.setForeground(PENDING_TEXT_COLOR);
                    break;
                case ACCEPTED:
                    statusLabel.setText("COMPLETED");
                    statusLabel.setForeground(ACCEPTED_TEXT_COLOR);
                    break;
                case REQUIRES_INTERACTION:
                    statusLabel.setText("NEEDS ACTION");
                    statusLabel.setForeground(REQUIRES_ACTION_TEXT_COLOR);
                    break;
                case DECLINED:
                    statusLabel.setText("DECLINED");
                    statusLabel.setForeground(DECLINED_TEXT_COLOR);
                    break;
                default:
                    return; // No overlay for NOT_SUBMITTED
            }
            
            overlayPanel.add(statusLabel, BorderLayout.SOUTH);
            panel.add(overlayPanel, BorderLayout.SOUTH);
        }
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
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (clickCallback != null) {
                    clickCallback.onTileClicked(tile, evt);
                }
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBackground(originalColor.brighter());
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
     *
     * @param tile The tile data
     * @param currentBingo The current bingo for context
     * @return HTML tooltip string
     */
    public String createDetailedTooltip(Tile tile, Bingo currentBingo) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><head><style>")
                .append(".tooltip-container { width: 280px; padding: 12px; background: #1a1d29; border-radius: 8px; font-family: 'Segoe UI', sans-serif; }")
                .append(".header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }")
                .append(".title { font-size: 14px; font-weight: 600; color: #ffffff; margin-right: 8px; line-height: 1.3; }")
                .append(".xp-badge { background: #f59e0b; color: #ffffff; padding: 4px 8px; border-radius: 16px; font-size: 11px; font-weight: 600; white-space: nowrap; }")
                .append(".status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; margin-bottom: 8px; }")
                .append(".status-approved { background: #065f46; color: #10b981; }")
                .append(".status-pending { background: #1e3a8a; color: #3b82f6; }")
                .append(".status-needs-review { background: #92400e; color: #f59e0b; }")
                .append(".status-declined { background: #7f1d1d; color: #ef4444; }")
                .append(".description { color: #9ca3af; font-size: 12px; line-height: 1.4; margin-bottom: 12px; }")
                .append(".goals-section { margin-top: 12px; }")
                .append(".goals-title { color: #ffffff; font-size: 11px; font-weight: 600; margin-bottom: 6px; }")
                .append(".goal-item { background: #374151; padding: 8px; border-radius: 6px; margin-bottom: 6px; }")
                .append(".goal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }")
                .append(".goal-desc { color: #d1d5db; font-size: 11px; line-height: 1.3; }")
                .append(".goal-target { color: #ffffff; font-size: 11px; font-weight: 500; }")
                .append(".progress-bar { width: 100%; height: 4px; background: #4b5563; border-radius: 2px; overflow: hidden; }")
                .append(".progress-fill { height: 100%; background: #10b981; transition: width 0.3s ease; }")
                .append(".progress-text { color: #9ca3af; font-size: 10px; margin-top: 2px; }")
                .append("</style></head><body>");
        
        tooltip.append("<div class='tooltip-container'>");
        
        // Header with title and XP badge
        tooltip.append("<div class='header'>")
                .append("<div class='title'>")
                .append(tile.getTitle())
                .append("</div>")
                .append("<div class='xp-badge'>⚡ ")
                .append(tile.getWeight())
                .append(" XP</div>")
                .append("</div>");
        
        // Status badge if available
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null && 
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            String statusClass = getStatusCssClass(tile.getSubmission().getStatus());
            String statusIcon = getStatusIcon(tile.getSubmission().getStatus());
            String statusText = getStatusText(tile.getSubmission().getStatus());
            
            tooltip.append("<div class='status-badge ")
                    .append(statusClass)
                    .append("'>")
                    .append(statusIcon)
                    .append(" ")
                    .append(statusText)
                    .append("</div>");
        }
        
        // Description
        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            String description = tile.getDescription().length() > 150 ? 
                tile.getDescription().substring(0, 150) + "..." : tile.getDescription();
            tooltip.append("<div class='description'>")
                    .append(description)
                    .append("</div>");
        }
        
        // Goals section with progress bars
        if (tile.getGoals() != null && !tile.getGoals().isEmpty()) {
            tooltip.append("<div class='goals-section'>")
                    .append("<div class='goals-title'>Goals:</div>");
            
            for (Goal goal : tile.getGoals()) {
                tooltip.append("<div class='goal-item'>")
                        .append("<div class='goal-header'>")
                        .append("<div class='goal-desc'>")
                        .append(goal.getDescription())
                        .append("</div>")
                        .append("<div class='goal-target'>Target: ")
                        .append(goal.getTargetValue())
                        .append("</div>")
                        .append("</div>");
                
                // Use actual progress data if available
                if (goal.getProgress() != null) {
                    GoalProgress progress = goal.getProgress();
                    
                    // Approved progress bar (green)
                    tooltip.append("<div style='display: flex; align-items: center; gap: 6px; margin-bottom: 4px;'>")
                            .append("<div style='color: #10b981; font-size: 10px; min-width: 60px;'>\u2713 Progress</div>")
                            .append("<div class='progress-bar'>")
                            .append("<div style='height: 100%; background: #10b981; width: ")
                            .append(progress.getApprovedPercentage())
                            .append("%'></div>")
                            .append("</div>")
                            .append("<div style='color: #9ca3af; font-size: 10px; min-width: 40px;'>")
                            .append(progress.getApprovedProgress())
                            .append("/")
                            .append(goal.getTargetValue())
                            .append("</div>")
                            .append("</div>");
                    
                    // If there's pending progress, show total progress bar (yellow)
                    if (progress.getTotalProgress() > progress.getApprovedProgress()) {
                        double totalPercentage = goal.getTargetValue() > 0 ? 
                            Math.min(100.0, (double) progress.getTotalProgress() / goal.getTargetValue() * 100) : 0;
                            
                        tooltip.append("<div style='display: flex; align-items: center; gap: 6px; margin-bottom: 4px;'>")
                                .append("<div style='color: #f59e0b; font-size: 10px; min-width: 60px;'>\u23f3 Pending</div>")
                                .append("<div class='progress-bar'>")
                                .append("<div style='height: 100%; background: #f59e0b; width: ")
                                .append(totalPercentage)
                                .append("%'></div>")
                                .append("</div>")
                                .append("<div style='color: #9ca3af; font-size: 10px; min-width: 40px;'>")
                                .append(progress.getTotalProgress())
                                .append("/")
                                .append(goal.getTargetValue())
                                .append("</div>")
                                .append("</div>");
                    }
                    
                    // Show completion status if completed
                    if (progress.isCompleted()) {
                        tooltip.append("<div style='color: #10b981; font-size: 10px; font-weight: 600; margin-top: 4px;'>")
                                .append("\u2713 Completed!")
                                .append("</div>");
                    }
                    
                } else {
                    // Fallback for goals without progress data
                    tooltip.append("<div class='progress-bar'>")
                            .append("<div class='progress-fill' style='width: 0%'></div>")
                            .append("</div>")
                            .append("<div class='progress-text'>0/")
                            .append(goal.getTargetValue())
                            .append("</div>");
                }
                
                tooltip.append("</div>");
            }
            
            tooltip.append("</div>");
        }
        
        tooltip.append("</div></body></html>");
        return tooltip.toString();
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
        
        switch (submission.getStatus()) {
            case PENDING: return PENDING_BG_COLOR;
            case ACCEPTED: return ACCEPTED_BG_COLOR;
            case REQUIRES_INTERACTION: return REQUIRES_ACTION_BG_COLOR;
            case DECLINED: return DECLINED_BG_COLOR;
            default: return ColorScheme.DARK_GRAY_COLOR;
        }
    }
    
    private Color getTileBorderColor(TileSubmission submission) {
        if (submission == null || submission.getStatus() == null)
            return ColorScheme.BORDER_COLOR;
        
        switch (submission.getStatus()) {
            case PENDING: return PENDING_BORDER_COLOR;
            case ACCEPTED: return ACCEPTED_BORDER_COLOR;
            case REQUIRES_INTERACTION: return REQUIRES_ACTION_BORDER_COLOR;
            case DECLINED: return DECLINED_BORDER_COLOR;
            default: return ColorScheme.BORDER_COLOR;
        }
    }
    
    private String getStatusText(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "Pending Review";
            case ACCEPTED: return "Completed";
            case REQUIRES_INTERACTION: return "Needs Action";
            case DECLINED: return "Declined";
            default: return "Not Submitted";
        }
    }
    
    private String getStatusHexColor(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "#3b82f6";
            case ACCEPTED: return "#22c55e";
            case REQUIRES_INTERACTION: return "#eab308";
            case DECLINED: return "#ef4444";
            default: return "#ffffff";
        }
    }
    
    private String getStatusCssClass(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "status-pending";
            case ACCEPTED: return "status-approved";
            case REQUIRES_INTERACTION: return "status-needs-review";
            case DECLINED: return "status-declined";
            default: return "";
        }
    }
    
    private String getStatusIcon(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "⏳";
            case ACCEPTED: return "✓";
            case REQUIRES_INTERACTION: return "!";
            case DECLINED: return "✗";
            default: return "";
        }
    }
}