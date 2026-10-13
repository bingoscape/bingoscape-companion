package org.bingoscape.notifications;

import org.bingoscape.board.BoardDiff;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DropNotificationOverlayTest {
    private static final long HOLD = 5000;

    @Test
    public void alphaFadesInHoldsAndFadesOut() {
        assertEquals(0f, DropNotificationOverlay.alpha(0, HOLD, false), 0.001f);
        assertEquals(1f, DropNotificationOverlay.alpha(DropNotificationOverlay.SLIDE_MS, HOLD, false), 0.001f);
        assertEquals(1f, DropNotificationOverlay.alpha(3000, HOLD, false), 0.001f);

        long fadeStart = DropNotificationOverlay.SLIDE_MS + HOLD;
        assertEquals(0.5f, DropNotificationOverlay.alpha(fadeStart + DropNotificationOverlay.FADE_MS / 2, HOLD, false), 0.001f);
        assertEquals(0f, DropNotificationOverlay.alpha(fadeStart + DropNotificationOverlay.FADE_MS, HOLD, false), 0.001f);
    }

    @Test
    public void captionShowsPlayerForTeamDrops() {
        DropNotification own = new DropNotification(1, "Item", 1, 10, DropTier.COMMON, null, 0, null);
        DropNotification team = new DropNotification(1, "Item", 1, 10, DropTier.COMMON, null, 0, null, "Mate");
        assertEquals("Bingo Item", DropNotificationOverlay.caption(own));
        assertEquals("Mate received", DropNotificationOverlay.caption(team));
    }

    @Test
    public void queuedPopupsHoldShorter() {
        long normal = DropNotificationOverlay.totalMs(HOLD, false);
        long hurried = DropNotificationOverlay.totalMs(HOLD, true);
        assertTrue(hurried < normal);
        assertEquals(DropNotificationOverlay.SLIDE_MS + 3000 + DropNotificationOverlay.FADE_MS, hurried);
    }

    @Test
    public void slideProgressIsMonotonicAndBounded() {
        assertEquals(0f, DropNotificationOverlay.slideProgress(0), 0.001f);
        assertEquals(1f, DropNotificationOverlay.slideProgress(DropNotificationOverlay.SLIDE_MS), 0.001f);
        assertEquals(1f, DropNotificationOverlay.slideProgress(10_000), 0.001f);
        float previous = 0;
        for (long t = 0; t <= DropNotificationOverlay.SLIDE_MS; t += 25) {
            float p = DropNotificationOverlay.slideProgress(t);
            assertTrue(p >= previous);
            previous = p;
        }
    }

    @Test
    public void layoutRowsDoNotOverlapAtAnyFontSize() {
        for (int small = 10; small <= 32; small += 2) {
            int big = small + 2;
            DropNotificationOverlay.Layout plain = DropNotificationOverlay.Layout.of(small, big, 8, false);
            assertTrue(plain.nameY >= plain.captionY + plain.pillHeight);
            assertTrue(plain.statY >= plain.nameY + big);
            assertTrue(plain.contentBottom >= plain.statY + small);
            assertEquals(plain.contentBottom, plain.height);

            DropNotificationOverlay.Layout warned = DropNotificationOverlay.Layout.of(small, big, 8, true);
            assertEquals(plain.contentBottom, warned.warningY);
            assertTrue(warned.height >= warned.warningY + small);
        }
    }

    private static DropNotification drop(String name) {
        return new DropNotification(1, name, 1, 10, DropTier.COMMON, null, 0, null);
    }

    private static TileCompletedNotification done(String title) {
        return new TileCompletedNotification(title, 5, "Team");
    }

    private static DropNotificationOverlay overlay() {
        return new DropNotificationOverlay(null, null);
    }

    @Test
    public void completionsComeFirstAndStayFifoWithinPriority() {
        DropNotificationOverlay overlay = overlay();
        overlay.enqueue(drop("d1"));
        overlay.enqueue(done("c1"));
        overlay.enqueue(drop("d2"));
        overlay.enqueue(done("c2"));
        assertEquals("c1", ((TileCompletedNotification) overlay.pollNext()).getTileTitle());
        assertEquals("c2", ((TileCompletedNotification) overlay.pollNext()).getTileTitle());
        assertEquals("d1", ((DropNotification) overlay.pollNext()).getItemName());
        assertEquals("d2", ((DropNotification) overlay.pollNext()).getItemName());
        assertNull(overlay.pollNext());
    }

    @Test
    public void overflowEvictsDropsBeforeCompletions() {
        DropNotificationOverlay overlay = overlay();
        for (int i = 0; i < 10; i++) {
            overlay.enqueue(drop("d" + i));
        }
        overlay.enqueue(drop("late"));
        assertEquals(10, overlay.queueSize());

        overlay.enqueue(done("c1"));
        assertEquals(10, overlay.queueSize());
        assertEquals("c1", ((TileCompletedNotification) overlay.pollNext()).getTileTitle());
        // The oldest drop made room
        assertEquals("d1", ((DropNotification) overlay.pollNext()).getItemName());
    }

    @Test
    public void completionIsDroppedWhenQueueHoldsOnlyCompletions() {
        DropNotificationOverlay overlay = overlay();
        for (int i = 0; i < 10; i++) {
            overlay.enqueue(done("c" + i));
        }
        overlay.enqueue(done("extra"));
        overlay.enqueue(drop("d"));
        assertEquals(10, overlay.queueSize());
        for (int i = 0; i < 10; i++) {
            assertEquals("c" + i, ((TileCompletedNotification) overlay.pollNext()).getTileTitle());
        }
    }

    @Test
    public void completionsHoldOneAndAHalfTimesLonger() {
        assertEquals(7500, DropNotificationOverlay.holdFor(done("c"), HOLD));
        assertEquals(HOLD, DropNotificationOverlay.holdFor(drop("d"), HOLD));
    }

    private static TierUnlockedNotification tier(int tier) {
        return new TierUnlockedNotification(tier, "Board");
    }

    private static ShipResultNotification shot(BoardDiff.Result result, int remaining) {
        return new ShipResultNotification("Zulrah", result, remaining);
    }

    @Test
    public void newTypesAreCompletionPriority() {
        assertEquals(Notification.PRIORITY_COMPLETION, tier(1).priority());
        for (BoardDiff.Result result : BoardDiff.Result.values()) {
            assertEquals(Notification.PRIORITY_COMPLETION, shot(result, 1).priority());
        }
    }

    @Test
    public void newTypesHoldLongerThanDrops() {
        assertEquals(7500, DropNotificationOverlay.holdFor(tier(1), HOLD));
        assertEquals(7500, DropNotificationOverlay.holdFor(shot(BoardDiff.Result.SUNK, 0), HOLD));
        assertEquals(HOLD, DropNotificationOverlay.holdFor(shot(BoardDiff.Result.MISS, 2), HOLD));
        assertTrue(DropNotificationOverlay.holdFor(shot(BoardDiff.Result.HIT, 2), HOLD) >= HOLD);
    }

    @Test
    public void newTypesQueueWithCompletionsInFifoOrderBeforeDrops() {
        DropNotificationOverlay overlay = overlay();
        overlay.enqueue(drop("d1"));
        overlay.enqueue(tier(2));
        overlay.enqueue(done("c1"));
        overlay.enqueue(shot(BoardDiff.Result.HIT, 2));
        overlay.enqueue(drop("d2"));

        assertEquals(2, ((TierUnlockedNotification) overlay.pollNext()).getTier());
        assertEquals("c1", ((TileCompletedNotification) overlay.pollNext()).getTileTitle());
        assertEquals(BoardDiff.Result.HIT, ((ShipResultNotification) overlay.pollNext()).getResult());
        assertEquals("d1", ((DropNotification) overlay.pollNext()).getItemName());
        assertEquals("d2", ((DropNotification) overlay.pollNext()).getItemName());
        assertNull(overlay.pollNext());
    }

    @Test
    public void newTypesEvictDropsWhenTheQueueIsFull() {
        DropNotificationOverlay overlay = overlay();
        for (int i = 0; i < 10; i++) {
            overlay.enqueue(drop("d" + i));
        }
        overlay.enqueue(shot(BoardDiff.Result.SUNK, 1));

        assertEquals(10, overlay.queueSize());
        assertEquals(BoardDiff.Result.SUNK, ((ShipResultNotification) overlay.pollNext()).getResult());
        assertEquals("d1", ((DropNotification) overlay.pollNext()).getItemName());
    }

    @Test
    public void bannersCarryTheTextsOfEachType() {
        DropNotificationOverlay.Banner completed = DropNotificationOverlay.bannerOf(done("Zulrah"));
        assertEquals("Tile Completed", completed.caption);
        assertEquals("Zulrah", completed.title);
        assertEquals("+5 pts", completed.stat);
        assertEquals("Team", completed.detail);

        DropNotificationOverlay.Banner unlocked = DropNotificationOverlay.bannerOf(tier(2));
        assertEquals("Tier Unlocked", unlocked.caption);
        assertEquals("Tier 2", unlocked.title);
        assertEquals("Board", unlocked.detail);

        assertEquals("Direct Hit", DropNotificationOverlay.bannerOf(shot(BoardDiff.Result.HIT, -1)).caption);
        assertEquals("Miss", DropNotificationOverlay.bannerOf(shot(BoardDiff.Result.MISS, -1)).caption);
        DropNotificationOverlay.Banner sunk = DropNotificationOverlay.bannerOf(shot(BoardDiff.Result.SUNK, 2));
        assertEquals("Ship Sunk", sunk.caption);
        assertEquals("Zulrah", sunk.title);
        assertEquals("Ships remaining: 2", sunk.stat);
        assertEquals("All ships sunk", DropNotificationOverlay.bannerOf(shot(BoardDiff.Result.SUNK, 0)).stat);
        assertEquals("", DropNotificationOverlay.bannerOf(shot(BoardDiff.Result.HIT, -1)).stat);
    }

    @Test
    public void boardEventsBecomeNotifications() {
        Notification unlocked = BoardNotifications.toNotification(new BoardDiff.TierUnlocked(3), "Board");
        assertEquals(3, ((TierUnlockedNotification) unlocked).getTier());
        assertEquals("Board", ((TierUnlockedNotification) unlocked).getBingoTitle());

        ShipResultNotification sunk = (ShipResultNotification) BoardNotifications.toNotification(
                new BoardDiff.ShipResult(java.util.UUID.randomUUID(), "Vorkath", BoardDiff.Result.SUNK, 1), "Board");
        assertNotNull(sunk);
        assertEquals("Vorkath", sunk.getTileTitle());
        assertEquals(BoardDiff.Result.SUNK, sunk.getResult());
        assertEquals(1, sunk.getShipsRemaining());

        ShipResultNotification untitled = (ShipResultNotification) BoardNotifications.toNotification(
                new BoardDiff.ShipResult(java.util.UUID.randomUUID(), null, BoardDiff.Result.MISS, -1), null);
        assertEquals("Tile", untitled.getTileTitle());

        assertNull(BoardNotifications.toNotification(new BoardDiff.Event() { }, "Board"));
    }
}
