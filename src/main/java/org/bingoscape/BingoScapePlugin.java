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
import org.bingoscape.services.BingoScapeApiService;
import org.bingoscape.services.AutoSubmissionHandler;
import org.bingoscape.services.TileRequirementMatcher;
import org.bingoscape.utils.EventComparator;
import org.bingoscape.utils.EventFilter;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.loottracker.LootReceived;

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
    private static final String HTTP_STATUS_LOCKED = "423";

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
    private org.bingoscape.notifications.NotificationManager notificationManager;

    @Getter
    @Inject
    private net.runelite.client.game.ItemManager itemManager;

    // Plugin components
    private NavigationButton navButton;
    private BingoScapePanel panel;

    // State
    private final List<EventData> activeEvents = new CopyOnWriteArrayList<>();
    @Getter
    private EventData currentEvent;
    @Getter
    private Bingo currentBingo;
    private boolean isLoggedIn;

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
        notificationManager.startUp();

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
        notificationManager.shutDown();
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(codephraseOverlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
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

        apiService.fetchEvents(
            events -> {
                activeEvents.clear();
                activeEvents.addAll(events);
                sortEvents(activeEvents);
                panel.updateEventsList(activeEvents);
            },
            error -> showErrorMessage(error)
        );
    }

    private void sortEvents(List<EventData> events) {
        // Filter events based on configuration
        EventFilter filter = new EventFilter(config);
        events.removeIf(filter.createRemovalPredicate());

        // Sort remaining events using complex business rules
        events.sort(new EventComparator());
    }

    public void setEventDetails(EventData eventData) {
        currentEvent = eventData;
        panel.updateEventDetails(eventData);

        if (eventData.getBingos() != null && !eventData.getBingos().isEmpty()) {
            // Default to first bingo or current one if it exists
            selectBingo(currentBingo == null ?
                    eventData.getBingos().get(0) : currentBingo);
        }
    }

    public void selectBingo(Bingo bingo) {
        currentBingo = bingo;
        panel.displayBingoBoard(currentBingo);

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
        drawManager.requestNextFrameListener(image -> {
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
        });
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
                showErrorMessage(error);
                if (error.contains(HTTP_STATUS_LOCKED)) { // HTTP 423 Locked
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
                log.error("Auto-submission failed: {}", error);
                if (error.contains(HTTP_STATUS_LOCKED)) { // HTTP 423 Locked
                    refreshBingoBoard();
                }
            }
        );
    }

    /**
     * Gets the current logged-in RuneScape account name.
     */
    public String getAccountName() {
        if (client.getLocalPlayer() != null) {
            return client.getLocalPlayer().getName();
        }
        return null;
    }

    public void refreshBingoBoard() {
        if (currentBingo == null || !hasApiKey()) {
            return;
        }

        apiService.refreshBingoBoard(
            currentBingo.getId(),
            this::updateCurrentBingoAndPanel,
            error -> log.error(error)
        );
    }

    private void updateCurrentBingoAndPanel(Bingo updatedBingo) {
        for(EventData e : activeEvents) {
            e.getBingos().replaceAll(b -> b.getId().equals(updatedBingo.getId()) ? updatedBingo : b);
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
