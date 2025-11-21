package org.bingoscape.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.AutoSubmissionMetadata;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles automatic tile submissions when game events match tile requirements.
 * Monitors loot events and triggers screenshot + submission when items are obtained.
 */
@Slf4j
@Singleton
public class AutoSubmissionHandler {
    // Track recently submitted tiles to avoid duplicates within a short time window
    private final Map<UUID, Long> recentSubmissions = new ConcurrentHashMap<>();
    private static final long SUBMISSION_COOLDOWN_MS = 5000; // 5 seconds

    @Inject
    private BingoScapePlugin plugin;

    @Inject
    private BingoScapeConfig config;

    @Inject
    private TileRequirementMatcher requirementMatcher;

    @Inject
    private org.bingoscape.notifications.NotificationManager notificationManager;

    /**
     * Handles NPC loot received events (most common source of item drops).
     */
    public void onNpcLootReceived(NpcLootReceived event) {
        if (!shouldProcessEvent()) {
            return;
        }

        NPC npc = event.getNpc();
        int npcId = npc.getId();
        String npcName = npc.getName();

        Collection<ItemStack> items = event.getItems();
        processItemDrops(items, npcId, npcName, "NPC loot");
    }

    /**
     * Handles generic loot received events (pickpocket, event rewards, etc.).
     */
    public void onLootReceived(LootReceived event) {
        if (!shouldProcessEvent()) {
            return;
        }

        // Only process non-NPC loot (NPC loot is handled by onNpcLootReceived)
        if (event.getType() == LootRecordType.NPC) {
            return;
        }

        // For non-NPC loot, we don't have an NPC ID
        Collection<ItemStack> items = event.getItems();
        processItemDrops(items, null, event.getName(), event.getType().name());
    }

    /**
     * Gets the current logged-in RuneScape account name.
     * Uses the plugin's client to get the local player name.
     */
    private String getAccountName() {
        try {
            return plugin.getAccountName();
        } catch (Exception e) {
            log.warn("Failed to get account name", e);
            return null;
        }
    }

    /**
     * Processes a collection of item drops to check if any match tile requirements.
     */
    private void processItemDrops(Collection<ItemStack> items, Integer npcId, String sourceName, String sourceType) {
        if (items == null || items.isEmpty()) {
            return;
        }

        log.debug("Processing {} items from {} ({})", items.size(), sourceName, sourceType);

        for (ItemStack item : items) {
            int itemId = item.getId();

            // Check if this item is required for any tile
            if (requirementMatcher.isRequiredItem(itemId, npcId)) {
                log.info("Found required item {} from {}", itemId, sourceName);

                // Get all tiles that can be completed with this item
                List<UUID> matchingTiles = requirementMatcher.getTilesForItem(itemId, npcId);

                for (UUID tileId : matchingTiles) {
                    // Check cooldown to avoid duplicate submissions
                    if (isOnCooldown(tileId)) {
                        log.debug("Tile {} is on submission cooldown, skipping", tileId);
                        continue;
                    }

                    // Submit this tile with full metadata (notification will be shown after successful submission)
                    submitTileAutomaticWithMetadata(tileId, itemId, sourceName, npcId, sourceType);
                }
            }
        }
    }

    /**
     * Automatically submits a tile with a screenshot and metadata.
     */
    private void submitTileAutomatic(UUID tileId, int itemId, String sourceName) {
        submitTileAutomaticWithMetadata(tileId, itemId, sourceName, null, "Unknown");
    }

    /**
     * Automatically submits a tile with a screenshot and full metadata.
     */
    private void submitTileAutomaticWithMetadata(UUID tileId, int itemId, String sourceName, Integer npcId, String sourceType) {
        log.info("Auto-submitting tile {} for item {} from {}", tileId, itemId, sourceName);

        // Mark this tile as recently submitted
        recentSubmissions.put(tileId, System.currentTimeMillis());

        // Build metadata
        AutoSubmissionMetadata metadata = AutoSubmissionMetadata.builder()
                .itemId(itemId)
                .sourceName(sourceName)
                .npcId(npcId)
                .sourceType(sourceType)
                .accountName(getAccountName())
                .build();

        // Take screenshot and submit with metadata
        plugin.takeScreenshot(tileId, screenshotBytes -> {
            if (screenshotBytes == null) {
                log.error("Failed to capture screenshot for tile {}", tileId);
                showNotification("Auto-submission failed: could not capture screenshot");
                return;
            }

            // Submit to API with metadata
            plugin.submitTileAutomaticWithMetadata(tileId, screenshotBytes, metadata);

            // Show combined notification if enabled
            if (config.showAutoSubmitNotifications()) {
                String itemName = getItemName(itemId);
                showNotification("Bingo Item", String.format("Obtained %s - Tile submitted!", itemName));
            }
        });
    }

    /**
     * Checks if a tile is currently on submission cooldown.
     */
    private boolean isOnCooldown(UUID tileId) {
        Long lastSubmission = recentSubmissions.get(tileId);
        if (lastSubmission == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - lastSubmission;
        if (elapsed > SUBMISSION_COOLDOWN_MS) {
            // Cooldown expired, remove from map
            recentSubmissions.remove(tileId);
            return false;
        }

        return true;
    }

    /**
     * Checks if events should be processed based on config and plugin state.
     */
    private boolean shouldProcessEvent() {
        // Check if auto-submission is enabled
        if (!config.enableAutoSubmission()) {
            log.debug("Auto-submission disabled in config");
            return false;
        }

        // Check if loot auto-submission is enabled
        if (!config.autoSubmitLoot()) {
            log.debug("Loot auto-submission disabled in config");
            return false;
        }

        // Check if we have a current bingo loaded
        if (plugin.getCurrentBingo() == null) {
            log.debug("No current bingo loaded");
            return false;
        }

        // Check if the requirement matcher has any trackable tiles
        if (!requirementMatcher.hasTrackableTiles()) {
            log.warn("Requirement matcher has no trackable tiles. Stats: {}", requirementMatcher.getStats());
            return false;
        }

        log.debug("Event processing enabled. Trackable tiles: {}", requirementMatcher.getStats());
        return true;
    }

    /**
     * Shows a toast notification to the user.
     */
    private void showNotification(String title, String message) {
        if (!config.showAutoSubmitNotifications()) {
            return;
        }

        // Add toast notification if enabled
        if (config.showToastNotifications()) {
            // Convert Color to RGB int for notification
            int color = config.notificationColor().getRGB() & 0xFFFFFF;
            notificationManager.addNotification(title, message, color);
        }

        // Keep log for debugging
        log.info("Auto-submission notification: {} - {}", title, message);
    }

    /**
     * Shows a notification with default title.
     */
    private void showNotification(String message) {
        showNotification("BingoScape", message);
    }

    /**
     * Gets the display name for an item ID.
     */
    private String getItemName(int itemId) {
        try {
            return plugin.getItemManager().getItemComposition(itemId).getName();
        } catch (Exception e) {
            log.warn("Failed to get item name for ID {}", itemId, e);
            return "Item #" + itemId;
        }
    }

    /**
     * Cleans up old cooldown entries to prevent memory leaks.
     */
    public void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        recentSubmissions.entrySet().removeIf(entry ->
                now - entry.getValue() > SUBMISSION_COOLDOWN_MS
        );
    }

    /**
     * Gets statistics about auto-submission for debugging.
     */
    public String getStats() {
        return String.format("Auto-submission: enabled=%s, loot=%s, tiles tracked=%s, recent submissions=%d",
                config.enableAutoSubmission(),
                config.autoSubmitLoot(),
                requirementMatcher.hasTrackableTiles(),
                recentSubmissions.size());
    }
}
