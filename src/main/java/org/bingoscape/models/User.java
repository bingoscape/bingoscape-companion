package org.bingoscape.models;

import lombok.Data;

import java.util.UUID; /**
 * Represents a user in the system
 */
@Data
public class User {
    private UUID id;
    private String name;
    private String runescapeName;
}
