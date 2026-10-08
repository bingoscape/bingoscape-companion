package org.bingoscape;

/**
 * How the bingo board is displayed when opened from the side panel.
 */
public enum BoardDisplayMode {
    OVERLAY("In-game overlay"),
    WINDOW("Separate window (legacy)");

    private final String name;

    BoardDisplayMode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
