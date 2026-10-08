package org.bingoscape.notifications;

import java.util.Arrays;

/**
 * Maps the GE value of a drop to a {@link DropTier}, and formats gp amounts for the popup.
 */
public final class DropTierResolver {
    private DropTierResolver() {
    }

    /**
     * A value at or above a threshold falls into that threshold's tier. Thresholds are sorted first,
     * so a misconfigured order still gives monotonic tiers.
     */
    public static DropTier resolve(long stackValue, long uncommon, long rare, long epic, long legendary) {
        long[] thresholds = {uncommon, rare, epic, legendary};
        Arrays.sort(thresholds);

        DropTier[] tiers = DropTier.values();
        DropTier result = DropTier.COMMON;
        for (int i = 0; i < thresholds.length; i++) {
            if (stackValue >= Math.max(1, thresholds[i])) {
                result = tiers[i + 1];
            }
        }
        return result;
    }

    /**
     * Formats a gp amount the way the game does: 950, 12.5k, 3.2m, 1.1b.
     */
    public static String formatGp(long value) {
        if (value < 10_000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return trim(value / 1_000.0) + "k";
        }
        if (value < 1_000_000_000L) {
            return trim(value / 1_000_000.0) + "m";
        }
        return trim(value / 1_000_000_000.0) + "b";
    }

    private static String trim(double scaled) {
        // One decimal below 100, none above; no trailing ".0"
        if (scaled >= 100) {
            return String.valueOf(Math.round(scaled));
        }
        double rounded = Math.round(scaled * 10) / 10.0;
        return rounded == Math.floor(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }
}
