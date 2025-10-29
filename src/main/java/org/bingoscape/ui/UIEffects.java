package org.bingoscape.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utility class providing common UI effects and enhancements.
 * Encapsulates reusable patterns like button hover effects.
 */
public final class UIEffects {

    /**
     * Adds a hover effect to a button that changes its background color when the mouse enters.
     * The button's content area becomes filled with the specified hover color.
     *
     * @param button The button to enhance with hover effect
     * @param hoverColor The color to display when the mouse hovers over the button
     */
    public static void addButtonHoverEffect(JButton button, Color hoverColor) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });
    }

    /**
     * Adds a hover effect using the default accent blue color.
     *
     * @param button The button to enhance with hover effect
     */
    public static void addButtonHoverEffect(JButton button) {
        addButtonHoverEffect(button, ColorPalette.ACCENT_BLUE);
    }

    // Private constructor to prevent instantiation
    private UIEffects() {
        throw new AssertionError("UIEffects is a utility class and should not be instantiated");
    }
}
