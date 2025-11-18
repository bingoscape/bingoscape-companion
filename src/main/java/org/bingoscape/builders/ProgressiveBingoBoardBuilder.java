package org.bingoscape.builders;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.*;
import org.bingoscape.ui.ColorPalette;
import org.bingoscape.ui.StatusConstants;
import org.bingoscape.ui.UIStyleFactory;
import org.bingoscape.constants.BingoTypeConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        tierSection.setBorder(UIStyleFactory.createPaddingBorder(
            LARGE_SPACING, TIER_SECTION_PADDING, LARGE_SPACING, TIER_SECTION_PADDING
        ));
        
        // Check if tier is collapsed
        boolean isCollapsed = configuration.enableTierCollapse && collapsedTiers.contains(tierNum);
        
        // Create tier header (clickable for unlocked tiers)
        JPanel tierHeader = createTierHeader(tierNum, isUnlocked, isCollapsed, bingo);
        tierSection.add(tierHeader);
        
        // Create content panel that can be shown/hidden
        JPanel contentPanel = createTierContentPanel(tierNum, tierTiles, isUnlocked);
        
        // Always add content panel but set visibility based on collapse state
        // Locked tiers are never collapsible, so they're always visible
        contentPanel.setVisible(!isCollapsed || !isUnlocked);
        tierSection.add(contentPanel);
        
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
            
            // Add placeholder panel for collapsed state
            JPanel placeholderPanel = createCollapsedPlaceholder();
            contentPanel.add(placeholderPanel);
            
            // Set initial visibility based on collapsed state
            boolean isCollapsed = configuration.enableTierCollapse && collapsedTiers.contains(tierNum);
            tilesPanel.setVisible(!isCollapsed);
            placeholderPanel.setVisible(isCollapsed);
        } else {
            // Create locked tier panel (locked tiers are not collapsible)
            JPanel lockedPanel = createLockedTierPanel(tierNum);
            contentPanel.add(lockedPanel);
        }
        
        return contentPanel;
    }
    
    private JPanel createCollapsedPlaceholder() {
        JPanel placeholderPanel = new JPanel(new BorderLayout());
        placeholderPanel.setOpaque(false);
        placeholderPanel.setBorder(UIStyleFactory.createPaddingBorder(
            LARGE_SPACING, TILES_PANEL_SIDE_MARGIN, LARGE_SPACING, TILES_PANEL_SIDE_MARGIN
        ));
        
        // Create a horizontal line that spans the width
        JPanel linePanel = new JPanel();
        linePanel.setOpaque(true);
        linePanel.setBackground(ColorPalette.BORDER);
        linePanel.setPreferredSize(new Dimension(0, 2)); // Thin horizontal line
        linePanel.setMinimumSize(new Dimension(0, 2));
        linePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        
        // Add some vertical spacing around the line
        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new BoxLayout(lineContainer, BoxLayout.Y_AXIS));
        lineContainer.setOpaque(false);
        
        lineContainer.add(Box.createVerticalStrut(LARGE_SPACING));
        lineContainer.add(linePanel);
        lineContainer.add(Box.createVerticalStrut(LARGE_SPACING));
        
        placeholderPanel.add(lineContainer, BorderLayout.CENTER);
        
        return placeholderPanel;
    }
    
    private JPanel createTierHeader(Integer tierNum, boolean isUnlocked, boolean isCollapsed, Bingo bingo) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        
        // Enhanced background with hierarchy visual cues
        Color backgroundColor = ColorPalette.PINNED_TILE_BG;
        if (tierNum == 1) {
            backgroundColor = ColorPalette.CARD_BG;
        } else if (tierNum > 1) {
            // Darker for higher tiers to show hierarchy
            int darkness = Math.max(30, 50 - (tierNum - 1) * 5);
            backgroundColor = new Color(darkness, darkness + 5, darkness + 10);
        }
        
        header.setBackground(backgroundColor);
        header.setPreferredSize(new Dimension(0, TIER_HEADER_HEIGHT));
        header.setMinimumSize(new Dimension(0, TIER_HEADER_HEIGHT));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, TIER_HEADER_HEIGHT));
        
        // Enhanced border with hierarchy indentation effect
        Color borderColor = isUnlocked ? ColorPalette.SUCCESS : ColorPalette.TEXT_MEDIUM_GRAY;
        int borderWidth = isUnlocked ? 2 : 1;
        
        // Add hierarchical indentation
        int leftIndent = TIER_HEADER_PADDING + (tierNum - 1) * 8; // 8px indent per tier
        header.setBorder(UIStyleFactory.createStyledBorder(
            borderColor, borderWidth, TIER_HEADER_PADDING, leftIndent,
            TIER_HEADER_PADDING, TIER_HEADER_PADDING
        ));
        
        // Left side: Enhanced tier info with hierarchy indicators
        JPanel tierInfoPanel = createEnhancedTierInfoPanel(tierNum, bingo, isUnlocked);
        header.add(tierInfoPanel, BorderLayout.WEST);
        
        // Right side: Status and collapse indicator
        JPanel tierStatusPanel = createTierStatusPanel(isUnlocked, isCollapsed);
        header.add(tierStatusPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createEnhancedTierInfoPanel(Integer tierNum, Bingo bingo, boolean isUnlocked) {
        JPanel tierInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tierInfoPanel.setOpaque(false);
        
        // Add hierarchical connection indicators for tiers > 1
        if (tierNum > 1) {
            // Add hierarchy connector symbols
            JLabel connector = new JLabel("├─ ");
            connector.setForeground(ColorPalette.TEXT_MEDIUM_GRAY);
            connector.setFont(new Font("Monospaced", Font.PLAIN, 12));
            tierInfoPanel.add(connector);
        }
        
        // Enhanced tier icon with better visual hierarchy
        JLabel tierIcon = createEnhancedTierIcon(tierNum, isUnlocked);
        JLabel tierLabel = createTierLabel(tierNum);
        
        tierInfoPanel.add(tierIcon);
        tierInfoPanel.add(tierLabel);
        
        // Add progress text inline if available and enabled
        if (configuration.showProgressInfo) {
            String progressText = getProgressText(tierNum, bingo != null ? bingo.getProgression() : null);
            if (!progressText.isEmpty()) {
                JLabel progressLabel = new JLabel(" - " + progressText);
                progressLabel.setForeground(ColorPalette.TEXT_SECONDARY_GRAY);
                progressLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                tierInfoPanel.add(progressLabel);
            }
        }
        
        return tierInfoPanel;
    }
    
    
    private JLabel createEnhancedTierIcon(Integer tierNum, boolean isUnlocked) {
        JLabel tierIcon = new JLabel(String.valueOf(tierNum));
        tierIcon.setOpaque(true);
        
        // Enhanced color scheme based on tier and status
        Color backgroundColor;
        if (!isUnlocked) {
            backgroundColor = ColorPalette.BORDER;
        } else if (tierNum == 1) {
            backgroundColor = ColorPalette.SUCCESS;
        } else if (tierNum == 2) {
            backgroundColor = ColorPalette.ACCENT_BLUE;
        } else if (tierNum == 3) {
            backgroundColor = ColorPalette.TIER_3_PURPLE;
        } else {
            backgroundColor = ColorPalette.TIER_4_ORANGE;
        }
        
        tierIcon.setBackground(backgroundColor);
        tierIcon.setForeground(Color.WHITE);
        tierIcon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        // Add subtle shadow effect for unlocked tiers
        if (isUnlocked) {
            tierIcon.setBorder(UIStyleFactory.createStyledBorder(
                UIStyleFactory.darken(backgroundColor, 20), 1,
                MEDIUM_SPACING, TIER_ICON_PADDING, MEDIUM_SPACING, TIER_ICON_PADDING
            ));
        } else {
            tierIcon.setBorder(UIStyleFactory.createPaddingBorder(
                MEDIUM_SPACING, TIER_ICON_PADDING, MEDIUM_SPACING, TIER_ICON_PADDING
            ));
        }
        tierIcon.setHorizontalAlignment(SwingConstants.CENTER);
        
        return tierIcon;
    }
    
    private JLabel createTierIcon(Integer tierNum) {
        JLabel tierIcon = new JLabel(String.valueOf(tierNum));
        tierIcon.setOpaque(true);
        tierIcon.setBackground(ColorPalette.ACCENT_BLUE);
        tierIcon.setForeground(Color.WHITE);
        tierIcon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        tierIcon.setBorder(UIStyleFactory.createPaddingBorder(
            MEDIUM_SPACING, TIER_ICON_PADDING, MEDIUM_SPACING, TIER_ICON_PADDING
        ));
        tierIcon.setHorizontalAlignment(SwingConstants.CENTER);
        return tierIcon;
    }
    
    private JLabel createTierLabel(Integer tierNum) {
        JLabel tierLabel = new JLabel("Tier " + tierNum);
        tierLabel.setForeground(Color.WHITE);
        tierLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        tierLabel.setBorder(UIStyleFactory.createPaddingBorder(0, TIER_LABEL_LEFT_MARGIN, 0, 0));
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
        // Enhanced collapse indicator with better visibility
        String symbol = isCollapsed ? "▶" : "▼";
        JLabel collapseIndicator = new JLabel(symbol);
        collapseIndicator.setForeground(Color.WHITE);
        collapseIndicator.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); // Slightly larger and bold
        collapseIndicator.setOpaque(true);
        collapseIndicator.setBackground(ColorPalette.BORDER);
        collapseIndicator.setBorder(UIStyleFactory.createPaddingBorder(4, 8, 4, 8));
        collapseIndicator.setHorizontalAlignment(SwingConstants.CENTER);
        collapseIndicator.setToolTipText(isCollapsed ? "Click to expand tier" : "Click to collapse tier");
        return collapseIndicator;
    }
    
    private JLabel createStatusLabel(boolean isUnlocked) {
        JLabel statusLabel = new JLabel(isUnlocked ? "Unlocked" : "Locked");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(isUnlocked ? ColorPalette.SUCCESS : ColorPalette.TEXT_MEDIUM_GRAY);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statusLabel.setBorder(UIStyleFactory.createPaddingBorder(
            SMALL_SPACING, STATUS_BADGE_PADDING, SMALL_SPACING, STATUS_BADGE_PADDING
        ));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        return statusLabel;
    }
    
    private void addTierHeaderClickHandler(JPanel tierHeader, Integer tierNum, JPanel tierSection, JPanel contentPanel) {
        tierHeader.addMouseListener(new java.awt.event.MouseAdapter() {
            private final Color originalBackground = tierHeader.getBackground();
            private final Color highlightColor = UIStyleFactory.brighten(originalBackground, 10);
            private final Color pressedColor = UIStyleFactory.darken(originalBackground, 20);

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleTierCollapse(tierNum, tierSection, contentPanel);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                tierHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
                tierHeader.setBackground(highlightColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                tierHeader.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                tierHeader.setBackground(originalBackground);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                tierHeader.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                // Return to hover color if still hovering, otherwise original
                if (tierHeader.contains(e.getPoint())) {
                    tierHeader.setBackground(highlightColor);
                } else {
                    tierHeader.setBackground(originalBackground);
                }
            }
        });
    }
    
    private void toggleTierCollapse(Integer tierNum, JPanel tierSection, JPanel contentPanel) {
        boolean isCurrentlyCollapsed = collapsedTiers.contains(tierNum);
        
        // Find the tiles panel and placeholder panel within content panel
        JPanel tilesPanel = null;
        JPanel placeholderPanel = null;
        
        // Content panel structure: [vertical strut, tiles panel, placeholder panel]
        for (Component comp : contentPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                // Tiles panel has GridLayout, placeholder has BorderLayout
                if (panel.getLayout() instanceof GridLayout) {
                    tilesPanel = panel;
                } else if (panel.getLayout() instanceof BorderLayout && panel.getBorder() instanceof EmptyBorder) {
                    placeholderPanel = panel;
                }
            }
        }
        
        if (isCurrentlyCollapsed) {
            // Expand: remove from collapsed set, show tiles, hide placeholder
            collapsedTiers.remove(tierNum);
            if (tilesPanel != null) tilesPanel.setVisible(true);
            if (placeholderPanel != null) placeholderPanel.setVisible(false);
        } else {
            // Collapse: add to collapsed set, hide tiles, show placeholder
            collapsedTiers.add(tierNum);
            if (tilesPanel != null) tilesPanel.setVisible(false);
            if (placeholderPanel != null) placeholderPanel.setVisible(true);
        }
        
        // Update the collapse indicator in the header
        updateCollapseIndicator(tierSection, !isCurrentlyCollapsed);
        
        // Revalidate and repaint to reflect changes without rebuilding entire board
        SwingUtilities.invokeLater(() -> {
            if (targetPanel != null) {
                targetPanel.revalidate();
                targetPanel.repaint();
            }
        });
    }
    
    private void updateCollapseIndicator(JPanel tierSection, boolean isCollapsed) {
        // Find the collapse indicator in the header and update it
        JPanel tierHeader = (JPanel) tierSection.getComponent(0); // Header is always first component
        JPanel statusPanel = (JPanel) tierHeader.getComponent(1); // Status panel is on the right (EAST)
        
        // Find the collapse indicator (first component in status panel)
        if (statusPanel.getComponentCount() > 0) {
            Component firstComponent = statusPanel.getComponent(0);
            if (firstComponent instanceof JLabel) {
                JLabel collapseIndicator = (JLabel) firstComponent;
                String symbol = isCollapsed ? "▶" : "▼";
                collapseIndicator.setText(symbol);
                collapseIndicator.setToolTipText(isCollapsed ? "Click to expand tier" : "Click to collapse tier");
            }
        }
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
            noTilesLabel.setForeground(ColorPalette.TEXT_SECONDARY_GRAY);
            noTilesLabel.setHorizontalAlignment(SwingConstants.CENTER);
            tilesPanel.add(noTilesLabel, BorderLayout.CENTER);
            return tilesPanel;
        }
        
        // Calculate tiles per row (3 tiles as shown in screenshot)
        int tilesPerRow = TILES_PER_ROW_PROGRESSIVE;
        int rows = (int) Math.ceil((double) tierTiles.size() / tilesPerRow);
        
        tilesPanel.setLayout(new GridLayout(rows, tilesPerRow, LARGE_SPACING, LARGE_SPACING));
        tilesPanel.setOpaque(false);
        tilesPanel.setBorder(UIStyleFactory.createPaddingBorder(0, TILES_PANEL_SIDE_MARGIN, 0, TILES_PANEL_SIDE_MARGIN));
        
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
        lockedPanel.setBorder(UIStyleFactory.createPaddingBorder(LOCKED_PANEL_PADDING));
        
        // Lock icon (using text for simplicity)
        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, LOCK_ICON_FONT_SIZE));
        lockIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lockIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Dynamic message based on tier
        String previousTier = tierNum > 1 ? "Tier " + (tierNum - 1) : "previous tiers";
        JLabel lockMessage = new JLabel("Complete more tiles in " + previousTier + " to unlock these tiles");
        lockMessage.setForeground(ColorPalette.TEXT_SECONDARY_GRAY);
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

        // Attach hover card with detailed information using shared factory
        getTileFactory().attachHoverCard(panel, tile, getCurrentBingo(), plugin.getItemManager());
        
        // Add image if available, otherwise show title using factory
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            getTileFactory().loadTileImage(panel, tile, tileSize);
        } else {
            getTileFactory().addTileTitle(panel, tile);
        }
        
        // Add XP value indicator using factory (no tier indicator for progressive tiles)
        getTileFactory().addXpIndicator(panel, tile);

        // Add bottom overlays (status + progress indicator) using factory
        getTileFactory().addBottomOverlays(panel, tile);

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
    
    

    @Override
    protected ProgressiveBoardConfiguration getBoardConfiguration() {
        return configuration;
    }
}