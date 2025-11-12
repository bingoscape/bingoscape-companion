package org.bingoscape.ui;

import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;

/**
 * Factory class for creating consistent UI styling elements.
 * Eliminates border and styling duplication across the codebase.
 */
public final class UIStyleFactory {

    private UIStyleFactory() {
        throw new AssertionError("UIStyleFactory is a utility class and should not be instantiated");
    }

    /**
     * Creates a compound border with a colored line border and empty padding.
     *
     * @param color The line border color
     * @param lineWidth The width of the line border
     * @param padding Uniform padding on all sides
     * @return A compound border with line and padding
     */
    public static Border createStyledBorder(Color color, int lineWidth, int padding) {
        return new CompoundBorder(
            new LineBorder(color, lineWidth, true),
            new EmptyBorder(padding, padding, padding, padding)
        );
    }

    /**
     * Creates a compound border with a colored line border and custom padding.
     *
     * @param color The line border color
     * @param lineWidth The width of the line border
     * @param top Top padding
     * @param left Left padding
     * @param bottom Bottom padding
     * @param right Right padding
     * @return A compound border with line and custom padding
     */
    public static Border createStyledBorder(Color color, int lineWidth,
                                           int top, int left, int bottom, int right) {
        return new CompoundBorder(
            new LineBorder(color, lineWidth, true),
            new EmptyBorder(top, left, bottom, right)
        );
    }

    /**
     * Creates a rounded line border (convenience method).
     *
     * @param color The border color
     * @param lineWidth The width of the border
     * @return A rounded line border
     */
    public static Border createRoundedBorder(Color color, int lineWidth) {
        return new LineBorder(color, lineWidth, true);
    }

    /**
     * Creates an empty border with uniform padding.
     *
     * @param padding Uniform padding on all sides
     * @return An empty border with padding
     */
    public static Border createPaddingBorder(int padding) {
        return new EmptyBorder(padding, padding, padding, padding);
    }

    /**
     * Creates an empty border with custom padding.
     *
     * @param top Top padding
     * @param left Left padding
     * @param bottom Bottom padding
     * @param right Right padding
     * @return An empty border with custom padding
     */
    public static Border createPaddingBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    /**
     * Creates a brighter version of a color for hover effects.
     *
     * @param color The original color
     * @param amount The amount to brighten (0-255)
     * @return A brighter color
     */
    public static Color brighten(Color color, int amount) {
        return new Color(
            Math.min(255, color.getRed() + amount),
            Math.min(255, color.getGreen() + amount),
            Math.min(255, color.getBlue() + amount),
            color.getAlpha()
        );
    }

    /**
     * Creates a darker version of a color for pressed effects.
     *
     * @param color The original color
     * @param amount The amount to darken (0-255)
     * @return A darker color
     */
    public static Color darken(Color color, int amount) {
        return new Color(
            Math.max(0, color.getRed() - amount),
            Math.max(0, color.getGreen() - amount),
            Math.max(0, color.getBlue() - amount),
            color.getAlpha()
        );
    }
}
