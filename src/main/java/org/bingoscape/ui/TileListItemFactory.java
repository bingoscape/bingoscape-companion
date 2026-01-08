package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.ColorPalette;
import org.bingoscape.ui.components.TileProgressBar;
import org.bingoscape.utils.GoalTreeProgressCalculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Factory for creating tile list item UI components.
 * Provides both detailed and compact tile representations with consistent styling.
 */
public class TileListItemFactory {

    private final BingoScapePlugin plugin;

    public TileListItemFactory(BingoScapePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a detailed tile list item with full information.
     *
     * @param tile The tile to display
     * @param onClickAction Action to perform when tile is clicked
     * @param onRemoveAction Action to perform when tile is removed (Shift+Click)
     * @return A JPanel representing the detailed tile
     */
    public JPanel createDetailedTileListItem(Tile tile, Consumer<Tile> onClickAction, Consumer<String> onRemoveAction) {
        JPanel listItem = new JPanel(new BorderLayout());
        listItem.setBackground(ColorPalette.PINNED_TILE_BG);
        listItem.setBorder(UIStyleFactory.createStyledBorder(getStatusColor(tile), 2, 10, 12, 10, 12));
        listItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, listItem.getPreferredSize().height));

        // Left side: Header image
        JPanel imagePanel = createImagePanel(tile, 40, 18);

        // Center: Tile details
        JPanel detailsPanel = createDetailsPanel(tile, false);

        // Right: XP and tier info
        JPanel rightPanel = createRightInfoPanel(tile, false);

        // Assemble
        listItem.add(imagePanel, BorderLayout.WEST);
        listItem.add(detailsPanel, BorderLayout.CENTER);
        listItem.add(rightPanel, BorderLayout.EAST);

        // Add interaction
        addTileInteraction(listItem, tile, onClickAction, onRemoveAction, false);

        return listItem;
    }

    /**
     * Creates a compact tile list item for space-efficient display.
     *
     * @param tile The tile to display
     * @param bingo The bingo board containing this tile (for hover card)
     * @param itemManager The ItemManager for loading item icons in hover card
     * @param onClickAction Action to perform when tile is clicked
     * @param onRemoveAction Action to perform when tile is removed (Shift+Click)
     * @return A JPanel representing the compact tile
     */
    public JPanel createCompactTileListItem(Tile tile, org.bingoscape.models.Bingo bingo,
                                           net.runelite.client.game.ItemManager itemManager,
                                           Consumer<Tile> onClickAction, Consumer<String> onRemoveAction) {
        JPanel listItem = new JPanel(new BorderLayout());
        listItem.setBackground(ColorPalette.PINNED_TILE_BG);
        listItem.setBorder(UIStyleFactory.createStyledBorder(getStatusColor(tile), 1, 6, 8, 6, 8));
        listItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52)); // Increased height for progress bar

        // Left: Smaller icon
        JPanel imagePanel = createImagePanel(tile, 24, 12);

        // Center: Title only
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(UIStyleFactory.createPaddingBorder(0, 8, 0, 8));

        JLabel titleLabel = new JLabel(tile.getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // Right: XP and status
        JPanel rightPanel = createRightInfoPanel(tile, true);

        // Assemble main content
        listItem.add(imagePanel, BorderLayout.WEST);
        listItem.add(titlePanel, BorderLayout.CENTER);
        listItem.add(rightPanel, BorderLayout.EAST);

        // Add progress bar at bottom (like board tiles)
        if (tile.getGoalTree() != null && !tile.getGoalTree().isEmpty()) {
            GoalTreeProgressCalculator.ProgressResult progress =
                GoalTreeProgressCalculator.getProgressFromTile(tile.getGoalTree());

            if (progress != null) {
                TileProgressBar progressBar = TileProgressBar.createProgressBar(progress);
                if (progressBar != null) {
                    listItem.add(progressBar, BorderLayout.SOUTH);
                }
            }
        }

        // Add interaction
        addTileInteraction(listItem, tile, onClickAction, onRemoveAction, true);

        // Attach hover card (same as board tiles)
        if (bingo != null && itemManager != null) {
            TileHoverCardManager.getInstance().attachHoverCard(listItem, tile, bingo, itemManager);
        }

        return listItem;
    }

    /**
     * Creates an image panel for the tile.
     */
    private JPanel createImagePanel(Tile tile, int size, int iconFontSize) {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(size, size));

        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(size, size));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            if (size > 30) {
                imageLabel.setBorder(UIStyleFactory.createRoundedBorder(ColorPalette.BORDER, 1));
            }

            // Load image asynchronously
            SwingUtilities.invokeLater(() -> {
                try {
                    ImageIcon icon = new ImageIcon(new URL(tile.getHeaderImage()));
                    Image scaledImage = icon.getImage().getScaledInstance(size - 2, size - 2, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                } catch (Exception e) {
                    imageLabel.setText("🎯");
                    imageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, iconFontSize));
                }
            });

            imagePanel.add(imageLabel, BorderLayout.CENTER);
        } else {
            JLabel iconLabel = new JLabel("🎯");
            iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, iconFontSize));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setPreferredSize(new Dimension(size, size));
            if (size > 30) {
                iconLabel.setBorder(UIStyleFactory.createRoundedBorder(ColorPalette.BORDER, 1));
                iconLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                iconLabel.setOpaque(true);
            }
            imagePanel.add(iconLabel, BorderLayout.CENTER);
        }

        return imagePanel;
    }

    /**
     * Creates the details panel (title and description).
     */
    private JPanel createDetailsPanel(Tile tile, boolean compact) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(UIStyleFactory.createPaddingBorder(0, 12, 0, 12));

        JLabel titleLabel = new JLabel(tile.getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, compact ? 11 : 12));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(titleLabel);

        if (!compact && tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            detailsPanel.add(Box.createVerticalStrut(4));
            String truncatedDesc = tile.getDescription().length() > 80
                ? tile.getDescription().substring(0, 77) + "..."
                : tile.getDescription();
            JLabel descLabel = new JLabel("<html>" + truncatedDesc + "</html>");
            descLabel.setForeground(ColorPalette.TEXT_SECONDARY_GRAY);
            descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(descLabel);
        }

        return detailsPanel;
    }

    /**
     * Creates the right panel with XP, tier, and status indicators.
     */
    private JPanel createRightInfoPanel(Tile tile, boolean compact) {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        // Status indicator
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            JLabel statusDot = new JLabel("●");
            statusDot.setForeground(getStatusColor(tile));
            statusDot.setFont(new Font(Font.SANS_SERIF, Font.BOLD, compact ? 8 : 10));
            rightPanel.add(statusDot);
            rightPanel.add(Box.createHorizontalStrut(4));
        }

        // XP value
        JLabel xpLabel = new JLabel(tile.getWeight() + "XP");
        xpLabel.setForeground(ColorPalette.GOLD);
        xpLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, compact ? 9 : 10));
        rightPanel.add(xpLabel);

        // Tier
        if (tile.getTier() != null && tile.getTier() > 1) {
            rightPanel.add(Box.createHorizontalStrut(4));
            JLabel tierLabel = new JLabel("T" + tile.getTier());
            tierLabel.setForeground(ColorPalette.TEXT_SECONDARY_GRAY);
            tierLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, compact ? 8 : 9));
            rightPanel.add(tierLabel);
        }

        return rightPanel;
    }

    /**
     * Adds mouse interaction (click, hover) to the tile list item.
     */
    private void addTileInteraction(JPanel listItem, Tile tile, Consumer<Tile> onClickAction,
                                    Consumer<String> onRemoveAction, boolean compact) {
        // Pre-calculate colors and borders for hover effects
        final Color originalBg = ColorPalette.PINNED_TILE_BG;
        final Color hoverBg = UIStyleFactory.brighten(originalBg, 10);
        final Color statusColor = getStatusColor(tile);
        final Color hoverBorderColor = UIStyleFactory.brighten(statusColor, 40);

        final int borderWidth = compact ? 1 : 2;
        final int paddingVert = compact ? 6 : 10;
        final int paddingHoriz = compact ? 8 : 12;

        listItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isShiftDown()) {
                    onRemoveAction.accept(tile.getId().toString());
                } else {
                    SwingUtilities.invokeLater(() -> onClickAction.accept(tile));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                listItem.setBackground(hoverBg);
                listItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
                listItem.setBorder(UIStyleFactory.createStyledBorder(
                    hoverBorderColor, borderWidth, paddingVert, paddingHoriz, paddingVert, paddingHoriz
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                listItem.setBackground(originalBg);
                listItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                listItem.setBorder(UIStyleFactory.createStyledBorder(
                    statusColor, borderWidth, paddingVert, paddingHoriz, paddingVert, paddingHoriz
                ));
            }
        });
    }

    /**
     * Creates a compact tooltip for the tile.
     */
    private String createCompactTooltip(Tile tile) {
        StringBuilder tooltip = new StringBuilder("<html><b>" + tile.getTitle() + "</b><br>");
        tooltip.append("XP: ").append(tile.getWeight());

        if (tile.getTier() != null && tile.getTier() > 1) {
            tooltip.append(" • Tier ").append(tile.getTier());
        }

        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            tooltip.append("<br>Status: ").append(getStatusText(tile.getSubmission().getStatus()));
        }

        tooltip.append("<br><i>Click for actions • Shift+Click to unpin</i>");
        tooltip.append("</html>");

        return tooltip.toString();
    }

    /**
     * Gets the display color for a tile based on its submission status.
     * Delegates to StatusConstants for consistency.
     */
    private Color getStatusColor(Tile tile) {
        if (tile.getSubmission() == null || tile.getSubmission().getStatus() == null) {
            return ColorPalette.BORDER;
        }
        return StatusConstants.getStatusColor(tile.getSubmission().getStatus());
    }

    /**
     * Gets human-readable status text.
     * Delegates to StatusConstants for consistency.
     */
    private String getStatusText(TileSubmissionType status) {
        return StatusConstants.getStatusText(status);
    }
}
