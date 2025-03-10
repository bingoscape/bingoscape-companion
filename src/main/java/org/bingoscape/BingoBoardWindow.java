package org.bingoscape;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

public class BingoBoardWindow extends JFrame {
    private final BingoScapePlugin plugin;
    private final JPanel bingoBoard;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Bingo currentBingo;

    public BingoBoardWindow(BingoScapePlugin plugin, Bingo bingo) {
        this.plugin = plugin;
        this.currentBingo = bingo;

        setTitle("BingoScape - " + bingo.getTitle());
        setSize(600, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel titleLabel = new JLabel(bingo.getTitle());
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Create bingo board with dynamic grid size
        bingoBoard = new JPanel();
        updateGridLayout(bingo);
        bingoBoard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bingoBoard.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(bingoBoard);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(contentPanel);

        // Display the bingo board
        displayBingoBoard(bingo);
    }

    private void updateGridLayout(Bingo bingo) {
        int rows = bingo.getRows();
        int cols = bingo.getColumns();

        // Default to 5x5 if not specified
        if (rows <= 0) rows = 5;
        if (cols <= 0) cols = 5;

        bingoBoard.setLayout(new GridLayout(rows, cols, 4, 4));
    }

    public void updateBingoBoard(Bingo bingo) {
        this.currentBingo = bingo;
        SwingUtilities.invokeLater(() -> {
            setTitle("BingoScape - " + bingo.getTitle());
            updateGridLayout(bingo);
            displayBingoBoard(bingo);
        });
    }

    private void displayBingoBoard(Bingo bingo) {
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
                showSubmissionDialog(tile);
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
        JDialog dialog = new JDialog(this, "Submit Tile Completion", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(350, 300);
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Submit proof for: " + tile.getTitle());
        JTextField proofUrlField = new JTextField();
        JLabel urlLabel = new JLabel("Proof URL (optional):");
        JTextArea descriptionArea = new JTextArea(3, 20);
        JLabel descLabel = new JLabel("Description:");

        // Add screenshot option
        JCheckBox screenshotCheckbox = new JCheckBox("Take screenshot as proof", true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Submit");

        cancelButton.addActionListener(e -> dialog.dispose());

        submitButton.addActionListener(e -> {
            String proofUrl = proofUrlField.getText();
            String description = descriptionArea.getText();
            boolean takeScreenshot = screenshotCheckbox.isSelected();

            plugin.submitTileCompletion(tile.getId(), proofUrl, description, takeScreenshot);
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(screenshotCheckbox);
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

