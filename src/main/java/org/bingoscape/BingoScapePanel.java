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
    private final JPanel bingoBoard = new JPanel(new GridLayout(5, 5, 4, 4));
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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

        // Setup bingo board
        JPanel boardWrapper = new JPanel(new BorderLayout());
        boardWrapper.setBorder(new EmptyBorder(10, 0, 0, 0));

        bingoBoard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bingoBoard.setBorder(new EmptyBorder(5, 5, 5, 5));

        boardWrapper.add(bingoBoard, BorderLayout.CENTER);
        bingoPanel.add(boardWrapper, BorderLayout.CENTER);

        add(bingoPanel, BorderLayout.SOUTH);
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
        SwingUtilities.invokeLater(() -> {
           bingoBoard.removeAll();

            if (bingo == null || bingo.getTiles() == null) {
                return;
            }

            // Sort tiles by position
            bingo.getTiles().sort((a, b) -> {
                int aPos = a.getIndex();
                int bPos = b.getIndex();
                return Integer.compare(aPos, bPos);
            });

            for (Tile tile : bingo.getTiles()) {
                JPanel tilePanel = createTilePanel(tile);
                bingoBoard.add(tilePanel);
            }

            bingoBoard.revalidate();
            bingoBoard.repaint();
        });
    }

    private JPanel createTilePanel(Tile tile) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(getTileBackgroundColor(tile));
        panel.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
                new EmptyBorder(4, 4, 4, 4)
        ));

        JLabel titleLabel = new JLabel("<html><body style='width: 100%'>" + tile.getTitle() + "</body></html>");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.CENTER);

        // Add image if available
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            loadTileImage(panel, tile.getHeaderImage());
        }

        // Add click listener for submission
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
//                if (!tile.isCompleted()) {
                if (true) {
                    showSubmissionDialog(tile);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBackground(panel.getBackground().brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                panel.setBackground(getTileBackgroundColor(tile));
            }
        });

        return panel;
    }

    private Color getTileBackgroundColor(Tile tile) {
        // TODO: API Call for submission state?
        return new Color(0, 128, 0); // Green for completed
//        if (tile.isCompleted()) {
//            return new Color(0, 128, 0); // Green for completed
//        } else if (tile.getTeamId() != null) {
//            return new Color(0, 0, 128); // Blue for assigned
//        } else {
//            return ColorScheme.DARKER_GRAY_COLOR; // Default
//        }
    }

    private void loadTileImage(JPanel panel, String imageUrl) {
        executor.submit(() -> {
            try {
                URL url = new URL(imageUrl);
                BufferedImage image = ImageIO.read(url);

                if (image != null) {
                    // Resize image to fit panel
                    BufferedImage resized = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = resized.createGraphics();
                    g.drawImage(image, 0, 0, 32, 32, null);
                    g.dispose();

                    JLabel imageLabel = new JLabel(new ImageIcon(resized));
                    SwingUtilities.invokeLater(() -> {
                        panel.add(imageLabel, BorderLayout.NORTH);
                        panel.revalidate();
                    });
                }
            } catch (IOException e) {
                // Silently fail on image load error
            }
        });
    }

    private void showSubmissionDialog(Tile tile) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Submit Tile Completion");
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Submit proof for: " + tile.getTitle());
        JTextField proofUrlField = new JTextField();
        JLabel urlLabel = new JLabel("Proof URL (optional):");
        JTextArea descriptionArea = new JTextArea(3, 20);
        JLabel descLabel = new JLabel("Description:");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Submit");

        cancelButton.addActionListener(e -> dialog.dispose());

        submitButton.addActionListener(e -> {
            String proofUrl = proofUrlField.getText();
            String description = descriptionArea.getText();

            plugin.submitTileCompletion(tile.getId(), proofUrl, description);
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(urlLabel);
        contentPanel.add(proofUrlField);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(descLabel);
        contentPanel.add(descriptionArea);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}

