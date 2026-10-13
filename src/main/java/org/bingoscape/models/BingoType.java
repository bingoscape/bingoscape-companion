package org.bingoscape.models;

import com.google.gson.annotations.SerializedName;

/**
 * The kind of board a bingo is. Unknown or missing values are treated as STANDARD.
 */
public enum BingoType {
    @SerializedName("standard")
    STANDARD,
    @SerializedName("progression")
    PROGRESSION,
    @SerializedName("battleship")
    BATTLESHIP
}
