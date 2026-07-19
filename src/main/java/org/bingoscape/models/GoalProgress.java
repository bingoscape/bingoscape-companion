package org.bingoscape.models;

import lombok.Data;

/**
 * Represents the progress information for a goal.
 * 
 * Contains both approved progress (confirmed progress) and total progress
 * (including pending submissions), along with calculated percentages and completion status.
 * 
 * @author BingoScape Development Team
 */
@Data
public class GoalProgress {
    private int approvedProgress;
    private int totalProgress;
    private double approvedPercentage;
    private boolean isCompleted;
}
