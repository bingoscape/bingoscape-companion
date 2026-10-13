package org.bingoscape.board;

import org.bingoscape.BingoScapeConfig;
import org.bingoscape.ContractFixtures;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.BingoType;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BattleshipKindTest {
    private static BingoScapeConfig configWith(boolean showMarkers) {
        return (BingoScapeConfig) Proxy.newProxyInstance(BingoScapeConfig.class.getClassLoader(),
                new Class<?>[]{BingoScapeConfig.class},
                (proxy, method, args) -> method.getName().equals("battleshipShowMarkers") ? showMarkers : null);
    }

    private static BufferedImage paint(BoardModel model, int index, boolean hovered) {
        BufferedImage image = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            TilePainter painter = new TilePainter(null);
            painter.beginFrame(false, 96);
            model.kind.paintTile(g, new Rectangle(0, 0, 96, 96), model.views[index], hovered, painter);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static boolean differs(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void battleshipTypeSelectsBattleshipKind() {
        assertSame(BattleshipKind.SHOW_MARKERS, BoardKind.forType(BingoType.BATTLESHIP));
        assertEquals(BingoType.BATTLESHIP, BattleshipKind.SHOW_MARKERS.type());
        assertSame(BattleshipKind.SHOW_MARKERS, BoardModel.of(ContractFixtures.bingo("battleship-started")).kind);
    }

    @Test
    public void configPicksTheMarkerMode() {
        BoardKind kind = BoardKind.forType(BingoType.BATTLESHIP);

        assertSame(BattleshipKind.SHOW_MARKERS, kind.withOptions(configWith(true)));
        assertSame(BattleshipKind.HIDE_MARKERS, kind.withOptions(configWith(false)));
    }

    @Test
    public void hiddenMarkersLeaveTheViewsUnmarked() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        BoardModel hidden = BoardModel.of(bingo, BattleshipKind.HIDE_MARKERS);

        for (TileView view : hidden.views) {
            assertEquals(BattleshipMarks.Mark.NONE, view.mark);
        }
        assertNull(BattleshipKind.tooltipText(hidden.views[0]));
        // The ships line does not depend on the markers
        assertEquals("Ships remaining: 2/3", ((BattleshipMarks) hidden.data).legend);
    }

    @Test
    public void legendLineSitsBetweenHeaderAndGrid() {
        BoardModel model = BoardModel.of(ContractFixtures.bingo("battleship-started"));
        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, true);

        assertNotNull(layout.legend);
        assertTrue(layout.sections.isEmpty());
        assertEquals(layout.codephrase.y + layout.codephrase.height + BoardLayout.HEADER_SPACING, layout.legend.y);
        assertEquals(layout.legend.y + BoardLayout.LEGEND_HEIGHT + BoardLayout.LEGEND_SPACING, layout.gridArea.y);
        assertEquals(layout.gridArea.y + layout.gridArea.height + BoardLayout.PAD, layout.height);
    }

    @Test
    public void layoutMatchesTheStandardGridPlusTheLegendBlock() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        BoardModel model = BoardModel.of(bingo);
        BoardLayout layout = model.kind.layout(model, 2560, 1440, 1.0, false);
        BoardLayout plain = StandardKind.INSTANCE.layout(model, 2560, 1440, 1.0, false);

        assertEquals(plain.tileSize, layout.tileSize);
        assertEquals(plain.tiles.size(), layout.tiles.size());
        assertEquals(plain.height + BoardLayout.LEGEND_HEIGHT + BoardLayout.LEGEND_SPACING, layout.height);
        assertNull(plain.legend);
    }

    @Test
    public void everyTileIsHitAtItsCenter() {
        BoardModel model = BoardModel.of(ContractFixtures.bingo("battleship-started"));
        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, false);

        for (int i = 0; i < model.views.length; i++) {
            Rectangle r = layout.tiles.get(i);
            assertEquals(i, layout.tileIndexAt(r.x + r.width / 2, r.y + r.height / 2));
            assertTrue(layout.gridArea.contains(r));
        }
    }

    @Test
    public void blindTilesCannotBeOpenedOrSubmitted() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        BoardModel model = BoardModel.of(bingo);

        for (TileView view : model.views) {
            assertFalse(model.kind.canOpen(view));
            assertFalse(model.kind.canSubmit(view, bingo));
            assertEquals("Blind tile", model.kind.primaryLabel(view, bingo));
            assertEquals(BattleshipKind.BLIND_NOTICE, model.kind.detailsNotice(view));
        }
    }

    @Test
    public void nothingIsSubmittableBeforeTheStart() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        bingo.getTiles().get(0).setBlind(false);
        BoardModel model = BoardModel.of(bingo);
        TileView view = model.views[0];

        assertTrue(model.kind.canOpen(view));
        assertFalse(model.kind.canSubmit(view, bingo));
        assertEquals("Not started", model.kind.primaryLabel(view, bingo));
        assertEquals(BattleshipKind.NOT_STARTED_NOTICE, model.kind.detailsNotice(view));
    }

    @Test
    public void startedBoardOpensAndSubmitsLikeAStandardBoard() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        BoardModel model = BoardModel.of(bingo);

        for (TileView view : model.views) {
            assertTrue(model.kind.canOpen(view));
            assertTrue(model.kind.canSubmit(view, bingo));
            assertEquals(StandardKind.INSTANCE.primaryLabel(view, bingo), model.kind.primaryLabel(view, bingo));
        }
        assertEquals("Shot result: Hit", model.kind.detailsNotice(model.views[0]));
        assertEquals("Shot result: Miss", model.kind.detailsNotice(model.views[1]));
        assertEquals("Shot result: Ship sunk", model.kind.detailsNotice(model.views[2]));

        bingo.setLocked(true);
        assertFalse(model.kind.canSubmit(model.views[0], bingo));
    }

    @Test
    public void missingBattleshipBlockGatesOnlyBlindTiles() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        bingo.setBattleship(null);
        BoardModel model = BoardModel.of(bingo);

        assertTrue(model.kind.canSubmit(model.views[0], bingo));
        assertNull(model.kind.detailsNotice(model.views[0]));
        assertNotNull(model.kind.layout(model, 1920, 1080, 1.0, false).legend);
    }

    @Test
    public void tooltipTextNamesTheMark() {
        BoardModel model = BoardModel.of(ContractFixtures.bingo("battleship-started"));

        assertTrue(BattleshipKind.tooltipText(model.views[0]).endsWith("Hit</col>"));
        assertTrue(BattleshipKind.tooltipText(model.views[0]).startsWith("<col="));
        assertTrue(BattleshipKind.tooltipText(model.views[1]).contains("Miss"));
        assertTrue(BattleshipKind.tooltipText(model.views[2]).contains("Ship sunk"));
        assertTrue(BattleshipKind.tooltipText(model.views[2]).contains("Hydra"));
        for (TileView view : model.views) {
            assertNotNull(model.kind.tooltip(view, id -> null));
        }
    }

    @Test
    public void blindTileTooltipDoesNotLeakContent() {
        BoardModel model = BoardModel.of(ContractFixtures.bingo("battleship-before-start"));

        assertEquals("Tile 1</br>Blind tile", BattleshipKind.tooltipText(model.views[0]));
        assertNotNull(model.kind.tooltip(model.views[0], id -> null));
    }

    @Test
    public void unmarkedTileKeepsTheStandardHoverCard() {
        Bingo bingo = ContractFixtures.bingo("battleship-started");
        bingo.getBattleship().getMarks().getHitTileIds().clear();
        BoardModel model = BoardModel.of(bingo);

        assertNull(BattleshipKind.tooltipText(model.views[0]));
    }

    @Test
    public void marksChangeTheTileFaceByShape() {
        BoardModel marked = BoardModel.of(ContractFixtures.bingo("battleship-started"));
        BoardModel plain = BoardModel.of(ContractFixtures.bingo("battleship-started"), BattleshipKind.HIDE_MARKERS);

        BufferedImage[] faces = new BufferedImage[3];
        for (int i = 0; i < 3; i++) {
            faces[i] = paint(marked, i, false);
            assertTrue("mark " + i + " changes the face", differs(faces[i], paint(plain, i, false)));
        }
        // The three marks are told apart by more than a colour shift
        assertTrue(differs(faces[0], faces[1]));
        assertTrue(differs(faces[1], faces[2]));
        assertTrue(differs(faces[0], faces[2]));
    }

    @Test
    public void blindTileIsDrawnAsAnUnmarkedQuestionCell() {
        Bingo bingo = ContractFixtures.bingo("battleship-before-start");
        BoardModel model = BoardModel.of(bingo);
        BufferedImage image = paint(model, 0, false);
        BufferedImage expected = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = expected.createGraphics();
        try {
            new TilePainter(null).paintHidden(g, new Rectangle(0, 0, 96, 96));
        } finally {
            g.dispose();
        }

        assertFalse(differs(image, expected));
    }

    @Test
    public void startedAndBlindFixturePaintsWithoutNulls() {
        for (String name : new String[]{"battleship-started", "battleship-before-start"}) {
            BoardModel model = BoardModel.of(ContractFixtures.bingo(name));
            BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, true);
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
    }

    @Test
    public void legendAndGlyphsDrawPixels() {
        BoardModel model = BoardModel.of(ContractFixtures.bingo("battleship-started"));
        BoardLayout layout = model.kind.layout(model, 1920, 1080, 1.0, false);
        BufferedImage image = new BufferedImage(layout.width, layout.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            model.kind.paintHeaderExtras(g, layout, model);
        } finally {
            g.dispose();
        }

        boolean drawn = false;
        Rectangle legend = layout.legend;
        for (int y = legend.y; y < legend.y + legend.height && !drawn; y++) {
            for (int x = legend.x; x < legend.x + legend.width; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    drawn = true;
                    break;
                }
            }
        }
        assertTrue(drawn);
    }

    @Test
    public void glyphsAreCachedPerKindAndSize() {
        assertSame(Glyphs.hit(20), Glyphs.hit(20));
        assertSame(Glyphs.miss(20), Glyphs.miss(20));
        assertSame(Glyphs.sunk(20), Glyphs.sunk(20));
        assertNotEquals(Glyphs.hit(20), Glyphs.miss(20));
        // Alternating sizes in one frame (legend and tiles) must not evict each other
        BufferedImage small = Glyphs.sunk(12);
        BufferedImage large = Glyphs.sunk(30);
        assertSame(small, Glyphs.sunk(12));
        assertSame(large, Glyphs.sunk(30));
        assertEquals(30, large.getWidth());
        assertEquals(8, Glyphs.hit(1).getWidth());
    }
}
