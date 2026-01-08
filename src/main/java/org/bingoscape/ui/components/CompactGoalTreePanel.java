package org.bingoscape.ui.components;

import net.runelite.client.game.ItemManager;
import org.bingoscape.models.GoalTreeNode;
import org.bingoscape.ui.StyleConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Compact hierarchical goal tree visualization for the hover card.
 * Displays goals with indentation, item images, progress bars, and AND/OR badges.
 * Matches the design from the Bingoscape web app.
 */
public class CompactGoalTreePanel extends JPanel {

    private static final int INDENT_PER_LEVEL = 16; // Pixels to indent per hierarchy level

    private final List<GoalTreeNode> goalTree;
    private final ItemManager itemManager;

    public CompactGoalTreePanel(List<GoalTreeNode> goalTree, ItemManager itemManager) {
        this.goalTree = goalTree;
        this.itemManager = itemManager;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildTree();
    }

    private void buildTree() {
        if (goalTree == null || goalTree.isEmpty()) {
            JLabel noGoalsLabel = new JLabel("No goals defined");
            noGoalsLabel.setFont(StyleConstants.FONT_SMALL);
            noGoalsLabel.setForeground(StyleConstants.MUTED_FOREGROUND);
            noGoalsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(noGoalsLabel);
            return;
        }

        // Add header
        JLabel headerLabel = new JLabel("Goals:");
        headerLabel.setFont(StyleConstants.FONT_BADGE_SMALL);
        headerLabel.setForeground(StyleConstants.FOREGROUND);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(headerLabel);

        // Render each root node
        for (int i = 0; i < goalTree.size(); i++) {
            boolean isLast = (i == goalTree.size() - 1);
            renderNode(goalTree.get(i), 0, "", isLast);
        }

        // Force layout calculation
        revalidate();
    }

    private void renderNode(GoalTreeNode node, int depth, String prefix, boolean isLast) {
        if (node == null) return;

        if (node.isGroup()) {
            renderGroup(node, depth, prefix, isLast);
        } else if (node.isGoal()) {
            renderGoal(node, depth, prefix, isLast);
        }
    }

    private void renderGroup(GoalTreeNode group, int depth, String prefix, boolean isLast) {
        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
        groupPanel.setOpaque(false);
        // Apply left indentation based on depth for visual hierarchy
        int leftIndent = depth * INDENT_PER_LEVEL;
        groupPanel.setBorder(BorderFactory.createEmptyBorder(2, leftIndent, 2, 0));
        groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Group header row with BorderLayout (similar to goal layout)
        JPanel headerRow = new JPanel(new BorderLayout(StyleConstants.GAP_SMALL, 0));
        headerRow.setOpaque(false);
        headerRow.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Limit group header height

        // Left panel: tree connector + AND/OR badge
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        // Tree connector removed for cleaner look

        String operator = group.getLogicalOperator() != null ? group.getLogicalOperator() : "AND";
        JLabel operatorBadge = createOperatorBadge(operator);
        leftPanel.add(operatorBadge);

        headerRow.add(leftPanel, BorderLayout.WEST);

        // Center panel: Group name and progress bar
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Group name (if available)
        if (group.getName() != null && !group.getName().isEmpty()) {
            JLabel nameLabel = new JLabel(group.getName());
            nameLabel.setFont(StyleConstants.FONT_TINY);
            nameLabel.setForeground(StyleConstants.FOREGROUND);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(nameLabel);
        } else {
            // Add placeholder label if no name
            JLabel nameLabel = new JLabel("Group");
            nameLabel.setFont(StyleConstants.FONT_TINY);
            nameLabel.setForeground(StyleConstants.MUTED_FOREGROUND);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(nameLabel);
        }

        // Progress bar
        if (group.getProgress() != null) {
            int completed = group.getProgress().getCompletedCount();
            int total = group.getProgress().getTotalCount();

            MiniProgressBar progressBar = new MiniProgressBar(
                completed,
                total,
                group.getProgress().isComplete(),
                80  // Width
            );
            progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(Box.createVerticalStrut(2));
            centerPanel.add(progressBar);
        }

        headerRow.add(centerPanel, BorderLayout.CENTER);

        // Right panel: Progress text
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        if (group.getProgress() != null) {
            int completed = group.getProgress().getCompletedCount();
            int total = group.getProgress().getTotalCount();

            String progressText = completed + "/" + total;
            JLabel progressLabel = new JLabel(progressText);
            progressLabel.setFont(StyleConstants.FONT_SMALL);

            // Color based on completion status
            Color progressColor = group.getProgress().isComplete() ?
                StyleConstants.PROGRESS_COMPLETE :
                (completed > 0 ? StyleConstants.PROGRESS_PARTIAL : StyleConstants.MUTED_FOREGROUND);
            progressLabel.setForeground(progressColor);
            rightPanel.add(progressLabel);
        }

        headerRow.add(rightPanel, BorderLayout.EAST);

        groupPanel.add(headerRow);

        add(groupPanel);

        // Render children
        if (group.getChildren() != null) {
            for (int i = 0; i < group.getChildren().size(); i++) {
                boolean isLastChild = (i == group.getChildren().size() - 1);
                renderNode(group.getChildren().get(i), depth + 1, "", isLastChild);
            }
        }
    }

    private void renderGoal(GoalTreeNode goal, int depth, String prefix, boolean isLast) {
        JPanel goalRow = new JPanel(new BorderLayout(StyleConstants.GAP_SMALL, 0));
        goalRow.setOpaque(false);
        // Apply left indentation based on depth for visual hierarchy
        int leftIndent = depth * INDENT_PER_LEVEL;
        goalRow.setBorder(BorderFactory.createEmptyBorder(2, leftIndent, 2, 0));
        goalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        goalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Limit individual goal height

        // Left panel: tree connector + item image
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        // Tree connector removed for cleaner look

        if (goal.isItemGoal() && goal.getItemGoal() != null) {
            // Load item image from ItemManager
            BufferedImage itemImage = itemManager.getImage(goal.getItemGoal().getItemId());
            if (itemImage != null) {
                // Scale to 16x16
                Image scaledImage = itemImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                leftPanel.add(imageLabel);
            }
        }

        goalRow.add(leftPanel, BorderLayout.WEST);

        // Center panel: Goal name and progress bar
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Goal name
        String displayText = getGoalDisplayText(goal);
        JLabel nameLabel = new JLabel(displayText);
        nameLabel.setFont(StyleConstants.FONT_TINY);
        nameLabel.setForeground(StyleConstants.FOREGROUND);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(nameLabel);

        // Progress bar
        if (goal.getProgress() != null && goal.getTargetValue() != null) {
            MiniProgressBar progressBar = new MiniProgressBar(
                goal.getProgress().getCompletedCount(),
                goal.getTargetValue(),
                goal.getProgress().isComplete(),
                80  // Width
            );
            progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(Box.createVerticalStrut(2));
            centerPanel.add(progressBar);
        }

        goalRow.add(centerPanel, BorderLayout.CENTER);

        // Right panel: Progress text (current/target)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        String progressText = getProgressText(goal);
        JLabel progressLabel = new JLabel(progressText);
        progressLabel.setFont(StyleConstants.FONT_SMALL);
        progressLabel.setForeground(getCompletionColor(goal));
        rightPanel.add(progressLabel);

        goalRow.add(rightPanel, BorderLayout.EAST);

        add(goalRow);
    }

    /**
     * Get progress text showing current/target values
     */
    private String getProgressText(GoalTreeNode goal) {
        if (goal.getProgress() == null || goal.getTargetValue() == null) {
            return "0/1";
        }

        int current = goal.getProgress().getCompletedCount();
        int target = goal.getTargetValue();

        return current + "/" + target;
    }

    private Color getCompletionColor(GoalTreeNode goal) {
        if (goal.getProgress() == null) {
            return StyleConstants.MUTED_FOREGROUND;
        }

        if (goal.getProgress().isComplete()) {
            return StyleConstants.GREEN_500;
        }

        if (goal.getProgress().getCompletedCount() > 0) {
            return StyleConstants.AMBER_500;
        }

        return StyleConstants.MUTED_FOREGROUND;
    }

    private String getGoalDisplayText(GoalTreeNode goal) {
        if (goal.isItemGoal() && goal.getItemGoal() != null) {
            String baseName = goal.getItemGoal().getBaseName();
            String variant = goal.getItemGoal().getExactVariant();
            if (variant != null && !variant.isEmpty()) {
                return baseName + " (" + variant + ")";
            }
            return baseName;
        }

        return goal.getDescription() != null ? goal.getDescription() : "Unknown goal";
    }

    private JLabel createOperatorBadge(String operator) {
        JLabel badge = new JLabel(operator) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background color based on operator
                Color bgColor = "AND".equals(operator) ?
                    StyleConstants.SECONDARY_BG :
                    StyleConstants.withAlpha(StyleConstants.BLUE_500, 50);

                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2d.dispose();

                super.paintComponent(g);
            }
        };

        badge.setFont(StyleConstants.FONT_TINY);
        badge.setForeground(StyleConstants.FOREGROUND);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return badge;
    }

    private JLabel createItemBadge() {
        JLabel badge = new JLabel(StyleConstants.ICON_PACKAGE) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(StyleConstants.withAlpha(StyleConstants.AMBER_500, 50));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2d.dispose();

                super.paintComponent(g);
            }
        };

        badge.setFont(new Font("Arial", Font.PLAIN, 10));
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
        return badge;
    }

    /**
     * Mini progress bar component for compact goal tree
     */
    private static class MiniProgressBar extends JPanel {
        private final int current;
        private final int target;
        private final boolean isComplete;

        public MiniProgressBar(int current, int target, boolean isComplete, int width) {
            this.current = current;
            this.target = target;
            this.isComplete = isComplete;
            setOpaque(false);
            setPreferredSize(new Dimension(width, 6));  // Just the bar height, no text
            setMinimumSize(new Dimension(width, 6));
            setMaximumSize(new Dimension(width, 6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background
            g2d.setColor(StyleConstants.PROGRESS_BG);
            g2d.fillRoundRect(0, 0, width, height, 3, 3);

            // Progress fill
            if (target > 0) {
                double percentage = (double) current / target;
                int fillWidth = (int) (width * Math.min(percentage, 1.0));

                Color fillColor = isComplete ?
                    StyleConstants.PROGRESS_COMPLETE :
                    (current > 0 ? StyleConstants.PROGRESS_PARTIAL : StyleConstants.PROGRESS_NONE);

                g2d.setColor(fillColor);
                g2d.fillRoundRect(0, 0, fillWidth, height, 3, 3);
            }

            g2d.dispose();
        }
    }
}
