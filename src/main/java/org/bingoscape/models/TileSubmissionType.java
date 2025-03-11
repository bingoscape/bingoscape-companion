package org.bingoscape.models;

import com.google.gson.annotations.SerializedName;

/**
 * Enum representing the possible status values for a bingo tile
 */
public enum TileSubmissionType {
    @SerializedName("pending")
    PENDING("pending"),
    @SerializedName("accepted")
    ACCEPTED("accepted"),
    @SerializedName("requires_interaction")
    REQUIRES_INTERACTION("requires_interaction"),
    @SerializedName("declined")
    DECLINED("declined"),
    @SerializedName("not_submitted")
    NOT_SUBMITTED("not_submitted");

    private final String value;

    TileSubmissionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // Convert string value to enum
    public static TileSubmissionType fromValue(String value) {
        for (TileSubmissionType type : TileSubmissionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown status type: " + value);
    }
}
