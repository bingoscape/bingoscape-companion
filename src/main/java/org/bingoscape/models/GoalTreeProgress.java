package org.bingoscape.models;

import lombok.Data;

/**
 * Progress information for a goal tree node (either group or goal).
 */
@Data
public class GoalTreeProgress {
    private int completedCount;
    private int totalCount;
    private boolean isComplete;
}
