package org.bingoscape.models;

import lombok.Data;

@Data
public class Goal {
    private String id;
    private String description;
    private int targetValue;
}
