package org.bingoscape.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.models.ApiError;
import org.bingoscape.models.TeamCompletionsResponse;
import org.bingoscape.models.TeamDropsResponse;
import org.bingoscape.notifications.DropNotification;
import org.bingoscape.notifications.DropNotificationOverlay;
import org.bingoscape.notifications.DropTier;
import org.bingoscape.notifications.DropTierResolver;
import org.bingoscape.notifications.TileCompletedNotification;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Polls the team drop feed and the team tile completion feed and shows a popup for each new drop of a
 * teammate and each tile the team completes. Reschedules itself after
 * every response, so requests never overlap. The first poll after a start only fetches the head cursor,
 * which means drops from before the start are never replayed.
 */
@Slf4j
@Singleton
public class TeamDropPoller {
    static final int PAGE_LIMIT = 50;
    static final int MAX_POPUPS_PER_POLL = 5;
    static final long DEFAULT_INTERVAL_MS = 10_000;
    static final long MIN_INTERVAL_MS = 5_000;
    static final long MAX_BACKOFF_MS = 120_000;
    static final int MAX_COMPLETIONS_PER_POLL = 3;
    static final int MAX_SEEN = 500;
    private static final double JITTER = 0.2;

    private final BingoScapeApiService apiService;
    private final BingoScapeConfig config;
    private final ScheduledExecutorService executor;
    private final ClientThread clientThread;
    private final Client client;
    private final ItemManager itemManager;
    private final DropNotificationOverlay overlay;
    private final Random random = new Random();

    private final Set<String> seen = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_SEEN;
                }
            }));

    private final Set<String> seenCompletions = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_SEEN;
                }
            }));

    private volatile String cursor;
    private volatile String completionCursor;
    private volatile boolean running;
    private volatile boolean authBlocked;
    /** Bumped on every start/stop/reset so responses of an older request are dropped. */
    private volatile int generation;
    private volatile ScheduledFuture<?> scheduled;
    private volatile Supplier<String> accountName = () -> null;
    private volatile Supplier<String> currentBingoId = () -> null;
    private volatile Runnable boardRefresh = () -> { };
    private int failures;

    @Inject
    public TeamDropPoller(BingoScapeApiService apiService, BingoScapeConfig config, ScheduledExecutorService executor,
                          ClientThread clientThread, Client client, ItemManager itemManager, DropNotificationOverlay overlay) {
        this.apiService = apiService;
        this.config = config;
        this.executor = executor;
        this.clientThread = clientThread;
        this.client = client;
        this.itemManager = itemManager;
        this.overlay = overlay;
    }

    // ---- Lifecycle ----

    /**
     * Sets how the poller learns the shown bingo and how it asks for a board reload when a tile of that
     * bingo was completed. The callback may run on any thread.
     */
    public void configureBoardRefresh(Supplier<String> currentBingoId, Runnable boardRefresh) {
        this.currentBingoId = currentBingoId;
        this.boardRefresh = boardRefresh;
    }

    /**
     * Starts polling when the feature is enabled and an API key is set; a no-op while already running.
     */
    public synchronized void start(Supplier<String> accountName) {
        this.accountName = accountName;
        if (running || !isEnabled()) {
            return;
        }
        running = true;
        generation++;
        schedule(0, generation);
    }

    /**
     * Stops polling and forgets the cursor, so the next start re-baselines instead of replaying.
     */
    public synchronized void stop() {
        running = false;
        generation++;
        cancelScheduled();
        resetState();
    }

    /**
     * Stop, forget a 401/403 block and start again. For API key or toggle changes.
     */
    public synchronized void restart() {
        stop();
        authBlocked = false;
        start(accountName);
    }

    /**
     * Drops cursor and seen ids (bingo, team or event changed) and polls again right away.
     */
    public synchronized void rebaseline() {
        generation++;
        cancelScheduled();
        resetState();
        if (running && !authBlocked) {
            schedule(0, generation);
        }
    }

    private void resetState() {
        resetDrops();
        resetCompletions();
        failures = 0;
    }

    private void resetDrops() {
        cursor = null;
        seen.clear();
    }

    private void resetCompletions() {
        completionCursor = null;
        seenCompletions.clear();
    }

    private void cancelScheduled() {
        ScheduledFuture<?> future = scheduled;
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * True when the popup is on, at least one of the two feeds is wanted and an API key is set.
     */
    public boolean isEnabled() {
        return config.showToastNotifications()
                && (config.showTeamDropNotifications() || config.showTileCompletedNotifications())
                && config.apiKey() != null && !config.apiKey().isEmpty();
    }

    // ---- Polling ----

    private synchronized void schedule(long delayMs, int gen) {
        if (!running || gen != generation || authBlocked) {
            return;
        }
        scheduled = executor.schedule(() -> poll(gen), delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * One tick: up to two requests in parallel; the next tick is scheduled once both have finished, so a
     * failing feed delays but never blocks the other.
     */
    private static final class Tick {
        final AtomicInteger pending;
        boolean immediate;
        long delayMs;
        int errors;
        long retryAfterSeconds;

        Tick(int pending) {
            this.pending = new AtomicInteger(pending);
        }
    }

    private void poll(int gen) {
        if (!running || gen != generation) {
            return;
        }
        if (!isEnabled()) {
            // Toggled off without a config event reaching us; stop quietly
            stop();
            return;
        }
        boolean drops = config.showTeamDropNotifications();
        boolean completions = config.showTileCompletedNotifications();
        synchronized (this) {
            // A feed that is switched off forgets its cursor, so it baselines again when switched on
            if (!drops) {
                resetDrops();
            }
            if (!completions) {
                resetCompletions();
            }
        }
        Tick tick = new Tick((drops ? 1 : 0) + (completions ? 1 : 0));
        if (drops) {
            String since = cursor;
            try {
                apiService.fetchTeamDrops(since, PAGE_LIMIT,
                        response -> onResponse(gen, tick, since, response),
                        error -> onError(gen, tick, error, this::resetDrops));
            } catch (RuntimeException e) {
                log.warn("Team drop poll failed", e);
                onError(gen, tick, new ApiError(ApiError.NO_RESPONSE, e.getMessage()), this::resetDrops);
            }
        }
        if (completions) {
            String since = completionCursor;
            try {
                apiService.fetchTeamCompletions(since, PAGE_LIMIT,
                        response -> onCompletionsResponse(gen, tick, since, response),
                        error -> onError(gen, tick, error, this::resetCompletions));
            } catch (RuntimeException e) {
                log.warn("Team completion poll failed", e);
                onError(gen, tick, new ApiError(ApiError.NO_RESPONSE, e.getMessage()), this::resetCompletions);
            }
        }
    }

    private void onResponse(int gen, Tick tick, String requestedSince, TeamDropsResponse response) {
        List<TeamDropsResponse.Drop> fresh;
        synchronized (this) {
            if (gen != generation || !running) {
                return;
            }
            try {
                fresh = applyResponse(requestedSince, response, accountName.get(), config.teamDropMinValue());
            } catch (RuntimeException e) {
                log.warn("Team drop response could not be applied", e);
                fresh = List.of();
            }
            finishFeed(gen, tick, response.isHasMore() && !response.isResync(), response.getPollIntervalMs());
        }
        if (!fresh.isEmpty()) {
            show(fresh);
        }
    }

    private void onCompletionsResponse(int gen, Tick tick, String requestedSince, TeamCompletionsResponse response) {
        CompletionBatch batch;
        synchronized (this) {
            if (gen != generation || !running) {
                return;
            }
            try {
                batch = applyCompletions(requestedSince, response, currentBingoId.get());
            } catch (RuntimeException e) {
                log.warn("Team completion response could not be applied", e);
                batch = new CompletionBatch(List.of(), false);
            }
            finishFeed(gen, tick, response.isHasMore() && !response.isResync(), response.getPollIntervalMs());
        }
        if (batch.refreshBoard) {
            try {
                boardRefresh.run();
            } catch (RuntimeException e) {
                log.debug("Board refresh after tile completion failed", e);
            }
        }
        if (!batch.shown.isEmpty()) {
            showCompletions(batch.shown);
        }
    }

    private void onError(int gen, Tick tick, ApiError error, Runnable resetFeed) {
        synchronized (this) {
            if (gen != generation || !running) {
                return;
            }
            int status = error.getStatusCode();
            if (status == 401 || status == 403) {
                log.warn("Team feeds: not authorized ({}), polling stopped until the API key changes", status);
                authBlocked = true;
                return;
            }
            if (status == 400) {
                // Cursor rejected: re-baseline this feed
                resetFeed.run();
                finishFeed(gen, tick, false, 0);
                return;
            }
            tick.errors++;
            tick.retryAfterSeconds = Math.max(tick.retryAfterSeconds, error.getRetryAfterSeconds());
            log.debug("Team feed poll failed ({})", status);
            finishFeed(gen, tick, false, 0);
        }
    }

    /**
     * Records the outcome of one feed and schedules the next tick after the last one finished. Must be
     * called with the lock held.
     */
    private void finishFeed(int gen, Tick tick, boolean hasMore, long serverIntervalMs) {
        tick.immediate |= hasMore;
        tick.delayMs = Math.max(tick.delayMs, intervalMs(serverIntervalMs));
        if (tick.pending.decrementAndGet() > 0) {
            return;
        }
        long delay;
        if (tick.errors > 0) {
            failures++;
            delay = errorDelayMs(failures, DEFAULT_INTERVAL_MS, tick.retryAfterSeconds, random.nextDouble());
        } else {
            failures = 0;
            delay = tick.immediate ? 0 : tick.delayMs;
        }
        schedule(delay, gen);
    }

    /**
     * Applies a response to cursor and seen ids and returns the drops that deserve a popup. Package-private
     * and free of I/O so it can be tested directly.
     */
    synchronized List<TeamDropsResponse.Drop> applyResponse(String requestedSince, TeamDropsResponse response, String ownName, long minValue) {
        if (response.isResync()) {
            resetState();
            return List.of();
        }
        if (response.getCursor() != null) {
            cursor = response.getCursor();
        }
        if (requestedSince == null || response.getDrops() == null) {
            // Baseline request: nothing to show, even if the server sent something
            return List.of();
        }

        List<TeamDropsResponse.Drop> result = new ArrayList<>();
        String own = normalizeName(ownName);
        for (TeamDropsResponse.Drop drop : response.getDrops()) {
            if (drop == null || drop.getId() == null || !seen.add(drop.getId())) {
                continue;
            }
            if (!shouldShow(drop, own, minValue)) {
                continue;
            }
            if (result.size() < MAX_POPUPS_PER_POLL) {
                result.add(drop);
            }
        }
        return result;
    }

    /**
     * Result of a completion response: the completions that deserve a popup and whether the shown board
     * should be reloaded.
     */
    static final class CompletionBatch {
        final List<TeamCompletionsResponse.Completion> shown;
        final boolean refreshBoard;

        CompletionBatch(List<TeamCompletionsResponse.Completion> shown, boolean refreshBoard) {
            this.shown = shown;
            this.refreshBoard = refreshBoard;
        }
    }

    /**
     * Completion counterpart of {@link #applyResponse}. A completion is new when its id and completedAt
     * were not seen, so a tile that is completed again (same id, new time) shows again. The board refresh
     * is requested at most once per response, whatever the number of completions.
     */
    synchronized CompletionBatch applyCompletions(String requestedSince, TeamCompletionsResponse response, String bingoId) {
        if (response.isResync()) {
            resetCompletions();
            return new CompletionBatch(List.of(), false);
        }
        if (response.getCursor() != null) {
            completionCursor = response.getCursor();
        }
        if (requestedSince == null || response.getCompletions() == null) {
            return new CompletionBatch(List.of(), false);
        }

        List<TeamCompletionsResponse.Completion> result = new ArrayList<>();
        boolean refresh = false;
        for (TeamCompletionsResponse.Completion completion : response.getCompletions()) {
            if (completion == null || completion.getId() == null
                    || !seenCompletions.add(completion.getId() + "@" + completion.getCompletedAt())) {
                continue;
            }
            if (bingoId != null && bingoId.equals(completion.getBingoId())) {
                refresh = true;
            }
            if (result.size() < MAX_COMPLETIONS_PER_POLL) {
                result.add(completion);
            }
        }
        return new CompletionBatch(result, refresh);
    }

    static boolean shouldShow(TeamDropsResponse.Drop drop, String normalizedOwnName, long minValue) {
        if ("rejected".equalsIgnoreCase(drop.getStatus()) || drop.getItem() == null) {
            return false;
        }
        String player = drop.getPlayer() == null ? "" : normalizeName(drop.getPlayer().getRunescapeName());
        if (!normalizedOwnName.isEmpty() && normalizedOwnName.equals(player)) {
            return false;
        }
        return drop.getItem().getValue() >= minValue;
    }

    /**
     * Case-insensitive, with underscores and non-breaking spaces treated as spaces.
     */
    static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace('_', ' ').replace(' ', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static long intervalMs(long serverIntervalMs) {
        if (serverIntervalMs <= 0) {
            return DEFAULT_INTERVAL_MS;
        }
        return Math.max(MIN_INTERVAL_MS, serverIntervalMs);
    }

    /**
     * Unjittered backoff: twice the base per consecutive failure (20s, 40s, 80s for a 10s base), capped.
     */
    static long backoffMs(int failures, long baseMs) {
        long delay = baseMs;
        for (int i = 0; i < failures && delay < MAX_BACKOFF_MS; i++) {
            delay *= 2;
        }
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    /**
     * Delay before the retry: backoff with +-20% jitter ({@code roll} in [0, 1)), at least the server's
     * Retry-After, never above the cap.
     */
    static long errorDelayMs(int failures, long baseMs, long retryAfterSeconds, double roll) {
        long backoff = backoffMs(failures, baseMs);
        long jittered = Math.round(backoff * (1 - JITTER + 2 * JITTER * roll));
        long delay = Math.max(jittered, retryAfterSeconds > 0 ? retryAfterSeconds * 1000 : 0);
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    // ---- Popups ----

    private void show(List<TeamDropsResponse.Drop> drops) {
        // Item names need the client thread
        clientThread.invoke(() -> {
            for (TeamDropsResponse.Drop drop : drops) {
                if (!isEnabled()) {
                    return;
                }
                overlay.enqueue(toNotification(drop, itemName(drop.getItem().getItemId()), config.dropUncommonValue(), config.dropRareValue(), config.dropEpicValue(), config.dropLegendaryValue()));
            }
        });
    }

    private void showCompletions(List<TeamCompletionsResponse.Completion> completions) {
        clientThread.invoke(() -> {
            boolean shown = false;
            for (TeamCompletionsResponse.Completion completion : completions) {
                if (!isEnabled() || !config.showTileCompletedNotifications()) {
                    break;
                }
                overlay.enqueue(toNotification(completion));
                shown = true;
            }
            if (shown && config.tileCompletedSound()) {
                client.playSoundEffect(SoundEffectID.UI_BOOP);
            }
        });
    }

    static TileCompletedNotification toNotification(TeamCompletionsResponse.Completion completion) {
        int points = completion.getTile() == null ? 0 : completion.getTile().getPoints();
        String title = completion.getTileTitle() == null ? "Tile" : completion.getTileTitle();
        return new TileCompletedNotification(title, points, completion.getTeamName());
    }

    static DropNotification toNotification(TeamDropsResponse.Drop drop, String itemName, long uncommon, long rare, long epic, long legendary) {
        TeamDropsResponse.Item item = drop.getItem();
        DropTier tier = DropTierResolver.resolve(item.getValue(), uncommon, rare, epic, legendary);
        String player = drop.getPlayer() == null ? null : drop.getPlayer().getRunescapeName();
        return new DropNotification(item.getItemId(), itemName, Math.max(1, item.getQuantity()), item.getValue(), tier,
                drop.getTileTitle(), 0, null, player);
    }

    private String itemName(int itemId) {
        try {
            return itemManager.getItemComposition(itemId).getName();
        } catch (Exception e) {
            log.debug("Failed to get item name for ID {}", itemId, e);
            return "Item #" + itemId;
        }
    }

    // Test access
    String getCursor() {
        return cursor;
    }

    String getCompletionCursor() {
        return completionCursor;
    }
}
