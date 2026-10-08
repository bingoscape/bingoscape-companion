package org.bingoscape.board;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Tile;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Routes mouse and keyboard input to the board overlay. Unlike a modal overlay, only events inside the
 * board are consumed; mouse movement is never consumed so the client keeps tracking the cursor.
 * Callbacks run on the AWT event thread.
 */
@Singleton
public class BoardInputListener implements MouseListener, MouseWheelListener, KeyListener {
    private static final int SCROLL_STEP_PX = 16;
    private static final int SCALE_STEP = 10;
    private static final int MIN_SCALE = 50;
    private static final int MAX_SCALE = 200;

    private final BingoBoardOverlay overlay;
    private final BoardState state;
    private final BingoScapePlugin plugin;
    private final BingoScapeConfig config;
    private final ConfigManager configManager;

    // A press consumed by the board also consumes its release, so the game never sees half a click
    private volatile boolean pressStartedInside;

    @Inject
    public BoardInputListener(BingoBoardOverlay overlay, BoardState state, BingoScapePlugin plugin,
                              BingoScapeConfig config, ConfigManager configManager) {
        this.overlay = overlay;
        this.state = state;
        this.plugin = plugin;
        this.config = config;
        this.configManager = configManager;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        state.setLastMouse(e.getPoint());
        HitMap hitMap = overlay.hitMap();
        Point p = toLocal(e, hitMap);
        // Alt is RuneLite's overlay drag modifier; right click must reach the game to open the overlay menu
        if (p == null || e.isAltDown() || e.getButton() == MouseEvent.BUTTON3) {
            return e;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            handleLeftClick(hitMap, p);
        }

        pressStartedInside = true;
        e.consume();
        return e;
    }

    private void handleLeftClick(HitMap hitMap, Point p) {
        switch (hitMap.layout.buttonAt(p.x, p.y)) {
            case PIN:
                plugin.toggleBoardPin();
                return;
            case RELOAD:
                if (!state.isReloading()) {
                    plugin.reloadBoard();
                }
                return;
            case CLOSE:
                plugin.hideBoard();
                return;
            default:
                break;
        }

        TileDetailsView.DetailsHit details = hitMap.details;
        if (details != null) {
            if (details.closeButton.contains(p)) {
                state.closeDetails();
            } else if (details.primaryButton.contains(p) && details.primaryEnabled) {
                Tile tile = hitMap.detailsTile;
                state.closeDetails();
                plugin.captureAndPreviewSubmission(tile);
            }
            return;
        }

        Tile tile = hitMap.tileAt(p.x, p.y);
        if (tile != null && !tile.isHidden() && tile.getId() != null) {
            state.openDetails(tile.getId());
        }
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent e) {
        if (pressStartedInside) {
            pressStartedInside = false;
            e.consume();
        }
        return e;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent e) {
        Point p = toLocal(e, overlay.hitMap());
        if (p != null && !e.isAltDown() && e.getButton() == MouseEvent.BUTTON1) {
            e.consume();
        }
        return e;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent e) {
        state.setLastMouse(null);
        return e;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent e) {
        state.setLastMouse(e.getPoint());
        return e;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e) {
        state.setLastMouse(e.getPoint());
        return e;
    }

    @Override
    public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e) {
        HitMap hitMap = overlay.hitMap();
        if (toLocal(e, hitMap) == null) {
            return e;
        }

        if (hitMap.details != null) {
            state.setDetailsScrollPx(state.getDetailsScrollPx() + e.getWheelRotation() * SCROLL_STEP_PX);
        } else if (e.isControlDown()) {
            int scale = config.boardScale() - e.getWheelRotation() * SCALE_STEP;
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
            configManager.setConfiguration(BingoScapeConfig.CONFIG_GROUP, "boardScale", scale);
        }

        // Never let the wheel zoom the camera while over the board
        e.consume();
        return e;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_ESCAPE || !config.boardCloseOnEscape() || overlay.hitMap() == null) {
            return;
        }

        if (state.getDetailsTileId() != null) {
            state.closeDetails();
        } else {
            plugin.hideBoard();
        }
        e.consume();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Converts a canvas point to overlay coordinates, or null when the board is hidden or the point is outside it.
     */
    private Point toLocal(MouseEvent e, HitMap hitMap) {
        if (hitMap == null) {
            return null;
        }

        int x = e.getX() - hitMap.origin.x;
        int y = e.getY() - hitMap.origin.y;
        return hitMap.layout.contains(x, y) ? new Point(x, y) : null;
    }
}
