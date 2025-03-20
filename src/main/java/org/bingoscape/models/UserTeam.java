package org.bingoscape.models;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserTeam {
    private String name;
    private List<TeamMember> members;
}
