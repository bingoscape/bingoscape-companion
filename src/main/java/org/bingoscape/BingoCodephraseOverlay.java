package org.bingoscape;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import org.bingoscape.models.Bingo;
import org.bingoscape.ui.ColorPalette;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Overlay that displays the codephrase for the currently selected bingo
 */
public class BingoCodephraseOverlay extends OverlayPanel {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int MAX_WIDTH = 200; // Maximum width for the overlay

    private final BingoScapePlugin plugin;
    private final BingoScapeConfig config;
    private final Client client;

    @Inject
    public BingoCodephraseOverlay(Client client, BingoScapePlugin plugin, BingoScapeConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPriority(0.1f);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Check if overlay is enabled in settings
        if (!config.showCodephraseOverlay()) {
            return null;
        }

        // Check if a bingo is selected
        Bingo currentBingo = plugin.getCurrentBingo();
        if (currentBingo == null || currentBingo.getCodephrase() == null || currentBingo.getCodephrase().trim().isEmpty()) {
            return null;
        }

        // Get all text components that will be displayed
        String title = plugin.getCurrentEvent().getTitle();
        String boardText = "Board: " + currentBingo.getTitle();
        String codephraseText = "Codephrase: " + currentBingo.getCodephrase();
        String timeText = "Time: " + utcNow();

        // Calculate maximum width based on all components
        FontMetrics metrics = graphics.getFontMetrics();
        int maxWidth = Math.max(
            Math.max(metrics.stringWidth(title), metrics.stringWidth(boardText)),
            Math.max(metrics.stringWidth(codephraseText), metrics.stringWidth(timeText))
        );

        // Add some padding
        maxWidth += 20;

        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(title)
                .color(ColorPalette.GOLD)
                .build());

        // Add board
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Board:")
                .right(currentBingo.getTitle())
                .rightColor(Color.WHITE)
                .build());

        // Add codephrase
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Codephrase:")
                .right(currentBingo.getCodephrase())
                .rightColor(Color.WHITE)
                .build());

        // Add current time
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time:")
                .right(utcNow())
                .rightColor(Color.LIGHT_GRAY)
                .build());

        // Set preferred size based on maximum width
        panelComponent.setPreferredSize(new Dimension(maxWidth, 0));

        return super.render(graphics);
    }

    private String utcNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date()) + " UTC";
    }
}
