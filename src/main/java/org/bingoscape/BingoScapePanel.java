package org.bingoscape;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.EventData;
import org.bingoscape.models.Role;
import org.bingoscape.models.TeamMember;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.ColorPalette;
import org.bingoscape.ui.UIConstants;
import org.bingoscape.ui.UIEffects;
import org.bingoscape.ui.ButtonFactory;
import org.bingoscape.ui.PinnedTilesManager;
import org.bingoscape.ui.TileListItemFactory;
import org.bingoscape.ui.ScreenshotHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import javax.swing.DefaultListCellRenderer;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.DefaultComboBoxModel;

public class BingoScapePanel extends PluginPanel {
    // Note: Layout and color constants are in UIConstants and ColorPalette classes

    // Components
    private final JPanel mainContentPanel = new JPanel();
    private final JPanel headerPanel = new JPanel();
    private JPanel eventsPanel;
    private JPanel bingoPanel;
    private final JPanel eventDetailsPanel = new JPanel();
    private final JComboBox<EventData> eventSelector = new JComboBox<>();
    private final JComboBox<Bingo> bingoSelector = new JComboBox<>();
    private JButton showBingoBoardButton;
    private JButton reloadEventsButton;
    private JLabel loadingLabel;
    private final Timer fadeTimer;

    // UI components for enhanced functionality
    private JButton screenshotButton;
    private JButton refreshButton;

    // Managers
    private PinnedTilesManager pinnedTilesManager;
    private TileListItemFactory tileFactory;
    private ScreenshotHandler screenshotHandler;

    // Reference to plugin and other resources
    private final BingoScapePlugin plugin;
    private final ScheduledExecutorService executor;
    private Bingo currentBingo;
    private BingoBoardWindow bingoBoardWindow;

    public BingoScapePanel(BingoScapePlugin plugin) {
        super();
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        // Initialize fade timer
        this.fadeTimer = new Timer(UIConstants.FADE_TIMER_DELAY, null);
        fadeTimer.setRepeats(true);

        // Initialize managers and factories
        this.tileFactory = new TileListItemFactory(plugin);
        this.screenshotHandler = new ScreenshotHandler(plugin, this);
        this.pinnedTilesManager = new PinnedTilesManager(
            plugin,
            this::showTileQuickActionsForPinnedTile,
            this::showBingoBoardFromPinnedTiles
        );

        // Initialize components
        initializeComponents();

        // Build the complete panel
        buildPanel();
    }
    
    private void initializeComponents() {
        // Create all buttons
        showBingoBoardButton = createShowBingoBoardButton();
        reloadEventsButton = createReloadEventsButton();
        screenshotButton = createScreenshotButton();
        refreshButton = createRefreshButton();
        loadingLabel = createLoadingLabel();

        // Configure dropdowns
        configureEventSelector();
        configureBingoSelector();
    }
    
    private void buildPanel() {
        // Clean panel setup
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // Create main container
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        // Add all sections
        container.add(createHeaderSection());
        container.add(createSpacing(12));
        container.add(createEventSection());
        container.add(createSpacing(8));
        container.add(createBoardSection());
        container.add(createSpacing(8));
        container.add(createShowBoardButtonSection());
        container.add(createSpacing(12));
        container.add(createPinnedTilesSection());
        
        // Add glue to push everything to top
        container.add(Box.createVerticalGlue());
        
        add(container, BorderLayout.CENTER);

        // Initially hide board section
        bingoPanel.setVisible(false);
    }

    // Helper methods for clean panel building
    private Component createSpacing(int height) {
        return Box.createVerticalStrut(height);
    }
    
    private JPanel createSection(String title, JComponent content) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBackground(ColorScheme.DARK_GRAY_COLOR);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        if (title != null) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            section.add(titleLabel, BorderLayout.NORTH);
        }
        
        if (content != null) {
            section.add(content, BorderLayout.CENTER);
        }
        
        return section;
    }
    
    private JPanel createHeaderSection() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorPalette.HEADER_BG);
        header.setBorder(new CompoundBorder(
            new LineBorder(ColorPalette.BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Left: Branding
        JPanel branding = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        branding.setBackground(ColorPalette.HEADER_BG);
        
        JLabel icon = new JLabel("🎯");
        icon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        
        JLabel name = new JLabel("BingoScape");
        name.setForeground(ColorPalette.GOLD);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        name.setBorder(new EmptyBorder(0, 6, 0, 0));
        
        branding.add(icon);
        branding.add(name);
        
        // Right: Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setBackground(ColorPalette.HEADER_BG);
        actions.add(screenshotButton);
        
        header.add(branding, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createEventSection() {
        eventsPanel = new JPanel(new BorderLayout(0, 6));
        eventsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eventsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Header with reload button
        JPanel eventHeader = new JPanel(new BorderLayout());
        eventHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        JLabel eventLabel = new JLabel("🏆 Event");
        eventLabel.setForeground(Color.WHITE);
        eventLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.add(loadingLabel);
        buttonPanel.add(reloadEventsButton);
        
        eventHeader.add(eventLabel, BorderLayout.WEST);
        eventHeader.add(buttonPanel, BorderLayout.EAST);
        
        eventsPanel.add(eventHeader, BorderLayout.NORTH);
        eventsPanel.add(eventSelector, BorderLayout.CENTER);
        
        return eventsPanel;
    }
    
    private JPanel createBoardSection() {
        bingoPanel = createSection("📋 Board", bingoSelector);
        return bingoPanel;
    }
    
    private JPanel createShowBoardButtonSection() {
        JPanel buttonSection = new JPanel(new BorderLayout());
        buttonSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonSection.add(showBingoBoardButton, BorderLayout.CENTER);
        return buttonSection;
    }
    
    private JPanel createPinnedTilesSection() {
        // Use the PinnedTilesManager to create and manage the pinned tiles section
        return pinnedTilesManager.getPinnedTilesSection();
    }

    private JLabel createLoadingLabel() {
        JLabel label = new JLabel("Loading events...");
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        label.setVisible(false);
        return label;
    }




    private void configureEventSelector() {
        eventSelector.setRenderer(createEventRenderer());
        eventSelector.addActionListener(createEventSelectionListener());
        eventSelector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        eventSelector.setForeground(Color.WHITE);
        eventSelector.setFocusable(false);
    }

    private DefaultListCellRenderer createEventRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EventData) {
                    EventData event = (EventData) value;
                    StringBuilder text = new StringBuilder();

                    // Status indicator
                    if (event.isLocked()) {
                        text.append("🔒 ");
                    } else {
                        text.append("🏆 ");
                    }

                    text.append(event.getTitle());

                    // Add board count if available
                    if (event.getBingos() != null && !event.getBingos().isEmpty()) {
                        text.append(" (").append(event.getBingos().size()).append(")");
                    }

                    setText(text.toString());
                }

                if (isSelected) {
                    setBackground(ColorPalette.ACCENT_BLUE);
                } else {
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                setForeground(Color.WHITE);
                setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

                return this;
            }
        };
    }

    private ActionListener createEventSelectionListener() {
        return e -> {
            EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
            if (selectedEvent != null) {
                executor.submit(() -> plugin.setEventDetails(selectedEvent));
            }
        };
    }

    private static class SelectionState {
        final String eventId;
        final String bingoId;

        SelectionState(EventData event, Bingo bingo) {
            this.eventId = event != null ? event.getId().toString() : null;
            this.bingoId = bingo != null ? bingo.getId().toString() : null;
        }
    }

    private SelectionState saveCurrentSelections() {
        EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
        Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
        return new SelectionState(selectedEvent, selectedBingo);
    }

    private void restoreSelections(SelectionState state) {
        if (state.eventId != null) {
            restoreEventSelection(state.eventId);
        }
        if (state.bingoId != null) {
            restoreBingoSelection(state.bingoId);
        }
    }

    private void restoreEventSelection(String eventId) {
        for (int i = 0; i < eventSelector.getItemCount(); i++) {
            EventData event = eventSelector.getItemAt(i);
            if (event.getId().toString().equals(eventId)) {
                eventSelector.setSelectedIndex(i);
                break;
            }
        }
    }

    private void restoreBingoSelection(String bingoId) {
        for (int i = 0; i < bingoSelector.getItemCount(); i++) {
            Bingo bingo = bingoSelector.getItemAt(i);
            if (bingo.getId().toString().equals(bingoId)) {
                bingoSelector.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clearFadeTimerListeners() {
        for (ActionListener listener : fadeTimer.getActionListeners()) {
            fadeTimer.removeActionListener(listener);
        }
    }

    private void setUIElementsEnabled(boolean enabled) {
        reloadEventsButton.setEnabled(enabled);
        eventSelector.setEnabled(enabled);
        loadingLabel.setVisible(!enabled);
    }

    private JButton createReloadEventsButton() {
        JButton button = ButtonFactory.createIconButton("🔄", "Reload Events", UIConstants.BUTTON_SIZE);
        button.addActionListener(e -> handleReloadButtonClick());
        return button;
    }

    private JButton createScreenshotButton() {
        JButton button = ButtonFactory.createIconButton("📷", "Take Screenshot", UIConstants.QUICK_ACTION_BUTTON_SIZE);
        button.addActionListener(e -> openScreenshotDialog());
        return button;
    }


    private JButton createRefreshButton() {
        JButton button = ButtonFactory.createIconButton("⚙️", "Settings", UIConstants.QUICK_ACTION_BUTTON_SIZE);
        button.addActionListener(e -> refreshCurrentBoard());
        return button;
    }



    private void handleReloadButtonClick() {
        SelectionState savedState = saveCurrentSelections();
        setUIElementsEnabled(false);
        startFadeOutAnimation(savedState);
    }

    private void startFadeOutAnimation(SelectionState savedState) {
        clearFadeTimerListeners();
        fadeTimer.addActionListener(evt -> {
            float alpha = eventSelector.getForeground().getAlpha() - UIConstants.FADE_STEP;
            if (alpha <= 0) {
                fadeTimer.stop();
                executor.submit(() -> {
                    plugin.fetchActiveEvents();
                    SwingUtilities.invokeLater(() -> completeReload(savedState));
                });
            } else {
                eventSelector.setForeground(new Color(ColorPalette.WHITE.getRed(), ColorPalette.WHITE.getGreen(), ColorPalette.WHITE.getBlue(), (int)alpha));
            }
        });
        fadeTimer.start();
    }

    private void completeReload(SelectionState savedState) {
        setUIElementsEnabled(true);
        restoreSelections(savedState);
        startFadeInAnimation();
    }

    private void startFadeInAnimation() {
        clearFadeTimerListeners();
        fadeTimer.addActionListener(evt -> {
            float fadeInAlpha = eventSelector.getForeground().getAlpha() + UIConstants.FADE_STEP;
            if (fadeInAlpha >= UIConstants.MAX_ALPHA) {
                fadeTimer.stop();
            }
            eventSelector.setForeground(new Color(ColorPalette.WHITE.getRed(), ColorPalette.WHITE.getGreen(), ColorPalette.WHITE.getBlue(), (int)fadeInAlpha));
        });
        fadeTimer.start();
    }


    private void configureBingoSelector() {
        bingoSelector.setRenderer(createBingoRenderer());
        bingoSelector.addActionListener(createBingoSelectionListener());
        bingoSelector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bingoSelector.setForeground(Color.WHITE);
        bingoSelector.setFocusable(false);
    }

    private DefaultListCellRenderer createBingoRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bingo) {
                    Bingo bingo = (Bingo) value;
                    StringBuilder text = new StringBuilder();

                    // Add board type icon
                    if ("progression".equals(bingo.getBingoType())) {
                        text.append("🎯 "); // Target for progressive
                    } else {
                        text.append("📋 "); // Clipboard for standard
                    }

                    text.append(bingo.getTitle());

                    // Show if the bingo is locked
                    if (bingo.isLocked()) {
                        text.append(" 🔒");
                    }

                    // Show pin icon if this bingo is pinned
                    if (bingo.getId().toString().equals(plugin.getConfig().pinnedBingoId())) {
                        text.append(" 📌");
                    }

                    setText(text.toString());
                }

                if (isSelected) {
                    setBackground(ColorPalette.ACCENT_BLUE);
                } else {
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                setForeground(Color.WHITE);
                setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

                return this;
            }
        };
    }

    private ActionListener createBingoSelectionListener() {
        return e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo != null) {
                // Update the current bingo in the plugin first to ensure the overlay gets updated
                plugin.selectBingo(selectedBingo);
                // Update our current bingo reference and refresh pinned tiles
                currentBingo = selectedBingo;
                refreshPinnedTiles();
            }
        };
    }

    private JButton createShowBingoBoardButton() {
        JButton button = ButtonFactory.createPrimaryButton("Show Bingo Board", "Open bingo board window");
        button.setPreferredSize(new Dimension(0, 36));

        button.addActionListener(e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo != null) {
                showBingoBoardWindow(selectedBingo);
            }
        });
        return button;
    }

    private void showBingoBoardWindow(Bingo bingo) {
        // Ensure the plugin's currentBingo is set for auto-submission
        plugin.selectBingo(bingo);

        SwingUtilities.invokeLater(() -> {
            if (bingoBoardWindow != null) {
                bingoBoardWindow.dispose();
            }

            bingoBoardWindow = new BingoBoardWindow(plugin, bingo);
            bingoBoardWindow.setVisible(true);
        });
    }

    /**
     * Helper method for PinnedTilesManager to show bingo board.
     */
    private void showBingoBoardFromPinnedTiles() {
        Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
        if (selectedBingo != null) {
            showBingoBoardWindow(selectedBingo);
        }
    }

    public void updateEventsList(List<EventData> events) {
        SwingUtilities.invokeLater(() -> {
            // Store current selections
            EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
            String selectedEventId = selectedEvent != null ? selectedEvent.getId().toString() : null;
            String pinnedBingoId = plugin.getConfig().pinnedBingoId();

            // Update the event selector model
            DefaultComboBoxModel<EventData> model = new DefaultComboBoxModel<>();
            for (EventData event : events) {
                model.addElement(event);
                // If this event contains the pinned bingo, select it
                if (!pinnedBingoId.isEmpty() && event.getBingos().stream()
                        .anyMatch(b -> b.getId().toString().equals(pinnedBingoId))) {
                    selectedEventId = event.getId().toString();
                }
            }
            eventSelector.setModel(model);

            // Restore selection if possible
            if (selectedEventId != null) {
                for (int i = 0; i < model.getSize(); i++) {
                    EventData event = model.getElementAt(i);
                    if (event.getId().toString().equals(selectedEventId)) {
                        eventSelector.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (model.getSize() > 0) {
                eventSelector.setSelectedIndex(0);
            }

            // Update UI state
            eventSelector.setEnabled(model.getSize() > 0);
            reloadEventsButton.setEnabled(true);
            loadingLabel.setVisible(false);
        });
    }

    // Method to update event details with enhanced information
    public void updateEventDetails(EventData eventData) {
        if (eventData == null) {
            eventDetailsPanel.setVisible(false);
            bingoPanel.setVisible(false);
            return;
        }

        // Update bingo selector
        bingoSelector.removeAllItems();
        String pinnedBingoId = plugin.getConfig().pinnedBingoId();
        Bingo pinnedBingo = null;

        // First pass to find pinned bingo if it exists
        if (!pinnedBingoId.isEmpty()) {
            for (Bingo bingo : eventData.getBingos()) {
                if (bingo.getId().toString().equals(pinnedBingoId)) {
                    pinnedBingo = bingo;
                    break;
                }
            }
        }

        // Add all bingos, with pinned one first if it exists
        if (pinnedBingo != null) {
            bingoSelector.addItem(pinnedBingo);
        }
        for (Bingo bingo : eventData.getBingos()) {
            if (pinnedBingo == null || !bingo.getId().equals(pinnedBingo.getId())) {
                bingoSelector.addItem(bingo);
            }
        }

        // Select pinned bingo if it exists, otherwise first bingo
        if (pinnedBingo != null) {
            bingoSelector.setSelectedItem(pinnedBingo);
            currentBingo = pinnedBingo;
        } else if (bingoSelector.getItemCount() > 0) {
            bingoSelector.setSelectedIndex(0);
            currentBingo = (Bingo) bingoSelector.getSelectedItem();
        }

        // Refresh pinned tiles for the new bingo
        refreshPinnedTiles();

        // Rest of the event details update...
        // ... existing code ...

        SwingUtilities.invokeLater(() -> {
            // Clear previous event details
            eventDetailsPanel.removeAll();
            bingoSelector.removeAllItems();

            // Build event details panel
            if (eventData != null) {
                // Create compact event card
                JPanel eventCard = createEventCard(eventData);
                eventDetailsPanel.add(eventCard);


                eventDetailsPanel.setVisible(true);

                // Populate bingo selector
                if (eventData.getBingos() != null && !eventData.getBingos().isEmpty()) {
                    for (Bingo bingo : eventData.getBingos()) {
                        bingoSelector.addItem(bingo);
                    }
                    bingoPanel.setVisible(true);
                } else {
                    bingoPanel.setVisible(false);
                }
            } else {
                eventDetailsPanel.setVisible(false);
                bingoPanel.setVisible(false);
            }

            revalidate();
            repaint();
        });
    }

    private void addInfoLabel(JPanel container, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(label);
        container.add(Box.createVerticalStrut(4));
    }

    private void addCompactInfoRow(JPanel container, String icon, String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setBorder(new EmptyBorder(1, 0, 1, 0));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        iconLabel.setPreferredSize(new Dimension(16, 16));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        textLabel.setBorder(new EmptyBorder(0, 4, 0, 0)); // Small left margin for text

        row.add(iconLabel, BorderLayout.WEST);
        row.add(textLabel, BorderLayout.CENTER);
        container.add(row);
    }

    // Helper method to format GP amounts nicely
    private String formatGpAmount(long amount) {
        if (amount >= 1_000_000_000) {
            return (amount / 1_000_000_000) + "B GP";
        } else if (amount >= 1_000_000) {
            return (amount / 1_000_000) + "M GP";
        } else if (amount >= 1_000) {
            return (amount / 1_000) + "K GP";
        } else {
            return amount + " GP";
        }
    }

    private String formatRole(Role role) {
        if (role == null) return "Participant";

        switch (role) {
            case ADMIN:
                return "Admin";
            case MANAGEMENT:
                return "Manager";
            case PARTICIPANT:
                return "Participant";
            default:
                return role.toString();
        }
    }

    // New methods for enhanced functionality
    private void openScreenshotDialog() {
        screenshotHandler.openScreenshotDialog();
    }

    private void showScreenshotPreviewDialog(byte[] screenshotBytes) {
        JDialog previewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Screenshot Preview", true);
        previewDialog.setSize(600, 500);
        previewDialog.setLocationRelativeTo(this);
        previewDialog.setLayout(new BorderLayout());

        // Display the screenshot
        JLabel screenshotLabel = new JLabel(new ImageIcon(screenshotBytes));
        JScrollPane scrollPane = new JScrollPane(screenshotLabel);
        scrollPane.setPreferredSize(new Dimension(580, 400));
        previewDialog.add(scrollPane, BorderLayout.CENTER);

        // Add info label
        JLabel infoLabel = new JLabel("Screenshot taken! You can save this image or use it for manual submission.");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        previewDialog.add(infoLabel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton saveButton = ButtonFactory.createPrimaryButton("💾 Save Image", "Save screenshot to file");
        JButton closeButton = ButtonFactory.createSecondaryButton("Close", "Close preview");

        saveButton.addActionListener(e -> {
            saveScreenshotToFile(screenshotBytes);
        });

        closeButton.addActionListener(e -> previewDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        previewDialog.setVisible(true);
    }

    private void saveScreenshotToFile(byte[] screenshotBytes) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG files", "png"));
        fileChooser.setSelectedFile(new File("bingoscape_screenshot.png"));
        fileChooser.setDialogTitle("Save Screenshot");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Ensure .png extension
                if (!selectedFile.getName().toLowerCase().endsWith(".png")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".png");
                }

                java.nio.file.Files.write(selectedFile.toPath(), screenshotBytes);
                JOptionPane.showMessageDialog(this,
                    "Screenshot saved to: " + selectedFile.getAbsolutePath(),
                    "Screenshot Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Failed to save screenshot: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showTileScreenshotPreviewDialog(Tile tile, byte[] screenshotBytes) {
        screenshotHandler.showTileScreenshotPreviewDialog(tile, screenshotBytes);
    }


    private void refreshCurrentBoard() {
        Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
        if (selectedBingo != null && bingoBoardWindow != null && bingoBoardWindow.isVisible()) {
            // Use selectBingo to ensure requirement matcher is updated
            plugin.selectBingo(selectedBingo);
        }
    }

    public void addPinnedTile(Tile tile) {
        pinnedTilesManager.addPinnedTile(tile);
        // Update legacy field for backward compatibility
        // Sync with pinnedTilesManager (no longer needed with direct manager usage)
    }

    public void removePinnedTile(String tileId) {
        pinnedTilesManager.removePinnedTile(tileId);
        // Update legacy field for backward compatibility
        // Sync with pinnedTilesManager (no longer needed with direct manager usage)
    }

    public boolean isPinnedTile(String tileId) {
        return pinnedTilesManager.getPinnedTileIds().contains(tileId);
    }

    public void refreshPinnedTiles() {
        pinnedTilesManager.refreshPinnedTiles(currentBingo);
        // Update legacy field for backward compatibility
        // Sync with pinnedTilesManager (no longer needed with direct manager usage)
    }

    /**
     * Shows quick actions menu for a pinned tile (called from PinnedTilesManager).
     * Uses mouse position for popup location.
     */
    private void showTileQuickActionsForPinnedTile(Tile tile) {
        JPopupMenu popup = createTileActionsPopup(tile);

        // Show at mouse position
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, this);
        popup.show(this, mousePos.x, mousePos.y);
    }

    /**
     * Shows quick actions menu for a tile with specific component positioning.
     */
    private void showTileQuickActions(Tile tile, JPanel miniTile) {
        JPopupMenu popup = createTileActionsPopup(tile);

        if (miniTile != null) {
            popup.show(miniTile, miniTile.getWidth() / 2, miniTile.getHeight() / 2);
        }
    }

    /**
     * Creates the popup menu with actions for a tile.
     */
    private JPopupMenu createTileActionsPopup(Tile tile) {
        JPopupMenu popup = new JPopupMenu();

        // Only show screenshot option if tile is not already accepted
        boolean isAccepted = tile.getSubmission() != null &&
                           tile.getSubmission().getStatus() == TileSubmissionType.ACCEPTED;

        if (!isAccepted) {
            JMenuItem screenshotItem = new JMenuItem("📷 Quick Screenshot");
            screenshotItem.addActionListener(e -> {
                plugin.takeScreenshot(tile.getId(), (screenshotBytes) -> {
                    if (screenshotBytes != null) {
                        SwingUtilities.invokeLater(() -> {
                            showTileScreenshotPreviewDialog(tile, screenshotBytes);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this,
                                "Failed to take screenshot for " + tile.getTitle(),
                                "Screenshot Error",
                                JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });
            });
            popup.add(screenshotItem);
        }

        JMenuItem detailsItem = new JMenuItem("ℹ️ View Details");
        detailsItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                "<html><b>" + tile.getTitle() + "</b><br>" +
                "XP: " + tile.getWeight() + "<br>" +
                "Description: " + (tile.getDescription() != null ? tile.getDescription() : "No description") +
                "</html>",
                "Tile Details",
                JOptionPane.INFORMATION_MESSAGE);
        });
        popup.add(detailsItem);

        popup.addSeparator();

        JMenuItem unpinItem = new JMenuItem("📌 Unpin Tile");
        unpinItem.addActionListener(e -> removePinnedTile(tile.getId().toString()));
        popup.add(unpinItem);

        return popup;
    }

    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
    }

    public void displayBingoBoard(Bingo bingo) {
        // Update the current bingo reference
        currentBingo = bingo;

        // Refresh pinned tiles to show updated progress
        refreshPinnedTiles();

        // Update the bingo board window if it's open
        if (bingoBoardWindow != null && bingoBoardWindow.isVisible()) {
            // Update existing window smoothly instead of recreating
            SwingUtilities.invokeLater(() -> {
                bingoBoardWindow.updateBingoBoard(bingo);
            });
        }
    }

    private JPanel createEventCard(EventData eventData) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(ColorPalette.CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(ColorPalette.BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        // Top row: Title and status
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(ColorPalette.CARD_BG);

        JLabel titleLabel = new JLabel(eventData.getTitle());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        titleLabel.setForeground(ColorPalette.GOLD);

        // Status indicator
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        statusPanel.setBackground(ColorPalette.CARD_BG);

        Color statusColor = eventData.isLocked() ? Color.LIGHT_GRAY : ColorPalette.SUCCESS;
        String statusText = eventData.isLocked() ? "Locked" : "Active";

        JLabel statusDot = new JLabel("●");
        statusDot.setForeground(statusColor);
        statusDot.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setForeground(statusColor);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));

        statusPanel.add(statusDot);
        statusPanel.add(statusLabel);

        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(statusPanel, BorderLayout.EAST);

        // Bottom row: Key info - simplified
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        bottomRow.setBackground(ColorPalette.CARD_BG);

        if (eventData.getUserTeam() != null) {
            addSimpleInfoChip(bottomRow, "👥", eventData.getUserTeam().getName());
        }

        if (eventData.getBingos() != null && !eventData.getBingos().isEmpty()) {
            addSimpleInfoChip(bottomRow, "📋", eventData.getBingos().size() + " boards");
        }

        card.add(topRow, BorderLayout.NORTH);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    private void addInfoChip(JPanel container, String icon, String text) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chip.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        chip.setBorder(new CompoundBorder(
            new LineBorder(ColorPalette.BORDER, 1, true),
            new EmptyBorder(2, 6, 2, 6)
        ));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.LIGHT_GRAY);
        textLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        chip.add(iconLabel);
        chip.add(textLabel);

        container.add(chip);
        container.add(Box.createHorizontalStrut(6));
    }

    private void addSimpleInfoChip(JPanel container, String icon, String text) {
        JLabel chip = new JLabel(icon + " " + text);
        chip.setForeground(Color.LIGHT_GRAY);
        chip.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));

        container.add(chip);
        container.add(Box.createHorizontalStrut(8));
    }

    private boolean hasDetailedInfo(EventData eventData) {
        return (eventData.getDescription() != null && !eventData.getDescription().trim().isEmpty()) ||
               eventData.getBasePrizePool() > 0 ||
               (eventData.getStartDate() != null && eventData.getEndDate() != null);
    }

    private JPanel createExpandableDetailsSection(EventData eventData) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(ColorScheme.DARK_GRAY_COLOR);
        section.setBorder(new EmptyBorder(4, 0, 0, 0));

        // Collapsible header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel detailsLabel = new JLabel("ℹ️ Details");
        detailsLabel.setForeground(Color.LIGHT_GRAY);
        detailsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        JLabel expandIcon = new JLabel("▶");
        expandIcon.setForeground(Color.LIGHT_GRAY);
        expandIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));

        header.add(detailsLabel, BorderLayout.WEST);
        header.add(expandIcon, BorderLayout.EAST);

        // Details content (initially hidden)
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        content.setBorder(new EmptyBorder(8, 12, 8, 12));
        content.setVisible(false);

        if (eventData.getDescription() != null && !eventData.getDescription().trim().isEmpty()) {
            JLabel descLabel = new JLabel("<html><div style='width:180px;'>" + eventData.getDescription() + "</div></html>");
            descLabel.setForeground(Color.WHITE);
            descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            content.add(descLabel);
            content.add(Box.createVerticalStrut(8));
        }

        if (eventData.getStartDate() != null && eventData.getEndDate() != null) {
            addCompactInfoRow(content, "📅",
                UIConstants.DATE_FORMAT.format(eventData.getStartDate()) + " - " +
                UIConstants.DATE_FORMAT.format(eventData.getEndDate()));
        }

        if (eventData.getBasePrizePool() > 0) {
            addCompactInfoRow(content, "💰", formatGpAmount(eventData.getBasePrizePool()));
        }

        // Toggle functionality
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean isVisible = content.isVisible();
                content.setVisible(!isVisible);
                expandIcon.setText(isVisible ? "▶" : "▼");
                section.revalidate();
                section.repaint();
            }
        });

        section.add(header, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);

        return section;
    }
}
