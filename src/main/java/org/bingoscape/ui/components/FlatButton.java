package org.bingoscape.ui.components;

import net.runelite.client.ui.FontManager;
import org.bingoscape.ui.BingoTheme;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rounded flat button styled like the board overlay's header buttons: gray fill, hover wash,
 * dimmed when disabled.
 */
public class FlatButton extends JButton {
    private boolean hovered;

    public FlatButton(String text) {
        this(text, null);
    }

    public FlatButton(String text, Icon icon) {
        super(text, icon);
        setFont(FontManager.getRunescapeSmallFont());
        setForeground(BingoTheme.FOREGROUND);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setBorder(new EmptyBorder(5, 10, 5, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground(enabled ? BingoTheme.FOREGROUND : Color.GRAY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = BingoTheme.BUTTON_RADIUS;
            g2.setColor(isEnabled() ? BingoTheme.BUTTON_BACKGROUND : BingoTheme.withAlpha(BingoTheme.BUTTON_BACKGROUND, 120));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
            if (isEnabled() && (hovered || getModel().isPressed())) {
                g2.setColor(BingoTheme.BUTTON_HOVER);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
            }
            g2.setColor(BingoTheme.BUTTON_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
