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

        // Create bingo board that will resize with the window
        bingoBoard = new JPanel();
        bingoBoard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bingoBoard.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(bingoBoard);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        setContentPane(contentPanel);

        // Add a component listener to resize the grid when the window is resized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateGridLayout(bingo);
                displayBingoBoard(bingo);
            }
        });

        // Display the bingo board
        updateGridLayout(bingo);
        displayBingoBoard(bingo);
    }

    private void updateGridLayout(Bingo bingo) {
        int rows = bingo.getRows();
        int cols = bingo.getColumns();

        // Default to 5x5 if not specified
        if (rows <= 0) rows = 5;
        if (cols <= 0) cols = 5;

        // Use GridLayout with some spacing between cells
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

    // Update createTilePanel to use a uniform aspect ratio
    private JPanel createTilePanel(Tile tile) {
        // Create a panel that will maintain a square aspect ratio
        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                // Make sure tiles stay square
                Dimension size = super.getPreferredSize();
                int minSize = Math.min(size.width, size.height);
                return new Dimension(minSize, minSize);
            }
        };

        panel.setLayout(new BorderLayout());
        panel.setBackground(getTileBackgroundColor(tile));
        panel.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
                new EmptyBorder(4, 4, 4, 4)
        ));

        // Add tooltip with title and weight
        panel.setToolTipText(tile.getTitle() + " (Weight: " + tile.getWeight() + ")");

        // Add image if available
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            loadTileImage(panel, tile.getHeaderImage());
        } else {
            // If no image, show the tile title
            JLabel titleLabel = new JLabel("<html><center>" + tile.getTitle() + "</center></html>", SwingConstants.CENTER);
            titleLabel.setForeground(Color.WHITE);
            panel.add(titleLabel, BorderLayout.CENTER);
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

    // Update loadTileImage to create properly sized images
    private void loadTileImage(JPanel panel, String imageUrl) {
        executor.submit(() -> {
            try {
                URL url = new URL(imageUrl);
                BufferedImage originalImage = ImageIO.read(url);

                if (originalImage != null) {
                    // Create a custom JLabel that will maintain aspect ratio
                    JLabel imageLabel = new JLabel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            if (getIcon() != null) {
                                Image img = ((ImageIcon) getIcon()).getImage();
                                if (img != null) {
                                    // Calculate dimensions that preserve aspect ratio
                                    int width = getWidth();
                                    int height = getHeight();

                                    double imageRatio = (double) originalImage.getWidth() / originalImage.getHeight();
                                    double panelRatio = (double) width / height;

                                    int x = 0, y = 0;
                                    int drawWidth, drawHeight;

                                    if (imageRatio > panelRatio) {
                                        // Image is wider than panel proportionally
                                        drawWidth = width;
                                        drawHeight = (int) (width / imageRatio);
                                        y = (height - drawHeight) / 2; // Center vertically
                                    } else {
                                        // Image is taller than panel proportionally
                                        drawHeight = height;
                                        drawWidth = (int) (height * imageRatio);
                                        x = (width - drawWidth) / 2; // Center horizontally
                                    }

                                    g.drawImage(img, x, y, drawWidth, drawHeight, this);
                                }
                            }
                        }
                    };

                    imageLabel.setHorizontalAlignment(JLabel.CENTER);
                    imageLabel.setVerticalAlignment(JLabel.CENTER);

                    // Create initial scaled version
                    int tileSize = Math.min(100, panel.getWidth());
                    Image scaledImage = originalImage.getScaledInstance(tileSize, tileSize, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));

                    SwingUtilities.invokeLater(() -> {
                        panel.setLayout(new BorderLayout());
                        panel.add(imageLabel, BorderLayout.CENTER);
                        panel.revalidate();
                        panel.repaint();
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
