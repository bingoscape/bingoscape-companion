package org.bingoscape.board;

import javax.inject.Singleton;
import java.awt.Point;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Board overlay UI state. Written from the EDT (input, side panel) and read by the client thread
 * (render), so every field is volatile or atomic.
 */
@Singleton
public class BoardState {
    private volatile boolean visible;
    private final AtomicInteger pendingCaptures = new AtomicInteger();
    private volatile UUID detailsTileId;
    private volatile int detailsScrollPx;
    private volatile boolean reloading;
    private volatile long reloadStartedAtMs;
    private volatile Point lastMouse;

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        reset();
    }

    public void reset() {
        detailsTileId = null;
        detailsScrollPx = 0;
        reloading = false;
    }

    /**
     * True while at least one screenshot capture is pending; the board must not be drawn into it.
     */
    public boolean isCaptureSuppressed() {
        return pendingCaptures.get() > 0;
    }

    public void beginCapture() {
        pendingCaptures.incrementAndGet();
    }

    public void endCapture() {
        pendingCaptures.updateAndGet(count -> Math.max(0, count - 1));
    }

    public void resetCaptures() {
        pendingCaptures.set(0);
    }

    public UUID getDetailsTileId() {
        return detailsTileId;
    }

    public void openDetails(UUID tileId) {
        detailsScrollPx = 0;
        detailsTileId = tileId;
    }

    public void closeDetails() {
        detailsTileId = null;
        detailsScrollPx = 0;
    }

    public int getDetailsScrollPx() {
        return detailsScrollPx;
    }

    public void setDetailsScrollPx(int detailsScrollPx) {
        this.detailsScrollPx = Math.max(0, detailsScrollPx);
    }

    public boolean isReloading() {
        return reloading;
    }

    public void startReload() {
        reloadStartedAtMs = System.currentTimeMillis();
        reloading = true;
    }

    public void finishReload() {
        reloading = false;
    }

    public long getReloadStartedAtMs() {
        return reloadStartedAtMs;
    }

    public Point getLastMouse() {
        return lastMouse;
    }

    public void setLastMouse(Point lastMouse) {
        this.lastMouse = lastMouse;
    }
}
