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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

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
        // Initialize components
        panel = new BingoScapePanel(this);

        // Set up navigation button
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), ICON_PATH);
        navButton = NavigationButton.builder()
                .tooltip("BingoScape")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        // Add the overlay
        overlayManager.add(codephraseOverlay);

        // Initialize data
        if (hasApiKey()) {
            fetchActiveEvents();
        }
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(codephraseOverlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        boolean wasLoggedIn = isLoggedIn;
        isLoggedIn = gameStateChanged.getGameState() == GameState.LOGGED_IN;

        // Only fetch if player has just logged in
        if (isLoggedIn && !wasLoggedIn && hasApiKey()) {
            fetchActiveEvents();
        }
    }

    public void fetchActiveEvents() {
        if (!hasApiKey()) {
            return;
        }

        String apiUrl = config.apiBaseUrl() + "/api/runelite/events";

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + config.apiKey())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to fetch events", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        log.error("Unsuccessful response: " + response);
                        return;
                    }

                    String jsonData = responseBody.string();
                    EventData[] eventsResponse = gson.fromJson(jsonData, EventData[].class);

                    activeEvents.clear();
                    activeEvents.addAll(Arrays.asList(eventsResponse));
                    panel.updateEventsList(activeEvents);
                }
            }
        });
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


    // Add this method to your plugin
    private void showAlert(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    panel,
                    message,
                    title,
                    messageType
            );
        });
    }
    private byte[] convertImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, PNG_FORMAT, outputStream);
        return outputStream.toByteArray();
    }

    public void submitTileCompletionWithScreenshot(UUID tileId, byte[] screenshotBytes) {

        String apiUrl = config.apiBaseUrl() + "/api/runelite/tiles/" + tileId + "/submissions";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "screenshot.png", RequestBody.create(MEDIA_TYPE_PNG, screenshotBytes))
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + config.apiKey())
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to submit tile completion", e);
                showErrorMessage("Failed to submit tile completion to BingoScape.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String stringBody = responseBody.string();
                    if (!response.isSuccessful()) {
                        ErrorResponse errorResponse = gson.fromJson(stringBody, ErrorResponse.class);
                        log.error("Unsuccessful submission response: {}", errorResponse.getError());
                        showErrorMessage(errorResponse.getError());
                        if (response.code() == 423) { // 423 Locked
                            refreshBingoBoard();
                        }
                        return;
                    }

                    showSuccessMessage("Tile submission sent to BingoScape!");

                    // Refresh the current bingo board with updated tile statuses
                    if (currentBingo != null) {
                        Bingo updatedBingo = gson.fromJson(stringBody, Bingo.class);
                        updateCurrentBingoAndPanel(updatedBingo);
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    // Add this new method to handle refreshing the bingo board:
    private void refreshBingoBoard() {
        if (currentBingo == null || !hasApiKey()) {
            return;
        }

        String apiUrl = config.apiBaseUrl() + "/api/runelite/bingos/" + currentBingo.getId();

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + config.apiKey())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to refresh bingo board", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        log.error("Unsuccessful response when refreshing bingo: " + response);
                        return;
                    }

                    String jsonData = responseBody.string();
                    Bingo updatedBingo = gson.fromJson(jsonData, Bingo.class);

                    updateCurrentBingoAndPanel(updatedBingo);
                }
            }
        });
    }

    private void updateCurrentBingoAndPanel(Bingo updatedBingo) {
        for(EventData e : activeEvents) {
            e.getBingos().replaceAll(b -> b.getId().equals(updatedBingo.getId()) ? updatedBingo : b);
        }
        currentBingo = updatedBingo;
        panel.displayBingoBoard(updatedBingo);
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

    @Provides
    BingoScapeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BingoScapeConfig.class);
    }
}
