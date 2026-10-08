package org.bingoscape.notifications;

import org.bingoscape.ui.BingoTheme;
import org.bingoscape.ui.TileStatusStyle;

import java.awt.Color;

/**
 * Value tier of a drop. Decides the accent color of the drop popup.
 */
public enum DropTier {
    COMMON("Common", new Color(200, 200, 200)),
    UNCOMMON("Uncommon", BingoTheme.PROGRESS_COMPLETE),
    RARE("Rare", TileStatusStyle.PENDING),
    EPIC("Epic", new Color(168, 85, 247)),
    LEGENDARY("Legendary", BingoTheme.GOLD);

    private final String label;
    private final Color color;

    DropTier(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public Color color() {
        return color;
    }
}
