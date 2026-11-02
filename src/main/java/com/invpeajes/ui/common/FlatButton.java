package com.invpeajes.ui.common;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Botón plano con esquinas redondeadas, igual en cualquier Look and Feel.
 * Sirve como botón principal, secundario o como "ficha" seleccionable.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public class FlatButton extends JButton {

    private static final int ARC = 10;
    private Color base = Theme.PRIMARY;
    private Color hover = Theme.PRIMARY_DARK;
    private Color text = Color.WHITE;
    private Color outline;

    /**
     * Crea el botón con los colores del botón principal.
     * @param label Texto del botón.
     */
    public FlatButton(String label) {
        super(label);
        setUI(new BasicButtonUI());
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Theme.font(java.awt.Font.BOLD, 14));
        setBorder(new EmptyBorder(10, 16, 10, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(text);
    }

    /**
     * Cambia los colores del botón.
     * @param base Color de fondo normal.
     * @param hover Color de fondo al pasar el mouse.
     * @param text Color del texto.
     * @param outline Color del borde, o null para no dibujar borde.
     */
    public void setColors(Color base, Color hover, Color text, Color outline) {
        this.base = base;
        this.hover = hover;
        this.text = text;
        this.outline = outline;
        setForeground(isEnabled() ? text : Theme.MUTED);
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground(enabled ? text : Theme.MUTED);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill;
        if (!isEnabled()) {
            fill = Theme.DISABLED;
        } else if (getModel().isPressed() || getModel().isRollover()) {
            fill = hover;
        } else {
            fill = base;
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        if (outline != null && isEnabled()) {
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
