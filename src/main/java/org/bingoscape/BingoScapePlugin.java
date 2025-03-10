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

import java.time.temporal.ChronoUnit;
import java.awt.image.BufferedImage;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bingoscape.models.*;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

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

    private NavigationButton navButton;
    private BingoScapePanel panel;
    private final Gson gson = new GsonBuilder().create();
    private List<EventData> activeEvents = new ArrayList<>();
    private EventData currentEvent;
    private Bingo currentBingo;

    @Override
    protected void startUp() throws Exception {
        panel = new BingoScapePanel(this);

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
            if (config.apiKey() != null && !config.apiKey().isEmpty()) {
                fetchActiveEvents();
            }
        }
    }

    @Schedule(period = 5, unit = ChronoUnit.MINUTES)
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
        //currentEvent = eventDetail.getEvent();
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

    public void submitTileCompletion(UUID tileId, String proofImageUrl, String description) {
        if (config.apiKey() == null || config.apiKey().isEmpty()) {
            return;
        }

        String apiUrl = config.apiBaseUrl() + "/api/runelite/tiles/" + tileId + "/submissions";

        // Implementation for submission would go here
        // This would use OkHttp to POST the submission data

        // After successful submission:
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Tile submission sent to BingoScape!", null);

        // Refresh the current bingo board
        if (currentEvent != null) {
            setEventDetails(currentEvent);
        }
    }

    @Provides
    BingoScapeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BingoScapeConfig.class);
    }
}

