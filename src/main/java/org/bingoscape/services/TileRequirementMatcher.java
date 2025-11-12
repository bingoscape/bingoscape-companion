package org.bingoscape.services;

import lombok.extern.slf4j.Slf4j;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Goal;
import org.bingoscape.models.GoalTreeNode;
import org.bingoscape.models.Tile;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matches game events (item drops, skill levels, etc.) to tile requirements
 * to determine when tiles can be auto-submitted.
 *
 * This service queries the current bingo state from the plugin on-demand,
 * ensuring it's always in sync without requiring manual updates.
 */
@Slf4j
@Singleton
public class TileRequirementMatcher {
    // Maps itemId to list of tiles that require that item
    private final Map<Integer, List<UUID>> itemToTilesMap = new ConcurrentHashMap<>();

    // Maps itemId to the goal tree nodes that contain it (for checking restrictions)
    private final Map<Integer, List<GoalTreeNode>> itemToGoalMap = new ConcurrentHashMap<>();

    // Plugin reference to query current bingo state
    @Inject
    private BingoScapePlugin plugin;

    /**
     * Rebuilds the lookup maps from the current bingo data.
     * Queries the plugin for the current bingo state and parses the goalTree structure
     * to find all item-based goals.
     */
    public void rebuildLookupMaps() {
        itemToTilesMap.clear();
        itemToGoalMap.clear();

        Bingo currentBingo = plugin.getCurrentBingo();
        if (currentBingo == null || currentBingo.getTiles() == null) {
            log.warn("Cannot rebuild maps: currentBingo or tiles is null");
            return;
        }

        log.debug("Rebuilding lookup maps for {} tiles", currentBingo.getTiles().size());

        for (Tile tile : currentBingo.getTiles()) {
            // Skip tiles that are already approved
            if (isTileApproved(tile)) {
                log.debug("Skipping tile {} (already approved)", tile.getId());
                continue;
            }

            // Parse goalTree if available (new API format)
            if (tile.getGoalTree() != null && !tile.getGoalTree().isEmpty()) {
                log.debug("Tile {} has goalTree with {} nodes", tile.getId(), tile.getGoalTree().size());
                parseGoalTreeForItems(tile, tile.getGoalTree());
            }
            // Fallback to flat goals list (backwards compatibility)
//            else if (tile.getGoals() != null && !tile.getGoals().isEmpty()) {
//                log.debug("Tile {} has flat goals list with {} goals", tile.getId(), tile.getGoals().size());
//                for (Goal goal : tile.getGoals()) {
//                    if ("item".equals(goal.getGoalType()) && goal.getItemId() != null) {
//                        itemToTilesMap.computeIfAbsent(goal.getItemId(), k -> new ArrayList<>())
//                                .add(tile.getId());
//                        log.debug("Added item {} -> tile {}", goal.getItemId(), tile.getId());
//                    }
//                }
//            } else {
//                log.debug("Tile {} has no goals or goalTree", tile.getId());
//            }
        }

        log.debug("Built item lookup map with {} unique items mapping to tiles: {}",
            itemToTilesMap.size(), itemToTilesMap.keySet());
    }

    /**
     * Recursively parses a goal tree to find all item goals.
     */
    private void parseGoalTreeForItems(Tile tile, List<GoalTreeNode> nodes) {
        if (nodes == null) {
            return;
        }

        for (GoalTreeNode node : nodes) {
            log.debug("Parsing node: type={}, isItemGoal={}, isGroup={}",
                node.getType(), node.isItemGoal(), node.isGroup());

            if (node.isItemGoal() && node.getItemGoal() != null && node.getItemGoal().getItemId() != null) {
                // Found an item goal, add to lookup maps
                int itemId = node.getItemGoal().getItemId();
                itemToTilesMap.computeIfAbsent(itemId, k -> new ArrayList<>())
                        .add(tile.getId());
                itemToGoalMap.computeIfAbsent(itemId, k -> new ArrayList<>())
                        .add(node);
                log.debug("Added item {} -> tile {} from goalTree", itemId, tile.getId());
            } else if (node.isGroup() && node.getChildren() != null) {
                // Recursively process child nodes
                log.debug("Recursing into group with {} children", node.getChildren().size());
                parseGoalTreeForItems(tile, node.getChildren());
            } else {
                log.debug("Node skipped: type={}, goalType={}, hasItemGoal={}, hasChildren={}",
                    node.getType(), node.getGoalType(), node.getItemGoal() != null,
                    node.getChildren() != null);
            }
        }
    }

    /**
     * Checks if an item drop matches any tile requirements.
     *
     * @param itemId The OSRS item ID
     * @param npcId  The NPC ID (optional, can be null)
     * @return true if this item is required for any incomplete tile
     */
    public boolean isRequiredItem(int itemId, Integer npcId) {
        Bingo currentBingo = plugin.getCurrentBingo();
        if (currentBingo == null) {
            return false;
        }

        // Check if this item is tracked in our goal map
        List<GoalTreeNode> goalNodes = itemToGoalMap.get(itemId);
        if (goalNodes == null || goalNodes.isEmpty()) {
            return false;
        }

        // Item is tracked - check if any associated tile is not yet approved
        List<UUID> matchingTileIds = itemToTilesMap.get(itemId);
        if (matchingTileIds == null || matchingTileIds.isEmpty()) {
            return false;
        }

        for (UUID tileId : matchingTileIds) {
            Tile tile = getTileById(tileId);
            if (tile != null && !isTileApproved(tile)) {
                return true; // Found an incomplete tile that needs this item
            }
        }

        return false;
    }

    /**
     * Gets all tiles that match the given item and optional NPC.
     *
     * @param itemId The OSRS item ID
     * @param npcId  The NPC ID (optional, can be null)
     * @return List of tile IDs that should be auto-submitted
     */
    public List<UUID> getTilesForItem(int itemId, Integer npcId) {
        List<UUID> result = new ArrayList<>();

        Bingo currentBingo = plugin.getCurrentBingo();
        if (currentBingo == null) {
            return result;
        }

        List<UUID> matchingTileIds = itemToTilesMap.get(itemId);
        if (matchingTileIds == null || matchingTileIds.isEmpty()) {
            return result;
        }

        // Check each matching tile
        for (UUID tileId : matchingTileIds) {
            Tile tile = getTileById(tileId);
            if (tile == null || isTileApproved(tile)) {
                continue;
            }

            // Check if this tile's goals are satisfied
            if (isTileSatisfiedByItem(tile, itemId, npcId)) {
                result.add(tileId);
            }
        }

        return result;
    }

    /**
     * Checks if a tile is relevant for the given item.
     * We don't need to determine if the tile is COMPLETE - just if this item is part of its requirements.
     * The server will validate whether the tile is actually complete after submission.
     */
    private boolean isTileSatisfiedByItem(Tile tile, int itemId, Integer npcId) {
        // The tile is "satisfied" (ready for auto-submission) if:
        // 1. The item is part of ANY goal in the tile's requirements
        // 2. The server will determine if all requirements are met

        // Try goalTree first (new format)
        if (tile.getGoalTree() != null && !tile.getGoalTree().isEmpty()) {
            return containsItemInTree(tile.getGoalTree(), itemId);
        }

        // Fallback to flat goals list (legacy format)
        if (tile.getGoals() == null || tile.getGoals().isEmpty()) {
            return false;
        }

        // Check if any goal requires this item
        for (GoalTreeNode goal : tile.getGoalTree()) {
            if ("item".equals(goal.getGoalType()) && goal.getItemGoal().getItemId() != null && goal.getItemGoal().getItemId() == itemId) {
                return true;
//                // Check NPC restriction if specified
//                if (goal.getNpcIds() == null || goal.getNpcIds().isEmpty()) {
//                    return true; // No restriction
//                } else if (npcId != null && goal.getNpcIds().contains(npcId)) {
//                    return true; // NPC matches restriction
//                }
            }
        }

        return false;
    }

    /**
     * Recursively checks if an item is part of any goal in the tree.
     * Returns true if the item is relevant to this tile, regardless of AND/OR logic.
     * The server will validate the complete logic after submission.
     */
    private boolean containsItemInTree(List<GoalTreeNode> nodes, int itemId) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }

        for (GoalTreeNode node : nodes) {
            if (node.isItemGoal() &&
                node.getItemGoal() != null &&
                node.getItemGoal().getItemId() != null &&
                node.getItemGoal().getItemId() == itemId) {
                // Found the item in this goal
                return true;
            }

            // Recursively check child nodes
            if (node.isGroup() && node.getChildren() != null) {
                if (containsItemInTree(node.getChildren(), itemId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if a tile has already been approved (to avoid re-submitting).
     */
    private boolean isTileApproved(Tile tile) {
        return tile.getSubmission() != null && "approved".equals(tile.getSubmission().getStatus());
    }

    /**
     * Gets a tile by its ID from the current bingo.
     */
    private Tile getTileById(UUID tileId) {
        Bingo currentBingo = plugin.getCurrentBingo();
        if (currentBingo == null || currentBingo.getTiles() == null) {
            return null;
        }

        return currentBingo.getTiles().stream()
                .filter(t -> t.getId().equals(tileId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns true if there are any trackable tiles (tiles with item goals).
     */
    public boolean hasTrackableTiles() {
        return !itemToTilesMap.isEmpty();
    }

    /**
     * Gets statistics about tracked tiles for debugging.
     */
    public String getStats() {
        Bingo currentBingo = plugin.getCurrentBingo();
        if (currentBingo == null) {
            return "No bingo loaded";
        }

        int totalTiles = currentBingo.getTiles() != null ? currentBingo.getTiles().size() : 0;
        int trackableTiles = (int) itemToTilesMap.values().stream()
                .flatMap(List::stream)
                .distinct()
                .count();

        return String.format("Tracking %d items across %d/%d tiles",
                itemToTilesMap.size(), trackableTiles, totalTiles);
    }
}
