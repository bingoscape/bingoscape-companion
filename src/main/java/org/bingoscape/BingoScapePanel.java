package org.bingoscape;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.EventData;
import org.bingoscape.models.Tile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import javax.swing.DefaultListCellRenderer;

public class BingoScapePanel extends PluginPanel {
    private final BingoScapePlugin plugin;
    private final JPanel eventsPanel = new JPanel();
    private final JPanel bingoPanel = new JPanel();
    private final JPanel apiKeyPanel = new JPanel();
    private final JComboBox<EventData> eventSelector = new JComboBox<>();
    private final JComboBox<Bingo> bingoSelector = new JComboBox<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private JButton showBingoBoardButton;
    private BingoBoardWindow bingoBoardWindow;

    public BingoScapePanel(BingoScapePlugin plugin) {
        super();
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Setup API Key Panel
        setupApiKeyPanel();

        // Setup Events Panel
        setupEventsPanel();

        // Setup Bingo Panel
        setupBingoPanel();

        // Add panels to main panel
        add(apiKeyPanel, BorderLayout.NORTH);
        add(eventsPanel, BorderLayout.CENTER);
        bingoPanel.setVisible(false);
    }

    private void setupApiKeyPanel() {
        apiKeyPanel.setLayout(new BorderLayout());
        apiKeyPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel("BingoScape");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());

        apiKeyPanel.add(titleLabel, BorderLayout.NORTH);
    }

    private void setupEventsPanel() {
        eventsPanel.setLayout(new BorderLayout());

        JLabel eventsLabel = new JLabel("Select an Event:");
        eventsPanel.add(eventsLabel, BorderLayout.NORTH);

        eventSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EventData) {
                    setText(((EventData) value).getTitle());
                }
                return this;
            }
        });

        eventSelector.addActionListener(e -> {
            EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
            if (selectedEvent != null) {
                plugin.setEventDetails(selectedEvent);
            }
        });

        eventsPanel.add(eventSelector, BorderLayout.CENTER);
    }

    private void setupBingoPanel() {
        bingoPanel.setLayout(new BorderLayout());
        bingoPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel bingoHeaderPanel = new JPanel(new BorderLayout());

        JLabel bingoLabel = new JLabel("Select a Bingo Board:");
        bingoHeaderPanel.add(bingoLabel, BorderLayout.NORTH);

        bingoSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bingo) {
                    setText(((Bingo) value).getTitle());
                }
                return this;
            }
        });
        bingoSelector.addActionListener(e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo != null) {
                plugin.selectBingo(selectedBingo);
            }
        });

        bingoHeaderPanel.add(bingoSelector, BorderLayout.CENTER);
        bingoPanel.add(bingoHeaderPanel, BorderLayout.NORTH);

        // Add button to show bingo board in a popup
        showBingoBoardButton = new JButton("Show Bingo Board");
        showBingoBoardButton.addActionListener(e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo != null) {
                showBingoBoardWindow(selectedBingo);
            }
        });

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonPanel.add(showBingoBoardButton, BorderLayout.CENTER);
        bingoPanel.add(buttonPanel, BorderLayout.CENTER);

        add(bingoPanel, BorderLayout.SOUTH);
    }

    private void showBingoBoardWindow(Bingo bingo) {
        if (bingoBoardWindow != null) {
            bingoBoardWindow.dispose();
        }

        bingoBoardWindow = new BingoBoardWindow(plugin, bingo);
        bingoBoardWindow.setVisible(true);
    }

    public void showApiKeyPrompt() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel promptLabel = new JLabel("Please enter your BingoScape API key:");
        JPasswordField apiKeyField = new JPasswordField();
        JButton submitButton = new JButton("Submit");

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
    }

    public void updateEventsList(List<EventData> events) {
        SwingUtilities.invokeLater(() -> {
            eventSelector.removeAllItems();

            if (events == null || events.isEmpty()) {
                JLabel noEventsLabel = new JLabel("No active events found");
                eventsPanel.add(noEventsLabel, BorderLayout.SOUTH);
                return;
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
}

