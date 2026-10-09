package org.bingoscape.models;

/**
 * Error from a BingoScape API call, with the HTTP status so callers don't have to parse messages.
 */
public class ApiError {
    public static final int HTTP_LOCKED = 423;
    /** Status used when no HTTP response was received (network failure, missing API key). */
    public static final int NO_RESPONSE = 0;

    private final int statusCode;
    private final String message;
    private final long retryAfterSeconds;

    public ApiError(int statusCode, String message) {
        this(statusCode, message, -1);
    }

    /** @param retryAfterSeconds value of the Retry-After header, or -1 when absent */
    public ApiError(int statusCode, String message, long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
        this.statusCode = statusCode;
        this.message = message != null ? message : "HTTP " + statusCode + " Error";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public boolean isLocked() {
        return statusCode == HTTP_LOCKED;
    }

    @Override
    public String toString() {
        return message;
    }
}
