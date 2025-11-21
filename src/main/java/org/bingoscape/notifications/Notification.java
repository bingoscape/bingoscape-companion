package org.bingoscape.notifications;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents an in-game notification with a title, message text, and optional color.
 * <p>
 * Notifications are immutable once created and can be displayed to the player
 * using the RuneLite notification system.
 */
public class Notification
{
	/**
	 * Sentinel value indicating no custom color should be used for the notification.
	 */
	private static final int NO_COLOR = -1;

	private final String title;
	private final String text;
	private final int color;

	/**
	 * Creates a notification with a custom color.
	 *
	 * @param title The notification title
	 * @param text  The notification message text
	 * @param color The RGB color value for the notification, or {@link #NO_COLOR} for default color
	 */
	public Notification(String title, String text, int color)
	{
		this.title = title;
		this.text = text;
		this.color = color;
	}

	/**
	 * Creates a notification with the default color.
	 *
	 * @param title The notification title
	 * @param text  The notification message text
	 */
	public Notification(String title, String text)
	{
		this(title, text, NO_COLOR);
	}

	/**
	 * @return The notification title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @return The notification message text
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * @return The RGB color value, or {@link #NO_COLOR} if using default color
	 */
	public int getColor()
	{
		return color;
	}

	/**
	 * @return {@code true} if this notification has a custom color set
	 */
	public boolean hasCustomColor()
	{
		return color != NO_COLOR;
	}

	/**
	 * @return An Optional containing the color if set, or empty if using default color
	 */
	public Optional<Integer> getCustomColor()
	{
		return hasCustomColor() ? Optional.of(color) : Optional.empty();
	}
}
