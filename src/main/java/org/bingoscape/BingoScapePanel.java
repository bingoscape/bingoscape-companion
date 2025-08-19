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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
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
    // Layout Constants
    private static final int BORDER_SPACING = 10;
    private static final int COMPONENT_SPACING = 12;
    private static final int SECTION_SPACING = 14;
    private static final int CARD_SPACING = 10;
    private static final int PINNED_SECTION_SPACING = 8;
    private static final int QUICK_ACTION_SPACING = 6;

    // Button Constants
    private static final int BUTTON_SIZE = 24;
    private static final int QUICK_ACTION_BUTTON_SIZE = 28;
    private static final int MINI_TILE_SIZE = 50;
    private static final int PINNED_TILE_HEIGHT = 55;
    private static final int FADE_STEP = 10;
    private static final int FADE_TIMER_DELAY = 50;
    private static final int MAX_ALPHA = 255;

    // UI Constants
    private static final String NO_EVENTS_TEXT = "No active events found";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    // Color Constants
    private static final Color GOLD_COLOR = new Color(255, 215, 0);
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color WARNING_YELLOW = new Color(234, 179, 8);
    private static final Color ERROR_RED = new Color(239, 68, 68);
    private static final Color PINNED_TILE_BG = new Color(45, 55, 72);
    private static final Color CARD_BG = new Color(55, 65, 81);
    private static final Color HEADER_BG = new Color(31, 41, 55);
    private static final Color BORDER_COLOR = new Color(75, 85, 99);

    // Components
    private final JPanel mainContentPanel = new JPanel(); // Main container for all content
    private final JPanel headerPanel = new JPanel(); // New consolidated header
    private JPanel pinnedTilesSection; // New pinned tiles section
    private JPanel eventsPanel;
    private JPanel bingoPanel;
    private final JPanel eventDetailsPanel = new JPanel();
    private final JComboBox<EventData> eventSelector = new JComboBox<>();
    private final JComboBox<Bingo> bingoSelector = new JComboBox<>();
    private JButton showBingoBoardButton;
    private JButton reloadEventsButton; // New reload events button
    private JLabel loadingLabel; // New loading label
    private final Timer fadeTimer; // Timer for smooth transitions

    // New UI components for enhanced functionality
    private JButton screenshotButton;
    private JButton refreshButton;
    private JScrollPane pinnedTilesScrollPane;
    private JPanel pinnedTilesContainer;

    // Pinned tiles management
    public final Set<String> pinnedTileIds = new HashSet<>();
    private final List<Tile> pinnedTiles = new ArrayList<>();

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
        this.fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.setRepeats(true);

        // Load pinned tiles from config
        loadPinnedTilesFromConfig();

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

        // Setup pinned tiles container
        pinnedTilesContainer = new JPanel();
        pinnedTilesContainer.setLayout(new BoxLayout(pinnedTilesContainer, BoxLayout.Y_AXIS));
        pinnedTilesContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        pinnedTilesScrollPane = new JScrollPane(pinnedTilesContainer);
        pinnedTilesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pinnedTilesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pinnedTilesScrollPane.setBorder(null);
        pinnedTilesScrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

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
        
        // Initially hide board section and pinned section
        bingoPanel.setVisible(false);
        pinnedTilesSection.setVisible(false);
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
        header.setBackground(HEADER_BG);
        header.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Left: Branding
        JPanel branding = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        branding.setBackground(HEADER_BG);
        
        JLabel icon = new JLabel("🎯");
        icon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        
        JLabel name = new JLabel("BingoScape");
        name.setForeground(GOLD_COLOR);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        name.setBorder(new EmptyBorder(0, 6, 0, 0));
        
        branding.add(icon);
        branding.add(name);
        
        // Right: Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setBackground(HEADER_BG);
        actions.add(screenshotButton);
        actions.add(refreshButton);
        
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
        pinnedTilesSection = new JPanel(new BorderLayout(0, 8));
        pinnedTilesSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pinnedTilesSection.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));
        pinnedTilesSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Header
        JPanel pinnedHeader = new JPanel(new BorderLayout());
        pinnedHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pinnedHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel pinnedLabel = new JLabel("📌 Pinned");
        pinnedLabel.setForeground(Color.WHITE);
        pinnedLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        
        JLabel collapseIcon = new JLabel("▼");
        collapseIcon.setForeground(Color.LIGHT_GRAY);
        
        pinnedHeader.add(pinnedLabel, BorderLayout.WEST);
        pinnedHeader.add(collapseIcon, BorderLayout.EAST);
        
        // Toggle functionality
        pinnedHeader.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean isVisible = pinnedTilesScrollPane.isVisible();
                pinnedTilesScrollPane.setVisible(!isVisible);
                collapseIcon.setText(isVisible ? "▶" : "▼");
                revalidate();
                repaint();
            }
        });
        
        // Set scroll pane size - larger to show more tiles with overflow
        pinnedTilesScrollPane.setPreferredSize(new Dimension(0, 250));
        pinnedTilesScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400)); // Allow expansion up to 400px
        
        pinnedTilesSection.add(pinnedHeader, BorderLayout.NORTH);
        pinnedTilesSection.add(pinnedTilesScrollPane, BorderLayout.CENTER);
        
        updatePinnedTilesDisplay();
        
        return pinnedTilesSection;
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
                    setBackground(ACCENT_BLUE);
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
        JButton button = new JButton("🔄");
        button.setToolTipText("Reload Events");
        button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        button.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        button.setMinimumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(ACCENT_BLUE);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });

        button.addActionListener(e -> handleReloadButtonClick());

        return button;
    }

    private JButton createScreenshotButton() {
        JButton button = new JButton("📷");
        button.setToolTipText("Take Screenshot");
        button.setPreferredSize(new Dimension(QUICK_ACTION_BUTTON_SIZE, QUICK_ACTION_BUTTON_SIZE));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        button.setBackground(CARD_BG);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(true);
                button.setBackground(ACCENT_BLUE);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });

        button.addActionListener(e -> openScreenshotDialog());
        return button;
    }


    private JButton createRefreshButton() {
        JButton button = new JButton("⚙️");
        button.setToolTipText("Settings");
        button.setPreferredSize(new Dimension(QUICK_ACTION_BUTTON_SIZE, QUICK_ACTION_BUTTON_SIZE));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        button.setBackground(CARD_BG);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(true);
                button.setBackground(ACCENT_BLUE);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });

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
            float alpha = eventSelector.getForeground().getAlpha() - FADE_STEP;
            if (alpha <= 0) {
                fadeTimer.stop();
                executor.submit(() -> {
                    plugin.fetchActiveEvents();
                    SwingUtilities.invokeLater(() -> completeReload(savedState));
                });
            } else {
                eventSelector.setForeground(new Color(MAX_ALPHA, MAX_ALPHA, MAX_ALPHA, (int)alpha));
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
            float fadeInAlpha = eventSelector.getForeground().getAlpha() + FADE_STEP;
            if (fadeInAlpha >= MAX_ALPHA) {
                fadeTimer.stop();
            }
            eventSelector.setForeground(new Color(MAX_ALPHA, MAX_ALPHA, MAX_ALPHA, (int)fadeInAlpha));
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
                    setBackground(ACCENT_BLUE);
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
        JButton button = new JButton("Show Bingo Board");
        button.setBackground(ACCENT_BLUE);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(ACCENT_BLUE, 2, true));
        button.setPreferredSize(new Dimension(0, 32));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ACCENT_BLUE.brighter());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ACCENT_BLUE);
            }
        });

        button.addActionListener(e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo != null) {
                showBingoBoardWindow(selectedBingo);
            }
        });
        return button;
    }

    private void showBingoBoardWindow(Bingo bingo) {
        SwingUtilities.invokeLater(() -> {
            if (bingoBoardWindow != null) {
                bingoBoardWindow.dispose();
            }

            bingoBoardWindow = new BingoBoardWindow(plugin, bingo);
            bingoBoardWindow.setVisible(true);
        });
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
        // Take a screenshot of the RuneLite client
        plugin.takeScreenshot(null, (screenshotBytes) -> {
            if (screenshotBytes != null) {
                SwingUtilities.invokeLater(() -> {
                    showScreenshotPreviewDialog(screenshotBytes);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Failed to take screenshot.",
                        "Screenshot Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
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
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton saveButton = new JButton("Save Image");
        JButton closeButton = new JButton("Close");

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
        JDialog previewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Screenshot for " + tile.getTitle(), true);
        previewDialog.setSize(600, 550);
        previewDialog.setLocationRelativeTo(this);
        previewDialog.setLayout(new BorderLayout());

        // Display the screenshot
        JLabel screenshotLabel = new JLabel(new ImageIcon(screenshotBytes));
        JScrollPane scrollPane = new JScrollPane(screenshotLabel);
        scrollPane.setPreferredSize(new Dimension(580, 400));
        previewDialog.add(scrollPane, BorderLayout.CENTER);

        // Add info label with tile details
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html><center><b>" + tile.getTitle() + "</b><br>" +
                                     "Screenshot taken! You can submit, save, or close.</center></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        previewDialog.add(infoPanel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton submitButton = new JButton("Submit for " + tile.getTitle());
        JButton saveButton = new JButton("Save Image");
        JButton closeButton = new JButton("Close");

        submitButton.addActionListener(e -> {
            plugin.submitTileCompletionWithScreenshot(tile.getId(), screenshotBytes);
            previewDialog.dispose();
            JOptionPane.showMessageDialog(this,
                "Screenshot submitted for " + tile.getTitle(),
                "Submission Complete",
                JOptionPane.INFORMATION_MESSAGE);
        });

        saveButton.addActionListener(e -> {
            saveScreenshotToFile(screenshotBytes);
        });

        closeButton.addActionListener(e -> previewDialog.dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        previewDialog.setVisible(true);
    }


    private void refreshCurrentBoard() {
        Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
        if (selectedBingo != null && bingoBoardWindow != null && bingoBoardWindow.isVisible()) {
            displayBingoBoard(selectedBingo);
        }
    }

    public void addPinnedTile(Tile tile) {
        if (!pinnedTileIds.contains(tile.getId().toString())) {
            pinnedTileIds.add(tile.getId().toString());
            pinnedTiles.add(tile);
            savePinnedTilesToConfig();
            updatePinnedTilesDisplay();
        }
    }

    public void removePinnedTile(String tileId) {
        if (pinnedTileIds.remove(tileId)) {
            pinnedTiles.removeIf(tile -> tile.getId().toString().equals(tileId));
            savePinnedTilesToConfig();
            updatePinnedTilesDisplay();
        }
    }

    private void loadPinnedTilesFromConfig() {
        String configPinnedTileIds = plugin.getConfig().pinnedTileIds();
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

    private void savePinnedTilesToConfig() {
        String pinnedTileIdsString = String.join(",", pinnedTileIds);
        plugin.getConfig().pinnedTileIds(pinnedTileIdsString);
    }

    public void refreshPinnedTiles() {
        // Refresh pinned tiles when bingo data changes
        // Find tiles that match pinned IDs from current bingo
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

    private void updatePinnedTilesDisplay() {
        pinnedTilesContainer.removeAll();

        if (pinnedTiles.isEmpty()) {
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
        } else {
            // Create compact tile list items for better density
            for (int i = 0; i < pinnedTiles.size(); i++) {
                Tile tile = pinnedTiles.get(i);
                JPanel compactTile = createCompactTileListItem(tile);
                compactTile.setAlignmentX(Component.LEFT_ALIGNMENT);
                pinnedTilesContainer.add(compactTile);
                
                // Add smaller vertical spacing between tiles
                if (i < pinnedTiles.size() - 1) {
                    pinnedTilesContainer.add(Box.createVerticalStrut(3));
                }
            }

            // Add vertical spacing before the "Add More" button
            pinnedTilesContainer.add(Box.createVerticalStrut(8));

            // Add "Add More" button
            JPanel addButtonWrapper = createAddMoreButtonPanel();
            addButtonWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            pinnedTilesContainer.add(addButtonWrapper);

            pinnedTilesSection.setVisible(true);
        }

        revalidate();
        repaint();
    }

    private JPanel createDetailedTileListItem(Tile tile) {
        JPanel listItem = new JPanel(new BorderLayout());
        listItem.setBackground(PINNED_TILE_BG);
        listItem.setBorder(new CompoundBorder(
            new LineBorder(getStatusColor(tile), 2, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        listItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, listItem.getPreferredSize().height));

        // Left side: Header image (if available) or icon placeholder
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(40, 40));

        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            // Load actual header image (scaled down)
            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(40, 40));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setBorder(new LineBorder(BORDER_COLOR, 1, true));

            // Load image asynchronously
            SwingUtilities.invokeLater(() -> {
                try {
                    ImageIcon icon = new ImageIcon(new URL(tile.getHeaderImage()));
                    Image scaledImage = icon.getImage().getScaledInstance(38, 38, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                } catch (Exception e) {
                    // Fallback to text icon
                    imageLabel.setText("🎯");
                    imageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                }
            });

            imagePanel.add(imageLabel, BorderLayout.CENTER);
        } else {
            // Use emoji icon as placeholder with better styling
            JLabel iconLabel = new JLabel("🎯");
            iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setPreferredSize(new Dimension(40, 40));
            iconLabel.setBorder(new LineBorder(BORDER_COLOR, 1, true));
            iconLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            iconLabel.setOpaque(true);
            imagePanel.add(iconLabel, BorderLayout.CENTER);
        }

        // Center: Tile details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(new EmptyBorder(2, 12, 2, 8));

        // Title
        JLabel titleLabel = new JLabel(tile.getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(titleLabel);
        
        // Add small spacing after title
        detailsPanel.add(Box.createVerticalStrut(2));

        // Description (truncated if too long)
        if (tile.getDescription() != null && !tile.getDescription().trim().isEmpty()) {
            String description = tile.getDescription();
            if (description.length() > 80) {
                description = description.substring(0, 80) + "...";
            }
            JLabel descLabel = new JLabel("<html><div style='width: 140px;'>" + description + "</div></html>");
            descLabel.setForeground(new Color(180, 185, 190));
            descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(descLabel);
        }

        // Right side: Status and stats
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // XP Badge (always shown, most important info)
        JPanel xpBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        xpBadge.setOpaque(false);
        xpBadge.setBorder(new EmptyBorder(0, 0, 2, 0));

        JLabel xpLabel = new JLabel(tile.getWeight() + " XP");
        xpLabel.setForeground(GOLD_COLOR);
        xpLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        xpBadge.add(xpLabel);
        rightPanel.add(xpBadge);

        // Status indicator (if submitted)
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            statusBadge.setOpaque(false);
            statusBadge.setBorder(new EmptyBorder(2, 0, 2, 0));

            String statusText = getStatusText(tile.getSubmission().getStatus());
            JLabel statusLabel = new JLabel("● " + statusText);
            statusLabel.setForeground(getStatusColor(tile));
            statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            statusBadge.add(statusLabel);
            rightPanel.add(statusBadge);
        }

        // Tier info (if available and greater than 1)
        if (tile.getTier() != null && tile.getTier() > 1) {
            JPanel tierBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            tierBadge.setOpaque(false);
            tierBadge.setBorder(new EmptyBorder(2, 0, 0, 0));

            JLabel tierLabel = new JLabel("Tier " + tile.getTier());
            tierLabel.setForeground(new Color(156, 163, 175));
            tierLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            tierBadge.add(tierLabel);
            rightPanel.add(tierBadge);
        }

        // Assemble the list item
        listItem.add(imagePanel, BorderLayout.WEST);
        listItem.add(detailsPanel, BorderLayout.CENTER);
        listItem.add(rightPanel, BorderLayout.EAST);

        // Click handlers with improved feedback
        listItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Check if shift is pressed for unpinning (like in BingoBoardWindow)
                boolean shiftPressed = e.isShiftDown();
                
                if (shiftPressed) {
                    // Shift-click to unpin (matches BingoBoardWindow behavior)
                    removePinnedTile(tile.getId().toString());
                } else {
                    // Both left-click and right-click open context menu
                    showTileQuickActions(tile, listItem);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                Color brighterBg = new Color(
                    Math.min(255, PINNED_TILE_BG.getRed() + 15),
                    Math.min(255, PINNED_TILE_BG.getGreen() + 15), 
                    Math.min(255, PINNED_TILE_BG.getBlue() + 15)
                );
                listItem.setBackground(brighterBg);
                listItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                // Slightly brighten the border on hover
                listItem.setBorder(new CompoundBorder(
                    new LineBorder(getStatusColor(tile).brighter(), 2, true),
                    new EmptyBorder(10, 12, 10, 12)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                listItem.setBackground(PINNED_TILE_BG);
                listItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                
                // Reset border
                listItem.setBorder(new CompoundBorder(
                    new LineBorder(getStatusColor(tile), 2, true),
                    new EmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        // Enhanced tooltip with comprehensive information
        StringBuilder tooltipText = new StringBuilder("<html><b>" + tile.getTitle() + "</b><br>");
        
        if (tile.getDescription() != null && !tile.getDescription().trim().isEmpty()) {
            tooltipText.append("<br>").append(tile.getDescription()).append("<br>");
        }
        
        tooltipText.append("<br><b>Details:</b><br>");
        tooltipText.append("• XP: ").append(tile.getWeight()).append("<br>");
        
        if (tile.getTier() != null && tile.getTier() > 1) {
            tooltipText.append("• Tier: ").append(tile.getTier()).append("<br>");
        }
        
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            tooltipText.append("• Status: ").append(getStatusText(tile.getSubmission().getStatus())).append("<br>");
        }
        
        tooltipText.append("<br><i>Click for actions • Shift+Click to unpin</i>");
        tooltipText.append("</html>");
        
        listItem.setToolTipText(tooltipText.toString());

        return listItem;
    }
    
    private JPanel createCompactTileListItem(Tile tile) {
        JPanel listItem = new JPanel(new BorderLayout());
        listItem.setBackground(PINNED_TILE_BG);
        listItem.setBorder(new CompoundBorder(
            new LineBorder(getStatusColor(tile), 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        listItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Compact height
        
        // Left: Icon/Image (smaller)
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(24, 24));
        
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(24, 24));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            SwingUtilities.invokeLater(() -> {
                try {
                    ImageIcon icon = new ImageIcon(new URL(tile.getHeaderImage()));
                    Image scaledImage = icon.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                } catch (Exception e) {
                    imageLabel.setText("🎯");
                    imageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                }
            });
            imagePanel.add(imageLabel, BorderLayout.CENTER);
        } else {
            JLabel iconLabel = new JLabel("🎯");
            iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setPreferredSize(new Dimension(24, 24));
            imagePanel.add(iconLabel, BorderLayout.CENTER);
        }
        
        // Center: Title only (no description for compactness)
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        
        JLabel titleLabel = new JLabel(tile.getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Right: XP and Status
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);
        
        // Status indicator (if applicable)
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            JLabel statusDot = new JLabel("●");
            statusDot.setForeground(getStatusColor(tile));
            statusDot.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
            rightPanel.add(statusDot);
            rightPanel.add(Box.createHorizontalStrut(4));
        }
        
        // XP value
        JLabel xpLabel = new JLabel(tile.getWeight() + "XP");
        xpLabel.setForeground(GOLD_COLOR);
        xpLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        rightPanel.add(xpLabel);
        
        // Tier (if applicable)
        if (tile.getTier() != null && tile.getTier() > 1) {
            rightPanel.add(Box.createHorizontalStrut(4));
            JLabel tierLabel = new JLabel("T" + tile.getTier());
            tierLabel.setForeground(new Color(156, 163, 175));
            tierLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
            rightPanel.add(tierLabel);
        }
        
        // Assemble the compact list item
        listItem.add(imagePanel, BorderLayout.WEST);
        listItem.add(titlePanel, BorderLayout.CENTER);
        listItem.add(rightPanel, BorderLayout.EAST);
        
        // Click handlers (same as detailed version)
        listItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean shiftPressed = e.isShiftDown();
                
                if (shiftPressed) {
                    removePinnedTile(tile.getId().toString());
                } else {
                    showTileQuickActions(tile, listItem);
                }
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                Color brighterBg = new Color(
                    Math.min(255, PINNED_TILE_BG.getRed() + 10),
                    Math.min(255, PINNED_TILE_BG.getGreen() + 10), 
                    Math.min(255, PINNED_TILE_BG.getBlue() + 10)
                );
                listItem.setBackground(brighterBg);
                listItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                listItem.setBorder(new CompoundBorder(
                    new LineBorder(getStatusColor(tile).brighter(), 1, true),
                    new EmptyBorder(6, 8, 6, 8)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                listItem.setBackground(PINNED_TILE_BG);
                listItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                
                listItem.setBorder(new CompoundBorder(
                    new LineBorder(getStatusColor(tile), 1, true),
                    new EmptyBorder(6, 8, 6, 8)
                ));
            }
        });
        
        // Compact tooltip
        StringBuilder tooltipText = new StringBuilder("<html><b>" + tile.getTitle() + "</b><br>");
        tooltipText.append("XP: ").append(tile.getWeight());
        
        if (tile.getTier() != null && tile.getTier() > 1) {
            tooltipText.append(" • Tier ").append(tile.getTier());
        }
        
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
            tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            tooltipText.append("<br>Status: ").append(getStatusText(tile.getSubmission().getStatus()));
        }
        
        tooltipText.append("<br><i>Click for actions • Shift+Click to unpin</i>");
        tooltipText.append("</html>");
        
        listItem.setToolTipText(tooltipText.toString());
        
        return listItem;
    }

    private String getStatusText(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "Pending";
            case ACCEPTED: return "Completed";
            case REQUIRES_INTERACTION: return "Action Needed";
            case DECLINED: return "Declined";
            default: return "Not Submitted";
        }
    }

    private Color getStatusColor(Tile tile) {
        if (tile.getSubmission() == null || tile.getSubmission().getStatus() == null) {
            return BORDER_COLOR;
        }

        switch (tile.getSubmission().getStatus()) {
            case PENDING: return ACCENT_BLUE;
            case ACCEPTED: return SUCCESS_COLOR;
            case REQUIRES_INTERACTION: return WARNING_YELLOW;
            case DECLINED: return ERROR_RED;
            default: return BORDER_COLOR;
        }
    }

    private void showTileQuickActions(Tile tile, JPanel miniTile) {
        JPopupMenu popup = new JPopupMenu();

        // Only show screenshot option if tile is not already accepted
        boolean isAccepted = tile.getSubmission() != null && 
                           tile.getSubmission().getStatus() == TileSubmissionType.ACCEPTED;
                           
        if (!isAccepted) {
            JMenuItem screenshotItem = new JMenuItem("📷 Quick Screenshot");
            screenshotItem.addActionListener(e -> {
                // Take a screenshot for this specific tile
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

        popup.show(miniTile, miniTile.getWidth() / 2, miniTile.getHeight() / 2);
    }

    private JPanel createMiniTile(Tile tile) {
        JPanel miniTile = new JPanel(new BorderLayout());
        miniTile.setPreferredSize(new Dimension(MINI_TILE_SIZE, PINNED_TILE_HEIGHT));
        miniTile.setMaximumSize(new Dimension(MINI_TILE_SIZE, PINNED_TILE_HEIGHT));
        miniTile.setMinimumSize(new Dimension(MINI_TILE_SIZE, PINNED_TILE_HEIGHT));
        miniTile.setBackground(PINNED_TILE_BG);
        miniTile.setBorder(new CompoundBorder(
            new LineBorder(getStatusColor(tile), 2, true),
            new EmptyBorder(3, 3, 3, 3)
        ));

        // Top: Image or icon
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(MINI_TILE_SIZE - 6, 26));

        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            JLabel imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            // Load image asynchronously and scale it down
            SwingUtilities.invokeLater(() -> {
                try {
                    ImageIcon icon = new ImageIcon(new URL(tile.getHeaderImage()));
                    Image scaledImage = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                } catch (Exception e) {
                    imageLabel.setText("🎯");
                    imageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
                }
            });
            imagePanel.add(imageLabel, BorderLayout.CENTER);
        } else {
            JLabel iconLabel = new JLabel("🎯");
            iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imagePanel.add(iconLabel, BorderLayout.CENTER);
        }

        // Bottom: XP value
        JLabel xpLabel = new JLabel(tile.getWeight() + "XP");
        xpLabel.setForeground(GOLD_COLOR);
        xpLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        xpLabel.setHorizontalAlignment(SwingConstants.CENTER);

        miniTile.add(imagePanel, BorderLayout.CENTER);
        miniTile.add(xpLabel, BorderLayout.SOUTH);

        // Interaction
        miniTile.setCursor(new Cursor(Cursor.HAND_CURSOR));
        miniTile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Check if shift is pressed for unpinning (like in BingoBoardWindow)
                boolean shiftPressed = e.isShiftDown();
                
                if (shiftPressed) {
                    // Shift-click to unpin
                    removePinnedTile(tile.getId().toString());
                } else {
                    // Both left-click and right-click open context menu
                    showTileQuickActions(tile, miniTile);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                miniTile.setBackground(PINNED_TILE_BG.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                miniTile.setBackground(PINNED_TILE_BG);
            }
        });

        // Tooltip
        miniTile.setToolTipText("<html><b>" + tile.getTitle() + "</b><br>" +
                               tile.getWeight() + " XP<br>" +
                               "<i>Click for actions • Shift+Click to unpin</i></html>");

        return miniTile;
    }

    private JPanel createAddMoreButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
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

        addButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                addButton.setContentAreaFilled(true);
                addButton.setBackground(ACCENT_BLUE);
                addButton.setForeground(Color.WHITE);
                buttonPanel.setBorder(new CompoundBorder(
                    new LineBorder(ACCENT_BLUE, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                addButton.setContentAreaFilled(false);
                addButton.setForeground(Color.LIGHT_GRAY);
                buttonPanel.setBorder(new CompoundBorder(
                    new LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });

        addButton.addActionListener(e -> {
            // Show the bingo board to pin more tiles
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo != null) {
                showBingoBoardWindow(selectedBingo);
            }
        });
        
        buttonPanel.add(addButton, BorderLayout.CENTER);
        return buttonPanel;
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
        // Update the bingo board window if it's open
        if (bingoBoardWindow != null && bingoBoardWindow.isVisible()) {
            // Close and reopen with fresh data to ensure a full refresh
            SwingUtilities.invokeLater(() -> {
                bingoBoardWindow.dispose();
                bingoBoardWindow = new BingoBoardWindow(plugin, bingo);
                bingoBoardWindow.setVisible(true);
            });
        }
    }

    private JPanel createEventCard(EventData eventData) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        // Top row: Title and status
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(CARD_BG);

        JLabel titleLabel = new JLabel(eventData.getTitle());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        titleLabel.setForeground(GOLD_COLOR);

        // Status indicator
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        statusPanel.setBackground(CARD_BG);

        Color statusColor = eventData.isLocked() ? Color.LIGHT_GRAY : SUCCESS_COLOR;
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
        bottomRow.setBackground(CARD_BG);

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
            new LineBorder(BORDER_COLOR, 1, true),
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
                DATE_FORMAT.format(eventData.getStartDate()) + " - " +
                DATE_FORMAT.format(eventData.getEndDate()));
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
