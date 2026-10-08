package org.bingoscape.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bingoscape.BingoScapeConfig;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-blocking cache for tile header images (usually OSRS wiki URLs).
 * Lookups never block: a miss starts a background download or scale and returns null,
 * so callers can poll from the render loop every frame.
 */
@Slf4j
@Singleton
public class TileImageCache {
    private static final int MAX_SOURCES = 96;
    private static final int MAX_SCALED = 256;
    private static final int MAX_SOURCE_EDGE = 512;
    private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;
    private static final long FAILURE_COOLDOWN_MS = 60_000;

    private final OkHttpClient httpClient;
    private final ScheduledExecutorService executor;
    private final BingoScapeConfig config;

    private final Map<String, BufferedImage> sources = Collections.synchronizedMap(lruMap(MAX_SOURCES));
    private final Map<String, BufferedImage> scaled = Collections.synchronizedMap(lruMap(MAX_SCALED));
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> failedAt = new ConcurrentHashMap<>();
    private final AtomicInteger generation = new AtomicInteger();

    @Inject
    public TileImageCache(OkHttpClient httpClient, ScheduledExecutorService executor, BingoScapeConfig config) {
        this.httpClient = httpClient;
        this.executor = executor;
        this.config = config;
    }

    /**
     * Returns the image scaled to fit inside {@code maxWidth x maxHeight} (aspect ratio kept),
     * or null if it is not ready yet. A miss schedules the download and/or scale.
     */
    public BufferedImage getScaled(String url, int maxWidth, int maxHeight) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            return null;
        }

        String resolved = resolve(url);
        if (resolved == null) {
            return null;
        }

        String key = resolved + "|" + maxWidth + "|" + maxHeight;
        BufferedImage hit = scaled.get(key);
        if (hit != null) {
            return hit;
        }

        BufferedImage source = sources.get(resolved);
        if (source == null) {
            fetch(resolved);
            return null;
        }

        scheduleScale(key, source, maxWidth, maxHeight);
        return null;
    }

    /**
     * Returns the downloaded (unscaled) image if available, without scheduling anything.
     */
    public BufferedImage getSource(String url) {
        String resolved = resolve(url);
        return resolved == null ? null : sources.get(resolved);
    }

    public boolean hasFailed(String url) {
        String resolved = resolve(url);
        if (resolved == null) {
            return true;
        }
        Long failed = failedAt.get(resolved);
        return failed != null && System.currentTimeMillis() - failed < FAILURE_COOLDOWN_MS;
    }

    public void prefetch(Collection<String> urls) {
        for (String url : urls) {
            String resolved = resolve(url);
            if (resolved != null && !sources.containsKey(resolved)) {
                fetch(resolved);
            }
        }
    }

    public void clear() {
        generation.incrementAndGet();
        sources.clear();
        scaled.clear();
        failedAt.clear();
        // inFlight is left alone: running requests remove themselves, and the generation check drops their results
    }

    private void fetch(String url) {
        if (hasFailed(url) || !inFlight.add(url)) {
            return;
        }

        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            markFailed(url);
            inFlight.remove(url);
            return;
        }

        int startGeneration = generation.get();
        Request request = new Request.Builder().url(httpUrl).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("Failed to download tile image {}", url, e);
                markFailed(url);
                inFlight.remove(url);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null || body.contentLength() > MAX_DOWNLOAD_BYTES) {
                        log.debug("Tile image {} not usable (HTTP {})", url, response.code());
                        markFailed(url);
                        return;
                    }

                    byte[] bytes = readCapped(body.byteStream());
                    BufferedImage image = bytes == null ? null : ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image == null) {
                        log.debug("Tile image {} could not be decoded", url);
                        markFailed(url);
                        return;
                    }

                    if (generation.get() == startGeneration) {
                        sources.put(url, downscale(image));
                        failedAt.remove(url);
                    }
                } catch (IOException e) {
                    log.debug("Failed to read tile image {}", url, e);
                    markFailed(url);
                } finally {
                    inFlight.remove(url);
                }
            }
        });
    }

    private void scheduleScale(String key, BufferedImage source, int maxWidth, int maxHeight) {
        if (!inFlight.add(key)) {
            return;
        }

        int startGeneration = generation.get();
        executor.submit(() -> {
            try {
                BufferedImage result = fit(source, maxWidth, maxHeight);
                if (generation.get() == startGeneration) {
                    scaled.put(key, result);
                }
            } catch (Exception e) {
                log.debug("Failed to scale tile image", e);
            } finally {
                inFlight.remove(key);
            }
        });
    }

    private String resolve(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }

        // Relative path: resolve against the configured API base URL
        String base = config.apiBaseUrl();
        if (base == null || base.isEmpty()) {
            return null;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (trimmed.startsWith("/") ? "" : "/") + trimmed;
    }

    private void markFailed(String url) {
        failedAt.put(url, System.currentTimeMillis());
    }

    private static byte[] readCapped(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > MAX_DOWNLOAD_BYTES) {
                return null;
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static BufferedImage downscale(BufferedImage image) {
        int longest = Math.max(image.getWidth(), image.getHeight());
        if (longest <= MAX_SOURCE_EDGE) {
            return image;
        }
        return fit(image, MAX_SOURCE_EDGE, MAX_SOURCE_EDGE);
    }

    static BufferedImage fit(BufferedImage image, int maxWidth, int maxHeight) {
        double ratio = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
        int width = Math.max(1, (int) Math.round(image.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(image.getHeight() * ratio));
        return ImageUtil.resizeImage(image, width, height);
    }

    private static <V> Map<String, V> lruMap(int maxEntries) {
        return new LinkedHashMap<String, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > maxEntries;
            }
        };
    }
}
