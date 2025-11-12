package org.bingoscape.services;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.models.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class BingoScapeApiService {
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final BingoScapeConfig config;

    @Inject
    public BingoScapeApiService(OkHttpClient httpClient, Gson gson, BingoScapeConfig config) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.config = config;
    }

    public void refreshBingoBoard(UUID bingoId, Consumer<Bingo> onSuccess, Consumer<String> onError) {
        if (!hasApiKey()) {
            onError.accept("No API key configured");
            return;
        }

        String apiUrl = config.apiBaseUrl() + "/api/runelite/bingos/" + bingoId;
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + config.apiKey())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to refresh bingo board", e);
                onError.accept("Failed to refresh bingo board: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String error = "Unsuccessful response when refreshing bingo: " + response;
                        log.error(error);
                        onError.accept(error);
                        return;
                    }

                    String jsonData = responseBody.string();
                    Bingo updatedBingo = gson.fromJson(jsonData, Bingo.class);
                    onSuccess.accept(updatedBingo);
                }
            }
        });
    }

    public void submitTileCompletion(UUID tileId, byte[] screenshotBytes, Consumer<Bingo> onSuccess, Consumer<String> onError) {
        if (!hasApiKey()) {
            onError.accept("No API key configured");
            return;
        }

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
                onError.accept("Failed to submit tile completion: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String stringBody = responseBody.string();
                    if (!response.isSuccessful()) {
                        ErrorResponse errorResponse = gson.fromJson(stringBody, ErrorResponse.class);
                        String error = errorResponse.getError();
                        log.error("Unsuccessful submission response: {}", error);
                        onError.accept(error);
                        return;
                    }

                    Bingo updatedBingo = gson.fromJson(stringBody, Bingo.class);
                    onSuccess.accept(updatedBingo);
                }
            }
        });
    }

    public void fetchEvents(Consumer<List<EventData>> onSuccess, Consumer<String> onError) {
        if (!hasApiKey()) {
            onError.accept("No API key configured");
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
                onError.accept("Failed to fetch events: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String error = "Unsuccessful response when fetching events: " + response;
                        log.error(error);
                        onError.accept(error);
                        return;
                    }

                    String jsonData = responseBody.string();
                    EventData[] events = gson.fromJson(jsonData, EventData[].class);
                    onSuccess.accept(Arrays.asList(events));
                }
            }
        });
    }

    private boolean hasApiKey() {
        return config.apiKey() != null && !config.apiKey().isEmpty();
    }
} 