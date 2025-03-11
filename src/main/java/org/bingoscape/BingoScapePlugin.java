package org.bingoscape;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.DrawManager;

import java.time.temporal.ChronoUnit;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Graphics;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bingoscape.apiclient.BingoScapeApiClient;
import org.bingoscape.models.*;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
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
    @Inject
    private Client client;

    @Inject
    private BingoScapeConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private DrawManager drawManager;

    @Inject
    private ScheduledExecutorService executor;

    private NavigationButton navButton;
    private BingoScapePanel panel;
    private final Gson gson = new GsonBuilder().create();
    private List<EventData> activeEvents = new ArrayList<>();
    private EventData currentEvent;
    private Bingo currentBingo;
    private BingoScapeApiClient bingoScapeApiClient;

    private boolean isLoggedIn;

    @Override
    protected void startUp() throws Exception {
        panel = new BingoScapePanel(this);
        bingoScapeApiClient = new BingoScapeApiClient(this.httpClient, config);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/sidepanel_icon.png");

        navButton = NavigationButton.builder()
                .tooltip("BingoScape")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        if (config.apiKey() != null && !config.apiKey().isEmpty()) {
            fetchActiveEvents();
        } else {
            panel.showApiKeyPrompt();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            isLoggedIn = true;
            if (config.apiKey() != null && !config.apiKey().isEmpty()) {
                fetchActiveEvents();
            }
        } else {
            isLoggedIn = false;
        }
    }

    @Schedule(period = 2, unit = ChronoUnit.MINUTES)
    public void scheduledRefresh() {
        if (client.getGameState() == GameState.LOGGED_IN && config.apiKey() != null && !config.apiKey().isEmpty()) {
            fetchActiveEvents();
            if (currentEvent != null) {
                setEventDetails(currentEvent);
            }
        }
    }

    public void setApiKey(String apiKey) {
        config.apiKey(apiKey);
        fetchActiveEvents();
    }

    public BingoTileResponse fetchBingoTileStatus(UUID bingoId) {
        try {
            return this.bingoScapeApiClient.getBingoTiles(bingoId);
        } catch (IOException e) {
            return null;
        }
    }


    public void fetchActiveEvents() {
        if (config.apiKey() == null || config.apiKey().isEmpty()) {
            return;
        }

        String apiUrl = config.apiBaseUrl() + "/api/runelite/events";

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", String.format("Bearer %s", config.apiKey()))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to fetch events", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    log.error("Unsuccessful response: " + response);
                    return;
                }

                String jsonData = response.body().string();
                log.info(jsonData);
                EventData[] eventsResponse = gson.fromJson(jsonData, EventData[].class);

                activeEvents = Arrays.asList(eventsResponse);
                panel.updateEventsList(activeEvents);
            }
        });
    }

    public void setEventDetails(EventData eventData) {
        currentEvent = eventData;
        panel.updateEventDetails(eventData);

        if (eventData.getBingos() != null && !eventData.getBingos().isEmpty()) {
            // Default to first bingo
            selectBingo(eventData.getBingos().get(0));
        }
    }

    public void selectBingo(Bingo bingo) {
        currentBingo = bingo;
        panel.displayBingoBoard(currentBingo);
    }

    public void submitTileCompletion(UUID tileId) {
        if (!isLoggedIn) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        panel,
                        "You must be logged into RuneScape to submit a tile completion.",
                        "Not Logged In",
                        JOptionPane.ERROR_MESSAGE
                );
            });
            return;
        }

        if (config.apiKey() == null || config.apiKey().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        panel,
                        "API key is missing. Please set your API key in the plugin settings.",
                        "Missing API Key",
                        JOptionPane.ERROR_MESSAGE
                );
            });
            return;
        }

        takeScreenshot(tileId);
    }

    private void takeScreenshot(UUID tileId) {
        Consumer<Image> imageCallback = (img) -> {
            executor.submit(() -> {
                try {
                    processScreenshot(tileId, img);
                } catch (IOException e) {
                    log.error("Failed to process screenshot", e);
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Failed to take screenshot for submission.", null);
                }
            });
        };

        drawManager.requestNextFrameListener(imageCallback);
    }

    private void processScreenshot(UUID tileId, Image image) throws IOException {
        BufferedImage screenshot = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = screenshot.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        ByteArrayOutputStream screenshotOutput = new ByteArrayOutputStream();
        ImageIO.write(screenshot, "png", screenshotOutput);
        byte[] screenshotBytes = screenshotOutput.toByteArray();

        submitTileCompletionWithScreenshot(tileId, screenshotBytes);
    }

    private void submitTileCompletionWithScreenshot(UUID tileId, byte[] screenshotBytes) {
        String apiUrl = config.apiBaseUrl() + "/api/runelite/tiles/" + tileId + "/submissions";

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        if (screenshotBytes != null) {
            multipartBuilder.addFormDataPart("image", "screenshot.png",
                    RequestBody.create(MediaType.parse("image/png"), screenshotBytes));
        }

        RequestBody requestBody = multipartBuilder.build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", String.format("Bearer %s", config.apiKey()))
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to submit tile completion", e);
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Failed to submit tile completion to BingoScape.", null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    log.error("Unsuccessful submission response: " + response);
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Failed to submit tile completion to BingoScape.", null);
                    return;
                }

                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Tile submission sent to BingoScape!", null);

                // Refresh the current bingo board
                if (currentEvent != null) {
                    setEventDetails(currentEvent);
                }
            }
        });
    }

    @Provides
    BingoScapeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BingoScapeConfig.class);
    }
}

