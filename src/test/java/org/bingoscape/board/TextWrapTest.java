package org.bingoscape.board;

import org.junit.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextWrapTest {
    private static final FontMetrics FM = metrics();

    private static FontMetrics metrics() {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        FontMetrics fm = g.getFontMetrics(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.dispose();
        return fm;
    }

    @Test
    public void wrapsOnWordBoundaries() {
        int width = FM.stringWidth("aaaa bbbb");
        List<String> lines = TextWrap.wrap("aaaa bbbb cccc dddd", FM, width, 10);

        assertEquals(2, lines.size());
        assertEquals("aaaa bbbb", lines.get(0));
        assertEquals("cccc dddd", lines.get(1));
    }

    @Test
    public void hardBreaksLongWords() {
        int width = FM.stringWidth("aaaa");
        List<String> lines = TextWrap.wrap("aaaaaaaaaa", FM, width, 10);

        assertEquals(3, lines.size());
        for (String line : lines) {
            assertTrue(FM.stringWidth(line) <= width);
        }
    }

    @Test
    public void truncatesWithEllipsis() {
        int width = FM.stringWidth("aaaa bbbb");
        List<String> lines = TextWrap.wrap("aaaa bbbb cccc dddd eeee ffff", FM, width, 2);

        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith(TextWrap.ELLIPSIS));
        assertTrue(FM.stringWidth(lines.get(1)) <= width);
    }

    @Test
    public void keepsExplicitLineBreaks() {
        List<String> lines = TextWrap.wrap("first\nsecond", FM, 1000, 10);

        assertEquals(2, lines.size());
        assertEquals("second", lines.get(1));
    }

    @Test
    public void emptyInputGivesNoLines() {
        assertTrue(TextWrap.wrap(null, FM, 100, 3).isEmpty());
        assertTrue(TextWrap.wrap("   ", FM, 100, 3).isEmpty());
        assertTrue(TextWrap.wrap("text", FM, 0, 3).isEmpty());
    }

    @Test
    public void ellipsizeOnlyShortensWhenNeeded() {
        assertEquals("short", TextWrap.ellipsize("short", FM, 1000));

        int width = FM.stringWidth("abcdef");
        String shortened = TextWrap.ellipsize("abcdefghijkl", FM, width);
        assertTrue(shortened.endsWith(TextWrap.ELLIPSIS));
        assertTrue(FM.stringWidth(shortened) <= width);
    }
}
