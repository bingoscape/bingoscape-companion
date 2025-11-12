package org.bingoscape.constants;

/**
 * Constants for bingo board types.
 * Provides type-safe string constants for board type comparisons.
 */
public final class BingoTypeConstants {

    /**
     * Standard grid-based bingo board (NxN grid layout).
     */
    public static final String STANDARD = "standard";

    /**
     * Progressive tier-based bingo board with unlockable tiers.
     */
    public static final String PROGRESSION = "progression";

    // Private constructor to prevent instantiation
    private BingoTypeConstants() {
        throw new AssertionError("BingoTypeConstants is a utility class and should not be instantiated");
    }

    /**
     * Checks if a bingo type string represents a progressive board.
     *
     * @param bingoType The bingo type string to check
     * @return true if the type is progressive
     */
    public static boolean isProgressive(String bingoType) {
        return PROGRESSION.equals(bingoType);
    }

    /**
     * Checks if a bingo type string represents a standard board.
     *
     * @param bingoType The bingo type string to check
     * @return true if the type is standard
     */
    public static boolean isStandard(String bingoType) {
        return STANDARD.equals(bingoType);
    }
}
