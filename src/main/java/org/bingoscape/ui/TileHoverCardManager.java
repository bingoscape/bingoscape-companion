package org.bingoscape.ui;

import net.runelite.client.game.ItemManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.bingoscape.ui.components.TileHoverCard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Manager class for handling tile hover card lifecycle, timing, and positioning.
 * Ensures only one hover card is visible at a time and handles show/hide delays.
 */
public class TileHoverCardManager {

    private static TileHoverCardManager instance;

    private TileHoverCard currentHoverCard;
    private Timer showTimer;
    private Timer hideTimer;

    private TileHoverCardManager() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance
     */
    public static TileHoverCardManager getInstance() {
        if (instance == null) {
            instance = new TileHoverCardManager();
        }
        return instance;
    }

    /**
     * Attach a hover card to a component
     * @param component The component to attach the hover card to
     * @param tile The tile data
     * @param bingo The bingo data
     * @param itemManager The item manager for loading item images
     */
    public void attachHoverCard(JComponent component, Tile tile, Bingo bingo, ItemManager itemManager) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Cancel hide timer when entering a tile (in case user is moving to hover card through other tiles)
                cancelHideTimer();
                scheduleShow(component, tile, bingo, itemManager);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Cancel show timer if we leave before it completes
                if (showTimer != null && showTimer.isRunning()) {
                    showTimer.stop();
                }
                // Schedule hide only if a hover card is currently visible
                if (currentHoverCard != null && currentHoverCard.isVisible()) {
                    scheduleHide();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Hide immediately on click
                cancelPendingActions();
                hideCurrentCard();
            }
        });

        // Also add mouse motion listener to tile to cancel hide when hovering
        component.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // If hide timer is running (mouse might have briefly left), immediately cancel it
                cancelHideTimer();
            }
        });
    }

    /**
     * Schedule showing the hover card after a delay
     */
    private void scheduleShow(Component component, Tile tile, Bingo bingo, ItemManager itemManager) {
        // Cancel any pending hide action
        cancelHideTimer();

        // Cancel any pending show action
        if (showTimer != null && showTimer.isRunning()) {
            showTimer.stop();
        }

        // Schedule new show action - but don't hide the current card yet
        // This allows moving over other tiles while navigating to the hover card
        showTimer = new Timer(StyleConstants.HOVER_SHOW_DELAY, e -> {
            // Only hide current card and show new one after the delay completes
            showHoverCard(component, tile, bingo, itemManager);
        });
        showTimer.setRepeats(false);
        showTimer.start();
    }

    /**
     * Schedule hiding the hover card after a delay
     */
    private void scheduleHide() {
        // Cancel any pending show action
        if (showTimer != null && showTimer.isRunning()) {
            showTimer.stop();
        }

        // If no card is visible, nothing to hide
        if (currentHoverCard == null || !currentHoverCard.isVisible()) {
            return;
        }

        // Schedule hide action
        hideTimer = new Timer(StyleConstants.HOVER_HIDE_DELAY, e -> {
            hideCurrentCard();
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    /**
     * Show the hover card for a component
     */
    private void showHoverCard(Component component, Tile tile, Bingo bingo, ItemManager itemManager) {
        try {
            // Hide any existing card
            hideCurrentCard();

            // Get the parent window
            Window parentWindow = SwingUtilities.getWindowAncestor(component);

            if (parentWindow == null) {
                // No parent window found - cannot show hover card
                return;
            }

            // Create and show new hover card
            currentHoverCard = new TileHoverCard(parentWindow, tile, bingo, itemManager);
            currentHoverCard.showRelativeTo(component, "right");

            // Add mouse listener to hover card to keep it visible when hovering over it
            addHoverCardMouseListeners();
        } catch (Exception e) {
            // Failed to show hover card - silently ignore
        }
    }

    /**
     * Add mouse listeners to the hover card to handle hovering over it
     */
    private void addHoverCardMouseListeners() {
        if (currentHoverCard == null) return;

        MouseAdapter hoverCardListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Immediately cancel hide timer when mouse enters hover card
                cancelHideTimer();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Only schedule hide if mouse truly left the entire hover card window
                Point mousePos = e.getLocationOnScreen();
                Rectangle cardBounds = currentHoverCard.getBounds();
                cardBounds.setLocation(currentHoverCard.getLocationOnScreen());

                if (!cardBounds.contains(mousePos)) {
                    scheduleHide();
                }
            }
        };

        java.awt.event.MouseMotionAdapter motionAdapter = new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Immediately cancel any pending hide when mouse moves inside hover card
                cancelHideTimer();
            }
        };

        // Add listeners to the hover card window and ALL its child components recursively
        addListenersRecursively(currentHoverCard, hoverCardListener, motionAdapter);
    }

    /**
     * Recursively add mouse listeners to a component and all its children
     */
    private void addListenersRecursively(java.awt.Container container, MouseAdapter mouseAdapter, java.awt.event.MouseMotionAdapter motionAdapter) {
        // Add to the container itself
        container.addMouseListener(mouseAdapter);
        container.addMouseMotionListener(motionAdapter);

        // Add to all child components
        for (java.awt.Component child : container.getComponents()) {
            child.addMouseListener(mouseAdapter);
            child.addMouseMotionListener(motionAdapter);

            // If child is a container, recurse into it
            if (child instanceof java.awt.Container) {
                addListenersRecursively((java.awt.Container) child, mouseAdapter, motionAdapter);
            }
        }
    }

    /**
     * Cancel the hide timer if it's running
     */
    private void cancelHideTimer() {
        if (hideTimer != null && hideTimer.isRunning()) {
            hideTimer.stop();
        }
    }

    /**
     * Hide the current hover card
     */
    private void hideCurrentCard() {
        if (currentHoverCard != null) {
            currentHoverCard.disposeCard();
            currentHoverCard = null;
        }
    }

    /**
     * Cancel all pending show/hide actions
     */
    private void cancelPendingActions() {
        if (showTimer != null && showTimer.isRunning()) {
            showTimer.stop();
        }
        if (hideTimer != null && hideTimer.isRunning()) {
            hideTimer.stop();
        }
    }

    /**
     * Clean up resources (call when plugin is disabled or unloaded)
     */
    public void cleanup() {
        cancelPendingActions();
        hideCurrentCard();
    }
}
