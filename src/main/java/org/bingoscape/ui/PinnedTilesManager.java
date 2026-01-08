package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the pinned tiles section of the UI.
 * Handles loading, saving, displaying, and interacting with pinned tiles.
 */
public class PinnedTilesManager {

    private final BingoScapePlugin plugin;
    private final TileListItemFactory tileFactory;
    private final Consumer<Tile> onTileClickAction;
    private final Runnable onShowBoardAction;

    private final Set<String> pinnedTileIds = new HashSet<>();
    private final List<Tile> pinnedTiles = new ArrayList<>();
    private Bingo currentBingo; // Current bingo for hover card display

    private JPanel pinnedTilesSection;
    private JScrollPane pinnedTilesScrollPane;
    private JPanel pinnedTilesContainer;

    /**
     * Creates a new pinned tiles manager.
     *
     * @param plugin The main plugin instance
     * @param onTileClickAction Action to perform when a pinned tile is clicked
     * @param onShowBoardAction Action to perform when "Add More" button is clicked
     */
    public PinnedTilesManager(BingoScapePlugin plugin, Consumer<Tile> onTileClickAction, Runnable onShowBoardAction) {
        this.plugin = plugin;
        this.tileFactory = new TileListItemFactory(plugin);
        this.onTileClickAction = onTileClickAction;
        this.onShowBoardAction = onShowBoardAction;

        initializeUI();
        loadPinnedTilesFromConfig();
    }

    /**
     * Initializes the UI components for the pinned tiles section.
     */
    private void initializeUI() {
        // Container for pinned tiles
        pinnedTilesContainer = new JPanel();
        pinnedTilesContainer.setLayout(new BoxLayout(pinnedTilesContainer, BoxLayout.Y_AXIS));
        pinnedTilesContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Scroll pane
        pinnedTilesScrollPane = new JScrollPane(pinnedTilesContainer);
        pinnedTilesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pinnedTilesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pinnedTilesScrollPane.setBorder(null);
        pinnedTilesScrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pinnedTilesScrollPane.setPreferredSize(new Dimension(0, 250));
        pinnedTilesScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        // Main section panel
        pinnedTilesSection = createPinnedTilesSection();
        pinnedTilesSection.setVisible(false); // Hidden by default until tiles are pinned
    }

    /**
     * Creates the pinned tiles section with header.
     */
    private JPanel createPinnedTilesSection() {
        JPanel section = new JPanel(new BorderLayout(0, UIConstants.PINNED_SECTION_SPACING));
        section.setBackground(ColorScheme.DARK_GRAY_COLOR);
        section.setBorder(new CompoundBorder(
            new LineBorder(ColorPalette.BORDER, 1, true),
            new EmptyBorder(UIConstants.SECTION_SPACING, UIConstants.BORDER_SPACING,
                          UIConstants.SECTION_SPACING, UIConstants.BORDER_SPACING)
        ));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header with toggle
        JPanel pinnedHeader = createPinnedHeader();
        section.add(pinnedHeader, BorderLayout.NORTH);
        section.add(pinnedTilesScrollPane, BorderLayout.CENTER);

        return section;
    }

    /**
     * Creates the header for the pinned tiles section.
     */
    private JPanel createPinnedHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel("📌 Pinned Tiles");
        titleLabel.setForeground(ColorPalette.GOLD);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JLabel toggleLabel = new JLabel("▼");
        toggleLabel.setForeground(Color.LIGHT_GRAY);
        toggleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        header.add(titleLabel, BorderLayout.WEST);
        header.add(toggleLabel, BorderLayout.EAST);

        // Toggle collapse on click
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = pinnedTilesScrollPane.isVisible();
                pinnedTilesScrollPane.setVisible(!isVisible);
                toggleLabel.setText(isVisible ? "▶" : "▼");
            }
        });

        return header;
    }

    /**
     * Gets the main pinned tiles section panel to add to the UI.
     */
    public JPanel getPinnedTilesSection() {
        return pinnedTilesSection;
    }

    /**
     * Gets the set of pinned tile IDs for external access.
     */
    public Set<String> getPinnedTileIds() {
        return Collections.unmodifiableSet(pinnedTileIds);
    }

    /**
     * Adds a tile to the pinned list.
     */
    public void addPinnedTile(Tile tile) {
        if (!pinnedTileIds.contains(tile.getId().toString())) {
            pinnedTileIds.add(tile.getId().toString());
            pinnedTiles.add(tile);
            savePinnedTilesToConfig();
            updatePinnedTilesDisplay();
        }
    }

    /**
     * Removes a tile from the pinned list.
     */
    public void removePinnedTile(String tileId) {
        if (pinnedTileIds.remove(tileId)) {
            pinnedTiles.removeIf(tile -> tile.getId().toString().equals(tileId));
            savePinnedTilesToConfig();
            updatePinnedTilesDisplay();
        }
    }

    /**
     * Refreshes pinned tiles from the current bingo.
     */
    public void refreshPinnedTiles(Bingo currentBingo) {
        this.currentBingo = currentBingo; // Store bingo reference for hover cards
        pinnedTiles.clear();
        if (currentBingo != null && !pinnedTileIds.isEmpty()) {
            for (Tile tile : currentBingo.getTiles()) {
                if (pinnedTileIds.contains(tile.getId().toString())) {
                    pinnedTiles.add(tile);
                }
            }
        }
        updatePinnedTilesDisplay();
    }

    /**
     * Loads pinned tile IDs from configuration.
     */
    private void loadPinnedTilesFromConfig() {
        BingoScapeConfig config = plugin.getConfig();
        String configPinnedTileIds = config.pinnedTileIds();
        if (configPinnedTileIds != null && !configPinnedTileIds.trim().isEmpty()) {
            String[] tileIds = configPinnedTileIds.split(",");
            for (String tileId : tileIds) {
                String trimmedId = tileId.trim();
                if (!trimmedId.isEmpty()) {
                    pinnedTileIds.add(trimmedId);
                }
            }
        }
    }

    /**
     * Saves pinned tile IDs to configuration.
     */
    private void savePinnedTilesToConfig() {
        String pinnedTileIdsString = String.join(",", pinnedTileIds);
        plugin.getConfig().pinnedTileIds(pinnedTileIdsString);
    }

    /**
     * Updates the pinned tiles display.
     */
    private void updatePinnedTilesDisplay() {
        pinnedTilesContainer.removeAll();

        if (pinnedTiles.isEmpty()) {
            showEmptyState();
        } else {
            showPinnedTiles();
        }

        pinnedTilesContainer.revalidate();
        pinnedTilesContainer.repaint();
    }

    /**
     * Shows the empty state when no tiles are pinned.
     */
    private void showEmptyState() {
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        emptyPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel emptyLabel = new JLabel("<html><center>📌<br><br>No pinned tiles<br><small>Pin tiles from the board for quick access</small></center></html>");
        emptyLabel.setForeground(Color.LIGHT_GRAY);
        emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);

        pinnedTilesContainer.add(emptyPanel);
        pinnedTilesSection.setVisible(false);
    }

    /**
     * Shows the list of pinned tiles.
     */
    private void showPinnedTiles() {
        for (int i = 0; i < pinnedTiles.size(); i++) {
            Tile tile = pinnedTiles.get(i);
            JPanel compactTile = tileFactory.createCompactTileListItem(
                tile,
                currentBingo,  // Pass current bingo for hover card
                plugin.getItemManager(),  // Pass ItemManager for hover card
                onTileClickAction,
                this::removePinnedTile
            );
            compactTile.setAlignmentX(Component.LEFT_ALIGNMENT);
            pinnedTilesContainer.add(compactTile);

            if (i < pinnedTiles.size() - 1) {
                pinnedTilesContainer.add(Box.createVerticalStrut(3));
            }
        }

        // Add vertical spacing before the "Add More" button
        pinnedTilesContainer.add(Box.createVerticalStrut(8));

        // Add "Add More" button
        JPanel addButtonWrapper = createAddMoreButton();
        addButtonWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        pinnedTilesContainer.add(addButtonWrapper);

        pinnedTilesSection.setVisible(true);
    }

    /**
     * Creates the "Add More" button panel.
     */
    private JPanel createAddMoreButton() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new CompoundBorder(
            new LineBorder(ColorPalette.BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JButton addButton = new JButton("📌 Pin More Tiles");
        addButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        addButton.setFocusPainted(false);
        addButton.setContentAreaFilled(false);
        addButton.setBorder(new EmptyBorder(6, 0, 6, 0));
        addButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        addButton.setForeground(Color.LIGHT_GRAY);
        addButton.setHorizontalAlignment(SwingConstants.CENTER);
        addButton.setToolTipText("Open the bingo board to pin more tiles");

        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                addButton.setContentAreaFilled(true);
                addButton.setBackground(ColorPalette.ACCENT_BLUE);
                addButton.setForeground(Color.WHITE);
                buttonPanel.setBorder(new CompoundBorder(
                    new LineBorder(ColorPalette.ACCENT_BLUE, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
                ));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                addButton.setContentAreaFilled(false);
                addButton.setForeground(Color.LIGHT_GRAY);
                buttonPanel.setBorder(new CompoundBorder(
                    new LineBorder(ColorPalette.BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });

        addButton.addActionListener(e -> onShowBoardAction.run());

        buttonPanel.add(addButton, BorderLayout.CENTER);
        return buttonPanel;
    }
}
