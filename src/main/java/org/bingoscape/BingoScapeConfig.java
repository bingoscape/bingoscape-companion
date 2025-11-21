package org.bingoscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup("bingoscape")
public interface BingoScapeConfig extends Config
{
    @ConfigItem(
            keyName = "apiKey",
            name = "API Key",
            description = "Your BingoScape API key",
            secret = true
    )
    default String apiKey()
    {
        return "";
    }

    @ConfigItem(
            keyName = "apiKey",
            name = "API Key",
            description = "Your BingoScape API key"
    )
    void apiKey(String key);

    @ConfigItem(
            keyName = "apiBaseUrl",
            name = "API Base URL",
            description = "The base URL for the BingoScape API"
    )
    default String apiBaseUrl()
    {
        return "https://bingoscape.org";
    }

    @ConfigItem(
            keyName = "showCodephraseOverlay",
            name = "Show Bingo Codephrase Overlay",
            description = "Display the codephrase for the currently selected bingo as an overlay"
    )
    default boolean showCodephraseOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "hidePastEvents",
            name = "Hide Past Events",
            description = "Hide events that have already ended"
    )
    default boolean hidePastEvents()
    {
        return false;
    }

    @ConfigItem(
            keyName = "hideLockedEvents",
            name = "Hide Locked Events",
            description = "Hide events that are locked"
    )
    default boolean hideLockedEvents()
    {
        return false;
    }

    @ConfigItem(
            keyName = "hideUpcomingEvents",
            name = "Hide Upcoming Events",
            description = "Hide events that haven't started yet"
    )
    default boolean hideUpcomingEvents()
    {
        return false;
    }

    @ConfigItem(
            keyName = "pinnedBingoId",
            name = "Pinned Bingo ID",
            description = "The ID of the pinned bingo to display on startup",
            hidden = true
    )
    default String pinnedBingoId()
    {
        return "";
    }

    @ConfigItem(
            keyName = "pinnedBingoId",
            name = "Pinned Bingo ID",
            description = "The ID of the pinned bingo to display on startup"
    )
    void pinnedBingoId(String id);

    @ConfigItem(
            keyName = "pinnedTileIds",
            name = "Pinned Tile IDs",
            description = "Comma-separated list of pinned tile IDs",
            hidden = true
    )
    default String pinnedTileIds()
    {
        return "";
    }

    @ConfigItem(
            keyName = "pinnedTileIds",
            name = "Pinned Tile IDs",
            description = "Comma-separated list of pinned tile IDs"
    )
    void pinnedTileIds(String ids);

    @ConfigItem(
            keyName = "enableAutoSubmission",
            name = "Enable Auto-Submission",
            description = "Automatically submit tiles when requirements are met (e.g., when you obtain a required item)"
    )
    default boolean enableAutoSubmission()
    {
        return false; // Off by default for safety
    }

    @ConfigItem(
            keyName = "autoSubmitLoot",
            name = "Auto-Submit Loot",
            description = "Automatically submit tiles when required items are obtained from loot"
    )
    default boolean autoSubmitLoot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showAutoSubmitNotifications",
            name = "Show Auto-Submit Notifications",
            description = "Display notifications when tiles are automatically submitted"
    )
    default boolean showAutoSubmitNotifications()
    {
        return true;
    }

    @ConfigSection(
            name = "Notifications",
            description = "Toast notification settings",
            position = 10
    )
    String notificationSection = "notifications";

    @ConfigItem(
            keyName = "showToastNotifications",
            name = "Show Toast Notifications",
            description = "Display in-game toast notifications for bingo events",
            section = notificationSection
    )
    default boolean showToastNotifications()
    {
        return true;
    }

    @ConfigItem(
            keyName = "notificationColor",
            name = "Notification Color",
            description = "Color of toast notifications (requires restart to take effect)",
            section = notificationSection
    )
    default Color notificationColor()
    {
        return new Color(255, 98, 0); // BingoScape orange
    }
}
