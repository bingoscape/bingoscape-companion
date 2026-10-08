package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;
import org.bingoscape.models.TileSubmissionType;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TileStatusStyleTest {
    @Test
    public void submittedStatesHaveDistinctColorsAndLabels() {
        assertEquals(new Color(30, 64, 122), TileStatusStyle.background(TileSubmissionType.PENDING));
        assertEquals(new Color(34, 197, 94), TileStatusStyle.border(TileSubmissionType.ACCEPTED));
        assertEquals(new Color(234, 179, 8), TileStatusStyle.accent(TileSubmissionType.REQUIRES_INTERACTION));
        assertEquals("#ef4444", TileStatusStyle.hex(TileSubmissionType.DECLINED));
        assertEquals("NEEDS ACTION", TileStatusStyle.shortLabel(TileSubmissionType.REQUIRES_INTERACTION));
        assertEquals("Completed", TileStatusStyle.displayText(TileSubmissionType.ACCEPTED));
        assertTrue(TileStatusStyle.isSubmitted(TileSubmissionType.PENDING));
    }

    @Test
    public void notSubmittedAndNullFallBackToDefaults() {
        for (TileSubmissionType status : new TileSubmissionType[]{null, TileSubmissionType.NOT_SUBMITTED}) {
            assertEquals(ColorScheme.DARK_GRAY_COLOR, TileStatusStyle.background(status));
            assertEquals(ColorScheme.BORDER_COLOR, TileStatusStyle.border(status));
            assertEquals(Color.LIGHT_GRAY, TileStatusStyle.accent(status));
            assertEquals("#ffffff", TileStatusStyle.hex(status));
            assertNull(TileStatusStyle.shortLabel(status));
            assertEquals("Not Submitted", TileStatusStyle.displayText(status));
            assertFalse(TileStatusStyle.isSubmitted(status));
        }
    }
}
