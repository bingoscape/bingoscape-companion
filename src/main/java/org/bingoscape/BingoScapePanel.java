package org.bingoscape;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.EventData;

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

public class BingoScapePanel extends PluginPanel {
    // Constants
    private static final int BORDER_SPACING = 10;
    private static final int COMPONENT_SPACING = 10;
    private static final String NO_EVENTS_TEXT = "No active events found";

    // Components
    private final JPanel eventsPanel = new JPanel();
    private final JPanel bingoPanel = new JPanel();
    private final JPanel apiKeyPanel = new JPanel();
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

        // Setup API Key Panel
        setupApiKeyPanel();

        // Setup Events Panel
        setupEventsPanel();

        // Create show bingo board button
        showBingoBoardButton = createShowBingoBoardButton();

        // Setup Bingo Panel
        setupBingoPanel();


        // Add panels to main panel
        add(apiKeyPanel, BorderLayout.NORTH);
        add(eventsPanel, BorderLayout.CENTER);
        bingoPanel.setVisible(false);
    }

    private void setupApiKeyPanel() {
        apiKeyPanel.setLayout(new BorderLayout());
        apiKeyPanel.setBorder(new EmptyBorder(0, 0, COMPONENT_SPACING, 0));
        apiKeyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel titleLabel = new JLabel("BingoScape");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);

        apiKeyPanel.add(titleLabel, BorderLayout.NORTH);
    }

    private void setupEventsPanel() {
        eventsPanel.setLayout(new BorderLayout(0, COMPONENT_SPACING));
        eventsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        eventsPanel.setBorder(new EmptyBorder(0, 0, COMPONENT_SPACING, 0));

        JLabel eventsLabel = new JLabel("Select an Event:");
        eventsLabel.setForeground(Color.WHITE);
        eventsPanel.add(eventsLabel, BorderLayout.NORTH);

        configureEventSelector();
        eventsPanel.add(eventSelector, BorderLayout.CENTER);
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
        bingoPanel.setBorder(new EmptyBorder(COMPONENT_SPACING, 0, 0, 0));
        bingoPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

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

        add(bingoPanel, BorderLayout.SOUTH);
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
                    setText(((Bingo) value).getTitle());
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

    public void showApiKeyPrompt() {
        SwingUtilities.invokeLater(() -> {
            JPanel inputPanel = new JPanel(new BorderLayout(0, COMPONENT_SPACING));
            inputPanel.setBorder(new EmptyBorder(COMPONENT_SPACING, 0, COMPONENT_SPACING, 0));
            inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

            JLabel promptLabel = new JLabel("Please enter your BingoScape API key:");
            promptLabel.setForeground(Color.WHITE);

            JPasswordField apiKeyField = new JPasswordField();
            apiKeyField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            apiKeyField.setForeground(Color.WHITE);
            apiKeyField.setBorder(new CompoundBorder(
                    new MatteBorder(1, 1, 1, 1, ColorScheme.MEDIUM_GRAY_COLOR),
                    new EmptyBorder(5, 5, 5, 5)
            ));

            JButton submitButton = new JButton("Submit");
            submitButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            submitButton.setForeground(Color.WHITE);
            submitButton.setFocusPainted(false);

            submitButton.addActionListener(e -> {
                String apiKey = new String(apiKeyField.getPassword());
                if (!apiKey.isEmpty()) {
                    plugin.setApiKey(apiKey);
                    apiKeyPanel.remove(inputPanel);
                    apiKeyPanel.revalidate();
                    apiKeyPanel.repaint();
                }
            });

            inputPanel.add(promptLabel, BorderLayout.NORTH);
            inputPanel.add(apiKeyField, BorderLayout.CENTER);
            inputPanel.add(submitButton, BorderLayout.SOUTH);

            apiKeyPanel.add(inputPanel, BorderLayout.CENTER);
            apiKeyPanel.revalidate();
            apiKeyPanel.repaint();
        });
    }

    public void updateEventsList(List<EventData> events) {
        SwingUtilities.invokeLater(() -> {
            eventSelector.removeAllItems();

            if (events == null || events.isEmpty()) {
                JLabel noEventsLabel = new JLabel(NO_EVENTS_TEXT);
                noEventsLabel.setForeground(Color.LIGHT_GRAY);
                eventsPanel.add(noEventsLabel, BorderLayout.SOUTH);
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

    public void updateEventDetails(EventData event) {
        SwingUtilities.invokeLater(() -> {
            bingoSelector.removeAllItems();

            if (event.getBingos() == null || event.getBingos().isEmpty()) {
                bingoPanel.setVisible(false);
                return;
            }

            for (Bingo bingo : event.getBingos()) {
                bingoSelector.addItem(bingo);
            }

            bingoPanel.setVisible(true);
            revalidate();
            repaint();
        });
    }

    public void displayBingoBoard(Bingo bingo) {
        // Update the bingo board window if it's open
        if (bingoBoardWindow != null && bingoBoardWindow.isVisible()) {
            bingoBoardWindow.updateBingoBoard(bingo);
        }
    }


//    // Clean up resources when panel is removed
//    @Override
//    public void onUninstall() {
//        if (bingoBoardWindow != null) {
//            bingoBoardWindow.dispose();
//        }
//        executor.shutdown();
//        super.onUninstall();
//    }
}
