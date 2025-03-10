package org.bingoscape.models;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Data
public class EventData {
    private UUID id;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private UUID creatorId;
    private UUID clanId;
    private Date createdAt;
    private Date updatedAt;
    private boolean locked;
    private boolean isPublic;
    private long basePrizePool;
    private long minimumBuyIn;
    private List<EventParticipant> eventParticipants;
    private Clan clan;
    private List<Bingo> bingos;
    private UserTeam userTeam;
    private Role role;
}

