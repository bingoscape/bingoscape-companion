package org.bingoscape.models;

import lombok.Data;

import java.util.List;

/**
 * Response of the team drop feed. Unknown fields are ignored by Gson.
 */
@Data
public class TeamDropsResponse {
    private String cursor;
    private boolean hasMore;
    private boolean resync;
    private long pollIntervalMs;
    private List<Drop> drops;

    @Data
    public static class Drop {
        private String id;
        private String createdAt;
        /** "pending", "approved" or "rejected". */
        private String status;
        private String eventId;
        private String bingoId;
        private String teamId;
        private String teamName;
        private String tileId;
        private String tileTitle;
        private Player player;
        private Item item;
        private Source source;
        private String imageUrl;
    }

    @Data
    public static class Player {
        private String runescapeName;
    }

    @Data
    public static class Item {
        private int itemId;
        private int quantity;
        private long value;
    }

    @Data
    public static class Source {
        private String name;
        private String type;
        private Integer npcId;
    }
}
