package org.bingoscape;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.util.HotkeyListener;
import org.bingoscape.board.BingoBoardOverlay;
import org.bingoscape.board.BoardInputListener;
import org.bingoscape.board.BoardState;
import org.bingoscape.board.ScreenshotPreviewDialog;
import org.bingoscape.services.BingoScapeApiService;
import org.bingoscape.services.TileImageCache;

import java.time.temporal.ChronoUnit;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Graphics;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.*;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bingoscape.models.*;

import org.bingoscape.models.AutoSubmissionMetadata;
import org.bingoscape.services.AutoSubmissionHandler;
import org.bingoscape.services.TileRequirementMatcher;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.loottracker.LootReceived;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.Date;

@Slf4j
@PluginDescriptor(
        name = "BingoScape",
        description = "Participate in bingo events with your clan or friends",
        tags = {"bingo", "clan", "event", "minigame"}
)
public class BingoScapePlugin extends Plugin {
    // Constants
    private static final String ICON_PATH = "/sidepanel_icon.png";
    private static final String PNG_FORMAT = "png";
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    // Injected components
    @Inject
    private Client client;

    @Inject
    private ClientUI clientUI;

    @Inject
    private ClientThread clientThread;

    @Getter
    @Inject
    private BingoScapeConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private DrawManager drawManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BingoCodephraseOverlay codephraseOverlay;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private Gson gson;

    @Inject
    private BingoScapeApiService apiService;

    @Inject
    private TileRequirementMatcher requirementMatcher;

    @Inject
    private AutoSubmissionHandler autoSubmissionHandler;

    @Inject
    private org.bingoscape.services.TeamDropPoller teamDropPoller;

    @Inject
    private org.bingoscape.notifications.DropNotificationOverlay dropOverlay;

    @Getter
    @Inject
    private net.runelite.client.game.ItemManager itemManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private BingoBoardOverlay boardOverlay;

    @Inject
    private BoardInputListener boardInputListener;

    @Inject
    private BoardState boardState;

    @Inject
    private TileImageCache tileImageCache;

    // Plugin components
    private NavigationButton navButton;
    private BingoScapePanel panel;
    private JDialog screenshotPreviewDialog;

    private final HotkeyListener boardHotkeyListener = new HotkeyListener(() -> config.boardToggleHotkey()) {
        @Override
        public void hotkeyPressed() {
            toggleBoard();
        }
    };

    // State (written from HTTP callback threads, read by overlays on the client thread)
    private final List<EventData> activeEvents = new CopyOnWriteArrayList<>();
    @Getter
    private volatile EventData currentEvent;
    @Getter
    private volatile Bingo currentBingo;
    private boolean isLoggedIn;
    // Event, bingo and team the team drop feed is baselined for
    private volatile String teamDropScope = "";

    @Override
    protected void startUp() {
        // Load the icon for the side panel
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), ICON_PATH);

        // Create and initialize the side panel
        panel = new BingoScapePanel(this);
        navButton = NavigationButton.builder()
                .tooltip("BingoScape")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        overlayManager.add(codephraseOverlay);
        overlayManager.add(boardOverlay);
        overlayManager.add(dropOverlay);
        mouseManager.registerMouseListener(boardInputListener);
        mouseManager.registerMouseWheelListener(boardInputListener);
        keyManager.registerKeyListener(boardInputListener);
        keyManager.registerKeyListener(boardHotkeyListener);

        teamDropPoller.configureBoardRefresh(
                () -> currentBingo == null || currentBingo.getId() == null ? null : currentBingo.getId().toString(),
                this::refreshBingoBoard);
        updateTeamDropPolling(false);

        // Load all events and handle pinned bingo
        if (hasApiKey()) {
            String pinnedBingoId = config.pinnedBingoId();
            apiService.fetchEvents(
                events -> {
                    activeEvents.clear();
                    activeEvents.addAll(events);
                    sortEvents(activeEvents);
                    panel.updateEventsList(activeEvents);

                    // If there's a pinned bingo, find and select its event
                    if (!pinnedBingoId.isEmpty()) {
                        UUID pinnedId = UUID.fromString(pinnedBingoId);
                        for (EventData event : events) {
                            if (event.getBingos().stream().anyMatch(b -> b.getId().equals(pinnedId))) {
                                // Found the event with pinned bingo, select it and load the bingo
                                setEventDetails(event);
                                apiService.refreshBingoBoard(
                                    pinnedId,
                                    bingo -> selectBingo(bingo),
                                    error -> log.error("Failed to load pinned bingo: " + error)
                                );
                                break;
                            }
                        }
                    }
                },
                error -> log.error("Failed to load events: " + error)
            );
        }
    }

    @Override
    protected void shutDown() {
        teamDropPoller.stop();
        dropOverlay.clear();
        overlayManager.remove(dropOverlay);
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(codephraseOverlay);
        overlayManager.remove(boardOverlay);
        mouseManager.unregisterMouseListener(boardInputListener);
        mouseManager.unregisterMouseWheelListener(boardInputListener);
        keyManager.unregisterKeyListener(boardInputListener);
        keyManager.unregisterKeyListener(boardHotkeyListener);
        boardState.hide();
        boardState.resetCaptures();
        tileImageCache.clear();

        BingoScapePanel closingPanel = panel;
        JDialog closingDialog = screenshotPreviewDialog;
        screenshotPreviewDialog = null;
        SwingUtilities.invokeLater(() -> {
            closingPanel.shutdown();
            if (closingDialog != null) {
                closingDialog.dispose();
            }
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!BingoScapeConfig.CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }

        switch (event.getKey()) {
            case "apiKey":
                updateTeamDropPolling(true);
                return;
            case "showToastNotifications":
            case "showTeamDropNotifications":
            case "showTileCompletedNotifications":
                updateTeamDropPolling(false);
                return;
            default:
                break;
        }
        if (!"boardDisplayMode".equals(event.getKey())) {
            return;
        }

        // Close whichever board view no longer matches the selected mode
        if (config.boardDisplayMode() == BoardDisplayMode.WINDOW) {
            hideBoard();
        } else {
            SwingUtilities.invokeLater(panel::disposeBoardWindow);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        switch (gameStateChanged.getGameState()) {
            case HOPPING:
            case LOGGING_IN:
            case LOGIN_SCREEN:
            case LOGIN_SCREEN_AUTHENTICATOR:
            case CONNECTION_LOST:
                dropOverlay.clear();
                break;
            default:
                break;
        }

        switch (gameStateChanged.getGameState()) {
            case LOGGED_IN:
                updateTeamDropPolling(false);
                break;
            case LOGIN_SCREEN:
            case CONNECTION_LOST:
                teamDropPoller.stop();
                break;
            default:
                break;
        }

        // Only update login state if transitioning to LOGGED_IN from a non-logged-in state
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN && !isLoggedIn) {
            isLoggedIn = true;
            if (hasApiKey()) {
                fetchActiveEvents();
            }
        } else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            isLoggedIn = false;
        }
    }

    public void fetchActiveEvents() {
        if (!hasApiKey()) {
            return;
        }

        apiService.fetchActiveEvents(
            events -> {
                activeEvents.clear();
                activeEvents.addAll(events);
                sortEvents(activeEvents);
                panel.updateEventsList(activeEvents);
            },
            error -> showErrorMessage(error.getMessage())
        );
    }

    private void sortEvents(List<EventData> events) {
        Date now = new Date();
        
        // Filter events based on configuration
        events.removeIf(event -> {
            // Filter past events
            if (config.hidePastEvents() && event.getEndDate().before(now)) {
                return true;
            }
            
            // Filter locked events
            if (config.hideLockedEvents() && event.isLocked()) {
                return true;
            }
            
            // Filter upcoming events
            if (config.hideUpcomingEvents() && event.getStartDate().after(now)) {
                return true;
            }
            
            return false;
        });

        // Sort remaining events
        events.sort((e1, e2) -> {
            // First sort by locked status (active events first)
            if (e1.isLocked() != e2.isLocked()) {
                return e1.isLocked() ? 1 : -1;
            }

            // Then sort by start date (upcoming events first)
            boolean e1Upcoming = e1.getStartDate().after(now);
            boolean e2Upcoming = e2.getStartDate().after(now);
            
            if (e1Upcoming != e2Upcoming) {
                return e1Upcoming ? -1 : 1;
            }

            // For events with the same status, sort by start date (most recent first)
            int dateComparison = e2.getStartDate().compareTo(e1.getStartDate());
            if (dateComparison != 0) {
                return dateComparison;
            }

            // Finally, sort alphabetically by title
            return e1.getTitle().compareToIgnoreCase(e2.getTitle());
        });
    }

    public void setEventDetails(EventData eventData) {
        currentEvent = eventData;
        panel.updateEventDetails(eventData);

        Bingo bingo = pickBingo(eventData);
        if (bingo != null) {
            selectBingo(bingo);
        }
        rebaselineTeamDropsIfScopeChanged();
    }

    /**
     * Starts or stops the team drop feed for the current game state and config.
     *
     * @param apiKeyChanged forget an earlier 401/403 and start from a fresh baseline
     */
    private void updateTeamDropPolling(boolean apiKeyChanged) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            teamDropPoller.stop();
        } else if (apiKeyChanged) {
            teamDropPoller.restart();
        } else if (teamDropPoller.isEnabled()) {
            teamDropPoller.start(this::getAccountName);
        } else {
            teamDropPoller.stop();
        }
    }

    private void rebaselineTeamDropsIfScopeChanged() {
        EventData event = currentEvent;
        Bingo bingo = currentBingo;
        String scope = (event == null ? "" : event.getId() + "/" + (event.getUserTeam() == null ? "" : event.getUserTeam().getName()))
                + "|" + (bingo == null ? "" : bingo.getId());
        if (!scope.equals(teamDropScope)) {
            teamDropScope = scope;
            teamDropPoller.rebaseline();
        }
    }

    /**
     * The bingo to show for an event: the pinned one, else the current one if it belongs to the event,
     * else the first. The panel uses the same rule, so both agree regardless of which thread runs first.
     */
    public Bingo pickBingo(EventData eventData) {
        List<Bingo> bingos = eventData.getBingos() == null ? List.of() : new ArrayList<>(eventData.getBingos());
        if (bingos.isEmpty()) {
            return null;
        }

        String pinnedId = config.pinnedBingoId();
        Bingo current = currentBingo;
        for (Bingo bingo : bingos) {
            if (bingo.getId().toString().equals(pinnedId)) {
                return bingo;
            }
        }
        if (current != null) {
            for (Bingo bingo : bingos) {
                if (bingo.getId().equals(current.getId())) {
                    return bingo;
                }
            }
        }
        return bingos.get(0);
    }

    public void selectBingo(Bingo bingo) {
        currentBingo = bingo;
        panel.displayBingoBoard(currentBingo);
        rebaselineTeamDropsIfScopeChanged();

        // Rebuild requirement matcher lookup maps for auto-submission
        // The matcher will query currentBingo directly from the plugin
        requirementMatcher.rebuildLookupMaps();

        if (bingo != null) {
            log.info("Selected bingo '{}' - Auto-submission ready. {}", bingo.getTitle(), requirementMatcher.getStats());
        } else {
            log.info("Cleared bingo selection - Auto-submission disabled");
        }
    }

    public void takeScreenshot(UUID tileId, Consumer<byte[]> callback) {
        // Unlike the old window, the board overlay is drawn into the game frame; keep it out of the capture.
        // The frame listener is registered from the client thread so it fires for a frame drawn after
        // the suppression flag is set.
        // A counter rather than a flag, so overlapping captures (auto + manual) all exclude the board.
        boardState.beginCapture();
        clientThread.invokeLater(() -> drawManager.requestNextFrameListener(image -> {
            boardState.endCapture();
            executor.submit(() -> {
                try {
                    BufferedImage screenshot = convertToBufferedImage(image);
                    byte[] screenshotBytes = convertImageToBytes(screenshot);
                    callback.accept(screenshotBytes);
                } catch (IOException e) {
                    log.error("Failed to process screenshot", e);
                    showErrorMessage("Failed to take screenshot for submission.");
                    callback.accept(null);
                }
            });
        }));
    }

    /**
     * Manual submission from the board overlay: capture, let the user confirm in a preview dialog, then submit.
     */
    public void captureAndPreviewSubmission(Tile tile) {
        takeScreenshot(tile.getId(), screenshotBytes -> SwingUtilities.invokeLater(() -> {
            // takeScreenshot already reported the failure in chat
            if (screenshotBytes == null) {
                return;
            }

            if (screenshotPreviewDialog != null) {
                screenshotPreviewDialog.dispose();
            }
            screenshotPreviewDialog = ScreenshotPreviewDialog.create(
                    SwingUtilities.getWindowAncestor(client.getCanvas()),
                    tile,
                    screenshotBytes,
                    bytes -> submitTileCompletionWithScreenshot(tile.getId(), bytes));
            screenshotPreviewDialog.setVisible(true);
        }));
    }

    private BufferedImage convertToBufferedImage(Image image) {
        BufferedImage screenshot = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics graphics = screenshot.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return screenshot;
    }

    private byte[] convertImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, PNG_FORMAT, outputStream);
        return outputStream.toByteArray();
    }

    public void submitTileCompletionWithScreenshot(UUID tileId, byte[] screenshotBytes) {
        apiService.submitTileCompletion(
            tileId,
            screenshotBytes,
            updatedBingo -> {
                showSuccessMessage("Tile submission sent to BingoScape!");
                updateCurrentBingoAndPanel(updatedBingo);
            },
            error -> {
                showErrorMessage(error.getMessage());
                if (error.isLocked()) {
                    // Our copy of the bingo is stale; reload so the board shows it as locked
                    refreshBingoBoard();
                }
            }
        );
    }

    /**
     * Submits a tile completion automatically with metadata.
     * Used by the auto-submission handler to include context about the drop.
     */
    public void submitTileAutomaticWithMetadata(UUID tileId, byte[] screenshotBytes, AutoSubmissionMetadata metadata) {
        apiService.submitTileAutomatic(
            tileId,
            screenshotBytes,
            metadata,
            updatedBingo -> {
                log.info("Auto-submission successful for tile {}", tileId);
                updateCurrentBingoAndPanel(updatedBingo);
            },
            error -> {
                log.error("Auto-submission failed (HTTP {}): {}", error.getStatusCode(), error.getMessage());
                if (error.isLocked()) {
                    // Reloading marks the bingo locked locally, which stops further auto-submission attempts
                    refreshBingoBoard();
                }
            }
        );
    }

    public void refreshBingoBoard() {
        refreshBingoBoard(null);
    }

    /**
     * Reloads the current bingo; {@code onDone} runs after success or failure (on an HTTP callback thread).
     */
    public void refreshBingoBoard(Runnable onDone) {
        if (currentBingo == null || !hasApiKey()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        apiService.refreshBingoBoard(
            currentBingo.getId(),
            bingo -> {
                updateCurrentBingoAndPanel(bingo);
                if (onDone != null) {
                    onDone.run();
                }
            },
            error -> {
                log.error("Failed to refresh bingo board (HTTP {}): {}", error.getStatusCode(), error.getMessage());
                if (onDone != null) {
                    onDone.run();
                }
            }
        );
    }

    public boolean isBoardVisible() {
        return boardState.isVisible();
    }

    public void toggleBoard() {
        if (config.boardDisplayMode() == BoardDisplayMode.OVERLAY && boardState.isVisible()) {
            hideBoard();
        } else {
            showBoard();
        }
    }

    public void showBoard() {
        Bingo bingo = currentBingo;
        if (bingo == null) {
            showErrorMessage("Select a bingo board first.");
            return;
        }

        if (config.boardDisplayMode() == BoardDisplayMode.WINDOW) {
            panel.openBoardWindow(bingo);
            return;
        }

        boardState.show();
        panel.setBoardButtonState(true);
    }

    public void hideBoard() {
        boardState.hide();
        panel.setBoardButtonState(false);
    }

    public void reloadBoard() {
        boardState.startReload();
        refreshBingoBoard(boardState::finishReload);
    }

    public void toggleBoardPin() {
        Bingo bingo = currentBingo;
        if (bingo == null) {
            return;
        }

        if (bingo.getId().toString().equals(config.pinnedBingoId())) {
            unpinBingo();
        } else {
            pinBingo(bingo.getId());
        }
        panel.refreshPinState();
    }

    private void updateCurrentBingoAndPanel(Bingo updatedBingo) {
        for(EventData e : activeEvents) {
            // Swap in a new list instead of mutating: the panel iterates these lists on the EDT
            List<Bingo> bingos = new ArrayList<>(e.getBingos());
            bingos.replaceAll(b -> b.getId().equals(updatedBingo.getId()) ? updatedBingo : b);
            e.setBingos(bingos);
        }
        currentBingo = updatedBingo;
        panel.displayBingoBoard(updatedBingo);

        // Rebuild requirement matcher lookup maps after bingo changes
        // The matcher will query currentBingo directly from the plugin
        requirementMatcher.rebuildLookupMaps();
    }

    private void showErrorMessage(String message) {
        clientThread.invokeLater(() ->
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "BingoScape: " + message, null));
    }

    private void showSuccessMessage(String message) {
        clientThread.invokeLater(() ->
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "BingoScape: " + message, null));
    }

    private boolean hasApiKey() {
        return config.apiKey() != null && !config.apiKey().isEmpty();
    }

    public void pinBingo(UUID bingoId) {
        config.pinnedBingoId(bingoId.toString());
    }

    public void unpinBingo() {
        config.pinnedBingoId("");
    }

    public BingoScapePanel getPanel() {
        return panel;
    }

    /**
     * Gets the current logged-in RuneScape account name.
     */
    public String getAccountName() {
        if (client != null && client.getLocalPlayer() != null) {
            return client.getLocalPlayer().getName();
        }
        return null;
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        autoSubmissionHandler.onNpcLootReceived(event);
    }

    @Subscribe
    public void onLootReceived(LootReceived event) {
        autoSubmissionHandler.onLootReceived(event);
    }

    @Schedule(period = 60, unit = ChronoUnit.SECONDS)
    public void cleanupAutoSubmissionCooldowns() {
        autoSubmissionHandler.cleanupCooldowns();
    }

    @Provides
    BingoScapeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BingoScapeConfig.class);
    }
}
