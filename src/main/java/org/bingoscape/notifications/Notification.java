package org.bingoscape.notifications;

public class Notification
{
	private final String title;
	private final String text;
	private final int color;

	// Constructor with color
	public Notification(String title, String text, int color)
	{
		this.title = title;
		this.text = text;
		this.color = color;
	}

	// Constructor without color
	public Notification(String title, String text)
	{
		this.title = title;
		this.text = text;
		this.color = -1; // Default or sentinel value for no color
	}

	public String getTitle()
	{
		return title;
	}

	public String getText()
	{
		return text;
	}

	public int getColor()
	{
		return color;
	}
}
