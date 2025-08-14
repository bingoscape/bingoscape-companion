package org.bingoscape.builders;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.BingoScapePlugin;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * Builder for progressive tier-based bingo boards.
 * 
 * Creates vertical tier-based layouts with collapsible sections, unlockable tiers,
 * and specialized progression mechanics. Handles both locked and unlocked tiers
 * with appropriate visual indicators and interactive functionality.
 * 
 * @author BingoScape Development Team
 */
public class ProgressiveBingoBoardBuilder extends BingoBoardBuilder {
    
    // Progressive board specific constants
    private static final int TIER_SPACING = 15;
    private static final int TILES_PER_ROW_PROGRESSIVE = 3;
    private static final int PROGRESSIVE_TILE_SIZE = 150;
    
    // Progressive Layout Constants
    private static final int TIER_SECTION_PADDING = 15;
    private static final int TIER_HEADER_PADDING = 15;
    private static final int TIER_HEADER_HEIGHT = 60; // Fixed header height
    private static final int TIER_ICON_PADDING = 8;
    private static final int TIER_LABEL_LEFT_MARGIN = 10;
    private static final int STATUS_BADGE_PADDING = 8;
    private static final int LOCKED_PANEL_PADDING = 20;
    private static final int TILES_PANEL_SIDE_MARGIN = 20;
    private static final int LOCK_ICON_FONT_SIZE = 32;
    
    // Spacing constants
    private static final int SMALL_SPACING = 4;
    private static final int MEDIUM_SPACING = 5;
    private static final int LARGE_SPACING = 10;
    
    // UI Constants
    private static final int SMALL_FONT_SIZE = 10;
    private static final int MEDIUM_FONT_SIZE = 12;
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
    
    /**
     * Configuration class for progressive board customization.
     */
    public static class ProgressiveBoardConfiguration {
        private final boolean enableTierCollapse;
        private final boolean showLockedTiers;
        private final boolean showProgressInfo;
        
        public ProgressiveBoardConfiguration() {
            this.enableTierCollapse = true;
            this.showLockedTiers = true;
            this.showProgressInfo = true;
        }
        
        public ProgressiveBoardConfiguration(boolean enableTierCollapse, boolean showLockedTiers, boolean showProgressInfo) {
            this.enableTierCollapse = enableTierCollapse;
            this.showLockedTiers = showLockedTiers;
            this.showProgressInfo = showProgressInfo;
        }
        
        public boolean isEnableTierCollapse() { return enableTierCollapse; }
        public boolean isShowLockedTiers() { return showLockedTiers; }
        public boolean isShowProgressInfo() { return showProgressInfo; }
    }
    
    private final ProgressiveBoardConfiguration configuration;
    private final Set<Integer> collapsedTiers = new HashSet<>();
    
    /**
     * Creates a new progressive board builder with default configuration.
     *
     * @param plugin The main plugin instance
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache
     */
    public ProgressiveBingoBoardBuilder(BingoScapePlugin plugin, ExecutorService imageExecutor, 
                                      Map<String, ImageIcon> imageCache) {
        this(plugin, imageExecutor, imageCache, new ProgressiveBoardConfiguration());
    }
    
    /**
     * Creates a new progressive board builder with custom configuration.
     *
     * @param plugin The main plugin instance
     * @param imageExecutor Executor service for async image loading
     * @param imageCache Shared image cache
     * @param configuration Custom configuration for this builder
     */
    public ProgressiveBingoBoardBuilder(BingoScapePlugin plugin, ExecutorService imageExecutor, 
                                      Map<String, ImageIcon> imageCache, 
                                      ProgressiveBoardConfiguration configuration) {
        super(plugin, imageExecutor, imageCache);
        this.configuration = configuration;
    }
    
    @Override
    protected void setupLayout(JPanel panel, Bingo bingo) {
        // Use vertical box layout for progression tiers
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    }
    
    @Override
    protected void populateBoard(JPanel panel, Bingo bingo) {
        // Group tiles by tier (only for tiles that exist)
        Map<Integer, List<Tile>> tilesByTier = bingo.getTiles().stream()
                .filter(tile -> !tile.isHidden())
                .collect(Collectors.groupingBy(
                        tile -> tile.getTier() != null ? tile.getTier() : 1,
                        TreeMap::new,
                        Collectors.toList()
                ));
        
        // Get unlocked tiers and all possible tiers from progression metadata
        Set<Integer> unlockedTiers = extractUnlockedTiers(bingo);
        Set<Integer> allTiers = extractAllTiers(bingo, tilesByTier);
        
        // Create tier sections for ALL tiers (both locked and unlocked)
        for (Integer tierNum : allTiers) {
            List<Tile> tierTiles = tilesByTier.getOrDefault(tierNum, new ArrayList<>());
            boolean isUnlocked = unlockedTiers.contains(tierNum);
            
            // Only show locked tiers if configuration allows
            if (!isUnlocked && !configuration.showLockedTiers) {
                continue;
            }
            
            JPanel tierSection = createTierSection(tierNum, tierTiles, isUnlocked, bingo);
            panel.add(tierSection);
            
            // Add spacing between tiers
            panel.add(Box.createVerticalStrut(TIER_SPACING));
        }
    }
    
    @Override
    public BoardType getSupportedBoardType() {
        return BoardType.PROGRESSIVE;
    }
    
    @Override
    protected int calculateTileSize(Bingo bingo, Dimension availableSpace) {
        // Progressive boards use fixed tile size for consistency
        return PROGRESSIVE_TILE_SIZE;
    }
    
    @Override
    protected JPanel createTilePanel(Tile tile, int tileSize) {
        // Use the specialized progressive tile panel creation
        return createProgressiveTilePanel(tile);
    }
    
    private Set<Integer> extractUnlockedTiers(Bingo bingo) {
        Set<Integer> unlockedTiers = new HashSet<>();
        
        if (bingo.getProgression() != null && bingo.getProgression().getUnlockedTiers() != null) {
            unlockedTiers.addAll(bingo.getProgression().getUnlockedTiers());
        }
        
        return unlockedTiers;
    }
    
    private Set<Integer> extractAllTiers(Bingo bingo, Map<Integer, List<Tile>> tilesByTier) {
        Set<Integer> allTiers = new TreeSet<>();
        
        // Get all tiers from tier XP requirements (this tells us what tiers exist)
        if (bingo.getProgression() != null && bingo.getProgression().getTierXpRequirements() != null) {
            for (TierXpRequirement req : bingo.getProgression().getTierXpRequirements()) {
                allTiers.add(req.getTier());
            }
        }
        
        // If no progression metadata, fall back to tiers we have tiles for
        if (allTiers.isEmpty()) {
            allTiers.addAll(tilesByTier.keySet());
        }
        
        return allTiers;
    }
    
    private JPanel createTierSection(Integer tierNum, List<Tile> tierTiles, boolean isUnlocked, Bingo bingo) {
        JPanel tierSection = new JPanel();
        tierSection.setLayout(new BoxLayout(tierSection, BoxLayout.Y_AXIS));
        tierSection.setOpaque(false);
        tierSection.setBorder(new EmptyBorder(LARGE_SPACING, TIER_SECTION_PADDING, LARGE_SPACING, TIER_SECTION_PADDING));
        
        // Check if tier is collapsed
        boolean isCollapsed = configuration.enableTierCollapse && collapsedTiers.contains(tierNum);
        
        // Create tier header (clickable for unlocked tiers)
        JPanel tierHeader = createTierHeader(tierNum, isUnlocked, isCollapsed, bingo);
        tierSection.add(tierHeader);
        
        // Create content panel that can be shown/hidden
        JPanel contentPanel = createTierContentPanel(tierNum, tierTiles, isUnlocked);
        
        // Only add content panel if tier is not collapsed (or if it's locked)
        if (!isCollapsed || !isUnlocked) {
            tierSection.add(contentPanel);
        }
        
        // Add click handler to header for unlocked tiers only
        if (isUnlocked && configuration.enableTierCollapse) {
            addTierHeaderClickHandler(tierHeader, tierNum, tierSection, contentPanel);
        }
        
        return tierSection;
    }
    
    private JPanel createTierContentPanel(Integer tierNum, List<Tile> tierTiles, boolean isUnlocked) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        contentPanel.add(Box.createVerticalStrut(LARGE_SPACING));
        
        if (isUnlocked) {
            // Create tiles panel for unlocked tier
            JPanel tilesPanel = createTierTilesPanel(tierTiles);
            contentPanel.add(tilesPanel);
        } else {
            // Create locked tier panel (locked tiers are not collapsible)
            JPanel lockedPanel = createLockedTierPanel(tierNum);
            contentPanel.add(lockedPanel);
        }
        
        return contentPanel;
    }
    
    private JPanel createTierHeader(Integer tierNum, boolean isUnlocked, boolean isCollapsed, Bingo bingo) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(new Color(45, 55, 72));
        header.setPreferredSize(new Dimension(0, TIER_HEADER_HEIGHT)); // Fixed height
        header.setMinimumSize(new Dimension(0, TIER_HEADER_HEIGHT));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, TIER_HEADER_HEIGHT));
        header.setBorder(new CompoundBorder(
            new LineBorder(isUnlocked ? new Color(59, 130, 246) : new Color(100, 100, 100), 1, true),
            new EmptyBorder(TIER_HEADER_PADDING, TIER_HEADER_PADDING, TIER_HEADER_PADDING, TIER_HEADER_PADDING)
        ));
        
        // Left side: Tier info (single line layout)
        JPanel tierInfoPanel = createTierInfoPanel(tierNum, bingo);
        header.add(tierInfoPanel, BorderLayout.WEST);
        
        // Right side: Status and collapse indicator
        JPanel tierStatusPanel = createTierStatusPanel(isUnlocked, isCollapsed);
        header.add(tierStatusPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createTierInfoPanel(Integer tierNum, Bingo bingo) {
        JPanel tierInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tierInfoPanel.setOpaque(false);
        
        // Tier icon and label in single line
        JLabel tierIcon = createTierIcon(tierNum);
        JLabel tierLabel = createTierLabel(tierNum);
        
        tierInfoPanel.add(tierIcon);
        tierInfoPanel.add(tierLabel);
        
        // Add progress text inline if available and enabled
        if (configuration.showProgressInfo) {
            String progressText = getProgressText(tierNum, bingo != null ? bingo.getProgression() : null);
            if (!progressText.isEmpty()) {
                JLabel progressLabel = new JLabel(" - " + progressText);
                progressLabel.setForeground(new Color(156, 163, 175));
                progressLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                tierInfoPanel.add(progressLabel);
            }
        }
        
        return tierInfoPanel;
    }
    
    
    private JLabel createTierIcon(Integer tierNum) {
        JLabel tierIcon = new JLabel(String.valueOf(tierNum));
        tierIcon.setOpaque(true);
        tierIcon.setBackground(new Color(59, 130, 246));
        tierIcon.setForeground(Color.WHITE);
        tierIcon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        tierIcon.setBorder(new EmptyBorder(MEDIUM_SPACING, TIER_ICON_PADDING, MEDIUM_SPACING, TIER_ICON_PADDING));
        tierIcon.setHorizontalAlignment(SwingConstants.CENTER);
        return tierIcon;
    }
    
    private JLabel createTierLabel(Integer tierNum) {
        JLabel tierLabel = new JLabel("Tier " + tierNum);
        tierLabel.setForeground(Color.WHITE);
        tierLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        tierLabel.setBorder(new EmptyBorder(0, TIER_LABEL_LEFT_MARGIN, 0, 0));
        return tierLabel;
    }
    
    
    private JPanel createTierStatusPanel(boolean isUnlocked, boolean isCollapsed) {
        JPanel tierStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, MEDIUM_SPACING, 0));
        tierStatusPanel.setOpaque(false);
        
        // Add collapse indicator for unlocked tiers
        if (isUnlocked && configuration.enableTierCollapse) {
            JLabel collapseIndicator = createCollapseIndicator(isCollapsed);
            tierStatusPanel.add(collapseIndicator);
        }
        
        // Lock/Unlock status
        JLabel statusLabel = createStatusLabel(isUnlocked);
        tierStatusPanel.add(statusLabel);
        
        return tierStatusPanel;
    }
    
    private JLabel createCollapseIndicator(boolean isCollapsed) {
        JLabel collapseIndicator = new JLabel(isCollapsed ? "▶" : "▼");
        collapseIndicator.setForeground(Color.WHITE);
        collapseIndicator.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        return collapseIndicator;
    }
    
    private JLabel createStatusLabel(boolean isUnlocked) {
        JLabel statusLabel = new JLabel(isUnlocked ? "Unlocked" : "Locked");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(isUnlocked ? new Color(34, 197, 94) : new Color(107, 114, 128));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statusLabel.setBorder(new EmptyBorder(SMALL_SPACING, STATUS_BADGE_PADDING, SMALL_SPACING, STATUS_BADGE_PADDING));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        return statusLabel;
    }
    
    private void addTierHeaderClickHandler(JPanel tierHeader, Integer tierNum, JPanel tierSection, JPanel contentPanel) {
        tierHeader.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleTierCollapse(tierNum, tierSection, contentPanel);
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                tierHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                tierHeader.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }
    
    private void toggleTierCollapse(Integer tierNum, JPanel tierSection, JPanel contentPanel) {
        boolean isCurrentlyCollapsed = collapsedTiers.contains(tierNum);
        
        if (isCurrentlyCollapsed) {
            // Expand: remove from collapsed set and add content panel
            collapsedTiers.remove(tierNum);
            tierSection.add(contentPanel);
        } else {
            // Collapse: add to collapsed set and remove content panel
            collapsedTiers.add(tierNum);
            tierSection.remove(contentPanel);
        }
        
        // Refresh the display to show changes
        refreshTierDisplay();
    }
    
    private void refreshTierDisplay() {
        SwingUtilities.invokeLater(() -> {
            // Re-display the current bingo to refresh the tier headers with updated collapse states
            if (getCurrentBingo() != null && targetPanel != null) {
                buildBoard(getCurrentBingo(), targetPanel);
            }
        });
    }
    
    private JPanel createTierTilesPanel(List<Tile> tierTiles) {
        JPanel tilesPanel = new JPanel();
        
        // Handle case where there are no tiles (shouldn't happen for unlocked tiers, but safety check)
        if (tierTiles.isEmpty()) {
            tilesPanel.setLayout(new BorderLayout());
            tilesPanel.setOpaque(false);
            JLabel noTilesLabel = new JLabel("No tiles available");
            noTilesLabel.setForeground(new Color(156, 163, 175));
            noTilesLabel.setHorizontalAlignment(SwingConstants.CENTER);
            tilesPanel.add(noTilesLabel, BorderLayout.CENTER);
            return tilesPanel;
        }
        
        // Calculate tiles per row (3 tiles as shown in screenshot)
        int tilesPerRow = TILES_PER_ROW_PROGRESSIVE;
        int rows = (int) Math.ceil((double) tierTiles.size() / tilesPerRow);
        
        tilesPanel.setLayout(new GridLayout(rows, tilesPerRow, LARGE_SPACING, LARGE_SPACING));
        tilesPanel.setOpaque(false);
        tilesPanel.setBorder(new EmptyBorder(0, TILES_PANEL_SIDE_MARGIN, 0, TILES_PANEL_SIDE_MARGIN));
        
        // Sort tiles by index
        tierTiles.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        
        for (Tile tile : tierTiles) {
            JPanel tilePanel = createProgressiveTilePanel(tile);
            tilesPanel.add(tilePanel);
        }
        
        // Fill remaining slots with empty panels if needed
        int remainingSlots = (rows * tilesPerRow) - tierTiles.size();
        for (int i = 0; i < remainingSlots; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setOpaque(false);
            tilesPanel.add(emptyPanel);
        }
        
        return tilesPanel;
    }
    
    private JPanel createLockedTierPanel(Integer tierNum) {
        JPanel lockedPanel = new JPanel();
        lockedPanel.setLayout(new BoxLayout(lockedPanel, BoxLayout.Y_AXIS));
        lockedPanel.setOpaque(false);
        lockedPanel.setBorder(new EmptyBorder(LOCKED_PANEL_PADDING, LOCKED_PANEL_PADDING, LOCKED_PANEL_PADDING, LOCKED_PANEL_PADDING));
        
        // Lock icon (using text for simplicity)
        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, LOCK_ICON_FONT_SIZE));
        lockIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lockIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Dynamic message based on tier
        String previousTier = tierNum > 1 ? "Tier " + (tierNum - 1) : "previous tiers";
        JLabel lockMessage = new JLabel("Complete more tiles in " + previousTier + " to unlock these tiles");
        lockMessage.setForeground(new Color(156, 163, 175));
        lockMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        lockMessage.setHorizontalAlignment(SwingConstants.CENTER);
        lockMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        lockedPanel.add(lockIcon);
        lockedPanel.add(Box.createVerticalStrut(LARGE_SPACING));
        lockedPanel.add(lockMessage);
        
        return lockedPanel;
    }
    
    private JPanel createProgressiveTilePanel(Tile tile) {
        // Create a specialized tile panel for progressive layout (no tier indicator needed)
        int tileSize = PROGRESSIVE_TILE_SIZE; // Fixed size for progressive layout
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));
        
        // Set background and border based on submission status using factory
        getTileFactory().applyTileAppearance(panel, tile.getSubmission());
        
        // Set tooltip using shared factory
        panel.setToolTipText(getTileFactory().createDetailedTooltip(tile, getCurrentBingo()));
        
        // Add image if available, otherwise show title using factory
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            getTileFactory().loadTileImage(panel, tile, tileSize);
        } else {
            getTileFactory().addTileTitle(panel, tile);
        }
        
        // Add XP value indicator using factory (no tier indicator for progressive tiles)
        getTileFactory().addXpIndicator(panel, tile);
        
        // Add status overlay using factory
        getTileFactory().addStatusOverlay(panel, tile);
        
        // Add click behavior using factory
        getTileFactory().addTileInteractionListeners(panel, tile, tileClickCallback);
        
        return panel;
    }
    
    
    
    
    
    private String getProgressText(Integer tierNum, ProgressionMetadata progression) {
        if (progression != null && progression.getTierProgress() != null) {
            for (TierProgress tp : progression.getTierProgress()) {
                if (tp.getTier().equals(tierNum)) {
                    // For now, show simple XP progress
                    return "0/3 XP completed (5 XP required to unlock next tier)";
                }
            }
        }
        return "";
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
    
    @Override
    protected ProgressiveBoardConfiguration getBoardConfiguration() {
        return configuration;
    }
}