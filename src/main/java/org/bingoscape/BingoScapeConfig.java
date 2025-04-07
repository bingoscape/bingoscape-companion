package org.bingoscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
        return "https://next.bingoscape.org";
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
}
