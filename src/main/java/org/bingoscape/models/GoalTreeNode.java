package org.bingoscape.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/**
 * Base class for goal tree nodes. A goal tree represents the hierarchical
 * structure of requirements for completing a tile, with support for AND/OR logic.
 */
@Data
public class GoalTreeNode {
    private String type; // "group" or "goal"
    private String id;
    private int orderIndex;

    // Group-specific fields
    private String name;
    @SerializedName("logicalOperator")
    private String logicalOperator; // "AND" or "OR"
    private Integer minRequiredGoals; // For OR groups: how many goals must be satisfied
    private List<GoalTreeNode> children;
    private GoalTreeProgress progress;

    // Goal-specific fields
    private String description;
    private Integer targetValue;
    private String goalType; // "item" or "generic"
    private ItemGoal itemGoal;
    // Note: progress field is already declared above (shared between group and goal)

    /**
     * Checks if this node is a group node.
     */
    public boolean isGroup() {
        return "group".equals(type);
    }

    /**
     * Checks if this node is a goal (leaf) node.
     */
    public boolean isGoal() {
        return "goal".equals(type);
    }

    /**
     * Checks if this is an AND group.
     */
    public boolean isAndGroup() {
        return isGroup() && "AND".equals(logicalOperator);
    }

    /**
     * Checks if this is an OR group.
     */
    public boolean isOrGroup() {
        return isGroup() && "OR".equals(logicalOperator);
    }

    /**
     * Checks if this goal is for an item (has itemGoal).
     */
    public boolean isItemGoal() {
        return isGoal() && "item".equals(goalType) && itemGoal != null;
    }
}
