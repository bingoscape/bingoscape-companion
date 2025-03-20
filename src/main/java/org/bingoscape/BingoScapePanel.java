package org.bingoscape;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.EventData;
import org.bingoscape.models.Role;
import org.bingoscape.models.TeamMember;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;

import java.awt.*;
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

    // Reference to plugin and other resources
    private final BingoScapePlugin plugin;
    private final ScheduledExecutorService executor;
    private BingoBoardWindow bingoBoardWindow;

    public BingoScapePanel(BingoScapePlugin plugin) {
        super();
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor();

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

    private void setupEventsPanel() {
        eventsPanel.setLayout(new BorderLayout(0, COMPONENT_SPACING));
        eventsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eventsPanel.setBorder(new EmptyBorder(0, 0, COMPONENT_SPACING, 0));
        eventsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel eventsLabel = new JLabel("Select an Event:");
        eventsLabel.setForeground(Color.WHITE);
        eventsPanel.add(eventsLabel, BorderLayout.NORTH);

        configureEventSelector();
        eventsPanel.add(eventSelector, BorderLayout.CENTER);
    }

    private void setupEventDetailsPanel() {
        eventDetailsPanel.setLayout(new BoxLayout(eventDetailsPanel, BoxLayout.Y_AXIS));
        eventDetailsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eventDetailsPanel.setBorder(new EmptyBorder(COMPONENT_SPACING, 0, 0, 0));
        eventDetailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
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
                executor.submit(() -> plugin.selectBingo(selectedBingo));
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
                // Create a scrollable panel for event details to handle overflow
                JPanel detailsContent = new JPanel();
                detailsContent.setLayout(new BoxLayout(detailsContent, BoxLayout.Y_AXIS));
                detailsContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

                // Make sure the details content has proper alignment
                detailsContent.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Title with proper styling
                JLabel titleLabel = new JLabel(event.getTitle());
                titleLabel.setFont(FontManager.getRunescapeBoldFont());
                titleLabel.setForeground(Color.WHITE);
                titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                titleLabel.setBorder(new EmptyBorder(0, 0, COMPONENT_SPACING, 0));
                detailsContent.add(titleLabel);

                // Event description
                if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                    JTextArea descriptionArea = new JTextArea(event.getDescription());
                    descriptionArea.setWrapStyleWord(true);
                    descriptionArea.setLineWrap(true);
                    descriptionArea.setEditable(false);
                    descriptionArea.setForeground(Color.WHITE);
                    descriptionArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                    descriptionArea.setBorder(new EmptyBorder(0, 0, COMPONENT_SPACING, 0));
                    detailsContent.add(descriptionArea);
                }

                // Event dates
                JPanel datesPanel = createInfoRow("Event dates: " +
                        DATE_FORMAT.format(event.getStartDate()) + " - " +
                        DATE_FORMAT.format(event.getEndDate()));
                detailsContent.add(datesPanel);

                // Prize pool info
                if (event.getBasePrizePool() > 0) {
                    String prizeText = formatGpAmount(event.getBasePrizePool());
                    JPanel prizePanel = createInfoRow("Prize pool: " + prizeText);
                    detailsContent.add(prizePanel);

                    // Add minimum buy-in if applicable
                    if (event.getMinimumBuyIn() > 0) {
                        String buyInText = formatGpAmount(event.getMinimumBuyIn());
                        JPanel buyInPanel = createInfoRow("Minimum buy-in: " + buyInText);
                        detailsContent.add(buyInPanel);
                    }
                }

                // User role
                if (event.getRole() != null) {
                    JPanel rolePanel = createInfoRow("Role: " + formatRole(event.getRole()));
                    detailsContent.add(rolePanel);
                }

                // Clan info
                if (event.getClan() != null) {
                    JPanel clanPanel = createInfoRow("Clan: " + event.getClan().getName());
                    detailsContent.add(clanPanel);
                }

                // Team information (enhanced with member list)
                if (event.getUserTeam() != null) {
                    // Add team name
                    JPanel teamPanel = createInfoRow("Team: " + event.getUserTeam().getName());
                    detailsContent.add(teamPanel);

                    // Add team members list with leader indicator
                    if (event.getUserTeam().getMembers() != null && !event.getUserTeam().getMembers().isEmpty()) {
                        JPanel membersPanel = new JPanel();
                        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
                        membersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                        membersPanel.setBorder(new EmptyBorder(COMPONENT_SPACING, 10, 0, 0));
                        membersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                        JLabel membersLabel = new JLabel("Team Members:");
                        membersLabel.setForeground(Color.LIGHT_GRAY);
                        membersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        membersPanel.add(membersLabel);

                        for (TeamMember member : event.getUserTeam().getMembers()) {
                            String memberText = "• " + member.getRunescapeName();
                            if (member.isLeader()) {
                                memberText += " (Leader)";
                            }

                            JLabel memberLabel = new JLabel(memberText);
                            memberLabel.setForeground(member.isLeader() ?
                                    new Color(255, 215, 0) : Color.WHITE); // Gold color for leader
                            memberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            membersPanel.add(memberLabel);
                        }

                        detailsContent.add(membersPanel);
                    }
                }

                // Status information
                JPanel statusPanel = createInfoRow("Status: " +
                        (event.isLocked() ? "Locked" : "Active"));
                detailsContent.add(statusPanel);

                // Available bingos count
                if (event.getBingos() != null) {
                    JPanel bingoCountPanel = createInfoRow("Available boards: " +
                            event.getBingos().size());
                    detailsContent.add(bingoCountPanel);
                }

                // Add the details content to a scrollable pane
                JScrollPane scrollPane = new JScrollPane(detailsContent);
                scrollPane.setBorder(null);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

                eventDetailsPanel.add(scrollPane);
                eventDetailsPanel.setVisible(true);

                // Populate bingo selector and show bingo panel
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

            // Validate and repaint the entire panel
            revalidate();
            repaint();
        });
    }

    // Helper method to create consistent info rows
    private JPanel createInfoRow(String text) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(2, 0, 2, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(text);
        label.setForeground(Color.LIGHT_GRAY);
        panel.add(label);

        return panel;
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
