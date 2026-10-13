package org.bingoscape.services;

import org.bingoscape.models.Bingo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers the ETag of the last board response per bingo, so a refresh can ask "has it changed?" and the
 * server can answer 304. An ETag is only offered while it still describes the copy the caller holds: it is
 * tied to the exact {@link Bingo} instance parsed from that response, so a board swapped in from anywhere else
 * (a submission response, the event list) makes the next refresh a full one. All of it is dropped when the
 * scope (base URL, API key, team) changes. Pure logic, thread-safe.
 */
final class BoardEtags {
    private static final class Entry {
        final String etag;
        final Bingo body;

        Entry(String etag, Bingo body) {
            this.etag = etag;
            this.body = body;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();
    private String scope = "";

    /**
     * The ETag to send as If-None-Match, or null when there is none or {@code current} is not the copy it
     * was issued for.
     */
    synchronized String etagFor(UUID bingoId, String requestScope, Bingo current) {
        switchScope(requestScope);
        Entry entry = entries.get(bingoId);
        return entry != null && current != null && entry.body == current ? entry.etag : null;
    }

    /**
     * Records the ETag of a 200 response and the bingo parsed from it. A response without an ETag clears the
     * entry, so the next refresh is a full one.
     */
    synchronized void store(UUID bingoId, String requestScope, String etag, Bingo body) {
        switchScope(requestScope);
        if (bingoId == null || etag == null || etag.trim().isEmpty() || body == null) {
            entries.remove(bingoId);
            return;
        }
        entries.put(bingoId, new Entry(etag.trim(), body));
    }

    private void switchScope(String requestScope) {
        String next = requestScope == null ? "" : requestScope;
        if (!next.equals(scope)) {
            entries.clear();
            scope = next;
        }
    }

    /**
     * The scope key of a request: where it goes, who it is for and which team's view it is.
     */
    static String scopeOf(String baseUrl, String apiKey, String teamScope) {
        return baseUrl + "\n" + apiKey + "\n" + (teamScope == null ? "" : teamScope);
    }
}
