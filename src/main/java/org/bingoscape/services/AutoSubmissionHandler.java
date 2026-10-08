package org.bingoscape.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.AutoSubmissionMetadata;
import org.bingoscape.models.Tile;
import org.bingoscape.notifications.DropNotification;
import org.bingoscape.notifications.DropNotificationOverlay;
import org.bingoscape.notifications.DropTier;
import org.bingoscape.notifications.DropTierResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles automatic tile submissions when game events match tile requirements.
 * Monitors loot events and triggers screenshot + submission when items are
 * obtained.
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
    private DropNotificationOverlay dropOverlay;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

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
            int quantity = item.getQuantity();

            // Check if this item is required for any tile
            if (requirementMatcher.isRequiredItem(itemId, npcId)) {
                log.info("Found required item {} from {}", itemId, sourceName);

                // Get all tiles that can be completed with this item
                List<UUID> matchingTiles = requirementMatcher.getTilesForItem(itemId, npcId);
                if (matchingTiles.isEmpty()) {
                    continue;
                }
                String warning = notSubmittedReason();

                // One popup per drop, shown immediately when the item is obtained
                showDropNotification(itemId, quantity, matchingTiles, warning);

                if (warning != null) {
                    continue;
                }

                for (UUID tileId : matchingTiles) {
                    // Check cooldown to avoid duplicate submissions
                    if (isOnCooldown(tileId)) {
                        log.debug("Tile {} is on submission cooldown, skipping", tileId);
                        continue;
                    }
                    submitTileAutomaticWithMetadata(tileId, itemId, quantity, sourceName, npcId, sourceType);
                }
            }
        }
    }

    /**
     * Automatically submits a tile with a screenshot and full metadata.
     */
    private void submitTileAutomaticWithMetadata(UUID tileId, int itemId, int quantity, String sourceName, Integer npcId,
            String sourceType) {
        log.info("Auto-submitting tile {} for item {} from {}", tileId, itemId, sourceName);

        // Mark this tile as recently submitted
        recentSubmissions.put(tileId, System.currentTimeMillis());

        // Get item name NOW (on client thread) before entering background thread
        // This must be done here because ItemManager requires the client thread
        final String itemName = getItemName(itemId);

        // Capture location data (must be on client thread like getItemName)
        int worldX = 0;
        int worldY = 0;
        int plane = 0;
        int worldNumber = 0;
        int regionId = 0;

        try {
            if (client.getLocalPlayer() != null) {
                WorldPoint location = client.getLocalPlayer().getWorldLocation();
                if (location != null) {
                    worldX = location.getX();
                    worldY = location.getY();
                    plane = location.getPlane();
                    regionId = ((worldX >> 6) << 8) | (worldY >> 6);
                }
            }
            worldNumber = client.getWorld();
        } catch (Exception e) {
            log.debug("Failed to capture location metadata", e);
        }

        // Build metadata
        AutoSubmissionMetadata metadata = AutoSubmissionMetadata.builder()
                .itemId(itemId)
                .quantity(quantity)
                .sourceName(sourceName)
                .npcId(npcId)
                .sourceType(sourceType)
                .accountName(getAccountName())
                .worldX(worldX)
                .worldY(worldY)
                .plane(plane)
                .worldNumber(worldNumber)
                .regionId(regionId)
                .build();

        // Take screenshot and submit with metadata
        plugin.takeScreenshot(tileId, screenshotBytes -> {
            if (screenshotBytes == null) {
                log.error("Failed to capture screenshot for tile {}", tileId);
                showChatMessage("Auto-submission failed: could not capture screenshot");
                return;
            }

            // Submit to API with metadata
            plugin.submitTileAutomaticWithMetadata(tileId, screenshotBytes, metadata);

            // Log success in chatbox (use pre-fetched item name from client thread)
            if (config.showAutoSubmitNotifications()) {
                showChatMessage(String.format("Tile auto-submitted for %s", itemName));
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
     * Checks if loot events should be matched against tiles at all. This is independent of auto-submission:
     * drops of bingo items are announced even when they are not submitted automatically.
     */
    private boolean shouldProcessEvent() {
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
     * Returns why matching drops are not submitted automatically (shown on the popup), or null when they are.
     */
    private String notSubmittedReason() {
        if (!config.enableAutoSubmission()) {
            return "Not auto-submitted - submit manually";
        }
        // The server rejects submissions for locked bingos; don't take screenshots for nothing
        if (plugin.getCurrentBingo().isLocked()) {
            return "Bingo is locked - not submitted";
        }
        return null;
    }

    /**
     * Shows the drop popup. Must run on the client thread (item prices and names).
     */
    private void showDropNotification(int itemId, int quantity, List<UUID> tileIds, String warning) {
        if (!config.showAutoSubmitNotifications() || !config.showToastNotifications()) {
            return;
        }

        long stackValue = getItemPrice(itemId) * (long) quantity;
        DropTier tier = DropTierResolver.resolve(stackValue, config.dropUncommonValue(), config.dropRareValue(),
                config.dropEpicValue(), config.dropLegendaryValue());
        String itemName = getItemName(itemId);

        dropOverlay.enqueue(new DropNotification(itemId, itemName, quantity, stackValue, tier,
                getTileTitle(tileIds.get(0)), tileIds.size() - 1, warning));
        log.info("Bingo item drop: {} x{} ({} gp, {})", itemName, quantity, stackValue, tier);
    }

    private String getTileTitle(UUID tileId) {
        if (plugin.getCurrentBingo() == null || plugin.getCurrentBingo().getTiles() == null) {
            return null;
        }
        for (Tile tile : plugin.getCurrentBingo().getTiles()) {
            if (tileId.equals(tile.getId())) {
                return tile.getTitle();
            }
        }
        return null;
    }

    /**
     * GE price of an item, falling back to the high alchemy value for untradeables.
     */
    private long getItemPrice(int itemId) {
        try {
            long price = plugin.getItemManager().getItemPrice(itemId);
            if (price > 0) {
                return price;
            }
            return Math.max(0, plugin.getItemManager().getItemComposition(itemId).getHaPrice());
        } catch (Exception e) {
            log.debug("Failed to get item price for ID {}", itemId, e);
            return 0;
        }
    }

    /**
     * Shows a message in the game chatbox.
     */
    private void showChatMessage(String message) {
        clientThread.invokeLater(
                () -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "BingoScape: " + message, null));
        log.info("Auto-submission chat message: {}", message);
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
        recentSubmissions.entrySet().removeIf(entry -> now - entry.getValue() > SUBMISSION_COOLDOWN_MS);
    }

    /**
     * Gets statistics about auto-submission for debugging.
     */
    public String getStats() {
        return String.format("Auto-submission: enabled=%s, tiles tracked=%s, recent submissions=%d",
                config.enableAutoSubmission(),
                requirementMatcher.hasTrackableTiles(),
                recentSubmissions.size());
    }
}
