package org.bingoscape;

/**
 * How tiles of a locked progression tier are shown on the board overlay.
 */
public enum LockedTileDisplay {
    DIM("Dim with lock"),
    HIDE("Hide");

    private final String name;

    LockedTileDisplay(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
