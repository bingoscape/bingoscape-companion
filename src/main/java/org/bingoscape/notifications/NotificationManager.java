package org.bingoscape.notifications;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WidgetNode;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * Manages the display queue and lifecycle of in-game notifications.
 * <p>
 * This manager ensures notifications are displayed one at a time without
 * overlapping
 * with existing game notifications, and properly cleans up when the player logs
 * out.
 */
@Slf4j
@Singleton
public class NotificationManager {
    // RuneLite notification system constants
    private static final int NOTIFICATION_DISPLAY_SCRIPT_ID = 3343;
    private static final int NOTIFICATION_WIDGET_INTERFACE_ID = 660;
    private static final int NOTIFICATION_WIDGET_CHILD_ID = 1;
    private static final int NOTIFICATION_COMPONENT_ID = WidgetUtil.packComponentId(303, 2);

    private final Queue<Notification> pendingNotifications = new ConcurrentLinkedQueue<>();

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    /**
     * Processes the next queued notification on each game tick if no notification
     * is currently visible.
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        processNextNotification();
    }

    /**
     * Clears pending notifications when the player is not logged in.
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (isLoggedOut(event.getGameState())) {
            clearNotifications();
        }
    }

    /**
     * Initializes the notification manager and registers event listeners.
     */
    public void startUp() {
        log.debug("NotificationManager starting up");
        eventBus.register(this);
    }

    /**
     * Shuts down the notification manager, clears pending notifications, and
     * unregisters event listeners.
     */
    public void shutDown() {
        log.debug("NotificationManager shutting down");
        clearNotifications();
        eventBus.unregister(this);
    }

    /**
     * Queues a notification with default color for display.
     *
     * @param title The notification title
     * @param text  The notification message text
     */
    public void addNotification(String title, String text) {
        addNotification(title, text, -1);
    }

    /**
     * Queues a notification with custom color for display.
     *
     * @param title The notification title
     * @param text  The notification message text
     * @param color The RGB color value for the notification
     */
    public void addNotification(String title, String text, int color) {
        Notification notification = new Notification(title, text, color);
        pendingNotifications.offer(notification);
        log.debug("Queued notification: {} - {} (pending: {})", title, text, pendingNotifications.size());
    }

    /**
     * Removes all pending notifications from the queue.
     */
    public void clearNotifications() {
        int cleared = pendingNotifications.size();
        pendingNotifications.clear();
        if (cleared > 0) {
            log.debug("Cleared {} pending notification(s)", cleared);
        }
    }

    /**
     * Processes the next notification in the queue if no notification is currently
     * visible.
     */
    private void processNextNotification() {
        if (isNotificationCurrentlyVisible() || pendingNotifications.isEmpty()) {
            return;
        }

        Notification notification = pendingNotifications.poll();
        if (notification != null) {
            displayNotification(notification);
        }
    }

    /**
     * Checks if any notification widget is currently visible on screen.
     *
     * @return {@code true} if a notification is visible (prevents overlapping
     *         notifications)
     */
    private boolean isNotificationCurrentlyVisible() {
        return client.getWidget(NOTIFICATION_WIDGET_INTERFACE_ID, NOTIFICATION_WIDGET_CHILD_ID) != null;
    }

    /**
     * Determines if the given game state represents a logged-out state.
     *
     * @param gameState The current game state
     * @return {@code true} if the player is not logged in
     */
    private boolean isLoggedOut(GameState gameState) {
        switch (gameState) {
            case HOPPING:
            case LOGGING_IN:
            case LOGIN_SCREEN:
            case LOGIN_SCREEN_AUTHENTICATOR:
            case CONNECTION_LOST:
                return true;
            default:
                return false;
        }
    }

    /**
     * Displays a notification using the RuneLite notification system.
     * <p>
     * Opens the notification interface, runs the display script with the
     * notification data,
     * and schedules automatic cleanup when the notification finishes its animation.
     *
     * @param notification The notification to display
     * @throws IllegalStateException    if the client is in an invalid state
     * @throws IllegalArgumentException if the notification parameters are invalid
     */
    private void displayNotification(Notification notification) {
        WidgetNode notificationNode = client.openInterface(
                NOTIFICATION_COMPONENT_ID,
                NOTIFICATION_WIDGET_INTERFACE_ID,
                WidgetModalMode.MODAL_CLICKTHROUGH);

        Widget notificationWidget = client.getWidget(
                NOTIFICATION_WIDGET_INTERFACE_ID,
                NOTIFICATION_WIDGET_CHILD_ID);

        client.runScript(
                NOTIFICATION_DISPLAY_SCRIPT_ID,
                notification.getTitle(),
                notification.getText(),
                notification.getColor());

        scheduleNotificationCleanup(notificationNode, notificationWidget);
    }

    /**
     * Schedules the cleanup of a notification widget after its animation completes.
     * <p>
     * Polls the widget width until it reaches zero (indicating the slide-out
     * animation finished),
     * then closes the interface.
     *
     * @param notificationNode   The widget node to close
     * @param notificationWidget The widget to monitor for animation completion
     */
    private void scheduleNotificationCleanup(WidgetNode notificationNode, Widget notificationWidget) {
        clientThread.invokeLater(() -> {
            if (notificationWidget == null) {
                log.warn("Notification widget became null during cleanup");
                return true;
            }

            if (notificationWidget.getWidth() > 0) {
                return false; // Animation still in progress, check again next tick
            }

            client.closeInterface(notificationNode, true);
            return true; // Cleanup complete
        });
    }
}
