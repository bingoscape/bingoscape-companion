package org.bingoscape;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import java.util.List;

public class BingoBoardWindow extends JFrame {
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;
    private static final int PADDING = 10;
    private static final int SPACING = 4;
    private static final int IMAGE_LOADING_THREADS = 4;

    // UI Constants
    private static final int BUTTON_SIZE = 24;
    private static final int SMALL_FONT_SIZE = 10;
    private static final int MEDIUM_FONT_SIZE = 12;
    private static final int LARGE_FONT_SIZE = 24;
    private static final int DETAIL_IMAGE_SIZE = 150;
    private static final int IMAGE_MARGIN = 10;
    private static final int IMAGE_TITLE_OFFSET = 20;

    // Color Constants
    private static final Color GOLD_COLOR = new Color(255, 215, 0);

    // Status Background Colors
    private static final Color PENDING_BG_COLOR = new Color(30, 64, 122);
    private static final Color ACCEPTED_BG_COLOR = new Color(17, 99, 47);
    private static final Color REQUIRES_ACTION_BG_COLOR = new Color(117, 89, 4);
    private static final Color DECLINED_BG_COLOR = new Color(120, 34, 34);

    // Status Border Colors
    private static final Color PENDING_BORDER_COLOR = new Color(59, 130, 246);
    private static final Color ACCEPTED_BORDER_COLOR = new Color(34, 197, 94);
    private static final Color REQUIRES_ACTION_BORDER_COLOR = new Color(234, 179, 8);
    private static final Color DECLINED_BORDER_COLOR = new Color(239, 68, 68);

    // Status Text Colors
    private static final Color PENDING_TEXT_COLOR = new Color(59, 130, 246);
    private static final Color ACCEPTED_TEXT_COLOR = new Color(34, 197, 94);
    private static final Color REQUIRES_ACTION_TEXT_COLOR = new Color(234, 179, 8);
    private static final Color DECLINED_TEXT_COLOR = new Color(239, 68, 68);

    // Spacing Constants
    private static final int SMALL_SPACING = 4;
    private static final int MEDIUM_SPACING = 5;
    private static final int DEFAULT_GRID_SIZE = 5;

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
        String windowTitle = "BingoScape - " + bingo.getTitle();
        // Add codephrase to title if available
        if (bingo.getCodephrase() != null && !bingo.getCodephrase().isEmpty()) {
            windowTitle += " | Codephrase: " + bingo.getCodephrase();
        }

        setTitle(windowTitle);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setResizable(true); // Allow resizing for better image viewing
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main panel setup
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Title setup
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        titlePanel.setBorder(new EmptyBorder(0, 0, PADDING, 0));

        titleLabel = new JLabel(bingo.getTitle());
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // Create button container for reload and pin buttons
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Add pin button
        JButton pinButton = new JButton(isPinned() ? "Un-📌" : "📌");
        pinButton.setFocusPainted(false);
        pinButton.setContentAreaFilled(false);
        pinButton.setForeground(Color.WHITE);
        pinButton.setBorder(new EmptyBorder(0, MEDIUM_SPACING, 0, MEDIUM_SPACING));
        pinButton.setToolTipText(isPinned() ? "Unpin Board" : "Pin Board");

        // Add hover effect for pin button
        pinButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (pinButton.isEnabled()) {
                    pinButton.setContentAreaFilled(true);
                    pinButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pinButton.setContentAreaFilled(false);
            }
        });

        // Add pin action
        pinButton.addActionListener(e -> {
            if (isPinned()) {
                plugin.unpinBingo();
                pinButton.setText("📌");
                pinButton.setToolTipText("Pin Board");
            } else {
                plugin.pinBingo(currentBingo.getId());
                pinButton.setText("Un-📌");
                pinButton.setToolTipText("Unpin Board");
            }
        });

        // Add reload button
        JButton reloadButton = new JButton();
        reloadButton.setIcon(new ImageIcon(getClass().getResource("/refresh_icon.png")));
        reloadButton.setToolTipText("Reload Board");
        reloadButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        reloadButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        reloadButton.setMinimumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        reloadButton.setFocusPainted(false);
        reloadButton.setContentAreaFilled(false);
        reloadButton.setForeground(Color.WHITE);
        reloadButton.setBorder(new EmptyBorder(0, MEDIUM_SPACING, 0, MEDIUM_SPACING));

        // Create a container panel for the button to ensure proper spacing
        buttonContainer.add(pinButton);
        buttonContainer.add(reloadButton);

        // Add hover effect
        reloadButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (reloadButton.isEnabled()) {
                    reloadButton.setContentAreaFilled(true);
                    reloadButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                reloadButton.setContentAreaFilled(false);
            }
        });

        // Add reload action
        reloadButton.addActionListener(e -> {
            reloadButton.setEnabled(false);
            executor.submit(() -> {
                plugin.refreshBingoBoard();
                SwingUtilities.invokeLater(() -> reloadButton.setEnabled(true));
            });
        });

        titlePanel.add(buttonContainer, BorderLayout.EAST);
        contentPanel.add(titlePanel, BorderLayout.NORTH);

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
        int rows = bingo.getRows() <= 0 ? DEFAULT_GRID_SIZE : bingo.getRows();
        int cols = bingo.getColumns() <= 0 ? DEFAULT_GRID_SIZE : bingo.getColumns();
        bingoBoard.setLayout(new GridLayout(rows, cols, SPACING, SPACING));
    }

    public void updateBingoBoard(Bingo bingo) {
        this.currentBingo = bingo;
        SwingUtilities.invokeLater(() -> {
            String windowTitle = "BingoScape - " + bingo.getTitle();
            // Add codephrase to title if available
            if (bingo.getCodephrase() != null && !bingo.getCodephrase().isEmpty()) {
                windowTitle += " | Codephrase: " + bingo.getCodephrase();
            }
            titleLabel.setText(windowTitle);
            updateGridLayout(bingo);
            displayBingoBoard(bingo);
        });
    }

    // Updated to use new tile model with submission data directly
    private void displayBingoBoard(Bingo bingo) {
        SwingUtilities.invokeLater(() -> {
            bingoBoard.removeAll();

            if (bingo == null || bingo.getTiles() == null) {
                return;
            }

            // Sort tiles by position
            bingo.getTiles().sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

            // Create all tile panels at once
            for (Tile tile : bingo.getTiles()) {
                // Skip hidden tiles
                if (tile.isHidden()) {
                    JPanel hiddenPanel = createHiddenTilePanel();
                    bingoBoard.add(hiddenPanel);
                    continue;
                }

                JPanel tilePanel = createTilePanel(tile);
                bingoBoard.add(tilePanel);
            }

            bingoBoard.revalidate();
            bingoBoard.repaint();
        });
    }

    private JPanel createHiddenTilePanel() {
        // Calculate tile size based on board dimensions
        int rows = currentBingo.getRows() > 0 ? currentBingo.getRows() : DEFAULT_GRID_SIZE;
        int cols = currentBingo.getColumns() > 0 ? currentBingo.getColumns() : DEFAULT_GRID_SIZE;
        int availableWidth = WINDOW_WIDTH - 40;
        int availableHeight = WINDOW_HEIGHT - 100;
        int tileSize = Math.min(availableWidth / cols, availableHeight / rows) - PADDING;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));
        panel.setMaximumSize(new Dimension(tileSize, tileSize));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_COLOR, 1),
                new EmptyBorder(SMALL_SPACING, SMALL_SPACING, SMALL_SPACING, SMALL_SPACING)
        ));

        JLabel hiddenLabel = new JLabel("?", SwingConstants.CENTER);
        hiddenLabel.setForeground(Color.GRAY);
        hiddenLabel.setFont(new Font(hiddenLabel.getFont().getName(), Font.BOLD, LARGE_FONT_SIZE));
        panel.add(hiddenLabel, BorderLayout.CENTER);

        panel.setToolTipText("Hidden tile");
        return panel;
    }

    // Method to create enhanced tile panel with more information
    private JPanel createTilePanel(Tile tile) {
        // Calculate tile size based on board dimensions
        int rows = currentBingo.getRows() > 0 ? currentBingo.getRows() : DEFAULT_GRID_SIZE;
        int cols = currentBingo.getColumns() > 0 ? currentBingo.getColumns() : DEFAULT_GRID_SIZE;
        int availableWidth = WINDOW_WIDTH - 40;
        int availableHeight = WINDOW_HEIGHT - 100;
        int tileSize = Math.min(availableWidth / cols, availableHeight / rows) - PADDING;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(tileSize, tileSize));
        panel.setMinimumSize(new Dimension(tileSize, tileSize));

        // Don't set maximum size to allow proper image display
        // panel.setMaximumSize(new Dimension(tileSize, tileSize));

        // Set background based on submission status
        Color backgroundColor = getTileBackgroundColor(tile.getSubmission());
        panel.setBackground(backgroundColor);

        // Set border based on submission status
        panel.setBorder(new CompoundBorder(
                new LineBorder(getTileBorderColor(tile.getSubmission()), 2),
                new EmptyBorder(SMALL_SPACING, SMALL_SPACING, SMALL_SPACING, SMALL_SPACING)
        ));

        // Create tooltip with extended information
        panel.setToolTipText(createDetailedTooltip(tile));

        // Add image if available, otherwise show title
        if (tile.getHeaderImage() != null && !tile.getHeaderImage().isEmpty()) {
            loadTileImage(panel, tile, tileSize);
        } else {
            JLabel titleLabel = new JLabel("<html><center>" + tile.getTitle() + "</center></html>", SwingConstants.CENTER);
            titleLabel.setForeground(Color.WHITE);
            panel.add(titleLabel, BorderLayout.CENTER);
        }

        // Add XP value indicator in corner - make sure it doesn't overlap with image
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel xpLabel = new JLabel(String.valueOf(tile.getWeight()) + " XP");
        xpLabel.setForeground(GOLD_COLOR);
        xpLabel.setFont(new Font(xpLabel.getFont().getName(), Font.BOLD, SMALL_FONT_SIZE));
        xpLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(xpLabel, BorderLayout.NORTH);
        panel.add(topPanel, BorderLayout.NORTH);

        // Add status overlay
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
                tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {
            addStatusOverlay(panel, tile.getSubmission());
        }

        // Add click behavior
        addTilePanelListeners(panel, tile);

        return panel;
    }

    // Create a detailed HTML tooltip for the tile
    private String createDetailedTooltip(Tile tile) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><body style='width: 250px'>");

        // Title with weight
        tooltip.append("<div style='font-weight: bold; font-size: 12pt;'>")
                .append(tile.getTitle())
                .append(" (")
                .append(tile.getWeight())
                .append(" XP)</div>");

        // Description if available
        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            tooltip.append("<div style='margin-top: 5px;'>")
                    .append(tile.getDescription())
                    .append("</div>");
        }

        // Add submission status if available
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null) {
            String statusText = getStatusText(tile.getSubmission().getStatus());
            String statusColor = getStatusHexColor(tile.getSubmission().getStatus());

            tooltip.append("<div style='margin-top: 8px;'><b>Status:</b> ")
                    .append("<span style='color: ")
                    .append(statusColor)
                    .append(";'>")
                    .append(statusText)
                    .append("</span></div>");

            // Show submission count if any
            if (tile.getSubmission().getSubmissionCount() > 0) {
                tooltip.append("<div><b>Submissions:</b> ")
                        .append(tile.getSubmission().getSubmissionCount())
                        .append("</div>");
            }

            // Show last update time if available
            if (tile.getSubmission().getLastUpdated() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                tooltip.append("<div><b>Last updated:</b> ")
                        .append(dateFormat.format(tile.getSubmission().getLastUpdated()))
                        .append("</div>");
            }
        }

        // Goal information
        if (tile.getGoals() != null && !tile.getGoals().isEmpty()) {
            tooltip.append("<div style='margin-top: 5px;'><b>Goals:</b></div><ul style='margin-top: 2px; margin-left: 15px; padding-left: 0px;'>");
            for (Goal goal : tile.getGoals()) {
                tooltip.append("<li>")
                        .append(goal.getDescription())
                        .append(": ")
                        .append(goal.getTargetValue())
                        .append("</li>");
            }
            tooltip.append("</ul>");
        }

        tooltip.append("</body></html>");
        return tooltip.toString();
    }

    // Get background color based on submission status
    private Color getTileBackgroundColor(TileSubmission submission) {
        if (submission == null || submission.getStatus() == null ||
                submission.getStatus() == TileSubmissionType.NOT_SUBMITTED) {
            return ColorScheme.DARK_GRAY_COLOR;
        }

        switch (submission.getStatus()) {
            case PENDING:
                return PENDING_BG_COLOR;
            case ACCEPTED:
                return ACCEPTED_BG_COLOR;
            case REQUIRES_INTERACTION:
                return REQUIRES_ACTION_BG_COLOR;
            case DECLINED:
                return DECLINED_BG_COLOR;
            default:
                return ColorScheme.DARK_GRAY_COLOR;
        }
    }

    // Get border color based on submission status
    private Color getTileBorderColor(TileSubmission submission) {
        if (submission == null || submission.getStatus() == null)
            return ColorScheme.BORDER_COLOR;

        switch (submission.getStatus()) {
            case PENDING:
                return PENDING_BORDER_COLOR;
            case ACCEPTED:
                return ACCEPTED_BORDER_COLOR;
            case REQUIRES_INTERACTION:
                return REQUIRES_ACTION_BORDER_COLOR;
            case DECLINED:
                return DECLINED_BORDER_COLOR;
            default:
                return ColorScheme.BORDER_COLOR;
        }
    }

    // Get hex color for tooltip based on submission status
    private String getStatusHexColor(TileSubmissionType status) {
        switch (status) {
            case PENDING:
                return "#3b82f6"; // Blue
            case ACCEPTED:
                return "#22c55e"; // Green
            case REQUIRES_INTERACTION:
                return "#eab308"; // Yellow
            case DECLINED:
                return "#ef4444"; // Red
            default:
                return "#ffffff"; // White
        }
    }

    private void addStatusOverlay(JPanel panel, TileSubmission submission) {
        JPanel overlayPanel = new JPanel(new BorderLayout());
        overlayPanel.setOpaque(false);

        JLabel statusLabel = new JLabel();
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.BOLD, MEDIUM_FONT_SIZE));

        switch (submission.getStatus()) {
            case PENDING:
                statusLabel.setText("PENDING");
                statusLabel.setForeground(PENDING_TEXT_COLOR);
                break;
            case ACCEPTED:
                statusLabel.setText("COMPLETED");
                statusLabel.setForeground(ACCEPTED_TEXT_COLOR);
                break;
            case REQUIRES_INTERACTION:
                statusLabel.setText("NEEDS ACTION");
                statusLabel.setForeground(REQUIRES_ACTION_TEXT_COLOR);
                break;
            case DECLINED:
                statusLabel.setText("DECLINED");
                statusLabel.setForeground(DECLINED_TEXT_COLOR);
                break;
            default:
                // No overlay for NOT_SUBMITTED
                return;
        }

        overlayPanel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(overlayPanel, BorderLayout.SOUTH);
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
                            // Use a better scaling approach that keeps aspect ratio
                            Image scaledImage = getScaledImageImproved(originalImage, tileSize - IMAGE_MARGIN, tileSize - IMAGE_TITLE_OFFSET);
                            ImageIcon icon = new ImageIcon(scaledImage);

                            // Add to cache
                            imageCache.put(imageUrl, icon);

                            SwingUtilities.invokeLater(() -> {
                                imageLabel.setText("");
                                imageLabel.setIcon(icon);
                                // Center the image
                                imageLabel.setHorizontalAlignment(JLabel.CENTER);
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
            imageLabel.setText("<html><center>" + tile.getTitle() + "</center></html>");
            imageLabel.setIcon(null);
            imageLabel.setForeground(Color.WHITE);
            panel.revalidate();
        });
    }

    // Improved scaling method to better handle aspect ratios
    private Image getScaledImageImproved(BufferedImage img, int targetWidth, int targetHeight) {
        if (img == null) {
            return null;
        }

        // Calculate dimensions that preserve aspect ratio
        double imgRatio = (double) img.getWidth() / img.getHeight();
        double targetRatio = (double) targetWidth / targetHeight;

        int scaledWidth, scaledHeight;
        if (imgRatio > targetRatio) {
            // Image is wider than target ratio - constrain by width
            scaledWidth = targetWidth;
            scaledHeight = (int) (scaledWidth / imgRatio);
        } else {
            // Image is taller than target ratio - constrain by height
            scaledHeight = targetHeight;
            scaledWidth = (int) (scaledHeight * imgRatio);
        }

        // Create a high-quality scaled version with transparency support
        Image scaledImage = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

        // Create a new BufferedImage with transparency
        BufferedImage finalImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = finalImage.createGraphics();

        // Set up high quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the scaled image
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return finalImage;
    }

    private void showSubmissionDialog(Tile tile) {
        JDialog dialog = new JDialog(this, "Tile Details", true);
        dialog.setMinimumSize(new Dimension(450, 300));
        dialog.setLayout(new BorderLayout());

        // Main panel with scroll support
        JPanel mainPanel = createTileDetailsPanel(tile);
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Button panel
        JPanel buttonPanel = createDialogButtonPanel(dialog, tile);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Pack the dialog to fit content and ensure minimum size
        dialog.pack();

        // Set maximum height to 80% of screen height
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxHeight = (int)(screenSize.height * 0.8);
        if (dialog.getHeight() > maxHeight) {
            dialog.setSize(dialog.getWidth(), maxHeight);
        }

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createTileDetailsPanel(Tile tile) {
        // Create a split panel with info on left, image on right
        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, 0));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Left side: Info panel with vertical scrolling
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Title
        JLabel titleLabel = new JLabel("<html><h2 style='margin: 0;'>" + tile.getTitle() + "</h2></html>");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // XP (formerly weight)
        JLabel xpLabel = new JLabel("XP: " + tile.getWeight());
        xpLabel.setForeground(Color.LIGHT_GRAY);
        xpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        xpLabel.setBorder(new EmptyBorder(MEDIUM_SPACING, 0, PADDING, 0));

        // Description with proper wrapping
        JTextArea descriptionArea = new JTextArea(tile.getDescription());
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setForeground(Color.WHITE);
        descriptionArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Set preferred size for proper wrapping
        descriptionArea.setPreferredSize(new Dimension(250, descriptionArea.getPreferredSize().height));

        infoPanel.add(titleLabel);
        infoPanel.add(xpLabel);
        infoPanel.add(descriptionArea);

        // Add goals if present
        if (tile.getGoals() != null && !tile.getGoals().isEmpty()) {
            JPanel goalsPanel = new JPanel();
            goalsPanel.setLayout(new BoxLayout(goalsPanel, BoxLayout.Y_AXIS));
            goalsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            goalsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            goalsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel goalsLabel = new JLabel("Goals:");
            goalsLabel.setForeground(Color.WHITE);
            goalsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            goalsPanel.add(goalsLabel);
            goalsPanel.add(Box.createVerticalStrut(5));

            for (Goal goal : tile.getGoals()) {
                JLabel goalLabel = new JLabel("• " + goal.getDescription() + ": " + goal.getTargetValue());
                goalLabel.setForeground(Color.LIGHT_GRAY);
                goalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                goalsPanel.add(goalLabel);
                goalsPanel.add(Box.createVerticalStrut(2));
            }

            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(goalsPanel);
        }

        // Add submission status if any
        if (tile.getSubmission() != null && tile.getSubmission().getStatus() != null &&
                tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED) {

            JPanel statusPanel = new JPanel();
            statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
            statusPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            statusPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel statusLabel = new JLabel("Status: " + getStatusText(tile.getSubmission().getStatus()));
            statusLabel.setForeground(getStatusColor(tile.getSubmission().getStatus()));
            statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            statusPanel.add(statusLabel);

            if (tile.getSubmission().getSubmissionCount() > 0) {
                JLabel countLabel = new JLabel("Submissions: " + tile.getSubmission().getSubmissionCount());
                countLabel.setForeground(Color.LIGHT_GRAY);
                countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                statusPanel.add(Box.createVerticalStrut(2));
                statusPanel.add(countLabel);
            }

            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(statusPanel);
        }

        // Right side: Image panel
        JPanel imagePanel = createTileImagePanel(tile);

        mainPanel.add(infoPanel, BorderLayout.CENTER);
        mainPanel.add(imagePanel, BorderLayout.EAST);

        return mainPanel;
    }

    private String getStatusText(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "Pending Review";
            case ACCEPTED: return "Completed";
            case REQUIRES_INTERACTION: return "Needs Action";
            case DECLINED: return "Declined";
            default: return "Not Submitted";
        }
    }

    private Color getStatusColor(TileSubmissionType status) {
        switch (status) {
            case PENDING: return PENDING_TEXT_COLOR;
            case ACCEPTED: return ACCEPTED_TEXT_COLOR;
            case REQUIRES_INTERACTION: return REQUIRES_ACTION_TEXT_COLOR;
            case DECLINED: return DECLINED_TEXT_COLOR;
            default: return Color.LIGHT_GRAY;
        }
    }

    private JPanel createTileImagePanel(Tile tile) {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        imagePanel.setPreferredSize(new Dimension(DETAIL_IMAGE_SIZE, DETAIL_IMAGE_SIZE));

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
                                Image scaledImage = getScaledImageImproved(originalImage, DETAIL_IMAGE_SIZE, DETAIL_IMAGE_SIZE);
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

        // If tile is already completed, adjust the UI accordingly
        if (tile.getSubmission() != null &&
                tile.getSubmission().getStatus() == TileSubmissionType.ACCEPTED) {
            submitButton.setText("Already Completed");
            submitButton.setEnabled(false);
        }

        if (currentBingo.isLocked()) {
            submitButton.setText("Submissions locked");
            submitButton.setEnabled(false);
        }

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

    private boolean isPinned() {
        return currentBingo != null &&
               currentBingo.getId().toString().equals(plugin.getConfig().pinnedBingoId());
    }
}
