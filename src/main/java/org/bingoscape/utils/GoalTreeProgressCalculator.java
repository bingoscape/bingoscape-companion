package org.bingoscape.utils;

import lombok.Data;
import org.bingoscape.models.GoalTreeNode;

import java.util.List;

/**
 * Utility class for calculating progress from goal trees.
 * Based on the logic from the Bingoscape web app's goal-progress-tree component.
 */
public class GoalTreeProgressCalculator {

    /**
     * Result of progress calculation
     */
    @Data
    public static class ProgressResult {
        private final int completedCount;
        private final int totalCount;
        private final boolean isComplete;
        private final double percentage;

        public ProgressResult(int completedCount, int totalCount, boolean isComplete) {
            this.completedCount = completedCount;
            this.totalCount = totalCount;
            this.isComplete = isComplete;
            this.percentage = totalCount > 0 ? (double) completedCount / totalCount * 100.0 : 0.0;
        }
    }

    /**
     * Calculate progress from the root level of a goal tree.
     * Only evaluates the immediate children of the root, not nested levels.
     *
     * @param goalTree List of root-level GoalTreeNode objects
     * @return ProgressResult containing completion counts and percentage
     */
    public static ProgressResult calculateRootProgress(List<GoalTreeNode> goalTree) {
        if (goalTree == null || goalTree.isEmpty()) {
            return new ProgressResult(0, 0, false);
        }

        // If there's only one root node and it's a group, use its progress
        if (goalTree.size() == 1 && goalTree.get(0).isGroup()) {
            GoalTreeNode rootGroup = goalTree.get(0);
            return evaluateNode(rootGroup);
        }

        // Multiple root nodes - evaluate each and aggregate
        int totalNodes = goalTree.size();
        int completedNodes = 0;

        for (GoalTreeNode node : goalTree) {
            ProgressResult nodeProgress = evaluateNode(node);
            if (nodeProgress.isComplete()) {
                completedNodes++;
            }
        }

        boolean allComplete = completedNodes == totalNodes;
        return new ProgressResult(completedNodes, totalNodes, allComplete);
    }

    /**
     * Evaluate a single node (goal or group) for completion.
     *
     * @param node The GoalTreeNode to evaluate
     * @return ProgressResult for this node
     */
    private static ProgressResult evaluateNode(GoalTreeNode node) {
        if (node == null) {
            return new ProgressResult(0, 0, false);
        }

        // If it's a goal (leaf node), check its progress
        if (node.isGoal()) {
            boolean isComplete = node.getProgress() != null && node.getProgress().isComplete();
            return new ProgressResult(isComplete ? 1 : 0, 1, isComplete);
        }

        // If it's a group, evaluate based on children and logical operator
        if (node.isGroup()) {
            return evaluateGroup(node);
        }

        // Unknown node type
        return new ProgressResult(0, 0, false);
    }

    /**
     * Evaluate a group node based on its children and logical operator (AND/OR).
     * Matches the logic from the web app's goal-progress-tree.tsx.
     *
     * @param group The group node to evaluate
     * @return ProgressResult for this group
     */
    private static ProgressResult evaluateGroup(GoalTreeNode group) {
        List<GoalTreeNode> children = group.getChildren();

        if (children == null || children.isEmpty()) {
            // Empty group - consider incomplete
            return new ProgressResult(0, 0, false);
        }

        // Evaluate all children
        int completedChildren = 0;
        for (GoalTreeNode child : children) {
            ProgressResult childProgress = evaluateNode(child);
            if (childProgress.isComplete()) {
                completedChildren++;
            }
        }

        int totalChildren = children.size();
        boolean isComplete;
        int displayTotal;
        int displayCompleted = completedChildren;

        // Apply logical operator
        if ("AND".equalsIgnoreCase(group.getLogicalOperator())) {
            // AND: all children must be complete
            isComplete = totalChildren > 0 && completedChildren == totalChildren;
            displayTotal = totalChildren;
        } else {
            // OR: at least minRequiredGoals must be complete
            Integer minRequired = group.getMinRequiredGoals();
            if (minRequired == null || minRequired <= 0) {
                minRequired = 1; // Default to at least 1
            }

            // Cap minRequired at totalChildren
            minRequired = Math.min(minRequired, totalChildren);

            isComplete = completedChildren >= minRequired;
            displayTotal = minRequired;
        }

        return new ProgressResult(displayCompleted, displayTotal, isComplete);
    }

    /**
     * Helper method to get the progress result from a tile's goal tree.
     * This is a convenience method for use in UI components.
     *
     * @param goalTree The tile's goal tree
     * @return ProgressResult or null if no goals
     */
    public static ProgressResult getProgressFromTile(List<GoalTreeNode> goalTree) {
        if (goalTree == null || goalTree.isEmpty()) {
            return null;
        }
        return calculateRootProgress(goalTree);
    }
}
