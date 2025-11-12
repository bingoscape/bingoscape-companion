package org.bingoscape.models;

import lombok.Data;
import java.util.List;

@Data
public class Goal {
    private String id;
    private String description;
    private int targetValue;
    private GoalProgress progress;

    // Fields for item-based goals (auto-submission support)
    private String goalType; // "item" or "generic"
}
