package org.bingoscape.models;

import lombok.Data;

import java.util.List;

/**
 * Response of the team tile completion feed. Unknown fields are ignored by Gson.
 */
@Data
public class TeamCompletionsResponse {
    private String cursor;
    private boolean hasMore;
    private boolean resync;
    private long pollIntervalMs;
    private List<Completion> completions;

    @Data
    public static class Completion {
        private String id;
        private String completedAt;
        private String eventId;
        private String bingoId;
        private String teamId;
        private String teamName;
        private String tileId;
        private String tileTitle;
        private Tile tile;
    }

    @Data
    public static class Tile {
        private int points;
        private String tier;
    }
}
