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

        // Add tooltip with title and weight
        panel.setToolTipText("<html><b>" + tile.getTitle() + "</b><br>XP: " + tile.getWeight() +
                (tile.getDescription() != null ? "<br>" + tile.getDescription() : "") + "</html>");
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
        return ColorScheme.DARK_GRAY_COLOR;
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
                        panel.add(imageLabel, BorderLayout.CENTER);
                        panel.revalidate();
                    });
                }
            } catch (IOException e) {
                // Silently fail on image load error
            }
        });
    }

    private void showSubmissionDialog(Tile tile) {
        JDialog dialog = new JDialog(this, "Tile Details", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Tile title (bold and larger)
        JLabel titleLabel = new JLabel("<html><h2>" + tile.getTitle() + "</h2></html>");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tile weight
        JLabel weightLabel = new JLabel("Weight: " + tile.getWeight());
        weightLabel.setForeground(Color.LIGHT_GRAY);
        weightLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        weightLabel.setBorder(new EmptyBorder(5, 0, 10, 0));

        // Tile description
        JTextArea descriptionArea = new JTextArea(tile.getDescription());
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setForeground(Color.WHITE);
        descriptionArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionArea.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Submit Completion");

        cancelButton.addActionListener(e -> dialog.dispose());

        submitButton.addActionListener(e -> {
            // Always take screenshot, no additional data
            plugin.submitTileCompletion(tile.getId());
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        // Add everything to the content panel
        contentPanel.add(titleLabel);
        contentPanel.add(weightLabel);
        contentPanel.add(descriptionArea);

        // Add content and button panels to dialog
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
