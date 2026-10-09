package org.bingoscape.notifications;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
