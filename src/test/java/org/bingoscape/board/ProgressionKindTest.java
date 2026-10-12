package org.bingoscape.board;

import org.bingoscape.BingoScapeConfig;
import org.bingoscape.ContractFixtures;
import org.bingoscape.LockedTileDisplay;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.bingoscape.models.ProgressionMetadata;
import org.bingoscape.models.TierState;
import org.bingoscape.models.Tile;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ProgressionKindTest {
    private static TierState state(int tier, boolean unlocked, long earned, long required) {
        TierState state = new TierState();
        state.setTier(tier);
        state.setUnlocked(unlocked);
        state.setEarnedXp(earned);
        state.setXpRequired(required);
        return state;
    }

    /**
     * One band per entry of {@code perTier}; the last tier is locked and made of stubs.
     */
    private static Bingo board(int columns, int... perTier) {
        List<Tile> tiles = new ArrayList<>();
        List<TierState> states = new ArrayList<>();
        int index = 0;
        for (int tier = 0; tier < perTier.length; tier++) {
            boolean locked = tier == perTier.length - 1;
            states.add(state(tier, !locked, locked ? 0 : 3, 5));
            for (int i = 0; i < perTier[tier]; i++) {
                Tile tile = new Tile();
                tile.setId(UUID.randomUUID());
                tile.setIndex(index++);
                tile.setTier(tier);
                tile.setTierLocked(locked);
                if (!locked) {
                    tile.setTitle("Tile " + index);
                    tile.setWeight(5);
                }
                tiles.add(tile);
            }
        }

        Bingo bingo = new Bingo();
        bingo.setBingoType(BingoType.PROGRESSION);
        bingo.setColumns(columns);
        bingo.setRows(3);
        bingo.setTiles(tiles);
        ProgressionMetadata progression = new ProgressionMetadata();
        progression.setTiers(states);
        bingo.setProgression(progression);
        return bingo;
    }

    private static BingoScapeConfig configWith(LockedTileDisplay display) {
        return (BingoScapeConfig) Proxy.newProxyInstance(BingoScapeConfig.class.getClassLoader(),
                new Class<?>[]{BingoScapeConfig.class},
                (proxy, method, args) -> method.getName().equals("progressionShowLockedTiles") ? display : null);
    }

    @Test
    public void progressionTypeSelectsProgressionKind() {
        assertSame(ProgressionKind.SHOW_LOCKED, BoardKind.forType(BingoType.PROGRESSION));
        assertEquals(BingoType.PROGRESSION, ProgressionKind.SHOW_LOCKED.type());
        assertSame(ProgressionKind.SHOW_LOCKED, BoardModel.of(board(3, 2, 2, 2)).kind);
        assertSame(StandardKind.INSTANCE, BoardKind.forType(BingoType.STANDARD));
    }

    @Test
    public void configPicksTheLockedTileMode() {
        BoardKind kind = BoardKind.forType(BingoType.PROGRESSION);

        assertSame(ProgressionKind.SHOW_LOCKED, kind.withOptions(configWith(LockedTileDisplay.DIM)));
        assertSame(ProgressionKind.HIDE_LOCKED, kind.withOptions(configWith(LockedTileDisplay.HIDE)));
        // Other kinds ignore options
        assertSame(StandardKind.INSTANCE, StandardKind.INSTANCE.withOptions(configWith(LockedTileDisplay.HIDE)));
    }

    @Test
    public void bandsStackInTierOrderWithHeadersAboveTheirTiles() {
        Bingo bingo = board(3, 3, 2, 4);
        BoardModel model = BoardModel.of(bingo);

        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, true);

        assertEquals(3, layout.sections.size());
        assertEquals(9, layout.tiles.size());
        assertTrue(layout.legend.y >= layout.codephrase.y + layout.codephrase.height);
        assertTrue(layout.legend.y + layout.legend.height <= layout.gridArea.y);

        // Each header sits right above its band's first tile and below the previous band's last tile
        int[] first = {0, 3, 5};
        int[] last = {2, 4, 8};
        for (int b = 0; b < 3; b++) {
            Rectangle header = layout.sections.get(b);
            assertEquals(layout.gridArea.width, header.width);
            assertEquals(header.y + header.height, layout.tiles.get(first[b]).y);
            if (b > 0) {
                Rectangle above = layout.tiles.get(last[b - 1]);
                assertTrue(header.y >= above.y + above.height);
            }
        }
        // Row-major inside a band; the band of 4 wraps after 3 columns
        assertEquals(layout.tiles.get(5).x, layout.tiles.get(8).x);
        assertTrue(layout.tiles.get(8).y > layout.tiles.get(5).y);
        assertEquals(layout.tiles.get(5).y, layout.tiles.get(7).y);
    }

    @Test
    public void tilesStayInsideTheGridAndNeverOverlap() {
        Bingo bingo = board(4, 5, 4, 7);
        BoardModel model = BoardModel.of(bingo);
        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, false);

        for (int i = 0; i < layout.tiles.size(); i++) {
            Rectangle a = layout.tiles.get(i);
            assertTrue("tile " + i, layout.gridArea.contains(a));
            for (Rectangle header : layout.sections) {
                assertFalse(a.intersects(header));
            }
            for (int j = i + 1; j < layout.tiles.size(); j++) {
                assertFalse(i + " overlaps " + j, a.intersects(layout.tiles.get(j)));
            }
        }
        // The grid area ends exactly where the padding starts
        assertEquals(layout.gridArea.y + layout.gridArea.height + BoardLayout.PAD, layout.height);
    }

    @Test
    public void everyTileIsHitAtItsCenter() {
        Bingo bingo = board(3, 3, 2, 4);
        BoardModel model = BoardModel.of(bingo);
        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, false);

        for (int i = 0; i < layout.tiles.size(); i++) {
            Rectangle r = layout.tiles.get(i);
            assertEquals(i, layout.tileIndexAt(r.x + r.width / 2, r.y + r.height / 2));
        }
        // A band header is not a tile
        Rectangle header = layout.sections.get(1);
        assertEquals(-1, layout.tileIndexAt(header.x + 3, header.y + 3));
    }

    @Test
    public void layoutShrinksTilesToTheViewportLikeTheGrid() {
        Bingo bingo = board(5, 5, 5, 5, 5);
        BoardModel model = BoardModel.of(bingo);

        BoardLayout fixed = model.kind.layout(model, 765, 503, 1.0, true);
        BoardLayout big = model.kind.layout(model, 2560, 1440, 1.0, true);

        assertTrue(fixed.tileSize < big.tileSize);
        assertTrue(fixed.tileSize >= BoardLayout.TILE_MIN);
        assertEquals(BoardLayout.TILE_DEFAULT, big.tileSize);
        assertTrue(big.height <= 1440 * BoardLayout.VIEWPORT_FILL);
    }

    @Test
    public void hiddenLockedBandsCollapseToTheirHeader() {
        Bingo bingo = board(3, 3, 2, 4);
        BoardModel dim = BoardModel.of(bingo, ProgressionKind.SHOW_LOCKED);
        BoardModel hide = BoardModel.of(bingo, ProgressionKind.HIDE_LOCKED);

        BoardLayout shown = dim.kind.layout(dim, 1920, 1080, 1.0, false);
        BoardLayout collapsed = hide.kind.layout(hide, 1920, 1080, 1.0, false);

        assertEquals(3, collapsed.sections.size());
        assertTrue(collapsed.height < shown.height);
        for (int i = 5; i < 9; i++) {
            Rectangle r = collapsed.tiles.get(i);
            assertEquals(0, r.width * r.height);
            assertEquals(-1, collapsed.tileIndexAt(r.x, r.y));
            assertFalse(hide.kind.canOpen(hide.views[i]));
        }
        assertTrue(hide.kind.canOpen(hide.views[0]));
        assertTrue(dim.kind.canOpen(dim.views[5]));
    }

    @Test
    public void boardWithoutTilesFallsBackToThePlainGrid() {
        Bingo bingo = new Bingo();
        bingo.setBingoType(BingoType.PROGRESSION);
        bingo.setRows(3);
        bingo.setColumns(3);
        BoardModel model = BoardModel.of(bingo);

        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, false);

        assertTrue(layout.sections.isEmpty());
        assertNull(layout.legend);
        assertEquals(9, layout.tiles.size());
    }

    @Test
    public void lockedStubFixtureLaysOutAndPaintsWithoutNulls() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        BoardModel model = BoardModel.of(bingo);
        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, false);

        assertEquals(2, layout.sections.size());
        assertEquals(3, layout.tiles.size());

        BufferedImage image = new BufferedImage(layout.width, layout.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            TilePainter painter = new TilePainter(null);
            painter.beginFrame(false, layout.tileSize);
            for (int i = 0; i < model.views.length; i++) {
                model.kind.paintTile(g, layout.tiles.get(i), model.views[i], i == 1, painter);
            }
            model.kind.paintHeaderExtras(g, layout, model);
        } finally {
            g.dispose();
        }
    }

    @Test
    public void lockedStubDrawsALockOverAGrayFace() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        BoardModel model = BoardModel.of(bingo);

        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            model.kind.paintTile(g, new Rectangle(0, 0, 96, 96), model.views[1], false, new TilePainter(null));
        } finally {
            g.dispose();
        }

        // Face is opaque and gray, never a status color
        int corner = image.getRGB(3, 3);
        assertEquals(0xFF, corner >>> 24);
        assertEquals((corner >> 16) & 0xFF, corner & 0xFF);
        // The lock glyph adds light pixels in the middle
        int light = 0;
        for (int y = 30; y < 66; y++) {
            for (int x = 30; x < 66; x++) {
                if ((image.getRGB(x, y) & 0xFF) > 120) {
                    light++;
                }
            }
        }
        assertTrue("lock glyph pixels: " + light, light > 100);
    }

    @Test
    public void hiddenLockedTilesPaintNothing() {
        Bingo bingo = ContractFixtures.bingo("progression-locked-stubs");
        BoardModel model = BoardModel.of(bingo, ProgressionKind.HIDE_LOCKED);

        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            model.kind.paintTile(g, new Rectangle(0, 0, 96, 96), model.views[1], false, new TilePainter(null));
        } finally {
            g.dispose();
        }
        assertEquals(0, image.getRGB(48, 48));
    }

    @Test
    public void lockGlyphIsCachedPerSize() {
        BufferedImage first = Glyphs.lock(24);

        assertSame(first, Glyphs.lock(24));
        assertEquals(24, first.getWidth());
        assertEquals(24, first.getHeight());
        assertEquals(8, Glyphs.lock(2).getWidth());
        // The body of the padlock is opaque
        assertEquals(0xFF, first.getRGB(12, 18) >>> 24);
    }
}
