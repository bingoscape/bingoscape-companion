package org.bingoscape;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.EventData;
import org.bingoscape.models.Role;
import org.bingoscape.models.TeamMember;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import javax.swing.DefaultListCellRenderer;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BingoScapePanel extends PluginPanel {
    // Constants
    private static final int BORDER_SPACING = 10;
    private static final int COMPONENT_SPACING = 10;
    private static final String NO_EVENTS_TEXT = "No active events found";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    // Components
    private final JPanel mainContentPanel = new JPanel(); // Main container for all content
    private final JPanel eventsPanel = new JPanel();
    private final JPanel bingoPanel = new JPanel();
    private final JPanel eventDetailsPanel = new JPanel();
    private final JComboBox<EventData> eventSelector = new JComboBox<>();
    private final JComboBox<Bingo> bingoSelector = new JComboBox<>();
    private final JButton showBingoBoardButton;
    private final JButton reloadEventsButton; // New reload events button
    private final JLabel loadingLabel; // New loading label
    private final Timer fadeTimer; // Timer for smooth transitions

    // Reference to plugin and other resources
    private final BingoScapePlugin plugin;
    private final ScheduledExecutorService executor;
    private BingoBoardWindow bingoBoardWindow;

    public BingoScapePanel(BingoScapePlugin plugin) {
        super();
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        // Initialize fade timer
        this.fadeTimer = new Timer(50, null);
        fadeTimer.setRepeats(true);

        // Panel setup
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(BORDER_SPACING, BORDER_SPACING, BORDER_SPACING, BORDER_SPACING));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Main content panel setup
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(mainContentPanel, BorderLayout.NORTH);

        // Create show bingo board button
        showBingoBoardButton = createShowBingoBoardButton();

        // Create reload events button and loading label
        reloadEventsButton = createReloadEventsButton();
        loadingLabel = createLoadingLabel();

        // Setup all panels
        setupEventsPanel();
        setupBingoPanel();
        setupEventDetailsPanel();

        // Add panels to main content panel
        mainContentPanel.add(eventsPanel);
        mainContentPanel.add(bingoPanel);
        mainContentPanel.add(eventDetailsPanel);

        // Initially hide the bingo panel
        bingoPanel.setVisible(false);
    }

    private JLabel createLoadingLabel() {
        JLabel label = new JLabel("Loading events...");
        label.setForeground(Color.LIGHT_GRAY);
        label.setVisible(false);
        return label;
    }

    private void setupEventsPanel() {
        eventsPanel.setLayout(new BorderLayout(0, COMPONENT_SPACING));
        eventsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eventsPanel.setBorder(new EmptyBorder(0, 0, COMPONENT_SPACING, 0));
        eventsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create a header panel that contains the label and reload button
        JPanel headerPanel = new JPanel(new BorderLayout(COMPONENT_SPACING, 0));
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel eventsLabel = new JLabel("Select an Event:");
        eventsLabel.setForeground(Color.WHITE);
        
        // Create a button panel to hold both the reload button and loading label
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.add(loadingLabel);
        buttonPanel.add(reloadEventsButton);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        headerPanel.add(eventsLabel, BorderLayout.WEST);

        eventsPanel.add(headerPanel, BorderLayout.NORTH);

        configureEventSelector();
        eventsPanel.add(eventSelector, BorderLayout.CENTER);
    }

    private void setupEventDetailsPanel() {
        eventDetailsPanel.setLayout(new BoxLayout(eventDetailsPanel, BoxLayout.Y_AXIS));
        eventDetailsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eventDetailsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
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
                    setText(((EventData) value).getTitle());
                }

                if (isSelected) {
                    setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                } else {
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                setForeground(Color.WHITE);

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

    private JButton createReloadEventsButton() {
        JButton button = new JButton();
        button.setIcon(new ImageIcon(getClass().getResource("/refresh_icon.png")));
        button.setToolTipText("Reload Events");
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.setMinimumSize(new Dimension(24, 24));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(Color.WHITE);

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });

        // Connect to plugin's fetchActiveEvents method with smooth transition
        button.addActionListener(e -> {
            reloadEventsButton.setEnabled(false);
            loadingLabel.setVisible(true);
            eventSelector.setEnabled(false);
            
            // Store current selections
            EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            String selectedEventId = selectedEvent != null ? selectedEvent.getId().toString() : null;
            String selectedBingoId = selectedBingo != null ? selectedBingo.getId().toString() : null;
            
            // Start fade out animation
            for (ActionListener listener : fadeTimer.getActionListeners()) {
                fadeTimer.removeActionListener(listener);
            }
            fadeTimer.addActionListener(evt -> {
                float alpha = eventSelector.getForeground().getAlpha() - 10;
                if (alpha <= 0) {
                    fadeTimer.stop();
                    executor.submit(() -> {
                        plugin.fetchActiveEvents();
                        SwingUtilities.invokeLater(() -> {
                            reloadEventsButton.setEnabled(true);
                            loadingLabel.setVisible(false);
                            eventSelector.setEnabled(true);
                            
                            // Restore selections if they still exist
                            if (selectedEventId != null) {
                                for (int i = 0; i < eventSelector.getItemCount(); i++) {
                                    EventData event = eventSelector.getItemAt(i);
                                    if (event.getId().toString().equals(selectedEventId)) {
                                        eventSelector.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            
                            if (selectedBingoId != null) {
                                for (int i = 0; i < bingoSelector.getItemCount(); i++) {
                                    Bingo bingo = bingoSelector.getItemAt(i);
                                    if (bingo.getId().toString().equals(selectedBingoId)) {
                                        bingoSelector.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            
                            // Start fade in animation
                            for (ActionListener listener : fadeTimer.getActionListeners()) {
                                fadeTimer.removeActionListener(listener);
                            }
                            fadeTimer.addActionListener(evt2 -> {
                                float fadeInAlpha = eventSelector.getForeground().getAlpha() + 10;
                                if (fadeInAlpha >= 255) {
                                    fadeTimer.stop();
                                }
                                eventSelector.setForeground(new Color(255, 255, 255, (int)fadeInAlpha));
                            });
                            fadeTimer.start();
                        });
                    });
                } else {
                    eventSelector.setForeground(new Color(255, 255, 255, (int)alpha));
                }
            });
            fadeTimer.start();
        });

        return button;
    }

    private void setupBingoPanel() {
        bingoPanel.setLayout(new BorderLayout(0, COMPONENT_SPACING));
        bingoPanel.setBorder(new EmptyBorder(COMPONENT_SPACING, 0, COMPONENT_SPACING, 0));
        bingoPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bingoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bingoHeaderPanel = new JPanel(new BorderLayout());
        bingoHeaderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel bingoLabel = new JLabel("Select a Bingo Board:");
        bingoLabel.setForeground(Color.WHITE);
        bingoHeaderPanel.add(bingoLabel, BorderLayout.NORTH);

        configureBingoSelector();
        bingoHeaderPanel.add(bingoSelector, BorderLayout.CENTER);
        bingoPanel.add(bingoHeaderPanel, BorderLayout.NORTH);

        // Add button to show bingo board in a popup
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(COMPONENT_SPACING, 0, 0, 0));
        buttonPanel.add(showBingoBoardButton, BorderLayout.CENTER);
        bingoPanel.add(buttonPanel, BorderLayout.CENTER);
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
                    StringBuilder text = new StringBuilder(bingo.getTitle());

                    // Show if the bingo is locked or visible
                    if (bingo.isLocked()) {
                        text.append(" [Locked]");
                    }

                    setText(text.toString());
                }

                if (isSelected) {
                    setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                } else {
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                setForeground(Color.WHITE);

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
            }
        };
    }

    private JButton createShowBingoBoardButton() {
        JButton button = new JButton("Show Bingo Board");
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
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
            eventSelector.removeAllItems();

            if (events == null || events.isEmpty()) {
                // Check if "no events" label already exists
                boolean hasNoEventsLabel = false;
                for (Component c : eventsPanel.getComponents()) {
                    if (c instanceof JLabel && NO_EVENTS_TEXT.equals(((JLabel) c).getText())) {
                        hasNoEventsLabel = true;
                        break;
                    }
                }

                if (!hasNoEventsLabel) {
                    JLabel noEventsLabel = new JLabel(NO_EVENTS_TEXT);
                    noEventsLabel.setForeground(Color.LIGHT_GRAY);
                    eventsPanel.add(noEventsLabel, BorderLayout.SOUTH);
                }

                eventDetailsPanel.setVisible(false);
                bingoPanel.setVisible(false);
                return;
            }

            // Remove "no events" label if it exists
            for (Component c : eventsPanel.getComponents()) {
                if (c instanceof JLabel && NO_EVENTS_TEXT.equals(((JLabel) c).getText())) {
                    eventsPanel.remove(c);
                }
            }

            for (EventData event : events) {
                eventSelector.addItem(event);
            }

            eventsPanel.revalidate();
            eventsPanel.repaint();
        });
    }

    // Method to update event details with enhanced information
    public void updateEventDetails(EventData event) {
        SwingUtilities.invokeLater(() -> {
            // Clear previous event details
            eventDetailsPanel.removeAll();
            bingoSelector.removeAllItems();

            // Build event details panel
            if (event != null) {
                // Title panel
                JPanel titlePanel = new JPanel();
                titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
                titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                titlePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
                
                JLabel titleLabel = new JLabel(event.getTitle());
                titleLabel.setFont(FontManager.getRunescapeBoldFont());
                titleLabel.setForeground(new Color(255, 215, 0)); // Gold color
                titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                titlePanel.add(titleLabel);
                
                // Add status indicator
                JLabel statusLabel = new JLabel(event.isLocked() ? "🔒 Locked" : "✅ Active");
                statusLabel.setForeground(event.isLocked() ? Color.LIGHT_GRAY : new Color(34, 197, 94));
                statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                titlePanel.add(statusLabel);
                
                eventDetailsPanel.add(titlePanel);

                // Event description
                if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                    JTextArea descriptionArea = new JTextArea(event.getDescription());
                    descriptionArea.setWrapStyleWord(true);
                    descriptionArea.setLineWrap(true);
                    descriptionArea.setEditable(false);
                    descriptionArea.setForeground(Color.WHITE);
                    descriptionArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    descriptionArea.setBorder(new CompoundBorder(
                        new MatteBorder(1, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                        new EmptyBorder(5, 5, 5, 5)
                    ));
                    descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                    eventDetailsPanel.add(descriptionArea);
                }

                // Info section
                JPanel infoSection = new JPanel();
                infoSection.setLayout(new BoxLayout(infoSection, BoxLayout.Y_AXIS));
                infoSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
                infoSection.setBorder(new EmptyBorder(5, 5, 5, 5));
                infoSection.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Event dates
                if (event.getStartDate() != null && event.getEndDate() != null) {
                    addInfoLabel(infoSection, "📅 Event dates: " +
                            DATE_FORMAT.format(event.getStartDate()) + " - " +
                            DATE_FORMAT.format(event.getEndDate()));
                }

                // Prize pool
                if (event.getBasePrizePool() > 0) {
                    addInfoLabel(infoSection, "💰 Prize pool: " + formatGpAmount(event.getBasePrizePool()));
                    if (event.getMinimumBuyIn() > 0) {
                        addInfoLabel(infoSection, "💎 Minimum buy-in: " + formatGpAmount(event.getMinimumBuyIn()));
                    }
                }

                // Role
                if (event.getRole() != null) {
                    addInfoLabel(infoSection, "👤 Role: " + formatRole(event.getRole()));
                }

                // Clan
                if (event.getClan() != null) {
                    addInfoLabel(infoSection, "🏰 Clan: " + event.getClan().getName());
                }

                // Team
                if (event.getUserTeam() != null) {
                    addInfoLabel(infoSection, "👥 Team: " + event.getUserTeam().getName());
                    if (event.getUserTeam().getMembers() != null && !event.getUserTeam().getMembers().isEmpty()) {
                        JPanel membersPanel = new JPanel();
                        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
                        membersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                        membersPanel.setBorder(new EmptyBorder(0, 15, 0, 0));
                        membersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                        // First add the leader
                        for (TeamMember member : event.getUserTeam().getMembers()) {
                            if (member.isLeader()) {
                                String memberText = "• " + member.getRunescapeName() + " 👑";
                                JLabel memberLabel = new JLabel(memberText);
                                memberLabel.setForeground(new Color(255, 215, 0));
                                memberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                                membersPanel.add(memberLabel);
                                membersPanel.add(Box.createVerticalStrut(2));
                                break;
                            }
                        }

                        // Then add other members
                        for (TeamMember member : event.getUserTeam().getMembers()) {
                            if (!member.isLeader()) {
                                String memberText = "• " + member.getRunescapeName();
                                JLabel memberLabel = new JLabel(memberText);
                                memberLabel.setForeground(Color.WHITE);
                                memberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                                membersPanel.add(memberLabel);
                                membersPanel.add(Box.createVerticalStrut(2));
                            }
                        }
                        infoSection.add(membersPanel);
                    }
                }

                // Available boards
                if (event.getBingos() != null) {
                    addInfoLabel(infoSection, "🎯 Available boards: " + event.getBingos().size());
                }

                eventDetailsPanel.add(infoSection);
                eventDetailsPanel.setVisible(true);

                // Populate bingo selector
                if (event.getBingos() != null && !event.getBingos().isEmpty()) {
                    for (Bingo bingo : event.getBingos()) {
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
}
