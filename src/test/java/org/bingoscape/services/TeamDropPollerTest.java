package org.bingoscape.services;

import com.google.gson.Gson;
import org.bingoscape.models.TeamCompletionsResponse;
import org.bingoscape.models.TeamDropsResponse;
import org.bingoscape.notifications.TileCompletedNotification;
import org.bingoscape.notifications.DropNotification;
import org.bingoscape.notifications.DropTier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TeamDropPollerTest {
    private final TeamDropPoller poller = new TeamDropPoller(null, null, null, null, null, null, null);

    private static TeamDropsResponse.Drop drop(String id, String player, String status, long value) {
        TeamDropsResponse.Drop drop = new TeamDropsResponse.Drop();
        drop.setId(id);
        drop.setStatus(status);
        TeamDropsResponse.Player p = new TeamDropsResponse.Player();
        p.setRunescapeName(player);
        drop.setPlayer(p);
        TeamDropsResponse.Item item = new TeamDropsResponse.Item();
        item.setItemId(4151);
        item.setQuantity(1);
        item.setValue(value);
        drop.setItem(item);
        drop.setTileTitle("Whip");
        return drop;
    }

    private static TeamDropsResponse response(String cursor, TeamDropsResponse.Drop... drops) {
        TeamDropsResponse r = new TeamDropsResponse();
        r.setCursor(cursor);
        r.setDrops(new ArrayList<>(List.of(drops)));
        return r;
    }

    @Test
    public void firstPollOnlyBaselinesAndNeverReplays() {
        List<TeamDropsResponse.Drop> shown = poller.applyResponse(null, response("c1", drop("a", "Mate", "approved", 5)), "Me", 0);
        assertTrue(shown.isEmpty());
        assertEquals("c1", poller.getCursor());
    }

    @Test
    public void cursorAdvancesAndDuplicatesAreSkipped() {
        poller.applyResponse(null, response("c1"), "Me", 0);
        List<TeamDropsResponse.Drop> first = poller.applyResponse("c1", response("c2", drop("a", "Mate", "approved", 5)), "Me", 0);
        assertEquals(1, first.size());
        assertEquals("c2", poller.getCursor());

        List<TeamDropsResponse.Drop> again = poller.applyResponse("c2",
                response("c3", drop("a", "Mate", "approved", 5), drop("b", "Mate", "pending", 5)), "Me", 0);
        assertEquals(1, again.size());
        assertEquals("b", again.get(0).getId());
        assertEquals("c3", poller.getCursor());
    }

    @Test
    public void ownRejectedAndLowValueDropsAreSkipped() {
        List<TeamDropsResponse.Drop> shown = poller.applyResponse("c", response("c2",
                drop("own", "my_name", "approved", 100),
                drop("rej", "Mate", "rejected", 100),
                drop("low", "Mate", "approved", 10),
                drop("ok", "Mate", "approved", 100)), "My Name", 50);
        assertEquals(1, shown.size());
        assertEquals("ok", shown.get(0).getId());
    }

    @Test
    public void resyncDropsCursorAndSeen() {
        poller.applyResponse("c", response("c2", drop("a", "Mate", "approved", 1)), "Me", 0);
        TeamDropsResponse resync = response("c9", drop("z", "Mate", "approved", 1));
        resync.setResync(true);
        assertTrue(poller.applyResponse("c2", resync, "Me", 0).isEmpty());
        assertNull(poller.getCursor());
        // Seen ids were cleared, so "a" counts as new again
        assertEquals(1, poller.applyResponse("c3", response("c4", drop("a", "Mate", "approved", 1)), "Me", 0).size());
    }

    @Test
    public void popupsPerPollAreCapped() {
        TeamDropsResponse.Drop[] drops = new TeamDropsResponse.Drop[12];
        for (int i = 0; i < drops.length; i++) {
            drops[i] = drop("d" + i, "Mate", "approved", 1);
        }
        assertEquals(TeamDropPoller.MAX_POPUPS_PER_POLL, poller.applyResponse("c", response("c2", drops), "Me", 0).size());
    }

    @Test
    public void seenSetIsBounded() {
        for (int i = 0; i < TeamDropPoller.MAX_SEEN + 100; i++) {
            poller.applyResponse("c", response("c", drop("d" + i, "Mate", "approved", 1)), "Me", 0);
        }
        // The oldest id was evicted, the newest is remembered
        assertEquals(1, poller.applyResponse("c", response("c", drop("d0", "Mate", "approved", 1)), "Me", 0).size());
        assertTrue(poller.applyResponse("c", response("c", drop("d" + (TeamDropPoller.MAX_SEEN + 99), "Mate", "approved", 1)), "Me", 0).isEmpty());
    }

    @Test
    public void intervalHonorsServerWithMinimumAndDefault() {
        assertEquals(10_000, TeamDropPoller.intervalMs(0));
        assertEquals(5_000, TeamDropPoller.intervalMs(1_000));
        assertEquals(15_000, TeamDropPoller.intervalMs(15_000));
    }

    @Test
    public void backoffDoublesUpToCap() {
        assertEquals(20_000, TeamDropPoller.backoffMs(1, 10_000));
        assertEquals(40_000, TeamDropPoller.backoffMs(2, 10_000));
        assertEquals(80_000, TeamDropPoller.backoffMs(3, 10_000));
        assertEquals(120_000, TeamDropPoller.backoffMs(4, 10_000));
        assertEquals(120_000, TeamDropPoller.backoffMs(30, 10_000));
    }

    @Test
    public void errorDelayAppliesJitterRetryAfterAndCap() {
        assertEquals(16_000, TeamDropPoller.errorDelayMs(1, 10_000, -1, 0.0));
        assertEquals(24_000, TeamDropPoller.errorDelayMs(1, 10_000, -1, 1.0));
        assertEquals(60_000, TeamDropPoller.errorDelayMs(1, 10_000, 60, 0.5));
        assertEquals(120_000, TeamDropPoller.errorDelayMs(1, 10_000, 600, 0.5));
        assertEquals(120_000, TeamDropPoller.errorDelayMs(10, 10_000, -1, 1.0));
    }

    @Test
    public void namesAreNormalized() {
        assertEquals("my name", TeamDropPoller.normalizeName("  My_Name "));
        assertEquals("my name", TeamDropPoller.normalizeName("My Name"));
        assertEquals("", TeamDropPoller.normalizeName(null));
    }

    @Test
    public void gsonParsesResponse() {
        String json = "{\"cursor\":\"abc\",\"hasMore\":true,\"resync\":false,\"pollIntervalMs\":8000,\"unknown\":1,"
                + "\"drops\":[{\"id\":\"1\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"status\":\"approved\",\"eventId\":\"e\","
                + "\"bingoId\":\"b\",\"teamId\":\"t\",\"teamName\":\"Team\",\"tileId\":\"ti\",\"tileTitle\":\"Whip\","
                + "\"player\":{\"runescapeName\":\"Mate\"},\"item\":{\"itemId\":4151,\"quantity\":2,\"value\":5000000},"
                + "\"source\":{\"name\":\"Abyssal demon\",\"type\":\"npc\",\"npcId\":415},\"imageUrl\":null}]}";
        TeamDropsResponse r = new Gson().fromJson(json, TeamDropsResponse.class);
        assertEquals("abc", r.getCursor());
        assertTrue(r.isHasMore());
        assertEquals(8000, r.getPollIntervalMs());
        TeamDropsResponse.Drop d = r.getDrops().get(0);
        assertEquals("Mate", d.getPlayer().getRunescapeName());
        assertEquals(2, d.getItem().getQuantity());
        assertEquals(5_000_000L, d.getItem().getValue());
        assertEquals(Integer.valueOf(415), d.getSource().getNpcId());
    }

    @Test
    public void notificationCarriesPlayerAndTier() {
        DropNotification n = TeamDropPoller.toNotification(drop("a", "Mate", "approved", 5_000_000), "Abyssal whip",
                100_000, 1_000_000, 10_000_000, 100_000_000);
        assertEquals("Mate", n.getPlayerName());
        assertEquals(DropTier.RARE, n.getTier());
        assertEquals("Whip", n.getTileTitle());
    }

    private static TeamCompletionsResponse.Completion completion(String id, String at, String bingoId) {
        TeamCompletionsResponse.Completion c = new TeamCompletionsResponse.Completion();
        c.setId(id);
        c.setCompletedAt(at);
        c.setBingoId(bingoId);
        c.setTileTitle("Tile " + id);
        c.setTeamName("Team");
        TeamCompletionsResponse.Tile t = new TeamCompletionsResponse.Tile();
        t.setPoints(7);
        c.setTile(t);
        return c;
    }

    private static TeamCompletionsResponse completions(String cursor, TeamCompletionsResponse.Completion... items) {
        TeamCompletionsResponse r = new TeamCompletionsResponse();
        r.setCursor(cursor);
        r.setCompletions(new ArrayList<>(List.of(items)));
        return r;
    }

    @Test
    public void completionBaselineShowsNothing() {
        TeamDropPoller.CompletionBatch batch = poller.applyCompletions(null, completions("c1", completion("a", "t1", "b")), "b");
        assertTrue(batch.shown.isEmpty());
        assertFalse(batch.refreshBoard);
        assertEquals("c1", poller.getCompletionCursor());
    }

    @Test
    public void completionsDedupOnIdAndCompletedAt() {
        poller.applyCompletions(null, completions("c1"), "b");
        assertEquals(1, poller.applyCompletions("c1", completions("c2", completion("a", "t1", "b")), "b").shown.size());
        assertTrue(poller.applyCompletions("c2", completions("c3", completion("a", "t1", "b")), "b").shown.isEmpty());
        // Same tile completed again later
        TeamDropPoller.CompletionBatch again = poller.applyCompletions("c3", completions("c4", completion("a", "t2", "b")), "b");
        assertEquals(1, again.shown.size());
        assertEquals("c4", poller.getCompletionCursor());
    }

    @Test
    public void nullCompletionsListIsEmpty() {
        TeamCompletionsResponse r = new TeamCompletionsResponse();
        r.setCursor("c2");
        TeamDropPoller.CompletionBatch batch = poller.applyCompletions("c1", r, "b");
        assertTrue(batch.shown.isEmpty());
        assertEquals("c2", poller.getCompletionCursor());
    }

    @Test
    public void completionsArePerPollCappedButAllRequestRefreshOnce() {
        TeamDropPoller.CompletionBatch batch = poller.applyCompletions("c", completions("c2",
                completion("1", "t", "other"), completion("2", "t", "b"), completion("3", "t", "b"),
                completion("4", "t", "b"), completion("5", "t", "b")), "b");
        assertEquals(TeamDropPoller.MAX_COMPLETIONS_PER_POLL, batch.shown.size());
        assertTrue(batch.refreshBoard);
    }

    @Test
    public void refreshOnlyForCurrentBingo() {
        assertFalse(poller.applyCompletions("c", completions("c2", completion("1", "t", "other")), "b").refreshBoard);
        assertFalse(poller.applyCompletions("c2", completions("c3", completion("2", "t", "b")), null).refreshBoard);
    }

    @Test
    public void completionResyncDropsCursorAndSeenIndependentOfDrops() {
        poller.applyResponse("c", response("d2", drop("a", "Mate", "approved", 1)), "Me", 0);
        poller.applyCompletions("c", completions("c2", completion("a", "t", "b")), "b");
        TeamCompletionsResponse resync = completions("c9", completion("z", "t", "b"));
        resync.setResync(true);
        assertTrue(poller.applyCompletions("c2", resync, "b").shown.isEmpty());
        assertNull(poller.getCompletionCursor());
        assertEquals("d2", poller.getCursor());
        // Seen set was cleared: "a" shows again
        assertEquals(1, poller.applyCompletions("c9", completions("c10", completion("a", "t", "b")), "b").shown.size());
    }

    @Test
    public void completionIdsDoNotCollideWithDropIds() {
        poller.applyResponse("c", response("d2", drop("a", "Mate", "approved", 1)), "Me", 0);
        assertEquals(1, poller.applyCompletions("c", completions("c2", completion("a", "t", "b")), "b").shown.size());
    }

    @Test
    public void gsonParsesCompletionsWithUnknownFields() {
        String json = "{\"cursor\":\"abc\",\"hasMore\":true,\"resync\":false,\"pollIntervalMs\":8000,\"extra\":{},"
                + "\"completions\":[{\"id\":\"1\",\"completedAt\":\"2026-01-01T00:00:00Z\",\"eventId\":\"e\",\"bingoId\":\"b\","
                + "\"teamId\":\"t\",\"teamName\":\"Team\",\"tileId\":\"ti\",\"tileTitle\":\"Whip\",\"x\":1,"
                + "\"tile\":{\"points\":10,\"tier\":3,\"other\":true}}]}";
        TeamCompletionsResponse r = new Gson().fromJson(json, TeamCompletionsResponse.class);
        assertEquals("abc", r.getCursor());
        assertTrue(r.isHasMore());
        assertEquals(8000, r.getPollIntervalMs());
        TeamCompletionsResponse.Completion c = r.getCompletions().get(0);
        assertEquals("b", c.getBingoId());
        assertEquals("Whip", c.getTileTitle());
        assertEquals(10, c.getTile().getPoints());

        assertNull(new Gson().fromJson("{\"cursor\":\"x\"}", TeamCompletionsResponse.class).getCompletions());
    }

    @Test
    public void completionNotificationCarriesTitlePointsAndTeam() {
        TileCompletedNotification n = TeamDropPoller.toNotification(completion("a", "t", "b"));
        assertEquals("Tile a", n.getTileTitle());
        assertEquals(7, n.getPoints());
        assertEquals("Team", n.getTeamName());
    }
}
