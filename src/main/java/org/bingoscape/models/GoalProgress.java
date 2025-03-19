package org.bingoscape.models;

import lombok.Data; /**
 * Represents progress towards a goal
 */
@Data
public class GoalProgress {
    private String id;
    private int currentValue;
    private int targetValue;
    private String description;
    private int progress; // Progress as percentage (0-100)
}
