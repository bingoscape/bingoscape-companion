package org.bingoscape.utils;

import org.bingoscape.BingoScapeConfig;
import org.bingoscape.models.EventData;

import java.util.Date;
import java.util.function.Predicate;

/**
 * Utility class for filtering events based on configuration settings.
 * Provides predicates for removing events that should be hidden.
 */
public class EventFilter {

    private final BingoScapeConfig config;
    private final Date now;

    /**
     * Creates a new event filter using the current date/time.
     *
     * @param config The configuration containing filter settings
     */
    public EventFilter(BingoScapeConfig config) {
        this(config, new Date());
    }

    /**
     * Creates a new event filter using a specific reference date.
     * Useful for testing with fixed dates.
     *
     * @param config The configuration containing filter settings
     * @param referenceDate The date to use for "now" comparisons
     */
    public EventFilter(BingoScapeConfig config, Date referenceDate) {
        this.config = config;
        this.now = referenceDate;
    }

    /**
     * Creates a predicate that returns true for events that should be removed.
     * Combines all configured filters (past events, locked events, upcoming events).
     *
     * @return A predicate for use with removeIf()
     */
    public Predicate<EventData> createRemovalPredicate() {
        return event -> shouldRemovePastEvent(event)
                     || shouldRemoveLockedEvent(event)
                     || shouldRemoveUpcomingEvent(event);
    }

    /**
     * Checks if a past event should be removed based on configuration.
     *
     * @param event The event to check
     * @return true if the event is past and hidePastEvents is enabled
     */
    private boolean shouldRemovePastEvent(EventData event) {
        return config.hidePastEvents() && event.getEndDate().before(now);
    }

    /**
     * Checks if a locked event should be removed based on configuration.
     *
     * @param event The event to check
     * @return true if the event is locked and hideLockedEvents is enabled
     */
    private boolean shouldRemoveLockedEvent(EventData event) {
        return config.hideLockedEvents() && event.isLocked();
    }

    /**
     * Checks if an upcoming event should be removed based on configuration.
     *
     * @param event The event to check
     * @return true if the event is upcoming and hideUpcomingEvents is enabled
     */
    private boolean shouldRemoveUpcomingEvent(EventData event) {
        return config.hideUpcomingEvents() && event.getStartDate().after(now);
    }
}
