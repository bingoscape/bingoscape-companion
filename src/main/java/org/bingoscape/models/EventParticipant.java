package org.bingoscape.models;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Data
public class EventParticipant {
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private Role role;
    private Date createdAt;
    private Date updatedAt;
}
