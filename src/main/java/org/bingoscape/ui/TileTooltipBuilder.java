package org.bingoscape.ui;

import org.bingoscape.models.Bingo;
import org.bingoscape.models.Goal;
import org.bingoscape.models.GoalProgress;
import org.bingoscape.models.GoalTreeNode;
import org.bingoscape.models.GoalTreeProgress;
import org.bingoscape.models.ItemGoal;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;

import java.util.List;

/**
 * Builder class for creating detailed tile tooltips with consistent styling.
 * Encapsulates tooltip HTML generation logic to reduce complexity in factory classes.
 */
public class TileTooltipBuilder {

    private static final String CSS_STYLES =
        ".tooltip-container { width: 280px; padding: 12px; background: #1a1d29; border-radius: 8px; font-family: 'Segoe UI', sans-serif; }" +
        ".header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }" +
        ".title { font-size: 14px; font-weight: 600; color: #ffffff; margin-right: 8px; line-height: 1.3; }" +
        ".xp-badge { background: #f59e0b; color: #ffffff; padding: 4px 8px; border-radius: 16px; font-size: 11px; font-weight: 600; white-space: nowrap; }" +
        ".status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; margin-bottom: 8px; }" +
        ".status-approved { background: #065f46; color: #10b981; }" +
        ".status-pending { background: #1e3a8a; color: #3b82f6; }" +
        ".status-needs-review { background: #92400e; color: #f59e0b; }" +
        ".status-declined { background: #7f1d1d; color: #ef4444; }" +
        ".description { color: #9ca3af; font-size: 12px; line-height: 1.4; margin-bottom: 12px; }" +
        ".goals-section { margin-top: 12px; }" +
        ".goals-title { color: #ffffff; font-size: 11px; font-weight: 600; margin-bottom: 6px; }" +
        ".goal-item { background: #374151; padding: 8px; border-radius: 6px; margin-bottom: 6px; }" +
        ".goal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }" +
        ".goal-desc { color: #d1d5db; font-size: 11px; line-height: 1.3; }" +
        ".goal-target { color: #ffffff; font-size: 11px; font-weight: 500; }" +
        ".progress-bar { width: 100%; height: 4px; background: #4b5563; border-radius: 2px; overflow: hidden; }" +
        ".progress-fill { height: 100%; background: #10b981; transition: width 0.3s ease; }" +
        ".progress-text { color: #9ca3af; font-size: 10px; margin-top: 2px; }" +
        // Compact goal tree styles
        ".goal-tree { margin-top: 8px; }" +
        ".tree-node { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; padding: 4px 6px; border-radius: 4px; background: rgba(55, 65, 81, 0.5); }" +
        ".tree-node:hover { background: rgba(55, 65, 81, 0.8); }" +
        ".tree-indent { display: inline-block; }" +
        ".node-icon { font-size: 12px; color: #9ca3af; min-width: 16px; text-align: center; }" +
        ".node-content { flex: 1; display: flex; align-items: center; gap: 6px; }" +
        ".node-label { color: #d1d5db; font-size: 11px; }" +
        ".logic-badge { padding: 2px 6px; border-radius: 4px; font-size: 9px; font-weight: 600; }" +
        ".badge-and { background: #1e3a8a; color: #60a5fa; }" +
        ".badge-or { background: #065f46; color: #34d399; }" +
        ".mini-progress { height: 2px; flex: 1; background: #374151; border-radius: 1px; overflow: hidden; min-width: 40px; max-width: 80px; }" +
        ".mini-progress-fill { height: 100%; }" +
        ".progress-complete { background: #10b981; }" +
        ".progress-partial { background: #f59e0b; }" +
        ".progress-none { background: #6b7280; }" +
        ".completion-icon { font-size: 10px; min-width: 14px; }" +
        ".icon-complete { color: #10b981; }" +
        ".icon-partial { color: #f59e0b; }" +
        ".icon-incomplete { color: #6b7280; }" +
        ".progress-count { color: #9ca3af; font-size: 9px; white-space: nowrap; }";

    private static final int MAX_DESCRIPTION_LENGTH = 150;

    /**
     * Creates a detailed HTML tooltip for a tile matching the web design.
     *
     * @param tile The tile data
     * @param currentBingo The current bingo for context (can be null)
     * @return HTML tooltip string
     */
    public String buildTooltip(Tile tile, Bingo currentBingo) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>").append(CSS_STYLES).append("</style></head><body>");
        html.append("<div class='tooltip-container'>");

        appendHeader(html, tile);
        appendStatusBadge(html, tile);
        appendDescription(html, tile);
        appendGoalsSection(html, tile);

        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Appends the header section with title and XP badge.
     */
    private void appendHeader(StringBuilder html, Tile tile) {
        html.append("<div class='header'>")
            .append("<div class='title'>").append(tile.getTitle()).append("</div>")
            .append("<div class='xp-badge'>⚡ ").append(tile.getWeight()).append(" XP</div>")
            .append("</div>");
    }

    /**
     * Appends the status badge if the tile has a submission status.
     */
    private void appendStatusBadge(StringBuilder html, Tile tile) {
        if (tile.getSubmission() == null || tile.getSubmission().getStatus() == null ||
            tile.getSubmission().getStatus() == TileSubmissionType.NOT_SUBMITTED) {
            return;
        }

        TileSubmissionType status = tile.getSubmission().getStatus();
        html.append("<div class='status-badge ").append(getStatusCssClass(status)).append("'>")
            .append(getStatusIcon(status)).append(" ").append(StatusConstants.getStatusText(status))
            .append("</div>");
    }

    /**
     * Appends the tile description (truncated if necessary).
     */
    private void appendDescription(StringBuilder html, Tile tile) {
        if (tile.getDescription() == null || tile.getDescription().isEmpty()) {
            return;
        }

        String description = tile.getDescription().length() > MAX_DESCRIPTION_LENGTH
            ? tile.getDescription().substring(0, MAX_DESCRIPTION_LENGTH) + "..."
            : tile.getDescription();

        html.append("<div class='description'>").append(description).append("</div>");
    }

    /**
     * Appends the goals section with progress bars.
     * Prioritizes goalTree if available, falls back to flat goals list.
     */
    private void appendGoalsSection(StringBuilder html, Tile tile) {
        // Check for goalTree first (new hierarchical format)
        if (tile.getGoalTree() != null && !tile.getGoalTree().isEmpty()) {
            html.append("<div class='goals-section'>")
                .append("<div class='goals-title'>Goals:</div>")
                .append("<div class='goal-tree'>");

            renderGoalTree(html, tile.getGoalTree(), 0);

            html.append("</div></div>");
            return;
        }

        // Fallback to flat goals list (legacy format)
        if (tile.getGoals() == null || tile.getGoals().isEmpty()) {
            return;
        }

        html.append("<div class='goals-section'>")
            .append("<div class='goals-title'>Goals:</div>");

        for (Goal goal : tile.getGoals()) {
            appendGoalItem(html, goal);
        }

        html.append("</div>");
    }

    /**
     * Recursively renders a goal tree with proper indentation and styling.
     *
     * @param html The StringBuilder to append HTML to
     * @param nodes The list of goal tree nodes to render
     * @param depth The current depth level for indentation
     */
    private void renderGoalTree(StringBuilder html, List<GoalTreeNode> nodes, int depth) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (GoalTreeNode node : nodes) {
            html.append("<div class='tree-node'>");

            // Add indentation based on depth (8px per level)
            if (depth > 0) {
                html.append("<span class='tree-indent' style='width: ").append(depth * 8).append("px;'></span>");
            }

            if (node.isGroup()) {
                renderGroupNode(html, node, depth);
            } else if (node.isItemGoal()) {
                renderItemGoalNode(html, node);
            } else {
                renderGenericGoalNode(html, node);
            }

            html.append("</div>");

            // Recursively render children
            if (node.isGroup() && node.getChildren() != null && !node.getChildren().isEmpty()) {
                renderGoalTree(html, node.getChildren(), depth + 1);
            }
        }
    }

    /**
     * Renders a group node with AND/OR badge and progress.
     */
    private void renderGroupNode(StringBuilder html, GoalTreeNode node, int depth) {
        html.append("<span class='node-icon'>📁</span>")
            .append("<div class='node-content'>");

        // AND/OR badge
        String logicOp = node.getLogicalOperator() != null ? node.getLogicalOperator().toUpperCase() : "AND";
        String badgeClass = "OR".equals(logicOp) ? "badge-or" : "badge-and";
        html.append("<span class='logic-badge ").append(badgeClass).append("'>")
            .append(logicOp).append("</span>");

        // Group name (if available)
        if (node.getName() != null && !node.getName().isEmpty()) {
            html.append("<span class='node-label'>").append(node.getName()).append("</span>");
        } else {
            html.append("<span class='node-label'>Group</span>");
        }

        // Progress bar and count
        if (node.getProgress() != null) {
            renderMiniProgress(html, node.getProgress());
        } else if (node.getMinRequiredGoals() != null && node.getChildren() != null) {
            // Show requirement even without progress
            html.append("<span class='progress-count'>0/")
                .append(node.getMinRequiredGoals()).append("</span>");
        }

        html.append("</div>");
    }

    /**
     * Renders an item goal node with icon and progress.
     */
    private void renderItemGoalNode(StringBuilder html, GoalTreeNode node) {
        ItemGoal itemGoal = node.getItemGoal();

        // Completion icon
        String completionIcon = getCompletionIcon(node.getProgress());
        String iconClass = getCompletionIconClass(node.getProgress());
        html.append("<span class='completion-icon ").append(iconClass).append("'>")
            .append(completionIcon).append("</span>");

        html.append("<div class='node-content'>");

        // Item name
        String itemName = getItemName(itemGoal);
        html.append("<span class='node-label'>").append(itemName).append("</span>");

        // Progress bar
        if (node.getProgress() != null) {
            renderMiniProgress(html, node.getProgress());
        }

        html.append("</div>");
    }

    /**
     * Renders a generic goal node (non-item goals).
     */
    private void renderGenericGoalNode(StringBuilder html, GoalTreeNode node) {
        // Completion icon
        String completionIcon = getCompletionIcon(node.getProgress());
        String iconClass = getCompletionIconClass(node.getProgress());
        html.append("<span class='completion-icon ").append(iconClass).append("'>")
            .append(completionIcon).append("</span>");

        html.append("<div class='node-content'>");

        // Goal description
        String description = node.getDescription() != null ? node.getDescription() : "Goal";
        html.append("<span class='node-label'>").append(description).append("</span>");

        // Progress bar
        if (node.getProgress() != null) {
            renderMiniProgress(html, node.getProgress());
        }

        html.append("</div>");
    }

    /**
     * Renders a mini progress bar (2px height).
     */
    private void renderMiniProgress(StringBuilder html, GoalTreeProgress progress) {
        if (progress == null) {
            return;
        }

        int completed = progress.getCompletedCount();
        int total = progress.getTotalCount();
        double percentage = total > 0 ? (double) completed / total * 100 : 0;

        String progressClass = progress.isComplete() ? "progress-complete" :
                              (completed > 0 ? "progress-partial" : "progress-none");

        html.append("<div class='mini-progress'>")
            .append("<div class='mini-progress-fill ").append(progressClass).append("' style='width: ")
            .append(String.format("%.1f", percentage)).append("%'></div>")
            .append("</div>")
            .append("<span class='progress-count'>").append(completed).append("/").append(total).append("</span>");
    }

    /**
     * Gets the completion icon based on progress.
     */
    private String getCompletionIcon(GoalTreeProgress progress) {
        if (progress == null) {
            return "○";
        }
        if (progress.isComplete()) {
            return "✓";
        }
        if (progress.getCompletedCount() > 0) {
            return "◐";
        }
        return "○";
    }

    /**
     * Gets the CSS class for completion icon color.
     */
    private String getCompletionIconClass(GoalTreeProgress progress) {
        if (progress == null) {
            return "icon-incomplete";
        }
        if (progress.isComplete()) {
            return "icon-complete";
        }
        if (progress.getCompletedCount() > 0) {
            return "icon-partial";
        }
        return "icon-incomplete";
    }

    /**
     * Gets a display name for an item goal.
     */
    private String getItemName(ItemGoal itemGoal) {
        if (itemGoal == null) {
            return "Item";
        }
        if (itemGoal.getExactVariant() != null && !itemGoal.getExactVariant().isEmpty()) {
            return itemGoal.getExactVariant();
        }
        if (itemGoal.getBaseName() != null && !itemGoal.getBaseName().isEmpty()) {
            return itemGoal.getBaseName();
        }
        if (itemGoal.getItemId() != null) {
            return "Item #" + itemGoal.getItemId();
        }
        return "Item";
    }

    /**
     * Appends a single goal item with its progress bars.
     */
    private void appendGoalItem(StringBuilder html, Goal goal) {
        html.append("<div class='goal-item'>")
            .append("<div class='goal-header'>")
            .append("<div class='goal-desc'>").append(goal.getDescription()).append("</div>")
            .append("<div class='goal-target'>Target: ").append(goal.getTargetValue()).append("</div>")
            .append("</div>");

        if (goal.getProgress() != null) {
            appendGoalProgressBars(html, goal);
        } else {
            appendGoalNoProgress(html, goal);
        }

        html.append("</div>");
    }

    /**
     * Appends progress bars when goal has progress data.
     */
    private void appendGoalProgressBars(StringBuilder html, Goal goal) {
        GoalProgress progress = goal.getProgress();

        // Approved progress bar (green)
        html.append("<div style='display: flex; align-items: center; gap: 6px; margin-bottom: 4px;'>")
            .append("<div style='color: #10b981; font-size: 10px; min-width: 60px;'>✓ Progress</div>")
            .append("<div class='progress-bar'>")
            .append("<div style='height: 100%; background: #10b981; width: ")
            .append(progress.getApprovedPercentage()).append("%'></div>")
            .append("</div>")
            .append("<div style='color: #9ca3af; font-size: 10px; min-width: 40px;'>")
            .append(progress.getApprovedProgress()).append("/").append(goal.getTargetValue())
            .append("</div>")
            .append("</div>");

        // Pending progress bar (yellow) if applicable
        if (progress.getTotalProgress() > progress.getApprovedProgress()) {
            double totalPercentage = goal.getTargetValue() > 0
                ? Math.min(100.0, (double) progress.getTotalProgress() / goal.getTargetValue() * 100)
                : 0;

            html.append("<div style='display: flex; align-items: center; gap: 6px; margin-bottom: 4px;'>")
                .append("<div style='color: #f59e0b; font-size: 10px; min-width: 60px;'>⏳ Pending</div>")
                .append("<div class='progress-bar'>")
                .append("<div style='height: 100%; background: #f59e0b; width: ")
                .append(totalPercentage).append("%'></div>")
                .append("</div>")
                .append("<div style='color: #9ca3af; font-size: 10px; min-width: 40px;'>")
                .append(progress.getTotalProgress()).append("/").append(goal.getTargetValue())
                .append("</div>")
                .append("</div>");
        }

        // Completion status
        if (progress.isCompleted()) {
            html.append("<div style='color: #10b981; font-size: 10px; font-weight: 600; margin-top: 4px;'>")
                .append("✓ Completed!")
                .append("</div>");
        }
    }

    /**
     * Appends a placeholder progress bar when goal has no progress data.
     */
    private void appendGoalNoProgress(StringBuilder html, Goal goal) {
        html.append("<div class='progress-bar'>")
            .append("<div class='progress-fill' style='width: 0%'></div>")
            .append("</div>")
            .append("<div class='progress-text'>0/").append(goal.getTargetValue()).append("</div>");
    }

    /**
     * Gets the CSS class for a status badge.
     */
    private String getStatusCssClass(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "status-pending";
            case ACCEPTED: return "status-approved";
            case REQUIRES_INTERACTION: return "status-needs-review";
            case DECLINED: return "status-declined";
            default: return "";
        }
    }

    /**
     * Gets the icon for a status badge.
     */
    private String getStatusIcon(TileSubmissionType status) {
        switch (status) {
            case PENDING: return "⏳";
            case ACCEPTED: return "✓";
            case REQUIRES_INTERACTION: return "!";
            case DECLINED: return "✗";
            default: return "";
        }
    }
}
