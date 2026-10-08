package org.bingoscape;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.EventData;
import org.bingoscape.models.Role;
import org.bingoscape.models.TeamMember;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.ui.BingoTheme;
import org.bingoscape.ui.TileStatusStyle;
import org.bingoscape.ui.components.CardPanel;
import org.bingoscape.ui.components.FlatButton;
import org.bingoscape.ui.components.Pill;
import org.bingoscape.ui.components.ProgressBar;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Side panel: event and board selection, event details and a board progress summary.
 * Styled to match the in-game board overlay (rounded cards, RuneScape fonts, status colors).
 */
public class BingoScapePanel extends PluginPanel {
    private static final int BORDER_SPACING = 10;
    private static final int GAP = 6;
    private static final int TEXT_WIDTH = PluginPanel.PANEL_WIDTH - 2 * BORDER_SPACING - 24;
    private static final int TITLE_WITH_PILL_WIDTH = TEXT_WIDTH - 70;
    private static final int RELOAD_FALLBACK_MS = 10_000;
    private static final String NO_EVENTS_TEXT = "No active events found";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    private final BingoScapePlugin plugin;
    private final ScheduledExecutorService executor;

    private final JComboBox<EventData> eventSelector = new JComboBox<>();
    private final JComboBox<Bingo> bingoSelector = new JComboBox<>();
    private final FlatButton reloadEventsButton;
    private final JLabel loadingLabel = new JLabel("Loading...");
    private final JLabel noEventsLabel = new JLabel(NO_EVENTS_TEXT);
    private final CardPanel eventCard = new CardPanel();
    private final JPanel boardSection = new JPanel();
    private final CardPanel boardCard = new CardPanel();
    private final FlatButton showBingoBoardButton = new FlatButton("Show Bingo Board");
    private final FlatButton pinButton = new FlatButton("Pin");
    private final FlatButton reloadBoardButton = new FlatButton("Reload");

    // Set while the bingo selector is repopulated, so programmatic changes don't re-select bingos
    private boolean suppressBingoSelection;
    private BingoBoardWindow bingoBoardWindow;

    public BingoScapePanel(BingoScapePlugin plugin) {
        super();
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(BORDER_SPACING, BORDER_SPACING, BORDER_SPACING, BORDER_SPACING));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(content, BorderLayout.NORTH);

        reloadEventsButton = createReloadEventsButton();

        content.add(createHeader());
        content.add(sectionLabel("Event"));
        configureEventSelector();
        content.add(fullWidth(eventSelector));

        noEventsLabel.setFont(FontManager.getRunescapeSmallFont());
        noEventsLabel.setForeground(BingoTheme.MUTED);
        noEventsLabel.setBorder(new EmptyBorder(GAP, 0, 0, 0));
        noEventsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        noEventsLabel.setVisible(false);
        content.add(noEventsLabel);

        setupBoardSection();
        content.add(boardSection);

        content.add(Box.createVerticalStrut(GAP * 2));
        eventCard.setVisible(false);
        content.add(eventCard);

        boardSection.setVisible(false);
    }

    // ---------------------------------------------------------------- layout

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(GAP, 0));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("BingoScape");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(BingoTheme.GOLD);
        header.add(title, BorderLayout.WEST);

        loadingLabel.setFont(FontManager.getRunescapeSmallFont());
        loadingLabel.setForeground(BingoTheme.MUTED);
        loadingLabel.setVisible(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0));
        actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
        actions.add(loadingLabel);
        actions.add(reloadEventsButton);
        header.add(actions, BorderLayout.EAST);

        return fullWidth(header);
    }

    private void setupBoardSection() {
        boardSection.setLayout(new BoxLayout(boardSection, BoxLayout.Y_AXIS));
        boardSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        boardSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        boardSection.add(sectionLabel("Board"));
        configureBingoSelector();
        boardSection.add(fullWidth(bingoSelector));
        boardSection.add(Box.createVerticalStrut(GAP));

        boardCard.setVisible(false);
        boardSection.add(boardCard);
        boardSection.add(Box.createVerticalStrut(GAP));

        showBingoBoardButton.setFont(FontManager.getRunescapeBoldFont());
        showBingoBoardButton.setBorder(new EmptyBorder(7, 10, 7, 10));
        showBingoBoardButton.addActionListener(e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (selectedBingo == null) {
                return;
            }

            if (plugin.getConfig().boardDisplayMode() == BoardDisplayMode.WINDOW) {
                openBoardWindow(selectedBingo);
            } else {
                plugin.toggleBoard();
            }
        });
        boardSection.add(fullWidth(showBingoBoardButton));
        boardSection.add(Box.createVerticalStrut(GAP));

        pinButton.setToolTipText("Load this board automatically on startup");
        pinButton.addActionListener(e -> plugin.toggleBoardPin());

        reloadBoardButton.setToolTipText("Reload board progress");
        reloadBoardButton.addActionListener(e -> {
            reloadBoardButton.setEnabled(false);
            plugin.reloadBoard();
            reenableLater(reloadBoardButton);
        });

        JPanel secondary = new JPanel(new GridLayout(1, 2, GAP, 0));
        secondary.setBackground(ColorScheme.DARK_GRAY_COLOR);
        secondary.add(pinButton);
        secondary.add(reloadBoardButton);
        boardSection.add(fullWidth(secondary));
    }

    private FlatButton createReloadEventsButton() {
        BufferedImage icon = ImageUtil.loadImageResource(BingoScapePlugin.class, "/refresh_icon.png");
        FlatButton button = new FlatButton("", new ImageIcon(icon));
        button.setBorder(new EmptyBorder(4, 4, 4, 4));
        button.setPreferredSize(new Dimension(24, 24));
        button.setToolTipText("Reload events");
        button.addActionListener(e -> {
            button.setEnabled(false);
            loadingLabel.setVisible(true);
            plugin.fetchActiveEvents();
            // fetchActiveEvents only calls back on success; don't leave the button disabled on errors
            Timer fallback = new Timer(RELOAD_FALLBACK_MS, evt -> {
                button.setEnabled(true);
                loadingLabel.setVisible(false);
            });
            fallback.setRepeats(false);
            fallback.start();
        });
        return button;
    }

    private void configureEventSelector() {
        eventSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EventData) {
                    setText(((EventData) value).getTitle());
                }
                styleCell(this, isSelected);
                return this;
            }
        });
        eventSelector.addActionListener(e -> {
            EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
            if (selectedEvent != null) {
                executor.submit(() -> plugin.setEventDetails(selectedEvent));
            }
        });
        eventSelector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        eventSelector.setForeground(Color.WHITE);
        eventSelector.setFocusable(false);
    }

    private void configureBingoSelector() {
        bingoSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bingo) {
                    Bingo bingo = (Bingo) value;
                    StringBuilder text = new StringBuilder("<html>").append(escape(bingo.getTitle()));
                    if (bingo.isLocked()) {
                        text.append(" <font color='").append(hex(BingoTheme.MUTED)).append("'>[Locked]</font>");
                    }
                    if (isPinned(bingo)) {
                        text.append(" <font color='").append(hex(BingoTheme.GOLD)).append("'>[Pinned]</font>");
                    }
                    setText(text.append("</html>").toString());
                }
                styleCell(this, isSelected);
                return this;
            }
        });
        bingoSelector.addActionListener(e -> {
            Bingo selectedBingo = (Bingo) bingoSelector.getSelectedItem();
            if (!suppressBingoSelection && selectedBingo != null && selectedBingo != plugin.getCurrentBingo()) {
                plugin.selectBingo(selectedBingo);
            }
        });
        bingoSelector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bingoSelector.setForeground(Color.WHITE);
        bingoSelector.setFocusable(false);
    }

    // ---------------------------------------------------------------- public API

    /**
     * Opens the legacy board window (used when the board display mode is WINDOW).
     */
    public void openBoardWindow(Bingo bingo) {
        SwingUtilities.invokeLater(() -> {
            if (bingoBoardWindow != null) {
                bingoBoardWindow.dispose();
            }

            bingoBoardWindow = new BingoBoardWindow(plugin, bingo);
            bingoBoardWindow.setVisible(true);
        });
    }

    /**
     * Stops the panel's worker thread. Called when the plugin shuts down.
     */
    public void shutdown() {
        executor.shutdownNow();
        disposeBoardWindow();
    }

    public void disposeBoardWindow() {
        SwingUtilities.invokeLater(() -> {
            if (bingoBoardWindow != null) {
                bingoBoardWindow.dispose();
                bingoBoardWindow = null;
            }
        });
    }

    /**
     * Updates the board button label to match the overlay's visibility.
     */
    public void setBoardButtonState(boolean boardVisible) {
        SwingUtilities.invokeLater(() ->
                showBingoBoardButton.setText(boardVisible ? "Hide Bingo Board" : "Show Bingo Board"));
    }

    /**
     * Refreshes pin markers after a pin change made outside the panel (e.g. from the overlay).
     */
    public void refreshPinState() {
        SwingUtilities.invokeLater(() -> {
            bingoSelector.repaint();
            updatePinButton();
        });
    }

    public void updateEventsList(List<EventData> events) {
        SwingUtilities.invokeLater(() -> {
            EventData selectedEvent = (EventData) eventSelector.getSelectedItem();
            String selectedEventId = selectedEvent != null ? selectedEvent.getId().toString() : null;
            String pinnedBingoId = plugin.getConfig().pinnedBingoId();

            DefaultComboBoxModel<EventData> model = new DefaultComboBoxModel<>();
            for (EventData event : events) {
                model.addElement(event);
                // Prefer the event that contains the pinned bingo
                if (!pinnedBingoId.isEmpty() && event.getBingos().stream()
                        .anyMatch(b -> b.getId().toString().equals(pinnedBingoId))) {
                    selectedEventId = event.getId().toString();
                }
            }
            eventSelector.setModel(model);

            if (selectedEventId != null) {
                for (int i = 0; i < model.getSize(); i++) {
                    if (model.getElementAt(i).getId().toString().equals(selectedEventId)) {
                        eventSelector.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (model.getSize() > 0) {
                eventSelector.setSelectedIndex(0);
            }

            boolean hasEvents = model.getSize() > 0;
            eventSelector.setEnabled(hasEvents);
            noEventsLabel.setVisible(!hasEvents);
            if (!hasEvents) {
                eventCard.setVisible(false);
                boardSection.setVisible(false);
            }
            reloadEventsButton.setEnabled(true);
            loadingLabel.setVisible(false);
            revalidate();
            repaint();
        });
    }

    public void updateEventDetails(EventData eventData) {
        SwingUtilities.invokeLater(() -> {
            if (eventData == null) {
                eventCard.setVisible(false);
                boardSection.setVisible(false);
            } else {
                rebuildEventCard(eventData);
                populateBingoSelector(eventData);
            }
            revalidate();
            repaint();
        });
    }

    /**
     * Called whenever the plugin selects or refreshes a bingo.
     */
    public void displayBingoBoard(Bingo bingo) {
        SwingUtilities.invokeLater(() -> {
            syncBingoSelection(bingo);
            rebuildBoardCard(bingo);
            updatePinButton();
            reloadBoardButton.setEnabled(true);

            // Refresh the legacy window if it's open
            if (bingoBoardWindow != null && bingoBoardWindow.isVisible() && bingo != null) {
                bingoBoardWindow.dispose();
                bingoBoardWindow = new BingoBoardWindow(plugin, bingo);
                bingoBoardWindow.setVisible(true);
            }

            revalidate();
            repaint();
        });
    }

    // ---------------------------------------------------------------- bingo selector

    private void populateBingoSelector(EventData eventData) {
        // Copy first: refreshes replace bingos in this list from HTTP callback threads
        List<Bingo> bingos = eventData.getBingos() == null ? new ArrayList<>() : new ArrayList<>(eventData.getBingos());
        DefaultComboBoxModel<Bingo> model = new DefaultComboBoxModel<>();
        for (Bingo bingo : bingos) {
            if (isPinned(bingo)) {
                model.insertElementAt(bingo, 0);
            } else {
                model.addElement(bingo);
            }
        }

        Bingo current = plugin.getCurrentBingo();
        Bingo picked = plugin.pickBingo(eventData);
        Bingo toSelect = picked == null ? null : findById(model, picked);

        suppressBingoSelection = true;
        try {
            bingoSelector.setModel(model);
            bingoSelector.setSelectedItem(toSelect);
        } finally {
            suppressBingoSelection = false;
        }

        boardSection.setVisible(model.getSize() > 0);
        if (toSelect != null && (current == null || !toSelect.getId().equals(current.getId()))) {
            plugin.selectBingo(toSelect);
        }
    }

    /**
     * Keeps the selector in sync with the plugin's bingo, swapping in refreshed bingo objects.
     */
    private void syncBingoSelection(Bingo bingo) {
        if (bingo == null) {
            return;
        }

        DefaultComboBoxModel<Bingo> model = (DefaultComboBoxModel<Bingo>) bingoSelector.getModel();
        suppressBingoSelection = true;
        try {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).getId().equals(bingo.getId())) {
                    if (model.getElementAt(i) != bingo) {
                        model.removeElementAt(i);
                        model.insertElementAt(bingo, i);
                    }
                    bingoSelector.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            suppressBingoSelection = false;
        }
    }

    private static Bingo findById(DefaultComboBoxModel<Bingo> model, Bingo bingo) {
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).getId().equals(bingo.getId())) {
                return model.getElementAt(i);
            }
        }
        return null;
    }

    private void updatePinButton() {
        Bingo bingo = (Bingo) bingoSelector.getSelectedItem();
        boolean pinned = bingo != null && isPinned(bingo);
        pinButton.setText(pinned ? "Unpin" : "Pin");
        pinButton.setEnabled(bingo != null);
    }

    private boolean isPinned(Bingo bingo) {
        return bingo.getId() != null && bingo.getId().toString().equals(plugin.getConfig().pinnedBingoId());
    }

    // ---------------------------------------------------------------- cards

    private void rebuildBoardCard(Bingo bingo) {
        boardCard.removeAll();
        if (bingo == null) {
            boardCard.setVisible(false);
            return;
        }

        JPanel titleRow = row();
        if (bingo.isLocked()) {
            Pill locked = new Pill();
            locked.setStatus("Locked", BingoTheme.MUTED);
            titleRow.add(pillHolder(locked), BorderLayout.EAST);
            titleRow.add(wrappedLabel(bingo.getTitle(), FontManager.getRunescapeBoldFont(), Color.WHITE, TITLE_WITH_PILL_WIDTH), BorderLayout.CENTER);
        } else {
            titleRow.add(wrappedLabel(bingo.getTitle(), FontManager.getRunescapeBoldFont(), Color.WHITE), BorderLayout.CENTER);
        }
        boardCard.add(titleRow);

        if (bingo.getCodephrase() != null && !bingo.getCodephrase().trim().isEmpty()) {
            boardCard.add(Box.createVerticalStrut(2));
            boardCard.add(wrappedLabel("Codephrase: " + bingo.getCodephrase().trim(), FontManager.getRunescapeSmallFont(), BingoTheme.GOLD));
        }

        int tiles = 0;
        int completed = 0;
        int pending = 0;
        int needsAction = 0;
        int declined = 0;
        int xpTotal = 0;
        int xpEarned = 0;
        if (bingo.getTiles() != null) {
            for (Tile tile : bingo.getTiles()) {
                if (tile == null || tile.isHidden()) {
                    continue;
                }
                tiles++;
                xpTotal += tile.getWeight();
                TileSubmissionType status = TileStatusStyle.statusOf(tile);
                if (status == null) {
                    continue;
                }
                switch (status) {
                    case ACCEPTED:
                        completed++;
                        xpEarned += tile.getWeight();
                        break;
                    case PENDING:
                        pending++;
                        break;
                    case REQUIRES_INTERACTION:
                        needsAction++;
                        break;
                    case DECLINED:
                        declined++;
                        break;
                    default:
                        break;
                }
            }
        }

        boardCard.add(Box.createVerticalStrut(GAP));
        addProgress(boardCard, "Completed", completed, tiles, completed + " / " + tiles);
        boardCard.add(Box.createVerticalStrut(GAP));
        addProgress(boardCard, "XP", xpEarned, xpTotal, xpEarned + " / " + xpTotal);
        boardCard.add(Box.createVerticalStrut(GAP));

        JPanel stats = new JPanel(new GridLayout(1, 3, GAP, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(stat(pending, "Pending", TileStatusStyle.PENDING));
        stats.add(stat(needsAction, "Action", TileStatusStyle.NEEDS_ACTION));
        stats.add(stat(declined, "Declined", TileStatusStyle.DECLINED));
        boardCard.add(fullWidth(stats));

        boardCard.setVisible(true);
    }

    private void rebuildEventCard(EventData event) {
        eventCard.removeAll();

        JPanel titleRow = row();
        titleRow.add(wrappedLabel(event.getTitle(), FontManager.getRunescapeBoldFont(), BingoTheme.GOLD, TITLE_WITH_PILL_WIDTH), BorderLayout.CENTER);
        Pill status = new Pill();
        applyEventStatus(status, event);
        titleRow.add(pillHolder(status), BorderLayout.EAST);
        eventCard.add(titleRow);

        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            eventCard.add(Box.createVerticalStrut(GAP));
            eventCard.add(wrappedLabel(event.getDescription().trim(), FontManager.getRunescapeSmallFont(), BingoTheme.MUTED));
        }

        eventCard.add(Box.createVerticalStrut(GAP));
        if (event.getStartDate() != null && event.getEndDate() != null) {
            addInfoRow(eventCard, "Starts", DATE_FORMAT.format(event.getStartDate()));
            addInfoRow(eventCard, "Ends", DATE_FORMAT.format(event.getEndDate()));
        }
        if (event.getBasePrizePool() > 0) {
            addInfoRow(eventCard, "Prize pool", formatGpAmount(event.getBasePrizePool()));
            if (event.getMinimumBuyIn() > 0) {
                addInfoRow(eventCard, "Minimum buy-in", formatGpAmount(event.getMinimumBuyIn()));
            }
        }
        if (event.getRole() != null) {
            addInfoRow(eventCard, "Role", formatRole(event.getRole()));
        }
        if (event.getClan() != null) {
            addInfoRow(eventCard, "Clan", event.getClan().getName());
        }
        if (event.getBingos() != null) {
            addInfoRow(eventCard, "Boards", String.valueOf(event.getBingos().size()));
        }

        if (event.getUserTeam() != null) {
            addInfoRow(eventCard, "Team", event.getUserTeam().getName());
            List<TeamMember> members = event.getUserTeam().getMembers();
            if (members != null && !members.isEmpty()) {
                eventCard.add(Box.createVerticalStrut(GAP));
                // Leader first, then everyone else
                for (TeamMember member : members) {
                    if (member.isLeader()) {
                        addMember(eventCard, member.getRunescapeName() + " (Leader)", BingoTheme.GOLD);
                    }
                }
                for (TeamMember member : members) {
                    if (!member.isLeader()) {
                        addMember(eventCard, member.getRunescapeName(), Color.WHITE);
                    }
                }
            }
        }

        eventCard.setVisible(true);
    }

    private static void applyEventStatus(Pill pill, EventData event) {
        Date now = new Date();
        if (event.isLocked()) {
            pill.setStatus("Locked", BingoTheme.MUTED);
        } else if (event.getStartDate() != null && event.getStartDate().after(now)) {
            pill.setStatus("Upcoming", TileStatusStyle.PENDING);
        } else if (event.getEndDate() != null && event.getEndDate().before(now)) {
            pill.setStatus("Ended", BingoTheme.MUTED);
        } else {
            pill.setStatus("Active", TileStatusStyle.ACCEPTED);
        }
    }

    // ---------------------------------------------------------------- small builders

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(BingoTheme.MUTED);
        label.setBorder(new EmptyBorder(GAP * 2, 0, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JPanel row() {
        JPanel row = new JPanel(new BorderLayout(GAP, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /**
     * Keeps a pill at its preferred size at the top of a BorderLayout cell.
     */
    private static JPanel pillHolder(Pill pill) {
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        holder.setOpaque(false);
        holder.add(pill);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(holder, BorderLayout.NORTH);
        return top;
    }

    private static void addInfoRow(JPanel container, String key, String value) {
        JPanel row = row();
        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(FontManager.getRunescapeSmallFont());
        keyLabel.setForeground(BingoTheme.MUTED);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(FontManager.getRunescapeSmallFont());
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        valueLabel.setToolTipText(value);
        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(1, 0, 1, 0));
        container.add(row);
    }

    private static void addMember(JPanel container, String name, Color color) {
        JLabel label = new JLabel("- " + name);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(color);
        label.setBorder(new EmptyBorder(1, 4, 1, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(label);
    }

    private static void addProgress(JPanel container, String key, int current, int target, String value) {
        addInfoRow(container, key, value);
        ProgressBar bar = new ProgressBar();
        bar.setProgress(current, target);
        container.add(Box.createVerticalStrut(2));
        container.add(bar);
    }

    private static JPanel stat(int count, String caption, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel number = new JLabel(String.valueOf(count), SwingConstants.CENTER);
        number.setFont(FontManager.getRunescapeBoldFont());
        number.setForeground(count > 0 ? color : BingoTheme.MUTED);
        JLabel label = new JLabel(caption, SwingConstants.CENTER);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(BingoTheme.MUTED);

        panel.add(number, BorderLayout.CENTER);
        panel.add(label, BorderLayout.SOUTH);
        return panel;
    }

    private static JLabel wrappedLabel(String text, Font font, Color color) {
        return wrappedLabel(text, font, color, TEXT_WIDTH);
    }

    private static JLabel wrappedLabel(String text, Font font, Color color, int width) {
        JLabel label = new JLabel("<html><body style='width:" + width + "px'>" + escape(text) + "</body></html>");
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static <T extends JComponent> T fullWidth(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    private static void styleCell(JLabel cell, boolean selected) {
        cell.setBackground(selected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
        cell.setForeground(Color.WHITE);
    }

    private static void reenableLater(JComponent component) {
        Timer timer = new Timer(RELOAD_FALLBACK_MS, e -> component.setEnabled(true));
        timer.setRepeats(false);
        timer.start();
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String formatGpAmount(long amount) {
        if (amount >= 1_000_000_000) {
            return (amount / 1_000_000_000) + "B GP";
        } else if (amount >= 1_000_000) {
            return (amount / 1_000_000) + "M GP";
        } else if (amount >= 1_000) {
            return (amount / 1_000) + "K GP";
        } else {
            return amount + " GP";
        }
    }

    private static String formatRole(Role role) {
        if (role == null) {
            return "Participant";
        }

        switch (role) {
            case ADMIN:
                return "Admin";
            case MANAGEMENT:
                return "Manager";
            case PARTICIPANT:
                return "Participant";
            default:
                return role.toString();
        }
    }
}
