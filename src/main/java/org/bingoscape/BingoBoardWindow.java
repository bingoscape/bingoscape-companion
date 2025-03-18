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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public class BingoBoardWindow extends JFrame {
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PADDING = 10;
    private static final int SPACING = 4;
    private static final int IMAGE_LOADING_THREADS = 4;

    private final BingoScapePlugin plugin;
    private final JPanel bingoBoard;
    private JLabel titleLabel;
    private final ExecutorService executor;
    private Bingo currentBingo;
    private final Map<String, ImageIcon> imageCache = new ConcurrentHashMap<>();

    public BingoBoardWindow(BingoScapePlugin plugin, Bingo bingo) {
        this.plugin = plugin;
        this.currentBingo = bingo;
        this.executor = Executors.newFixedThreadPool(IMAGE_LOADING_THREADS);

        // Window setup
        setTitle("BingoScape - " + bingo.getTitle());
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main panel setup
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Title setup
        titleLabel = new JLabel(bingo.getTitle());
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(0, 0, PADDING, 0));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Bingo board setup
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
        updateBingoBoard(bingo);
    }

    private void updateGridLayout(Bingo bingo) {
        int rows = bingo.getRows() <= 0 ? 5 : bingo.getRows();
        int cols = bingo.getColumns() <= 0 ? 5 : bingo.getColumns();
        bingoBoard.setLayout(new GridLayout(rows, cols, SPACING, SPACING));
    }

    public void updateBingoBoard(Bingo bingo) {
        this.currentBingo = bingo;
        SwingUtilities.invokeLater(() -> {
            setTitle("BingoScape - " + bingo.getTitle());
            titleLabel.setText(bingo.getTitle());
            updateGridLayout(bingo);
            displayBingoBoard(bingo);
        });
    }

    // Ensure the displayBingoBoard method properly fetches fresh status data:
    private void displayBingoBoard(Bingo bingo) {
        // Fetch tile status in background thread to not block UI
        executor.submit(() -> {
            // Force a fresh fetch of tile statuses
            BingoTileResponse bingoTileResponse = this.plugin.fetchBingoTileStatus(bingo.getId());
            Map<UUID, TileStatus> tileStatuses = bingoTileResponse != null ? bingoTileResponse.getTiles() : null;

            SwingUtilities.invokeLater(() -> {
                bingoBoard.removeAll();

                if (bingo == null || bingo.getTiles() == null) {
                    return;
                }

                // Sort tiles by position
                bingo.getTiles().sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

                // Create all tile panels at once
                for (Tile tile : bingo.getTiles()) {
                    TileStatus status = tileStatuses != null ? tileStatuses.get(tile.getId()) : null;
                    JPanel tilePanel = createTilePanel(tile, status);
                    bingoBoard.add(tilePanel);
                }

                bingoBoard.revalidate();
                bingoBoard.repaint();
            });
        });
    }

    private JPanel createTilePanel(Tile tile, TileStatus status) {
        // Calculate tile size based on board dimensions
        int rows = currentBingo.getRows() > 0 ? currentBingo.getRows() : 5;
        int cols = currentBingo.getColumns() > 0 ? currentBingo.getColumns() : 5;
        int availableWidth = WINDOW_WIDTH - 40;
        int availableHeight = WINDOW_HEIGHT - 100;
        int tileSize = Math.min(availableWidth / cols, availableHeight / rows) - PADDING;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));
        panel.setMaximumSize(new Dimension(tileSize, tileSize));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new CompoundBorder(
                new LineBorder(getTileBorderColor(status), 1),
                new EmptyBorder(4, 4, 4, 4)
        ));

        // Add tooltip with title and weight
        panel.setToolTipText(tile.getTitle() + " (XP: " + tile.getWeight() + ")");

        // Add image if available, otherwise show title
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            loadTileImage(panel, tile, tileSize);
        } else {
            JLabel titleLabel = new JLabel("<html><center>" + tile.getTitle() + "</center></html>", SwingConstants.CENTER);
            titleLabel.setForeground(Color.WHITE);
            panel.add(titleLabel, BorderLayout.CENTER);
        }

        // Add click behavior
        addTilePanelListeners(panel, tile);

        return panel;
    }

    private void addTilePanelListeners(JPanel panel, Tile tile) {
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            private final Color originalColor = panel.getBackground();

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showSubmissionDialog(tile);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBackground(originalColor.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                panel.setBackground(originalColor);
            }
        });
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
            default:
                return ColorScheme.BORDER_COLOR;
        }
    }

    private void loadTileImage(JPanel panel, Tile tile, int tileSize) {
        String imageUrl = tile.getHeaderImage();

        // Create and add placeholder/loading label immediately
        JLabel imageLabel = new JLabel("Loading...");
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        panel.add(imageLabel, BorderLayout.CENTER);

        // Check cache first (still do this async to not block UI)
        executor.submit(() -> {
            if (imageCache.containsKey(imageUrl)) {
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setText("");
                    imageLabel.setIcon(imageCache.get(imageUrl));
                    panel.revalidate();
                });
                return;
            }

            try {
                // Fetch the image
                URL url = new URL(imageUrl);
                BufferedImage originalImage = ImageIO.read(url);

                if (originalImage != null) {
                    // Create a separate thread for the CPU-intensive scaling operation
                    executor.submit(() -> {
                        try {
                            // Scale the image to fit (CPU intensive)
                            Image scaledImage = getScaledImage(originalImage, tileSize, tileSize);
                            ImageIcon icon = new ImageIcon(scaledImage);

                            // Add to cache
                            imageCache.put(imageUrl, icon);

                            SwingUtilities.invokeLater(() -> {
                                imageLabel.setText("");
                                imageLabel.setIcon(icon);
                                panel.revalidate();
                            });
                        } catch (Exception e) {
                            handleImageLoadError(panel, imageLabel, tile);
                        }
                    });
                } else {
                    handleImageLoadError(panel, imageLabel, tile);
                }
            } catch (IOException e) {
                handleImageLoadError(panel, imageLabel, tile);
            }
        });
    }

    private void handleImageLoadError(JPanel panel, JLabel imageLabel, Tile tile) {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setText(tile.getTitle());
            imageLabel.setIcon(null);
            imageLabel.setForeground(Color.WHITE);
            panel.revalidate();
        });
    }

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

        // Main panel - split into info and image
        JPanel mainPanel = createTileDetailsPanel(tile);

        // Button panel
        JPanel buttonPanel = createDialogButtonPanel(dialog, tile);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createTileDetailsPanel(Tile tile) {
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
        JPanel imagePanel = createTileImagePanel(tile);

        mainPanel.add(infoPanel, BorderLayout.CENTER);
        mainPanel.add(imagePanel, BorderLayout.EAST);

        return mainPanel;
    }

    private JPanel createTileImagePanel(Tile tile) {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        imagePanel.setPreferredSize(new Dimension(150, 150));

        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            // Create a label with loading state
            JLabel imageLabel = new JLabel("Loading...");
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setVerticalAlignment(JLabel.CENTER);
            imageLabel.setForeground(Color.LIGHT_GRAY);
            imagePanel.add(imageLabel, BorderLayout.CENTER);

            // Load image in background thread pool
            executor.submit(() -> {
                String imageUrl = tile.getHeaderImage();

                // Check cache first
                if (imageCache.containsKey(imageUrl)) {
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setText("");
                        imageLabel.setIcon(imageCache.get(imageUrl));
                        imagePanel.revalidate();
                    });
                    return;
                }

                try {
                    // Fetch image in network thread
                    URL url = new URL(imageUrl);
                    BufferedImage originalImage = ImageIO.read(url);

                    if (originalImage != null) {
                        // Process image in separate thread
                        executor.submit(() -> {
                            try {
                                // CPU-intensive scaling operation
                                Image scaledImage = getScaledImage(originalImage, 150, 150);
                                ImageIcon icon = new ImageIcon(scaledImage);

                                // Add to cache
                                imageCache.put(imageUrl, icon);

                                SwingUtilities.invokeLater(() -> {
                                    imageLabel.setText("");
                                    imageLabel.setIcon(icon);
                                    imagePanel.revalidate();
                                });
                            } catch (Exception e) {
                                SwingUtilities.invokeLater(() -> {
                                    imageLabel.setText(tile.getTitle());
                                    imageLabel.setForeground(Color.WHITE);
                                });
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setText(tile.getTitle());
                            imageLabel.setForeground(Color.WHITE);
                        });
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setText(tile.getTitle());
                        imageLabel.setForeground(Color.WHITE);
                    });
                }
            });
        }

        return imagePanel;
    }

    private JPanel createDialogButtonPanel(JDialog dialog, Tile tile) {
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

        return buttonPanel;
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

    // Clean up resources when window is closed
    @Override
    public void dispose() {
        executor.shutdown();
        imageCache.clear();
        super.dispose();
    }
}
