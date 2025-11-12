package org.bingoscape.utils;

import org.bingoscape.models.EventData;

import java.util.Comparator;
import java.util.Date;

/**
 * Comparator for sorting EventData objects with complex business rules.
 *
 * Sorting priority:
 * 1. Active events before locked events
 * 2. Upcoming events before past events (within same lock status)
 * 3. Most recent start date first
 * 4. Alphabetically by title as final tiebreaker
 */
public class EventComparator implements Comparator<EventData> {

    private final Date now;

    /**
     * Creates a new event comparator using the current date/time.
     */
    public EventComparator() {
        this(new Date());
    }

    /**
     * Creates a new event comparator using a specific reference date.
     * Useful for testing with fixed dates.
     *
     * @param referenceDate The date to use for "now" comparisons
     */
    public EventComparator(Date referenceDate) {
        this.now = referenceDate;
    }

    @Override
    public int compare(EventData e1, EventData e2) {
        // Priority 1: Sort by locked status (active events first)
        int lockedComparison = compareByLockedStatus(e1, e2);
        if (lockedComparison != 0) {
            return lockedComparison;
        }

        // Priority 2: Sort by upcoming status (upcoming first)
        int upcomingComparison = compareByUpcomingStatus(e1, e2);
        if (upcomingComparison != 0) {
            return upcomingComparison;
        }

        // Priority 3: Sort by start date (most recent first)
        int dateComparison = compareByStartDate(e1, e2);
        if (dateComparison != 0) {
            return dateComparison;
        }

        // Priority 4: Sort alphabetically by title
        return compareByTitle(e1, e2);
    }

    /**
     * Compares events by locked status.
     * Active (not locked) events come before locked events.
     *
     * @return -1 if e1 is active and e2 is locked, 1 if opposite, 0 if same
     */
    private int compareByLockedStatus(EventData e1, EventData e2) {
        if (e1.isLocked() != e2.isLocked()) {
            return e1.isLocked() ? 1 : -1;
        }
        return 0;
    }

    /**
     * Compares events by upcoming status.
     * Upcoming events come before past/current events.
     *
     * @return -1 if e1 is upcoming and e2 is not, 1 if opposite, 0 if same
     */
    private int compareByUpcomingStatus(EventData e1, EventData e2) {
        boolean e1Upcoming = e1.getStartDate().after(now);
        boolean e2Upcoming = e2.getStartDate().after(now);

        if (e1Upcoming != e2Upcoming) {
            return e1Upcoming ? -1 : 1;
        }
        return 0;
    }

    /**
     * Compares events by start date.
     * More recent start dates come first (descending order).
     *
     * @return Negative if e1 is more recent, positive if e2 is more recent, 0 if equal
     */
    private int compareByStartDate(EventData e1, EventData e2) {
        return e2.getStartDate().compareTo(e1.getStartDate());
    }

    /**
     * Compares events alphabetically by title (case-insensitive).
     *
     * @return Standard string comparison result
     */
    private int compareByTitle(EventData e1, EventData e2) {
        return e1.getTitle().compareToIgnoreCase(e2.getTitle());
    }
}
