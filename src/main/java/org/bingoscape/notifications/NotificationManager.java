package org.bingoscape.notifications;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WidgetNode;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class NotificationManager
{
	private static final int SCRIPT_ID = 3343; // NOTIFICATION_DISPLAY_INIT

	private static final int COMPONENT_ID = WidgetUtil.packComponentId(303, 2); // (interfaceId << 16) | childId

	private static final int INTERFACE_ID = 660;

	private final Queue<Notification> notifications = new ConcurrentLinkedQueue<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Subscribe
	public void onGameTick(GameTick event)
	{
		processNotification();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		// Clear notifications when not logged in
		switch (gameStateChanged.getGameState())
		{
			case HOPPING:
			case LOGGING_IN:
			case LOGIN_SCREEN:
			case LOGIN_SCREEN_AUTHENTICATOR:
			case CONNECTION_LOST:
				clearNotifications();
				break;
		}
	}

	public void startUp()
	{
		log.debug("NotificationManager startUp()");
		eventBus.register(this);
	}

	public void shutDown()
	{
		log.debug("NotificationManager shutDown()");
		clearNotifications();
		eventBus.unregister(this);
	}

	public void addNotification(String title, String text)
	{
		addNotification(title, text, -1);
	}

	public void addNotification(String title, String text, int color)
	{
		Notification notification = new Notification(title, text, color);
		notifications.offer(notification);
	}

	/**
	 * Processes a notification
	 */
	private void processNotification()
	{
		// Don't process when any type of notification is visible (collection log, league task, one of our own, ...)
		if (client.getWidget(INTERFACE_ID, 1) != null)
		{
			return;
		}
		// Only process notifications if the queue is not empty AND the queue is not processing any notifications.
		if (!notifications.isEmpty())
		{
			// Get and remove the first element in the notifications queue.
			Notification notification = notifications.poll();

			// Display notification
			displayNotification(notification);
		}
	}


	/**
	 * Clears the current list of notifications and makes sure the processing notifications state is set to false
	 */
	public void clearNotifications()
	{
		notifications.clear();
	}

	/**
	 * Display a notification and close it afterward.
	 *
	 * @param notification Notification
	 */
	private void displayNotification(Notification notification) throws IllegalStateException, IllegalArgumentException
	{
		WidgetNode notificationNode = client.openInterface(COMPONENT_ID, INTERFACE_ID, WidgetModalMode.MODAL_CLICKTHROUGH);
		Widget notificationWidget = client.getWidget(INTERFACE_ID, 1);

		// Runs a client script to set the initial title, text and color values of the notifications
		client.runScript(SCRIPT_ID, notification.getTitle(), notification.getText(), notification.getColor());

		// Only remove notification when widget is fully closed.
		clientThread.invokeLater(() -> {
			assert notificationWidget != null;

			if (notificationWidget.getWidth() > 0)
			{
				return false;
			}

			// Close the interface
			client.closeInterface(notificationNode, true);

			// Invoke done
			return true;
		});
	}
}
