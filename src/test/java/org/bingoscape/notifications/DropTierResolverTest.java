package org.bingoscape.notifications;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DropTierResolverTest {
    private static DropTier tier(long value) {
        return DropTierResolver.resolve(value, 100_000, 1_000_000, 10_000_000, 100_000_000);
    }

    @Test
    public void valueAtThresholdFallsIntoHigherTier() {
        assertEquals(DropTier.COMMON, tier(0));
        assertEquals(DropTier.COMMON, tier(99_999));
        assertEquals(DropTier.UNCOMMON, tier(100_000));
        assertEquals(DropTier.RARE, tier(1_000_000));
        assertEquals(DropTier.EPIC, tier(10_000_000));
        assertEquals(DropTier.LEGENDARY, tier(100_000_000));
        assertEquals(DropTier.LEGENDARY, tier(2_000_000_000L));
    }

    @Test
    public void unsortedThresholdsStayMonotonic() {
        assertEquals(DropTier.UNCOMMON, DropTierResolver.resolve(150, 1000, 100, 10_000, 500));
        assertEquals(DropTier.LEGENDARY, DropTierResolver.resolve(10_000, 1000, 100, 10_000, 500));
    }

    @Test
    public void zeroThresholdDoesNotPromoteWorthlessDrops() {
        assertEquals(DropTier.COMMON, DropTierResolver.resolve(0, 0, 0, 0, 0));
    }

    @Test
    public void tiersHaveDistinctColors() {
        for (DropTier a : DropTier.values()) {
            for (DropTier b : DropTier.values()) {
                assertTrue(a == b || !a.color().equals(b.color()));
            }
        }
    }

    @Test
    public void formatsGpLikeTheGame() {
        assertEquals("0", DropTierResolver.formatGp(0));
        assertEquals("9999", DropTierResolver.formatGp(9_999));
        assertEquals("10k", DropTierResolver.formatGp(10_000));
        assertEquals("12.5k", DropTierResolver.formatGp(12_500));
        assertEquals("250k", DropTierResolver.formatGp(250_000));
        assertEquals("1m", DropTierResolver.formatGp(1_000_000));
        assertEquals("3.3m", DropTierResolver.formatGp(3_250_000));
        assertEquals("100k", DropTierResolver.formatGp(99_960));
        assertEquals("1.1b", DropTierResolver.formatGp(1_100_000_000L));
    }
}
