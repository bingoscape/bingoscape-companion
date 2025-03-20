package org.bingoscape;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import org.bingoscape.models.Bingo;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Overlay that displays the codephrase for the currently selected bingo
 */
public class BingoCodephraseOverlay extends OverlayPanel {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final BingoScapePlugin plugin;
    private final BingoScapeConfig config;
    private final Client client;

    @Inject
    public BingoCodephraseOverlay(Client client, BingoScapePlugin plugin, BingoScapeConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPriority(OverlayPriority.LOW);
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

        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.getCurrentEvent().getTitle())
                .color(new Color(255, 215, 0)) // Gold color
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

        return super.render(graphics);
    }

    private String utcNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date()) + " UTC";
    }
}
