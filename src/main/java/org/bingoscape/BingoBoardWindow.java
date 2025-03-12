package org.bingoscape;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoTileResponse;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileStatus;

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
    private JLabel titleLabel;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Bingo currentBingo;

    public BingoBoardWindow(BingoScapePlugin plugin, Bingo bingo) {
        this.plugin = plugin;
        this.currentBingo = bingo;

        setTitle("BingoScape - " + bingo.getTitle());
        setSize(600, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        titleLabel = new JLabel(bingo.getTitle());
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
            titleLabel.setText(bingo.getTitle()); // Update the title label
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


            BingoTileResponse bingoTileResponse = this.plugin.fetchBingoTileStatus(bingo.getId());
            for (Tile tile : bingo.getTiles()) {
                JPanel tilePanel = createTilePanel(tile, bingoTileResponse != null ? bingoTileResponse.getTiles().getOrDefault(tile.getId(), null) : null);
                bingoBoard.add(tilePanel);
            }

            bingoBoard.revalidate();
            bingoBoard.repaint();
        });
    }

    // Update createTilePanel to use a uniform aspect ratio
    private JPanel createTilePanel(Tile tile, TileStatus status) {

        // Calculate fixed tile size based on board dimensions
        int rows = currentBingo.getRows() > 0 ? currentBingo.getRows() : 5;
        int cols = currentBingo.getColumns() > 0 ? currentBingo.getColumns() : 5;

        // Calculate available space accounting for borders and padding
        int availableWidth = 600 - 40; // Window width minus padding/borders
        int availableHeight = 600 - 100; // Window height minus title/padding/borders

        // Calculate tile size (square)
        int tileSize = Math.min(availableWidth / cols, availableHeight / rows) - 10; // Subtract spacing

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));
        panel.setMaximumSize(new Dimension(tileSize, tileSize));
        panel.setBackground(getTileBackgroundColor(tile));
        panel.setBorder(new CompoundBorder(
                new LineBorder(getTileBorderColor(status), 1),
                new EmptyBorder(4, 4, 4, 4)
        ));
        // Add tooltip with title and weight
        panel.setToolTipText(tile.getTitle() + " (XP: " + tile.getWeight() + ")");

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

    private Color getTileBorderColor(TileStatus status) {
        if (status == null || status.getStatus() == null)
            return ColorScheme.BORDER_COLOR;
        switch (status.getStatus()) {
            case PENDING:
                return new Color(59, 130, 246);
            case ACCEPTED:
                return new Color(34, 197, 94);
            case REQUIRES_INTERACTION:
                return new Color(234, 179, 8);
            case DECLINED:
                return new Color(239, 68, 68);
        }
        return ColorScheme.BORDER_COLOR;
    }

    private Color getTileBackgroundColor(Tile tile) {
        // TODO: API Call for submission state?
        return ColorScheme.DARK_GRAY_COLOR;
    }

    private void loadTileImage(JPanel panel, String imageUrl) {
        executor.submit(() -> {
            try {
                URL url = new URL(imageUrl);
                BufferedImage originalImage = ImageIO.read(url);

                if (originalImage != null) {
                    // Calculate the fixed tile size based on board dimensions
                    int rows = currentBingo.getRows() > 0 ? currentBingo.getRows() : 5;
                    int cols = currentBingo.getColumns() > 0 ? currentBingo.getColumns() : 5;

                    // Calculate available space accounting for borders and padding
                    int availableWidth = 600 - 40; // Window width minus padding/borders
                    int availableHeight = 600 - 100; // Window height minus title/padding/borders

                    // Calculate tile size (square)
                    int tileSize = Math.min(availableWidth / cols, availableHeight / rows) - 10; // Subtract spacing

                    // Create a fixed-size panel for the image
                    JLabel imageLabel = new JLabel();
                    imageLabel.setPreferredSize(new Dimension(tileSize, tileSize));
                    imageLabel.setMinimumSize(new Dimension(tileSize, tileSize));
                    imageLabel.setMaximumSize(new Dimension(tileSize, tileSize));
                    imageLabel.setHorizontalAlignment(JLabel.CENTER);
                    imageLabel.setVerticalAlignment(JLabel.CENTER);

                    // Scale the image to fit within the tile while maintaining aspect ratio
                    Image scaledImage = getScaledImage(originalImage, tileSize, tileSize);
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

    // Helper method to scale images while maintaining aspect ratio
    private Image getScaledImage(BufferedImage img, int targetWidth, int targetHeight) {
        // Calculate dimensions that preserve aspect ratio
        double imgRatio = (double) img.getWidth() / img.getHeight();

        int scaledWidth, scaledHeight;
        if (imgRatio > 1) {
            // Image is wider than tall
            scaledWidth = Math.min(targetWidth, img.getWidth());
            scaledHeight = (int) (scaledWidth / imgRatio);
        } else {
            // Image is taller than wide
            scaledHeight = Math.min(targetHeight, img.getHeight());
            scaledWidth = (int) (scaledHeight * imgRatio);
        }

        // Create a new BufferedImage for drawing
        BufferedImage scaledImg = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImg.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill with transparent background
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        g2d.setComposite(AlphaComposite.SrcOver);

        // Calculate position to center the image
        int x = (targetWidth - scaledWidth) / 2;
        int y = (targetHeight - scaledHeight) / 2;

        // Draw the scaled image centered
        g2d.drawImage(img, x, y, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaledImg;
    }

    private void showSubmissionDialog(Tile tile) {
        JDialog dialog = new JDialog(this, "Tile Details", true);
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Create a split panel with info on left, image on right
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Left side: Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Title
        JLabel titleLabel = new JLabel("<html><h2>" + tile.getTitle() + "</h2></html>");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // XP (formerly weight)
        JLabel xpLabel = new JLabel("XP: " + tile.getWeight());
        xpLabel.setForeground(Color.LIGHT_GRAY);
        xpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        xpLabel.setBorder(new EmptyBorder(5, 0, 10, 0));

        // Description
        JTextArea descriptionArea = new JTextArea(tile.getDescription());
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setForeground(Color.WHITE);
        descriptionArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(titleLabel);
        infoPanel.add(xpLabel);
        infoPanel.add(descriptionArea);

        // Right side: Image panel
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        imagePanel.setPreferredSize(new Dimension(150, 150));

        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            JLabel imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setVerticalAlignment(JLabel.CENTER);

            // Load the image asynchronously
            executor.submit(() -> {
                try {
                    URL url = new URL(tile.getHeaderImage());
                    BufferedImage originalImage = ImageIO.read(url);
                    if (originalImage != null) {
                        Image scaledImage = getScaledImage(originalImage, 150, 150);
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setIcon(new ImageIcon(scaledImage));
                            imagePanel.revalidate();
                        });
                    }
                } catch (IOException e) {
                    // Silently fail on image load error
                }
            });

            imagePanel.add(imageLabel, BorderLayout.CENTER);
        }

        mainPanel.add(infoPanel, BorderLayout.CENTER);
        mainPanel.add(imagePanel, BorderLayout.EAST);

        // Button panel at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Take & Review Screenshot");

        cancelButton.addActionListener(e -> dialog.dispose());
        submitButton.addActionListener(e -> {
            dialog.dispose();
            takeScreenshotAndShowPreview(tile);
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void takeScreenshotAndShowPreview(Tile tile) {
        plugin.takeScreenshot(tile.getId(), (screenshotBytes) -> {
            if (screenshotBytes != null) {
                showScreenshotPreviewDialog(tile, screenshotBytes);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to take screenshot.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showScreenshotPreviewDialog(Tile tile, byte[] screenshotBytes) {
        JDialog previewDialog = new JDialog(this, "Screenshot Preview", true);
        previewDialog.setSize(600, 400);
        previewDialog.setLocationRelativeTo(this);
        previewDialog.setLayout(new BorderLayout());

        JLabel screenshotLabel = new JLabel(new ImageIcon(screenshotBytes));
        previewDialog.add(new JScrollPane(screenshotLabel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Submit");

        cancelButton.addActionListener(e -> previewDialog.dispose());
        submitButton.addActionListener(e -> {
            plugin.submitTileCompletionWithScreenshot(tile.getId(), screenshotBytes);
            previewDialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        previewDialog.setVisible(true);
    }
}
