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
import org.bingoscape.services.TileImageCache;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-game bingo board. Non-modal: it only takes input inside its own bounds (see {@link BoardInputListener}),
 * and can be moved with Alt-drag like any RuneLite overlay.
 */
@Singleton
public class BingoBoardOverlay extends Overlay {
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
    private final TilePainter painter;

    // Render-thread only
    private Bingo lastBingo;
    private BoardModel model;

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
        this.painter = new TilePainter(images);
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
        BoardKind kind = model.kind;
        BoardLayout layout = kind.layout(model, client.getCanvasWidth(), client.getCanvasHeight(),
                config.boardScale() / 100.0, hasCodephrase);

        if (getPreferredLocation() == null) {
            setPreferredLocation(new Point(
                    Math.max(0, (client.getCanvasWidth() - layout.width) / 2),
                    Math.max(0, (client.getCanvasHeight() - layout.height) / 2)));
        }

        painter.beginFrame(config.boardShowTileImages(), layout.tileSize);

        // Copy the origin once per frame; the listener reads it from the hit map instead of getBounds()
        Point origin = getBounds().getLocation();
        Point mouse = localMouse(layout, origin);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            paintBackground(g, layout);
            BoardLayout.Button hoveredButton = mouse == null ? BoardLayout.Button.NONE : layout.buttonAt(mouse.x, mouse.y);
            paintHeader(g, layout, bingo, hoveredButton);
            kind.paintHeaderExtras(g, layout, model);

            TileView detailsView = detailsView();
            Tile detailsTile = detailsView == null ? null : detailsView.tile;
            TileDetailsView.DetailsHit details = null;
            int hoveredTile = -1;
            if (detailsTile != null) {
                details = TileDetailsView.paint(g, layout.gridArea, detailsTile, bingo,
                        kind.canSubmit(detailsView, bingo), config.boardShowTileImages() ? images : null,
                        itemManager::getImage,
                        state.getDetailsScrollPx(), mouse);
                if (state.getDetailsScrollPx() > details.maxScroll) {
                    state.setDetailsScrollPx(details.maxScroll);
                }
            } else {
                hoveredTile = mouse == null ? -1 : layout.tileIndexAt(mouse.x, mouse.y);
                paintGrid(g, layout, kind, hoveredTile);
            }

            addTooltips(hoveredButton, hoveredTile, bingo, kind);
            hitMap = new HitMap(origin, layout, kind, model.views, detailsTile, details);
        } finally {
            g.dispose();
        }

        return new Dimension(layout.width, layout.height);
    }

    private void rebuildModel(Bingo bingo) {
        model = BoardModel.of(bingo);
        lastBingo = bingo;
        painter.reset();

        if (config.boardShowTileImages()) {
            List<String> urls = new ArrayList<>();
            for (TileView view : model.views) {
                if (!view.hidden && view.imageUrl != null) {
                    urls.add(view.imageUrl);
                }
            }
            images.prefetch(urls);
        }
    }

    private TileView detailsView() {
        UUID id = state.getDetailsTileId();
        if (id == null) {
            return null;
        }

        TileView view = model.viewById.get(id);
        if (view == null || !model.kind.canOpen(view)) {
            // Tile vanished after a refresh or bingo switch
            state.closeDetails();
            return null;
        }
        return view;
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

    private void paintGrid(Graphics2D g, BoardLayout layout, BoardKind kind, int hoveredTile) {
        TileView[] views = model.views;
        for (int i = 0; i < layout.tiles.size() && i < views.length; i++) {
            Rectangle r = layout.tiles.get(i);
            Graphics2D tg = (Graphics2D) g.create();
            try {
                tg.clipRect(r.x, r.y, r.width, r.height);
                kind.paintTile(tg, r, views[i], i == hoveredTile, painter);
            } finally {
                tg.dispose();
            }
        }
    }

    private void addTooltips(BoardLayout.Button hoveredButton, int hoveredTile, Bingo bingo, BoardKind kind) {
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

        if (config.boardShowTooltips() && hoveredTile >= 0 && hoveredTile < model.views.length) {
            Tooltip tooltip = kind.tooltip(model.views[hoveredTile], itemManager::getImage);
            if (tooltip != null) {
                tooltipManager.add(tooltip);
            }
        }
    }

    private boolean isPinned(Bingo bingo) {
        return bingo.getId() != null && bingo.getId().toString().equals(config.pinnedBingoId());
    }
}
