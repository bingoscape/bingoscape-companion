package org.bingoscape.board;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.models.Tile;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * Confirm step before a manual tile submission: shows the captured screenshot with Cancel / Submit.
 * Must be created on the EDT.
 */
public final class ScreenshotPreviewDialog {
    private static final int MARGIN = 80;

    private ScreenshotPreviewDialog() {
    }

    /**
     * Builds the modal preview. The caller shows it with {@code setVisible(true)}.
     *
     * @param onSubmit called with the screenshot when the user confirms
     */
    public static JDialog create(Window owner, Tile tile, byte[] screenshotPng, Consumer<byte[]> onSubmit) {
        JDialog dialog = new JDialog(owner, "Submit: " + tile.getTitle(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        ImageIcon icon = new ImageIcon(screenshotPng);
        JScrollPane scrollPane = new JScrollPane(new JLabel(icon));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Submit");
        cancelButton.addActionListener(e -> dialog.dispose());
        submitButton.addActionListener(e -> {
            dialog.dispose();
            onSubmit.accept(screenshotPng);
        });
        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Show the whole screenshot when it fits, but never exceed 80% of the screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(icon.getIconWidth() + MARGIN, (int) (screen.width * 0.8));
        int height = Math.min(icon.getIconHeight() + MARGIN + 40, (int) (screen.height * 0.8));
        dialog.setSize(Math.max(400, width), Math.max(300, height));
        dialog.setLocationRelativeTo(owner);
        dialog.getRootPane().setDefaultButton(submitButton);
        return dialog;
    }
}
