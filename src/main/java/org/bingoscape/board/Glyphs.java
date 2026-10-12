package org.bingoscape.board;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Small icons drawn once with Java2D and cached per size, so the render loop only blits them.
 */
final class Glyphs {
    private static final Color LOCK_BODY = new Color(156, 163, 175);
    private static final Color LOCK_KEYHOLE = new Color(31, 41, 55);

    static final Color HIT_COLOR = new Color(239, 68, 68);
    static final Color MISS_COLOR = new Color(203, 213, 225);
    static final Color SUNK_COLOR = new Color(56, 189, 248);
    private static final Color SHIP_COLOR = new Color(241, 245, 249);
    private static final Color OUTLINE = new Color(0, 0, 0, 170);

    private static final int MIN_SIZE = 8;
    private static final int MAX_SIZE = 96;
    private static final int LOCK = 0;
    private static final int HIT = 1;
    private static final int MISS = 2;
    private static final int SUNK = 3;

    /** Cached image per glyph kind and pixel size; sizes are clamped to {@link #MAX_SIZE}, so this stays small. */
    private static final AtomicReferenceArray<BufferedImage> CACHE = new AtomicReferenceArray<>(4 * (MAX_SIZE + 1));

    private interface Drawing {
        void draw(Graphics2D g, int s);
    }

    private Glyphs() {
    }

    /**
     * A padlock of {@code size} x {@code size} pixels. The returned image is shared and must not be modified.
     */
    static BufferedImage lock(int size) {
        return cached(LOCK, size, Glyphs::drawLock);
    }

    /**
     * A bold red X, the battleship "hit" mark. Shared; must not be modified.
     */
    static BufferedImage hit(int size) {
        return cached(HIT, size, Glyphs::drawHit);
    }

    /**
     * A hollow dot, the battleship "miss" mark. Shared; must not be modified.
     */
    static BufferedImage miss(int size) {
        return cached(MISS, size, Glyphs::drawMiss);
    }

    /**
     * A ship inside a ring, the battleship "sunk" mark. Shared; must not be modified.
     */
    static BufferedImage sunk(int size) {
        return cached(SUNK, size, Glyphs::drawSunk);
    }

    private static BufferedImage cached(int kind, int size, Drawing drawing) {
        int s = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
        int slot = kind * (MAX_SIZE + 1) + s;
        BufferedImage cached = CACHE.get(slot);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            drawing.draw(g, s);
        } finally {
            g.dispose();
        }

        CACHE.set(slot, image);
        return image;
    }

    private static void drawLock(Graphics2D g, int s) {
        double bodyX = s * 0.18;
        double bodyY = s * 0.46;
        double bodyW = s * 0.64;
        double bodyH = s * 0.44;

        // Shackle: a stroked half ring on top of the body
        float stroke = Math.max(1.5f, s * 0.12f);
        double shackleW = bodyW * 0.62;
        double shackleX = bodyX + (bodyW - shackleW) / 2;
        double shackleTop = s * 0.08;
        g.setColor(LOCK_BODY);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(shackleX, shackleTop, shackleW, (bodyY - shackleTop) * 1.4, 0, 180, Arc2D.OPEN));

        g.fill(new RoundRectangle2D.Double(bodyX, bodyY, bodyW, bodyH, s * 0.12, s * 0.12));

        double hole = Math.max(1.5, s * 0.12);
        g.setColor(LOCK_KEYHOLE);
        g.fill(new Ellipse2D.Double(s / 2.0 - hole / 2, bodyY + bodyH * 0.28, hole, hole));
        g.fill(new Rectangle2D.Double(s / 2.0 - hole * 0.2, bodyY + bodyH * 0.28 + hole * 0.6, hole * 0.4, bodyH * 0.35));
    }

    private static void drawHit(Graphics2D g, int s) {
        double pad = s * 0.14;
        float stroke = Math.max(2.5f, s * 0.2f);
        // Dark outline first so the X stays readable on any tile face
        g.setStroke(new BasicStroke(stroke + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(OUTLINE);
        cross(g, pad, s - pad);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(HIT_COLOR);
        cross(g, pad, s - pad);
    }

    private static void cross(Graphics2D g, double from, double to) {
        g.draw(new Line2D.Double(from, from, to, to));
        g.draw(new Line2D.Double(to, from, from, to));
    }

    private static void drawMiss(Graphics2D g, int s) {
        float stroke = Math.max(2f, s * 0.14f);
        double d = s * 0.5;
        Ellipse2D.Double ring = new Ellipse2D.Double((s - d) / 2, (s - d) / 2, d, d);
        g.setStroke(new BasicStroke(stroke + 2f));
        g.setColor(OUTLINE);
        g.draw(ring);
        g.setStroke(new BasicStroke(stroke));
        g.setColor(MISS_COLOR);
        g.draw(ring);
    }

    private static void drawSunk(Graphics2D g, int s) {
        float stroke = Math.max(1.5f, s * 0.08f);
        double inset = stroke / 2 + 1;
        Ellipse2D.Double ring = new Ellipse2D.Double(inset, inset, s - 2 * inset, s - 2 * inset);
        g.setColor(new Color(15, 23, 42, 200));
        g.fill(ring);
        g.setStroke(new BasicStroke(stroke));
        g.setColor(SUNK_COLOR);
        g.draw(ring);

        // Ship silhouette: hull, cabin and a mast
        g.setColor(SHIP_COLOR);
        Path2D.Double hull = new Path2D.Double();
        hull.moveTo(s * 0.22, s * 0.55);
        hull.lineTo(s * 0.78, s * 0.55);
        hull.lineTo(s * 0.68, s * 0.70);
        hull.lineTo(s * 0.32, s * 0.70);
        hull.closePath();
        g.fill(hull);
        g.fill(new Rectangle2D.Double(s * 0.40, s * 0.42, s * 0.20, s * 0.13));
        g.fill(new Rectangle2D.Double(s * 0.485, s * 0.28, Math.max(1.0, s * 0.03), s * 0.14));
    }
}
