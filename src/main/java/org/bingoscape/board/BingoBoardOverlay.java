package org.bingoscape.board;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ImageUtil;
import org.bingoscape.BingoScapeConfig;
import org.bingoscape.BingoScapePlugin;
import org.bingoscape.BoardDisplayMode;
import org.bingoscape.models.Bingo;
import org.bingoscape.models.Tile;
import org.bingoscape.models.TileSubmissionType;
import org.bingoscape.services.TileImageCache;
import org.bingoscape.ui.TileStatusStyle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-game bingo board. Non-modal: it only takes input inside its own bounds (see {@link BoardInputListener}),
 * and can be moved with Alt-drag like any RuneLite overlay.
 */
@Singleton
public class BingoBoardOverlay extends Overlay {
    private static final int MIN_DETAIL_TILE_SIZE = 56;
    private static final int ICON_SIZE = 16;
    private static final long RELOAD_TIMEOUT_MS = 10_000;
    private static final String MENU_TARGET = "Bingo board";

    private final Client client;
    private final BingoScapePlugin plugin;
    private final BingoScapeConfig config;
    private final BoardState state;
    private final TileImageCache images;
    private final TooltipManager tooltipManager;
    private final ItemManager itemManager;
    private final BufferedImage refreshIcon;

    // Render-thread only
    private Bingo lastBingo;
    private BoardModel model;
    private final Map<String, List<String>> wrapMemo = new HashMap<>();
    private int lastTileSize = -1;

    private volatile HitMap hitMap;

    @Inject
    public BingoBoardOverlay(Client client, BingoScapePlugin plugin, BingoScapeConfig config, BoardState state,
                             TileImageCache images, TooltipManager tooltipManager, ItemManager itemManager) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.state = state;
        this.images = images;
        this.tooltipManager = tooltipManager;
        this.itemManager = itemManager;
        this.refreshIcon = ImageUtil.loadImageResource(BingoScapePlugin.class, "/refresh_icon.png");

        setPosition(OverlayPosition.DETACHED);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
        setMovable(true);
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Close", MENU_TARGET, e -> plugin.hideBoard());
    }

    /**
     * Click regions from the last rendered frame, or null when the board is not drawn.
     */
    HitMap hitMap() {
        return hitMap;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Bingo bingo = plugin.getCurrentBingo();
        if (!state.isVisible() || state.isCaptureSuppressed()
                || config.boardDisplayMode() != BoardDisplayMode.OVERLAY || bingo == null) {
            hitMap = null;
            return null;
        }

        if (bingo != lastBingo) {
            rebuildModel(bingo);
        }

        if (state.isReloading() && System.currentTimeMillis() - state.getReloadStartedAtMs() > RELOAD_TIMEOUT_MS) {
            state.finishReload();
        }

        boolean hasCodephrase = bingo.getCodephrase() != null && !bingo.getCodephrase().trim().isEmpty();
        BoardLayout layout = BoardLayout.compute(bingo.getRows(), bingo.getColumns(), model.tiles.size(),
                client.getCanvasWidth(), client.getCanvasHeight(), config.boardScale() / 100.0, hasCodephrase);

        if (getPreferredLocation() == null) {
            setPreferredLocation(new Point(
                    Math.max(0, (client.getCanvasWidth() - layout.width) / 2),
                    Math.max(0, (client.getCanvasHeight() - layout.height) / 2)));
        }

        // Wrapped titles depend on tile size; drop them when it changes so the memo can't grow on resize
        if (layout.tileSize != lastTileSize) {
            wrapMemo.clear();
            lastTileSize = layout.tileSize;
        }

        // Copy the origin once per frame; the listener reads it from the hit map instead of getBounds()
        Point origin = getBounds().getLocation();
        Point mouse = localMouse(layout, origin);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            paintBackground(g, layout);
            BoardLayout.Button hoveredButton = mouse == null ? BoardLayout.Button.NONE : layout.buttonAt(mouse.x, mouse.y);
            paintHeader(g, layout, bingo, hoveredButton);

            Tile detailsTile = detailsTile();
            TileDetailsView.DetailsHit details = null;
            int hoveredTile = -1;
            if (detailsTile != null) {
                details = TileDetailsView.paint(g, layout.gridArea, detailsTile, bingo,
                        config.boardShowTileImages() ? images : null, itemManager::getImage,
                        state.getDetailsScrollPx(), mouse);
                if (state.getDetailsScrollPx() > details.maxScroll) {
                    state.setDetailsScrollPx(details.maxScroll);
                }
            } else {
                hoveredTile = mouse == null ? -1 : layout.tileIndexAt(mouse.x, mouse.y);
                paintGrid(g, layout, hoveredTile);
            }

            addTooltips(hoveredButton, hoveredTile, bingo);
            hitMap = new HitMap(origin, layout, model.tiles, detailsTile, details);
        } finally {
            g.dispose();
        }

        return new Dimension(layout.width, layout.height);
    }

    private void rebuildModel(Bingo bingo) {
        model = BoardModel.of(bingo);
        lastBingo = bingo;
        wrapMemo.clear();

        if (config.boardShowTileImages()) {
            List<String> urls = new ArrayList<>();
            for (Tile tile : model.tiles) {
                if (!tile.isHidden() && tile.getHeaderImage() != null && !tile.getHeaderImage().trim().isEmpty()) {
                    urls.add(tile.getHeaderImage());
                }
            }
            images.prefetch(urls);
        }
    }

    private Tile detailsTile() {
        UUID id = state.getDetailsTileId();
        if (id == null) {
            return null;
        }

        Tile tile = model.byId.get(id);
        if (tile == null || tile.isHidden()) {
            // Tile vanished after a refresh or bingo switch
            state.closeDetails();
            return null;
        }
        return tile;
    }

    /**
     * Mouse position relative to the overlay origin, or null when the mouse is outside the canvas.
     */
    private Point localMouse(BoardLayout layout, Point origin) {
        net.runelite.api.Point canvasMouse = client.getMouseCanvasPosition();
        Point mouse = canvasMouse != null && canvasMouse.getX() >= 0
                ? new Point(canvasMouse.getX(), canvasMouse.getY())
                : state.getLastMouse();
        if (mouse == null) {
            return null;
        }

        Point local = new Point(mouse.x - origin.x, mouse.y - origin.y);
        return layout.contains(local.x, local.y) ? local : null;
    }

    private void paintBackground(Graphics2D g, BoardLayout layout) {
        int alpha = Math.round(config.boardOpacity() * 255 / 100f);
        Graphics2D bg = (Graphics2D) g.create();
        try {
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setColor(BoardDraw.withAlpha(ColorScheme.DARK_GRAY_COLOR, alpha));
            bg.fillRoundRect(0, 0, layout.width, layout.height, 8, 8);
            bg.setColor(ColorScheme.BORDER_COLOR);
            bg.drawRoundRect(0, 0, layout.width - 1, layout.height - 1, 8, 8);

            Rectangle grid = layout.gridArea;
            bg.setColor(BoardDraw.withAlpha(ColorScheme.DARKER_GRAY_COLOR, alpha));
            bg.fillRect(grid.x - 2, grid.y - 2, grid.width + 4, grid.height + 4);
        } finally {
            bg.dispose();
        }
    }

    private void paintHeader(Graphics2D g, BoardLayout layout, Bingo bingo, BoardLayout.Button hovered) {
        // Title, with a gray [Locked] suffix like the side panel
        g.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics fm = g.getFontMetrics();
        String suffix = bingo.isLocked() ? " [Locked]" : "";
        int suffixWidth = fm.stringWidth(suffix);
        String title = TextWrap.ellipsize(bingo.getTitle(), fm, Math.max(0, layout.title.width - suffixWidth));
        int baseline = layout.title.y + (layout.title.height - fm.getHeight()) / 2 + fm.getAscent();
        BoardDraw.text(g, title, layout.title.x, baseline, Color.WHITE);
        if (!suffix.isEmpty()) {
            BoardDraw.text(g, suffix, layout.title.x + fm.stringWidth(title), baseline, Color.GRAY);
        }

        if (layout.codephrase != null) {
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics small = g.getFontMetrics();
            String codephrase = TextWrap.ellipsize("Codephrase: " + bingo.getCodephrase().trim(), small, layout.codephrase.width);
            BoardDraw.text(g, codephrase, layout.codephrase.x, layout.codephrase.y + small.getAscent(), BoardTooltipBuilder.GOLD);
        }

        // Pin
        g.setFont(FontManager.getRunescapeSmallFont());
        BoardDraw.button(g, layout.pinButton, hovered == BoardLayout.Button.PIN, true);
        BoardDraw.centeredText(g, isPinned(bingo) ? "Unpin" : "Pin", layout.pinButton, Color.WHITE);

        // Reload, dimmed while a reload is running
        boolean reloading = state.isReloading();
        BoardDraw.button(g, layout.reloadButton, hovered == BoardLayout.Button.RELOAD, !reloading);
        if (refreshIcon != null) {
            Rectangle r = layout.reloadButton;
            Graphics2D ig = (Graphics2D) g.create();
            try {
                ig.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, reloading ? 0.35f : 1f));
                ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                ig.drawImage(refreshIcon, r.x + (r.width - ICON_SIZE) / 2, r.y + (r.height - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, null);
            } finally {
                ig.dispose();
            }
        }

        // Close
        BoardDraw.button(g, layout.closeButton, hovered == BoardLayout.Button.CLOSE, true);
        BoardDraw.closeIcon(g, layout.closeButton, hovered == BoardLayout.Button.CLOSE);
    }

    private void paintGrid(Graphics2D g, BoardLayout layout, int hoveredTile) {
        for (int i = 0; i < layout.tiles.size() && i < model.tiles.size(); i++) {
            Rectangle r = layout.tiles.get(i);
            Tile tile = model.tiles.get(i);
            Graphics2D tg = (Graphics2D) g.create();
            try {
                tg.clipRect(r.x, r.y, r.width, r.height);
                if (tile.isHidden()) {
                    paintHiddenTile(tg, r);
                } else {
                    paintTile(tg, r, tile, i == hoveredTile);
                }
            } finally {
                tg.dispose();
            }
        }
    }

    private void paintHiddenTile(Graphics2D g, Rectangle r) {
        g.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(ColorScheme.BORDER_COLOR);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FontManager.getDefaultBoldFont().deriveFont(Font.BOLD, 24f));
        BoardDraw.centeredText(g, "?", r, Color.GRAY);
    }

    private void paintTile(Graphics2D g, Rectangle r, Tile tile, boolean hovered) {
        TileSubmissionType status = TileStatusStyle.statusOf(tile);
        Color background = TileStatusStyle.background(status);
        g.setColor(hovered ? background.brighter() : background);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(TileStatusStyle.border(status));
        g.setStroke(new BasicStroke(2));
        g.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);

        Rectangle inner = new Rectangle(r.x + 4, r.y + 4, r.width - 8, r.height - 8);
        boolean showDetails = r.width >= MIN_DETAIL_TILE_SIZE;
        int top = inner.y;
        int bottom = inner.y + inner.height;

        if (showDetails) {
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fm = g.getFontMetrics();
            String xp = tile.getWeight() + " XP";
            BoardDraw.text(g, xp, inner.x + inner.width - fm.stringWidth(xp), inner.y + fm.getAscent(), BoardTooltipBuilder.GOLD);
            top += fm.getHeight();

            String label = TileStatusStyle.shortLabel(status);
            if (label != null) {
                g.setFont(FontManager.getRunescapeBoldFont());
                FontMetrics bold = g.getFontMetrics();
                String fitted = TextWrap.ellipsize(label, bold, inner.width);
                bottom -= bold.getHeight();
                BoardDraw.text(g, fitted, inner.x + (inner.width - bold.stringWidth(fitted)) / 2,
                        bottom + bold.getAscent(), TileStatusStyle.border(status));
            }
        }

        Rectangle content = new Rectangle(inner.x, top, inner.width, Math.max(0, bottom - top));
        paintTileContent(g, content, tile);
    }

    private void paintTileContent(Graphics2D g, Rectangle box, Tile tile) {
        String url = tile.getHeaderImage();
        if (config.boardShowTileImages() && url != null && !url.trim().isEmpty() && !images.hasFailed(url)) {
            BufferedImage image = images.getScaled(url, box.width, box.height);
            if (image != null) {
                g.drawImage(image, box.x + (box.width - image.getWidth()) / 2, box.y + (box.height - image.getHeight()) / 2, null);
                return;
            }

            // Scaled copy not ready yet (first frame or after a resize): draw the source scaled on the fly
            BufferedImage source = images.getSource(url);
            if (source != null) {
                Rectangle target = BoardDraw.fitted(source.getWidth(), source.getHeight(), box);
                Graphics2D ig = (Graphics2D) g.create();
                try {
                    ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    ig.drawImage(source, target.x, target.y, target.width, target.height, null);
                } finally {
                    ig.dispose();
                }
                return;
            }

            g.setFont(FontManager.getRunescapeSmallFont());
            BoardDraw.centeredText(g, "Loading...", box, Color.LIGHT_GRAY);
            return;
        }

        paintTileTitle(g, box, tile);
    }

    private void paintTileTitle(Graphics2D g, Rectangle box, Tile tile) {
        g.setFont(TextWrap.displayable(FontManager.getRunescapeFont(), tile.getTitle()));
        FontMetrics fm = g.getFontMetrics();
        int maxLines = Math.max(1, box.height / fm.getHeight());
        // Keyed by text, not tile id: ids may be null, and equal titles wrap identically
        String key = tile.getTitle() + "|" + box.width + "|" + maxLines;
        List<String> lines = wrapMemo.computeIfAbsent(key, k -> TextWrap.wrap(tile.getTitle(), fm, box.width, maxLines));

        int y = box.y + (box.height - lines.size() * fm.getHeight()) / 2;
        for (String line : lines) {
            BoardDraw.text(g, line, box.x + (box.width - fm.stringWidth(line)) / 2, y + fm.getAscent(), Color.WHITE);
            y += fm.getHeight();
        }
    }

    private void addTooltips(BoardLayout.Button hoveredButton, int hoveredTile, Bingo bingo) {
        switch (hoveredButton) {
            case PIN:
                tooltipManager.add(new Tooltip(isPinned(bingo) ? "Unpin board" : "Pin board"));
                return;
            case RELOAD:
                tooltipManager.add(new Tooltip(state.isReloading() ? "Reloading..." : "Reload board"));
                return;
            case CLOSE:
                tooltipManager.add(new Tooltip("Close board"));
                return;
            default:
                break;
        }

        if (config.boardShowTooltips() && hoveredTile >= 0 && hoveredTile < model.tiles.size()) {
            Tile tile = model.tiles.get(hoveredTile);
            tooltipManager.add(tile.isHidden()
                    ? new Tooltip(BoardTooltipBuilder.build(tile))
                    : new Tooltip(new TileHoverCard(tile, itemManager::getImage)));
        }
    }

    private boolean isPinned(Bingo bingo) {
        return bingo.getId() != null && bingo.getId().toString().equals(config.pinnedBingoId());
    }
}
