package org.bingoscape.board;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Goal;
import org.bingoscape.models.GoalTreeNode;
import org.bingoscape.models.ItemGoal;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.services.TileImageCache;
import org.bingoscape.ui.TileStatusStyle;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Paints the tile details view inside the board's grid area: title, XP, description, goal tree with
 * progress, submission status, header image and the action buttons.
 */
final class TileDetailsView {
    private static final int INSET = 8;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 6;
    private static final int IMAGE_SIZE = 150;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int INDENT_PER_LEVEL = 16;
    private static final int ITEM_ICON_SIZE = 16;
    private static final int PROGRESS_BAR_WIDTH = 80;
    private static final int PROGRESS_BAR_HEIGHT = 6;
    private static final Color OR_BADGE_BG = new Color(59, 130, 246, 50);
    private static final Color AND_BADGE_BG = new Color(55, 65, 81, 180);

    /**
     * Click targets and scroll limits produced by a paint, in overlay coordinates.
     */
    static final class DetailsHit {
        final Rectangle primaryButton;
        final Rectangle closeButton;
        final Rectangle scrollArea;
        final boolean primaryEnabled;
        final int maxScroll;

        DetailsHit(Rectangle primaryButton, Rectangle closeButton, Rectangle scrollArea, boolean primaryEnabled, int maxScroll) {
            this.primaryButton = primaryButton;
            this.closeButton = closeButton;
            this.scrollArea = scrollArea;
            this.primaryEnabled = primaryEnabled;
            this.maxScroll = maxScroll;
        }
    }

    private TileDetailsView() {
    }

    static String primaryLabel(Tile tile, Bingo bingo) {
        if (bingo.isLocked()) {
            return "Submissions locked";
        }
        if (TileStatusStyle.statusOf(tile) == TileSubmissionType.ACCEPTED) {
            return "Already Completed";
        }
        return "Take & Review Screenshot";
    }

    static boolean primaryEnabled(Tile tile, Bingo bingo) {
        return !bingo.isLocked() && TileStatusStyle.statusOf(tile) != TileSubmissionType.ACCEPTED;
    }

    /**
     * @param images     image cache, or null when tile images are disabled
     * @param itemImages item id to item sprite (RuneLite ItemManager)
     */
    static DetailsHit paint(Graphics2D g, Rectangle area, Tile tile, Bingo bingo, TileImageCache images,
                            IntFunction<BufferedImage> itemImages, int scrollPx, Point mouse) {
        g.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g.fillRect(area.x, area.y, area.width, area.height);
        g.setColor(ColorScheme.BORDER_COLOR);
        g.drawRect(area.x, area.y, area.width - 1, area.height - 1);

        Rectangle inner = new Rectangle(area.x + INSET, area.y + INSET, area.width - 2 * INSET, area.height - 2 * INSET);

        // Footer buttons
        g.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics buttonFm = g.getFontMetrics();
        String primaryText = primaryLabel(tile, bingo);
        boolean enabled = primaryEnabled(tile, bingo);
        int buttonY = inner.y + inner.height - BUTTON_HEIGHT;
        int primaryWidth = buttonFm.stringWidth(primaryText) + 16;
        Rectangle primary = new Rectangle(inner.x + inner.width - primaryWidth, buttonY, primaryWidth, BUTTON_HEIGHT);
        int closeWidth = buttonFm.stringWidth("Close") + 16;
        Rectangle close = new Rectangle(primary.x - BUTTON_GAP - closeWidth, buttonY, closeWidth, BUTTON_HEIGHT);

        BoardDraw.button(g, close, contains(close, mouse), true);
        BoardDraw.centeredText(g, "Close", close, Color.WHITE);
        BoardDraw.button(g, primary, contains(primary, mouse), enabled);
        BoardDraw.centeredText(g, primaryText, primary, enabled ? Color.WHITE : Color.GRAY);

        // Header image on the right (not scrolled)
        int contentBottom = buttonY - BUTTON_GAP;
        boolean hasImage = images != null && tile.getHeaderImage() != null && !tile.getHeaderImage().trim().isEmpty();
        int imageSize = hasImage ? Math.min(IMAGE_SIZE, Math.min(inner.width / 3, contentBottom - inner.y)) : 0;
        if (imageSize > 0) {
            Rectangle imageBox = new Rectangle(inner.x + inner.width - imageSize, inner.y, imageSize, imageSize);
            paintImage(g, imageBox, tile, images);
        }

        // Scrollable text column on the left
        int columnWidth = inner.width - (imageSize > 0 ? imageSize + INSET : 0);
        Rectangle column = new Rectangle(inner.x, inner.y, Math.max(0, columnWidth), Math.max(0, contentBottom - inner.y));
        int textWidth = column.width - SCROLLBAR_WIDTH - 4;

        Graphics2D cg = (Graphics2D) g.create();
        int contentHeight;
        try {
            cg.clipRect(column.x, column.y, column.width, column.height);
            cg.translate(0, -scrollPx);
            contentHeight = paintContent(cg, column.x, column.y, textWidth, tile, itemImages) - column.y;
        } finally {
            cg.dispose();
        }

        int maxScroll = Math.max(0, contentHeight - column.height);
        if (maxScroll > 0) {
            paintScrollbar(g, column, contentHeight, Math.min(scrollPx, maxScroll));
        }

        return new DetailsHit(primary, close, column, enabled, maxScroll);
    }

    /**
     * Paints the text content starting at {@code y} and returns the y coordinate below the last line.
     */
    private static int paintContent(Graphics2D g, int x, int y, int width, Tile tile, IntFunction<BufferedImage> itemImages) {
        // Title
        g.setFont(FontManager.getRunescapeBoldFont());
        y = paintWrapped(g, tile.getTitle(), x, y, width, Color.WHITE);

        // XP
        g.setFont(FontManager.getRunescapeFont());
        y = paintWrapped(g, "XP: " + tile.getWeight(), x, y + 2, width, Color.LIGHT_GRAY) + 6;

        // Description
        if (tile.getDescription() != null && !tile.getDescription().trim().isEmpty()) {
            y = paintWrapped(g, tile.getDescription(), x, y, width, Color.WHITE) + 6;
        }

        y = paintGoals(g, tile, x, y, width, itemImages);

        // Submission status
        TileSubmissionType status = TileStatusStyle.statusOf(tile);
        if (TileStatusStyle.isSubmitted(status)) {
            g.setFont(FontManager.getRunescapeFont());
            y = paintWrapped(g, "Status: " + TileStatusStyle.displayText(status), x, y, width, TileStatusStyle.accent(status));
            if (tile.getSubmission().getSubmissionCount() > 0) {
                y = paintWrapped(g, "Submissions: " + tile.getSubmission().getSubmissionCount(), x, y + 2, width, Color.LIGHT_GRAY);
            }
        }

        return y;
    }

    /**
     * Paints the "Goals:" heading and the goal tree (or flat goal list) and returns the y below it.
     * Returns {@code y} unchanged when the tile has no goals.
     */
    static int paintGoals(Graphics2D g, Tile tile, int x, int y, int width, IntFunction<BufferedImage> itemImages) {
        // Prefer the hierarchical tree, fall back to the flat goal list
        List<GoalTreeNode> goalTree = tile.getGoalTree();
        List<Goal> goals = tile.getGoals();
        boolean hasTree = goalTree != null && !goalTree.isEmpty();
        if (!hasTree && (goals == null || goals.isEmpty())) {
            return y;
        }

        g.setFont(FontManager.getRunescapeBoldFont());
        y = paintWrapped(g, "Goals:", x, y, width, Color.WHITE) + 2;
        g.setFont(FontManager.getRunescapeSmallFont());
        if (hasTree) {
            for (GoalTreeNode node : goalTree) {
                y = paintNode(g, node, 0, x, y, width, itemImages);
            }
        } else {
            for (Goal goal : goals) {
                y = paintFlatGoal(g, goal, x, y, width);
            }
        }
        return y + 6;
    }

    private static int paintNode(Graphics2D g, GoalTreeNode node, int depth, int x, int y, int width,
                                 IntFunction<BufferedImage> itemImages) {
        if (node == null) {
            return y;
        }

        int indent = Math.min(depth * INDENT_PER_LEVEL, width / 3);
        if (node.isGroup()) {
            String operator = node.getLogicalOperator() != null ? node.getLogicalOperator() : "AND";
            boolean hasName = node.getName() != null && !node.getName().isEmpty();
            String name = hasName ? node.getName() : "Group";
            boolean hasProgress = node.getProgress() != null;
            int completed = hasProgress ? node.getProgress().getCompletedCount() : 0;
            int total = hasProgress ? node.getProgress().getTotalCount() : 0;
            boolean complete = hasProgress && node.getProgress().isComplete();

            y = paintGoalRow(g, x + indent, y, width - indent, operator, null, name, hasName ? BoardDraw.FOREGROUND : BoardDraw.MUTED,
                    hasProgress ? completed + "/" + total : null, completed, total, complete, hasProgress);

            if (node.getChildren() != null) {
                for (GoalTreeNode child : node.getChildren()) {
                    y = paintNode(g, child, depth + 1, x, y, width, itemImages);
                }
            }
            return y;
        }

        if (node.isGoal()) {
            BufferedImage icon = null;
            String name = node.getDescription() != null ? node.getDescription() : "Unknown goal";
            if (node.isItemGoal()) {
                ItemGoal item = node.getItemGoal();
                if (item.getItemId() != null) {
                    icon = itemImages.apply(item.getItemId());
                }
                if (item.getBaseName() != null) {
                    name = item.getExactVariant() != null && !item.getExactVariant().isEmpty()
                            ? item.getBaseName() + " (" + item.getExactVariant() + ")"
                            : item.getBaseName();
                }
            }

            boolean hasProgress = node.getProgress() != null && node.getTargetValue() != null;
            int current = node.getProgress() != null ? node.getProgress().getCompletedCount() : 0;
            int target = node.getTargetValue() != null ? node.getTargetValue() : 1;
            boolean complete = node.getProgress() != null && node.getProgress().isComplete();
            String progressText = hasProgress ? current + "/" + target : "0/1";

            return paintGoalRow(g, x + indent, y, width - indent, null, icon, name, BoardDraw.FOREGROUND,
                    progressText, current, target, complete, node.getProgress() != null);
        }

        return y;
    }

    private static int paintFlatGoal(Graphics2D g, Goal goal, int x, int y, int width) {
        String name = goal.getDescription() != null ? goal.getDescription() : "Unknown goal";
        boolean hasProgress = goal.getProgress() != null;
        int current = hasProgress ? goal.getProgress().getApprovedProgress() : 0;
        boolean complete = hasProgress && goal.getProgress().isCompleted();
        return paintGoalRow(g, x, y, width, null, null, name, BoardDraw.FOREGROUND,
                current + "/" + goal.getTargetValue(), current, goal.getTargetValue(), complete, hasProgress);
    }

    /**
     * One row of the compact goal tree: optional AND/OR badge or item icon, name with a progress bar
     * underneath, and "current/target" on the right.
     */
    private static int paintGoalRow(Graphics2D g, int x, int y, int width, String operator, BufferedImage icon,
                                    String name, Color nameColor, String progressText, int current, int target,
                                    boolean complete, boolean hasProgress) {
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        int top = y + 2;
        int baseline = top + fm.getAscent();
        int nameX = x;

        if (operator != null) {
            int badgeWidth = fm.stringWidth(operator) + 8;
            Graphics2D bg = (Graphics2D) g.create();
            try {
                bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                bg.setColor("OR".equalsIgnoreCase(operator) ? OR_BADGE_BG : AND_BADGE_BG);
                bg.fillRoundRect(x, top, badgeWidth, lineHeight, 4, 4);
            } finally {
                bg.dispose();
            }
            BoardDraw.text(g, operator, x + 4, baseline, BoardDraw.FOREGROUND);
            nameX += badgeWidth + 4;
        } else if (icon != null) {
            g.drawImage(icon, x, top, ITEM_ICON_SIZE, ITEM_ICON_SIZE, null);
            nameX += ITEM_ICON_SIZE + 4;
        }

        int right = x + width;
        if (progressText != null) {
            int progressWidth = fm.stringWidth(progressText);
            BoardDraw.text(g, progressText, right - progressWidth, baseline,
                    BoardDraw.progressTextColor(current, complete, hasProgress));
            right -= progressWidth + 6;
        }

        BoardDraw.text(g, TextWrap.ellipsize(name, fm, Math.max(0, right - nameX)), nameX, baseline, nameColor);

        int bottom = top + Math.max(lineHeight, icon != null ? ITEM_ICON_SIZE : 0);
        if (hasProgress) {
            int barWidth = Math.max(0, Math.min(PROGRESS_BAR_WIDTH, right - nameX));
            BoardDraw.progressBar(g, nameX, bottom + 2, barWidth, PROGRESS_BAR_HEIGHT, current, target, complete);
            bottom += 2 + PROGRESS_BAR_HEIGHT;
        }
        return bottom + 2;
    }

    private static int paintWrapped(Graphics2D g, String text, int x, int y, int width, Color color) {
        Font font = TextWrap.displayable(g.getFont(), text);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        for (String line : TextWrap.wrap(text, fm, width, Integer.MAX_VALUE)) {
            BoardDraw.text(g, line, x, y + fm.getAscent(), color);
            y += fm.getHeight();
        }
        return y;
    }

    private static void paintImage(Graphics2D g, Rectangle box, Tile tile, TileImageCache images) {
        BufferedImage image = images.getScaled(tile.getHeaderImage(), box.width, box.height);
        if (image == null) {
            image = images.getSource(tile.getHeaderImage());
        }

        if (image != null) {
            Rectangle target = BoardDraw.fitted(image.getWidth(), image.getHeight(), box);
            Graphics2D ig = (Graphics2D) g.create();
            try {
                ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                ig.drawImage(image, target.x, target.y, target.width, target.height, null);
            } finally {
                ig.dispose();
            }
            return;
        }

        if (!images.hasFailed(tile.getHeaderImage())) {
            g.setFont(FontManager.getRunescapeSmallFont());
            BoardDraw.centeredText(g, "Loading...", box, Color.LIGHT_GRAY);
        }
    }

    private static void paintScrollbar(Graphics2D g, Rectangle column, int contentHeight, int scrollPx) {
        int trackX = column.x + column.width - SCROLLBAR_WIDTH;
        g.setColor(ColorScheme.DARK_GRAY_COLOR);
        g.fillRect(trackX, column.y, SCROLLBAR_WIDTH, column.height);

        int thumbHeight = Math.max(16, column.height * column.height / contentHeight);
        int maxScroll = contentHeight - column.height;
        int thumbY = column.y + (int) ((long) (column.height - thumbHeight) * scrollPx / Math.max(1, maxScroll));
        g.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
        g.fillRect(trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight);
    }

    private static boolean contains(Rectangle r, Point p) {
        return p != null && r.contains(p);
    }
}
