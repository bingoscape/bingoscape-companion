package org.bingoscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BingoScapeConfig.CONFIG_GROUP)
public interface BingoScapeConfig extends Config {
    String CONFIG_GROUP = "bingoscape";

    @ConfigItem(keyName = "apiKey", name = "API Key", description = "Your BingoScape API key", secret = true)
    default String apiKey() {
        return "";
    }

    @ConfigItem(keyName = "apiKey", name = "API Key", description = "Your BingoScape API key")
    void apiKey(String key);

    @ConfigItem(keyName = "apiBaseUrl", name = "API Base URL", description = "The base URL for the BingoScape API")
    default String apiBaseUrl() {
        return "https://bingoscape.org";
    }

    @ConfigItem(keyName = "showCodephraseOverlay", name = "Show Bingo Codephrase Overlay", description = "Display the codephrase for the currently selected bingo as an overlay")
    default boolean showCodephraseOverlay() {
        return true;
    }

    @ConfigItem(keyName = "hidePastEvents", name = "Hide Past Events", description = "Hide events that have already ended")
    default boolean hidePastEvents() {
        return false;
    }

    @ConfigItem(keyName = "hideLockedEvents", name = "Hide Locked Events", description = "Hide events that are locked")
    default boolean hideLockedEvents() {
        return false;
    }

    @ConfigItem(keyName = "hideUpcomingEvents", name = "Hide Upcoming Events", description = "Hide events that haven't started yet")
    default boolean hideUpcomingEvents() {
        return false;
    }

    @ConfigItem(keyName = "pinnedBingoId", name = "Pinned Bingo ID", description = "The ID of the pinned bingo to display on startup", hidden = true)
    default String pinnedBingoId() {
        return "";
    }

    @ConfigItem(keyName = "pinnedBingoId", name = "Pinned Bingo ID", description = "The ID of the pinned bingo to display on startup")
    void pinnedBingoId(String id);

    @ConfigItem(keyName = "pinnedTileIds", name = "Pinned Tile IDs", description = "Comma-separated list of pinned tile IDs", hidden = true)
    default String pinnedTileIds() {
        return "";
    }

    @ConfigItem(keyName = "pinnedTileIds", name = "Pinned Tile IDs", description = "Comma-separated list of pinned tile IDs")
    void pinnedTileIds(String ids);

    @ConfigItem(keyName = "enableAutoSubmission", name = "Enable Auto-Submission", description = "Automatically submit tiles when requirements are met (e.g., when you obtain a required item)")
    default boolean enableAutoSubmission() {
        return false; // Off by default for safety
    }

    @ConfigItem(keyName = "showAutoSubmitNotifications", name = "Show Auto-Submit Notifications", description = "Show the drop popup and chat messages for bingo items and automatic submissions")
    default boolean showAutoSubmitNotifications() {
        return true;
    }

    @ConfigSection(name = "Bingo Board", description = "In-game bingo board overlay", position = 5)
    String boardSection = "boardSection";

    @ConfigItem(keyName = "boardDisplayMode", name = "Board Display", description = "Show the bingo board as an in-game overlay or as a separate window (legacy)", section = boardSection, position = 0)
    default BoardDisplayMode boardDisplayMode() {
        return BoardDisplayMode.OVERLAY;
    }

    @ConfigItem(keyName = "boardToggleHotkey", name = "Toggle Board Hotkey", description = "Hotkey to show/hide the bingo board", section = boardSection, position = 1)
    default Keybind boardToggleHotkey() {
        return Keybind.NOT_SET;
    }

    @Range(min = 50, max = 200)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "boardScale", name = "Board Scale", description = "Tile size of the board overlay (shrinks automatically to fit the game view). Ctrl + mouse wheel over the board also changes this.", section = boardSection, position = 2)
    default int boardScale() {
        return 100;
    }

    @Range(min = 20, max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "boardOpacity", name = "Background Opacity", description = "Opacity of the board overlay background", section = boardSection, position = 3)
    default int boardOpacity() {
        return 90;
    }

    @ConfigItem(keyName = "boardShowTooltips", name = "Show Tile Tooltips", description = "Show tile details when hovering a tile", section = boardSection, position = 4)
    default boolean boardShowTooltips() {
        return true;
    }

    @ConfigItem(keyName = "boardShowTileImages", name = "Show Tile Images", description = "Download and show tile header images", section = boardSection, position = 5)
    default boolean boardShowTileImages() {
        return true;
    }

    @ConfigItem(keyName = "boardCloseOnEscape", name = "Close With Escape", description = "Pressing Escape closes the tile details, then the board overlay", section = boardSection, position = 6)
    default boolean boardCloseOnEscape() {
        return true;
    }

    @ConfigSection(name = "Notifications", description = "Drop popup settings", position = 10)
    String notificationSection = "notifications";

    @ConfigItem(keyName = "showToastNotifications", name = "Show Drop Popup", description = "Show a popup when you receive an item that counts for a bingo tile. The popup color depends on the GE value of the drop", section = notificationSection, position = 0)
    default boolean showToastNotifications() {
        return true;
    }

    @Range(min = 50, max = 200)
    @Units(Units.PERCENT)
    @ConfigItem(keyName = "notificationScale", name = "Popup Scale", description = "Size of the drop popup", section = notificationSection, position = 1)
    default int notificationScale() {
        return 100;
    }

    @Range(min = 1, max = 15)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "notificationSeconds", name = "Popup Duration", description = "How long the drop popup stays on screen", section = notificationSection, position = 2)
    default int notificationSeconds() {
        return 5;
    }

    @ConfigItem(keyName = "dropUncommonValue", name = "Uncommon From", description = "Stack value (gp) from which a drop is Uncommon (green)", section = notificationSection, position = 3)
    default int dropUncommonValue() {
        return 100_000;
    }

    @ConfigItem(keyName = "dropRareValue", name = "Rare From", description = "Stack value (gp) from which a drop is Rare (blue)", section = notificationSection, position = 4)
    default int dropRareValue() {
        return 1_000_000;
    }

    @ConfigItem(keyName = "dropEpicValue", name = "Epic From", description = "Stack value (gp) from which a drop is Epic (purple)", section = notificationSection, position = 5)
    default int dropEpicValue() {
        return 10_000_000;
    }

    @ConfigItem(keyName = "dropLegendaryValue", name = "Legendary From", description = "Stack value (gp) from which a drop is Legendary (gold)", section = notificationSection, position = 6)
    default int dropLegendaryValue() {
        return 100_000_000;
    }

    @ConfigItem(keyName = "showTeamDropNotifications", name = "Show Team Drops", description = "Show a popup when a teammate receives a drop for a bingo tile. Requires the drop popup to be enabled", section = notificationSection, position = 7)
    default boolean showTeamDropNotifications() {
        return true;
    }

    @ConfigItem(keyName = "teamDropMinValue", name = "Team Drop Min Value", description = "Only show team drops worth at least this many gp (0 shows all)", section = notificationSection, position = 8)
    default int teamDropMinValue() {
        return 0;
    }

    @ConfigItem(keyName = "showTileCompletedNotifications", name = "Show Tile Completions", description = "Show a popup when your team completes a bingo tile. Requires the drop popup to be enabled", section = notificationSection, position = 9)
    default boolean showTileCompletedNotifications() {
        return true;
    }

    @ConfigItem(keyName = "tileCompletedSound", name = "Tile Completed Sound", description = "Play a sound when your team completes a bingo tile", section = notificationSection, position = 10)
    default boolean tileCompletedSound() {
        return true;
    }
}
