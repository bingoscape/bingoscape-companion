package org.bingoscape.models;

import lombok.Data;

/**
 * A fleet rule of a battleship board: how many ships of a given length each team places.
 */
@Data
public class ShipRule {
    private int length;
    private int count;
}
