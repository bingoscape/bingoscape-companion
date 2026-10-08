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

    public ApiError(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message != null ? message : "HTTP " + statusCode + " Error";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLocked() {
        return statusCode == HTTP_LOCKED;
    }

    @Override
    public String toString() {
        return message;
    }
}
