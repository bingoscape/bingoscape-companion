package org.bingoscape.ui.components;

import net.runelite.client.game.ItemManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.bingoscape.ui.StyleConstants;
import org.bingoscape.ui.TileHoverCardBuilder;

import javax.swing.*;
import java.awt.*;

/**
 * Custom popup window displaying rich tile information on hover.
 * Replaces the native JToolTip with a fully customizable component.
 * Matches the design from the Bingoscape web app.
 */
public class TileHoverCard extends JWindow {

    private final Tile tile;
    private final Bingo bingo;
    private final ItemManager itemManager;
    private JPanel contentPanel;

    public TileHoverCard(Window owner, Tile tile, Bingo bingo, ItemManager itemManager) {
        super(owner);
        this.tile = tile;
        this.bingo = bingo;
        this.itemManager = itemManager;
        initComponents();
    }

    private void initComponents() {
        try {
            // Build content using the builder
            TileHoverCardBuilder builder = new TileHoverCardBuilder(tile, bingo, itemManager);
            contentPanel = builder.build();

            // Wrap content in a panel with border and shadow effect
            JPanel rootPanel = new JPanel(new BorderLayout());
            rootPanel.setBackground(StyleConstants.BACKGROUND);
            rootPanel.setBorder(new StyleConstants.RoundedBorder(
                StyleConstants.SECONDARY,
                1,
                StyleConstants.BORDER_RADIUS
            ));

            rootPanel.add(contentPanel, BorderLayout.CENTER);

            setContentPane(rootPanel);
            pack();

            // Make sure the window is always on top but doesn't steal focus
            setAlwaysOnTop(true);
            setFocusableWindowState(false);
            setFocusable(false);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize hover card components");
            e.printStackTrace();
        }
    }

    /**
     * Show the hover card at the specified location relative to a component
     * @param component The component to position relative to
     * @param preferredSide The preferred side ("right", "left", "bottom", "top")
     */
    public void showRelativeTo(Component component, String preferredSide) {
        Point componentLocation = component.getLocationOnScreen();
        Dimension componentSize = component.getSize();
        Dimension hoverCardSize = getSize();

        // Calculate position based on preferred side
        int x, y;

        switch (preferredSide.toLowerCase()) {
            case "left":
                x = componentLocation.x - hoverCardSize.width - 8;
                y = componentLocation.y;
                break;

            case "top":
                x = componentLocation.x;
                y = componentLocation.y - hoverCardSize.height - 8;
                break;

            case "bottom":
                x = componentLocation.x;
                y = componentLocation.y + componentSize.height + 8;
                break;

            case "right":
            default:
                x = componentLocation.x + componentSize.width + 8;
                y = componentLocation.y;
                break;
        }

        // Ensure the hover card is fully visible on screen
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        Rectangle screenBounds = gc != null ? gc.getBounds() :
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();

        // Adjust x position if it goes off screen
        if (x + hoverCardSize.width > screenBounds.x + screenBounds.width) {
            // Try left side instead
            x = componentLocation.x - hoverCardSize.width - 8;
        }
        if (x < screenBounds.x) {
            // Center horizontally if doesn't fit on either side
            x = componentLocation.x + (componentSize.width - hoverCardSize.width) / 2;
        }

        // Adjust y position if it goes off screen
        if (y + hoverCardSize.height > screenBounds.y + screenBounds.height) {
            y = screenBounds.y + screenBounds.height - hoverCardSize.height - 8;
        }
        if (y < screenBounds.y) {
            y = screenBounds.y + 8;
        }

        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Hide the hover card
     */
    public void hideCard() {
        setVisible(false);
    }

    /**
     * Dispose of the hover card and free resources
     */
    public void disposeCard() {
        setVisible(false);
        dispose();
    }
}
