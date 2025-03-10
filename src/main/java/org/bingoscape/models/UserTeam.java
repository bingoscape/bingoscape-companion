package org.bingoscape.models;

import lombok.Data;

import java.util.UUID;

@Data
public class UserTeam {
    private UUID id;
    private String name;
    private boolean isLeader;
}
