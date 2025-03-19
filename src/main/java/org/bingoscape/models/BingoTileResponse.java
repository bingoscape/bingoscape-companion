package org.bingoscape.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced BingoTileResponse to include team details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BingoTileResponse {
    private String bingoId;
    private String teamId;
    private String teamName;
    private Map<UUID, TileStatus> tiles;
}
