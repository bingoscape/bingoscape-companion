package org.bingoscape.apiclient;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.models.BingoTileResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Utility class for making requests to the BingoScape API
 */
@RequiredArgsConstructor
public class BingoScapeApiClient {
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final BingoScapeConfig config;

    /**
     * Fetches the bingo tiles for the specified bingo ID
     */
    public BingoTileResponse getBingoTiles(UUID bingoId) throws IOException {
        Request request = new Request.Builder()
                .url(config.apiBaseUrl() + "/api/runelite/bingos/" + bingoId + "/tiles")
                .header("Authorization", "Bearer " + config.apiKey())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            String responseBody = response.body().string();
            return gson.fromJson(responseBody, BingoTileResponse.class);
        }
    }
}
