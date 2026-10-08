package org.bingoscape.board;

import net.runelite.client.util.ColorUtil;
import org.bingoscape.models.Goal;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmission;
import org.bingoscape.ui.BingoTheme;
import org.bingoscape.ui.TileStatusStyle;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds RuneLite tooltip text (color tags, {@code </br>} line breaks) for board tiles.
 */
final class BoardTooltipBuilder {
    static final Color GOLD = BingoTheme.GOLD;
    private static final String BR = "</br>";
    private static final int DESCRIPTION_LINE_CHARS = 45;
    private static final int MAX_GOALS = 8;

    private BoardTooltipBuilder() {
    }

    static String build(Tile tile) {
        if (tile.isHidden()) {
            return "Hidden tile";
        }

        StringBuilder text = new StringBuilder();
        text.append(ColorUtil.wrapWithColorTag(sanitize(tile.getTitle()) + " (" + tile.getWeight() + " XP)", GOLD));

        if (tile.getDescription() != null && !tile.getDescription().trim().isEmpty()) {
            for (String line : wrapChars(sanitize(tile.getDescription()), DESCRIPTION_LINE_CHARS)) {
                text.append(BR).append(line);
            }
        }

        TileSubmission submission = tile.getSubmission();
        if (submission != null && submission.getStatus() != null) {
            text.append(BR).append("Status: ")
                    .append("<col=").append(TileStatusStyle.hex(submission.getStatus()).substring(1)).append('>')
                    .append(TileStatusStyle.displayText(submission.getStatus()))
                    .append("</col>");

            if (submission.getSubmissionCount() > 0) {
                text.append(BR).append("Submissions: ").append(submission.getSubmissionCount());
            }

            if (submission.getLastUpdated() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                text.append(BR).append("Last updated: ").append(dateFormat.format(submission.getLastUpdated()));
            }
        }

        List<Goal> goals = tile.getGoals();
        if (goals != null && !goals.isEmpty()) {
            text.append(BR).append("Goals:");
            for (int i = 0; i < goals.size() && i < MAX_GOALS; i++) {
                Goal goal = goals.get(i);
                text.append(BR).append("- ").append(sanitize(goal.getDescription())).append(": ").append(goal.getTargetValue());
            }
            if (goals.size() > MAX_GOALS) {
                text.append(BR).append("+").append(goals.size() - MAX_GOALS).append(" more");
            }
        }

        return text.toString();
    }

    /**
     * The tooltip renderer treats anything in angle brackets as a tag, so user text must not contain them.
     */
    static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('<', '(').replace('>', ')').replaceAll("\\s+", " ").trim();
    }

    private static List<String> wrapChars(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > maxChars) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
