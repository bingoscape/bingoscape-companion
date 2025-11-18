package org.bingoscape.ui;

import net.runelite.client.game.ItemManager;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.components.CompactGoalTreePanel;
import org.bingoscape.ui.components.StatusBadge;
import org.bingoscape.ui.components.XPBadge;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

/**
 * Builder class for constructing tile hover card content panels.
 * Matches the design and layout from the Bingoscape web app.
 */
public class TileHoverCardBuilder {

    private final Tile tile;
    private final Bingo bingo;
    private final ItemManager itemManager;

    public TileHoverCardBuilder(Tile tile, Bingo bingo, ItemManager itemManager) {
        this.tile = tile;
        this.bingo = bingo;
        this.itemManager = itemManager;
    }

    /**
     * Build the complete hover card content panel
     */
    public JPanel build() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(StyleConstants.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
            StyleConstants.PADDING,
            StyleConstants.PADDING,
            StyleConstants.PADDING,
            StyleConstants.PADDING
        ));
        // Set max width, let height be calculated by layout
        mainPanel.setMaximumSize(new Dimension(StyleConstants.HOVER_CARD_WIDTH, Integer.MAX_VALUE));

        // Add sections with spacing
        addSection(mainPanel, buildHeader());

        if (tile.getGoalTree() != null && !tile.getGoalTree().isEmpty()) {
            addSection(mainPanel, buildGoalTree());
        }

        if (tile.isHidden()) {
            addSection(mainPanel, buildHiddenIndicator());
        }

        // Force layout calculation before checking size
        mainPanel.doLayout();
        mainPanel.validate();

        // Force width to be exactly HOVER_CARD_WIDTH
        Dimension preferredSize = mainPanel.getPreferredSize();
        mainPanel.setPreferredSize(new Dimension(StyleConstants.HOVER_CARD_WIDTH, preferredSize.height));

        // If the content is taller than 300px, wrap it in a scroll pane
        if (preferredSize.height > 300) {
            JScrollPane scrollPane = new JScrollPane(mainPanel);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setPreferredSize(new Dimension(StyleConstants.HOVER_CARD_WIDTH, 300));
            scrollPane.setMaximumSize(new Dimension(StyleConstants.HOVER_CARD_WIDTH, 300));

            // Wrap scroll pane in a panel to return
            JPanel scrollContainer = new JPanel(new BorderLayout());
            scrollContainer.setOpaque(false);
            scrollContainer.add(scrollPane, BorderLayout.CENTER);
            return scrollContainer;
        }

        return mainPanel;
    }

    /**
     * Add a section to the main panel with appropriate spacing
     */
    private void addSection(JPanel mainPanel, JComponent section) {
        if (section != null) {
            // Add spacing before this section (except for first section)
            if (mainPanel.getComponentCount() > 0) {
                mainPanel.add(Box.createVerticalStrut(StyleConstants.GAP_SECTIONS));
            }

            section.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainPanel.add(section);
        }
    }

    /**
     * Build header section: Title + XP Badge
     */
    private JPanel buildHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout(StyleConstants.GAP_ITEMS, 0));
        headerPanel.setOpaque(false);

        // Title label (left side, takes available space)
        JLabel titleLabel = new JLabel("<html>" + escapeHtml(tile.getTitle()) + "</html>");
        titleLabel.setFont(StyleConstants.FONT_TITLE);
        titleLabel.setForeground(StyleConstants.FOREGROUND);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // XP label (right side) - display like tile does: "1 XP"
        JLabel xpLabel = new JLabel(tile.getWeight() + " XP");
        xpLabel.setFont(StyleConstants.FONT_BADGE_SMALL);
        xpLabel.setForeground(StyleConstants.AMBER_500);
        JPanel xpContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        xpContainer.setOpaque(false);
        xpContainer.add(xpLabel);
        headerPanel.add(xpContainer, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Build status badge section
     */
    private JPanel buildStatusBadge() {
        String status = getSubmissionStatus();
        if (status == null) {
            return null;
        }

        StatusBadge statusBadge = new StatusBadge(status);
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        container.setOpaque(false);
        container.add(statusBadge);
        return container;
    }

    /**
     * Build description section with HTML rendering
     */
    private JPanel buildDescription() {
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setOpaque(false);

        // Truncate description if too long (max 150 characters)
        String description = tile.getDescription();
        if (description.length() > 150) {
            description = description.substring(0, 147) + "...";
        }

        // Use JEditorPane for better HTML/text rendering
        JEditorPane descPane = new JEditorPane();
        descPane.setContentType("text/html");
        descPane.setEditable(false);
        descPane.setOpaque(false);
        descPane.setFont(StyleConstants.FONT_TINY);
        descPane.setForeground(StyleConstants.MUTED_FOREGROUND);

        // Set HTML content with styling - same size as goal titles (10px)
        String styledHtml = String.format(
            "<html><body style='font-family: Arial; font-size: 10px; color: rgb(%d,%d,%d);'>%s</body></html>",
            StyleConstants.MUTED_FOREGROUND.getRed(),
            StyleConstants.MUTED_FOREGROUND.getGreen(),
            StyleConstants.MUTED_FOREGROUND.getBlue(),
            escapeHtml(description)
        );
        descPane.setText(styledHtml);

        // Make links non-clickable for hover card
        descPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        descPanel.add(descPane, BorderLayout.CENTER);
        return descPanel;
    }

    /**
     * Build goal tree section at full height
     */
    private JPanel buildGoalTree() {
        CompactGoalTreePanel goalTreePanel = new CompactGoalTreePanel(tile.getGoalTree(), itemManager);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(goalTreePanel, BorderLayout.CENTER);

        return container;
    }

    /**
     * Build hidden indicator section
     */
    private JPanel buildHiddenIndicator() {
        JPanel hiddenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, StyleConstants.GAP_SMALL, 0));
        hiddenPanel.setBackground(StyleConstants.SECONDARY_BG);
        hiddenPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(StyleConstants.SECONDARY, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        // Eye-off icon
        JLabel iconLabel = new JLabel(StyleConstants.ICON_EYE_OFF);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        hiddenPanel.add(iconLabel);

        // "Hidden tile" text
        JLabel textLabel = new JLabel("Hidden tile");
        textLabel.setFont(StyleConstants.FONT_SMALL);
        textLabel.setForeground(StyleConstants.FOREGROUND);
        hiddenPanel.add(textLabel);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(hiddenPanel, BorderLayout.WEST);

        return container;
    }

    /**
     * Check if tile has a submission
     */
    private boolean hasSubmission() {
        return tile.getSubmission() != null &&
               tile.getSubmission().getStatus() != null &&
               tile.getSubmission().getStatus() != TileSubmissionType.NOT_SUBMITTED;
    }

    /**
     * Get the submission status as a string for the status badge
     */
    private String getSubmissionStatus() {
        if (tile.getSubmission() == null || tile.getSubmission().getStatus() == null) {
            return null;
        }

        TileSubmissionType status = tile.getSubmission().getStatus();
        switch (status) {
            case ACCEPTED:
                return "approved";
            case PENDING:
                return "pending";
            case REQUIRES_INTERACTION:
                return "needs_review";
            case DECLINED:
                return "declined";
            case NOT_SUBMITTED:
            default:
                return null;
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br>");
    }
}
