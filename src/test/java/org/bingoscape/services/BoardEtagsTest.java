package org.bingoscape.services;

import org.bingoscape.models.Bingo;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BoardEtagsTest {
    private static final UUID ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID OTHER = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final String SCOPE = BoardEtags.scopeOf("https://next.bingoscape.org", "key", "event/Team A");

    @Test
    public void nothingIsSentBeforeTheFirstResponse() {
        assertNull(new BoardEtags().etagFor(ID, SCOPE, new Bingo()));
    }

    @Test
    public void etagIsSentForTheCopyItWasIssuedFor() {
        BoardEtags etags = new BoardEtags();
        Bingo body = new Bingo();
        etags.store(ID, SCOPE, "\"abc\"", body);

        assertEquals("\"abc\"", etags.etagFor(ID, SCOPE, body));
    }

    @Test
    public void aDifferentCopyOfTheSameBoardGetsNoEtag() {
        BoardEtags etags = new BoardEtags();
        etags.store(ID, SCOPE, "\"abc\"", new Bingo());

        // e.g. the board was swapped in by a submission response; an equal-looking copy is still another instance
        assertNull(etags.etagFor(ID, SCOPE, new Bingo()));
        assertNull(etags.etagFor(ID, SCOPE, null));
    }

    @Test
    public void etagsAreKeptPerBingo() {
        BoardEtags etags = new BoardEtags();
        Bingo a = new Bingo();
        Bingo b = new Bingo();
        etags.store(ID, SCOPE, "\"a\"", a);
        etags.store(OTHER, SCOPE, "\"b\"", b);

        assertEquals("\"a\"", etags.etagFor(ID, SCOPE, a));
        assertEquals("\"b\"", etags.etagFor(OTHER, SCOPE, b));
    }

    @Test
    public void aNewerResponseReplacesTheEtag() {
        BoardEtags etags = new BoardEtags();
        Bingo first = new Bingo();
        Bingo second = new Bingo();
        etags.store(ID, SCOPE, "\"1\"", first);
        etags.store(ID, SCOPE, "\"2\"", second);

        assertNull(etags.etagFor(ID, SCOPE, first));
        assertEquals("\"2\"", etags.etagFor(ID, SCOPE, second));
    }

    @Test
    public void aResponseWithoutEtagForgetsTheOldOne() {
        BoardEtags etags = new BoardEtags();
        Bingo body = new Bingo();
        etags.store(ID, SCOPE, "\"abc\"", body);
        etags.store(ID, SCOPE, null, body);

        assertNull(etags.etagFor(ID, SCOPE, body));
    }

    @Test
    public void teamChangeDropsEveryEtag() {
        BoardEtags etags = new BoardEtags();
        Bingo body = new Bingo();
        etags.store(ID, SCOPE, "\"abc\"", body);

        assertNull(etags.etagFor(ID, BoardEtags.scopeOf("https://next.bingoscape.org", "key", "event/Team B"), body));
        // and it stays gone after switching back
        assertNull(etags.etagFor(ID, SCOPE, body));
    }

    @Test
    public void apiKeyOrBaseUrlChangeDropsEveryEtag() {
        Bingo body = new Bingo();

        BoardEtags byKey = new BoardEtags();
        byKey.store(ID, SCOPE, "\"abc\"", body);
        assertNull(byKey.etagFor(ID, BoardEtags.scopeOf("https://next.bingoscape.org", "other", "event/Team A"), body));

        BoardEtags byUrl = new BoardEtags();
        byUrl.store(ID, SCOPE, "\"abc\"", body);
        assertNull(byUrl.etagFor(ID, BoardEtags.scopeOf("http://localhost:3000", "key", "event/Team A"), body));
    }
}
