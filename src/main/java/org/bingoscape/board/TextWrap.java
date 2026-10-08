package org.bingoscape.board;

import net.runelite.client.ui.FontManager;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * Text measuring helpers for the board overlay.
 */
final class TextWrap {
    // RuneScape fonts have no single-character ellipsis glyph
    static final String ELLIPSIS = "...";
    private static final float FALLBACK_FONT_SCALE = 0.8f;

    private TextWrap() {
    }

    /**
     * Word-wraps text to {@code maxWidth}. Words wider than a line are hard-broken. If the result
     * exceeds {@code maxLines}, the last kept line ends with an ellipsis.
     */
    static List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty() || maxWidth <= 0 || maxLines <= 0) {
            return lines;
        }

        for (String paragraph : text.trim().split("\\r?\\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }

                String candidate = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }

                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }

                while (word.length() > 1 && fm.stringWidth(word) > maxWidth) {
                    int cut = fittingChars(word, fm, maxWidth);
                    lines.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                line.append(word);
            }
            lines.add(line.toString());
        }

        if (lines.size() <= maxLines) {
            return lines;
        }

        List<String> truncated = new ArrayList<>(lines.subList(0, maxLines));
        int last = maxLines - 1;
        truncated.set(last, withEllipsis(truncated.get(last), fm, maxWidth));
        return truncated;
    }

    /**
     * Shortens text with an ellipsis so it fits {@code maxWidth}. Returns the text unchanged if it fits.
     */
    static String ellipsize(String text, FontMetrics fm, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        return withEllipsis(text, fm, maxWidth);
    }

    /**
     * Returns {@code font} if it can draw every character of {@code text}, otherwise a fallback font
     * (RuneScape fonts lack emoji and most non-Latin glyphs).
     */
    static Font displayable(Font font, String text) {
        if (text == null || font.canDisplayUpTo(text) == -1) {
            return font;
        }
        // RuneScape fonts are nominally larger than they look; shrink the fallback to a similar visual size
        return FontManager.getDefaultFont().deriveFont(font.getStyle(), font.getSize2D() * FALLBACK_FONT_SCALE);
    }

    private static String withEllipsis(String text, FontMetrics fm, int maxWidth) {
        String trimmed = text;
        while (!trimmed.isEmpty() && fm.stringWidth(trimmed + ELLIPSIS) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.trim() + ELLIPSIS;
    }

    private static int fittingChars(String word, FontMetrics fm, int maxWidth) {
        int count = 1;
        while (count < word.length() && fm.stringWidth(word.substring(0, count + 1)) <= maxWidth) {
            count++;
        }
        return count;
    }
}
