package org.bingoscape;

import com.google.gson.Gson;
import org.bingoscape.models.Bingo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;

/**
 * Loads the API contract fixtures from src/test/resources/contracts with a plain Gson, as the plugin does.
 */
public final class ContractFixtures {
    private static final Gson GSON = new Gson();

    private ContractFixtures() {
    }

    public static Bingo bingo(String name) {
        try (InputStream in = ContractFixtures.class.getResourceAsStream("/contracts/" + name + ".json")) {
            assertNotNull("missing fixture " + name, in);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, Bingo.class);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
