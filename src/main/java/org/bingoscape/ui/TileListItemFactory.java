package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
        listItem.setBorder(new CompoundBorder(
            new LineBorder(getStatusColor(tile), 2, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
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
     * @param onClickAction Action to perform when tile is clicked
     * @param onRemoveAction Action to perform when tile is removed (Shift+Click)
     * @return A JPanel representing the compact tile
     */
    public JPanel createCompactTileListItem(Tile tile, Consumer<Tile> onClickAction, Consumer<String> onRemoveAction) {
        JPanel listItem = new JPanel(new BorderLayout());
        listItem.setBackground(ColorPalette.PINNED_TILE_BG);
        listItem.setBorder(new CompoundBorder(
            new LineBorder(getStatusColor(tile), 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        listItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Left: Smaller icon
        JPanel imagePanel = createImagePanel(tile, 24, 12);

        // Center: Title only
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 8, 0, 8));

        JLabel titleLabel = new JLabel(tile.getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // Right: XP and status
        JPanel rightPanel = createRightInfoPanel(tile, true);

        // Assemble
        listItem.add(imagePanel, BorderLayout.WEST);
        listItem.add(titlePanel, BorderLayout.CENTER);
        listItem.add(rightPanel, BorderLayout.EAST);

        // Add interaction
        addTileInteraction(listItem, tile, onClickAction, onRemoveAction, true);

        // Compact tooltip
        listItem.setToolTipText(createCompactTooltip(tile));

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
                imageLabel.setBorder(new LineBorder(ColorPalette.BORDER, 1, true));
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
                iconLabel.setBorder(new LineBorder(ColorPalette.BORDER, 1, true));
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
        detailsPanel.setBorder(new EmptyBorder(0, 12, 0, 12));

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
            descLabel.setForeground(new Color(156, 163, 175));
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
            tierLabel.setForeground(new Color(156, 163, 175));
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
        listItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isShiftDown()) {
                    onRemoveAction.accept(tile.getId().toString());
                } else {
                    // Pass the event to get the source component for popup positioning
                    SwingUtilities.invokeLater(() -> onClickAction.accept(tile));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Color brighterBg = new Color(
                    Math.min(255, ColorPalette.PINNED_TILE_BG.getRed() + 10),
                    Math.min(255, ColorPalette.PINNED_TILE_BG.getGreen() + 10),
                    Math.min(255, ColorPalette.PINNED_TILE_BG.getBlue() + 10)
                );
                listItem.setBackground(brighterBg);
                listItem.setCursor(new Cursor(Cursor.HAND_CURSOR));

                listItem.setBorder(new CompoundBorder(
                    new LineBorder(getStatusColor(tile).brighter(), compact ? 1 : 2, true),
                    new EmptyBorder(compact ? 6 : 10, compact ? 8 : 12, compact ? 6 : 10, compact ? 8 : 12)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                listItem.setBackground(ColorPalette.PINNED_TILE_BG);
                listItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                listItem.setBorder(new CompoundBorder(
                    new LineBorder(getStatusColor(tile), compact ? 1 : 2, true),
                    new EmptyBorder(compact ? 6 : 10, compact ? 8 : 12, compact ? 6 : 10, compact ? 8 : 12)
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
     */
    private Color getStatusColor(Tile tile) {
        if (tile.getSubmission() == null || tile.getSubmission().getStatus() == null) {
            return ColorPalette.BORDER;
        }

        switch (tile.getSubmission().getStatus()) {
            case PENDING: return ColorPalette.ACCENT_BLUE;
            case ACCEPTED: return ColorPalette.SUCCESS;
            case REQUIRES_INTERACTION: return ColorPalette.WARNING_YELLOW;
            case DECLINED: return ColorPalette.ERROR_RED;
            default: return ColorPalette.BORDER;
        }
    }

    /**
     * Gets human-readable status text.
     */
    private String getStatusText(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "Pending";
            case ACCEPTED: return "Completed";
            case REQUIRES_INTERACTION: return "Action Needed";
            case DECLINED: return "Declined";
            default: return "Not Submitted";
        }
    }
}
