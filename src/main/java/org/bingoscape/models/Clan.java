package org.bingoscape.models;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Data
public class Clan {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private Date createdAt;
    private Date updatedAt;
}
