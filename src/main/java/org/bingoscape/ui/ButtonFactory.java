package org.bingoscape.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Factory class for creating consistently styled buttons across the application.
 * Provides various button styles with unified appearance and behavior.
 */
public final class ButtonFactory {

    /**
     * Button style variants for different use cases.
     */
    public enum ButtonStyle {
        /** Primary action button - blue accent */
        PRIMARY,
        /** Secondary action button - darker gray */
        SECONDARY,
        /** Icon-only button - compact with transparent background */
        ICON,
        /** Danger/warning button - red tones */
        DANGER,
        /** Success/confirm button - green tones */
        SUCCESS
    }

    /**
     * Creates a styled button with text and icon.
     *
     * @param text The button text
     * @param icon The button icon (emoji or null)
     * @param tooltip The tooltip text
     * @param style The button style variant
     * @return A styled JButton
     */
    public static JButton createButton(String text, String icon, String tooltip, ButtonStyle style) {
        String displayText = icon != null ? icon + " " + text : text;
        JButton button = new JButton(displayText);

        applyBaseStyle(button);
        applyStyleVariant(button, style);

        if (tooltip != null && !tooltip.isEmpty()) {
            button.setToolTipText(tooltip);
        }

        return button;
    }

    /**
     * Creates a styled button with just text.
     *
     * @param text The button text
     * @param tooltip The tooltip text
     * @param style The button style variant
     * @return A styled JButton
     */
    public static JButton createButton(String text, String tooltip, ButtonStyle style) {
        return createButton(text, null, tooltip, style);
    }

    /**
     * Creates a compact icon-only button (for toolbars and headers).
     *
     * @param icon The icon (emoji)
     * @param tooltip The tooltip text
     * @param size The button size in pixels
     * @return A styled icon button
     */
    public static JButton createIconButton(String icon, String tooltip, int size) {
        JButton button = new JButton(icon);

        applyBaseStyle(button);
        button.setPreferredSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setContentAreaFilled(false);
        button.setBorder(new CompoundBorder(
            new LineBorder(ColorPalette.BORDER, 1, true),
            new EmptyBorder(2, 2, 2, 2)
        ));

        if (tooltip != null && !tooltip.isEmpty()) {
            button.setToolTipText(tooltip);
        }

        // Add hover effect
        UIEffects.addButtonHoverEffect(button, ColorPalette.ACCENT_BLUE.darker());

        return button;
    }

    /**
     * Creates a primary action button (default blue style).
     *
     * @param text The button text
     * @param tooltip The tooltip text
     * @return A styled primary button
     */
    public static JButton createPrimaryButton(String text, String tooltip) {
        return createButton(text, tooltip, ButtonStyle.PRIMARY);
    }

    /**
     * Creates a secondary action button (gray style).
     *
     * @param text The button text
     * @param tooltip The tooltip text
     * @return A styled secondary button
     */
    public static JButton createSecondaryButton(String text, String tooltip) {
        return createButton(text, tooltip, ButtonStyle.SECONDARY);
    }

    /**
     * Applies base styling common to all buttons.
     */
    private static void applyBaseStyle(JButton button) {
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Applies style variant specific styling.
     */
    private static void applyStyleVariant(JButton button, ButtonStyle style) {
        switch (style) {
            case PRIMARY:
                button.setBackground(ColorPalette.ACCENT_BLUE);
                button.setBorder(new CompoundBorder(
                    new LineBorder(ColorPalette.ACCENT_BLUE, 2, true),
                    new EmptyBorder(8, 16, 8, 16)
                ));
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                addPrimaryHoverEffect(button);
                break;

            case SECONDARY:
                button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                button.setBorder(new CompoundBorder(
                    new LineBorder(ColorPalette.BORDER, 1, true),
                    new EmptyBorder(8, 16, 8, 16)
                ));
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                UIEffects.addButtonHoverEffect(button, ColorScheme.MEDIUM_GRAY_COLOR);
                break;

            case ICON:
                button.setContentAreaFilled(false);
                button.setBorder(new EmptyBorder(4, 8, 4, 8));
                UIEffects.addButtonHoverEffect(button, ColorPalette.ACCENT_BLUE.darker());
                break;

            case DANGER:
                button.setBackground(new Color(150, 40, 40));
                button.setBorder(new CompoundBorder(
                    new LineBorder(new Color(180, 50, 50), 2, true),
                    new EmptyBorder(8, 16, 8, 16)
                ));
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                addDangerHoverEffect(button);
                break;

            case SUCCESS:
                button.setBackground(new Color(40, 120, 40));
                button.setBorder(new CompoundBorder(
                    new LineBorder(new Color(50, 150, 50), 2, true),
                    new EmptyBorder(8, 16, 8, 16)
                ));
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                addSuccessHoverEffect(button);
                break;
        }
    }

    /**
     * Adds hover effect for primary buttons.
     */
    private static void addPrimaryHoverEffect(JButton button) {
        Color hoverColor = ColorPalette.ACCENT_BLUE.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(ColorPalette.ACCENT_BLUE);
                }
            }
        });
    }

    /**
     * Adds hover effect for danger buttons.
     */
    private static void addDangerHoverEffect(JButton button) {
        Color normalColor = new Color(150, 40, 40);
        Color hoverColor = new Color(180, 50, 50);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(normalColor);
                }
            }
        });
    }

    /**
     * Adds hover effect for success buttons.
     */
    private static void addSuccessHoverEffect(JButton button) {
        Color normalColor = new Color(40, 120, 40);
        Color hoverColor = new Color(50, 150, 50);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(normalColor);
                }
            }
        });
    }

    // Private constructor to prevent instantiation
    private ButtonFactory() {
        throw new AssertionError("ButtonFactory is a utility class and should not be instantiated");
    }
}
