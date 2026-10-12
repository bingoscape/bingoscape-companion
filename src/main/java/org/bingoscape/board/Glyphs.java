package org.bingoscape.board;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Small icons drawn once with Java2D and cached, so the render loop only blits them.
 */
final class Glyphs {
    private static final Color LOCK_BODY = new Color(156, 163, 175);
    private static final Color LOCK_KEYHOLE = new Color(31, 41, 55);

    /** Last lock rendered; tiles share one size per frame, so a single entry is enough. */
    private static volatile BufferedImage lastLock;

    private Glyphs() {
    }

    /**
     * A padlock of {@code size} x {@code size} pixels. The returned image is shared and must not be modified.
     */
    static BufferedImage lock(int size) {
        int s = Math.max(8, size);
        BufferedImage cached = lastLock;
        if (cached != null && cached.getWidth() == s) {
            return cached;
        }

        BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

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
        } finally {
            g.dispose();
        }

        lastLock = image;
        return image;
    }
}
