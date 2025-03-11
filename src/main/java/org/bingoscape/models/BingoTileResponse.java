package org.bingoscape.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;
import java.util.UUID;

/**
 * Response class for the bingo tiles API endpoint
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
