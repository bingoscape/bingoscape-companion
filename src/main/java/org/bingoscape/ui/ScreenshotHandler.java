package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Tile;
import org.bingoscape.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Handles screenshot capture, preview, and saving functionality.
 * Extracted from BingoScapePanel to improve separation of concerns.
 */
public class ScreenshotHandler {

    private final BingoScapePlugin plugin;
    private final Component parentComponent;

    /**
     * Creates a new screenshot handler.
     *
     * @param plugin The main plugin instance
     * @param parentComponent The parent component for dialogs
     */
    public ScreenshotHandler(BingoScapePlugin plugin, Component parentComponent) {
        this.plugin = plugin;
        this.parentComponent = parentComponent;
    }

    /**
     * Opens a screenshot dialog for general screenshot capture.
     */
    public void openScreenshotDialog() {
        plugin.takeScreenshot(null, (screenshotBytes) -> {
            if (screenshotBytes != null) {
                SwingUtilities.invokeLater(() -> {
                    showScreenshotPreviewDialog(screenshotBytes);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentComponent,
                        "Failed to take screenshot.",
                        "Screenshot Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    /**
     * Shows a preview dialog for a screenshot with tile submission options.
     *
     * @param tile The tile to submit the screenshot for
     * @param screenshotBytes The screenshot data
     */
    public void showTileScreenshotPreviewDialog(Tile tile, byte[] screenshotBytes) {
        JDialog previewDialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(parentComponent),
            "Screenshot for " + tile.getTitle(),
            true
        );
        previewDialog.setSize(UIConstants.SCREENSHOT_DIALOG_WIDTH, UIConstants.SCREENSHOT_DIALOG_HEIGHT_WITH_TILE);
        previewDialog.setLocationRelativeTo(parentComponent);
        previewDialog.setLayout(new BorderLayout());

        // Display the screenshot
        JLabel screenshotLabel = new JLabel(new ImageIcon(screenshotBytes));
        JScrollPane scrollPane = new JScrollPane(screenshotLabel);
        scrollPane.setPreferredSize(new Dimension(UIConstants.SCREENSHOT_SCROLL_WIDTH, UIConstants.SCREENSHOT_SCROLL_HEIGHT));
        previewDialog.add(scrollPane, BorderLayout.CENTER);

        // Add info label with tile details
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html><center><b>" + tile.getTitle() + "</b><br>" +
                                     "Screenshot taken! You can submit, save, or close.</center></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(UIConstants.DIALOG_PADDING, UIConstants.DIALOG_PADDING,
                                           UIConstants.DIALOG_PADDING, UIConstants.DIALOG_PADDING));
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        previewDialog.add(infoPanel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton submitButton = new JButton("Submit for " + tile.getTitle());
        JButton saveButton = new JButton("Save Image");
        JButton closeButton = new JButton("Close");

        submitButton.addActionListener(e -> {
            plugin.submitTileCompletionWithScreenshot(tile.getId(), screenshotBytes);
            previewDialog.dispose();
            JOptionPane.showMessageDialog(parentComponent,
                "Screenshot submitted for " + tile.getTitle(),
                "Submission Sent",
                JOptionPane.INFORMATION_MESSAGE);
        });

        saveButton.addActionListener(e -> {
            saveScreenshotToFile(screenshotBytes);
        });

        closeButton.addActionListener(e -> previewDialog.dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        previewDialog.setVisible(true);
    }

    /**
     * Shows a preview dialog for a general screenshot.
     *
     * @param screenshotBytes The screenshot data
     */
    private void showScreenshotPreviewDialog(byte[] screenshotBytes) {
        JDialog previewDialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(parentComponent),
            "Screenshot Preview",
            true
        );
        previewDialog.setSize(UIConstants.SCREENSHOT_DIALOG_WIDTH, UIConstants.SCREENSHOT_DIALOG_HEIGHT);
        previewDialog.setLocationRelativeTo(parentComponent);
        previewDialog.setLayout(new BorderLayout());

        // Display the screenshot
        JLabel screenshotLabel = new JLabel(new ImageIcon(screenshotBytes));
        JScrollPane scrollPane = new JScrollPane(screenshotLabel);
        scrollPane.setPreferredSize(new Dimension(UIConstants.SCREENSHOT_SCROLL_WIDTH, UIConstants.SCREENSHOT_SCROLL_HEIGHT));
        previewDialog.add(scrollPane, BorderLayout.CENTER);

        // Add info label
        JLabel infoLabel = new JLabel("Screenshot taken! You can save this image or use it for manual submission.");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(UIConstants.DIALOG_PADDING, UIConstants.DIALOG_PADDING,
                                           UIConstants.DIALOG_PADDING, UIConstants.DIALOG_PADDING));
        previewDialog.add(infoLabel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton saveButton = new JButton("Save Image");
        JButton closeButton = new JButton("Close");

        saveButton.addActionListener(e -> {
            saveScreenshotToFile(screenshotBytes);
        });

        closeButton.addActionListener(e -> previewDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        previewDialog.add(buttonPanel, BorderLayout.SOUTH);
        previewDialog.setVisible(true);
    }

    /**
     * Saves a screenshot to a file chosen by the user.
     *
     * @param screenshotBytes The screenshot data to save
     */
    private void saveScreenshotToFile(byte[] screenshotBytes) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG files", "png"));
        fileChooser.setSelectedFile(new File("bingoscape_screenshot.png"));
        fileChooser.setDialogTitle("Save Screenshot");

        if (fileChooser.showSaveDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Ensure .png extension
                if (!selectedFile.getName().toLowerCase().endsWith(".png")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".png");
                }

                java.nio.file.Files.write(selectedFile.toPath(), screenshotBytes);
                JOptionPane.showMessageDialog(parentComponent,
                    "Screenshot saved to: " + selectedFile.getAbsolutePath(),
                    "Screenshot Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentComponent,
                    "Failed to save screenshot: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
