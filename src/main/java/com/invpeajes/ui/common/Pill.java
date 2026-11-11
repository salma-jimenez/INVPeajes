package com.invpeajes.ui.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Etiqueta redondeada de color ("pastilla") para mostrar estados: Registrada, Disponible, etc.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class Pill extends JLabel {

    private Color background;

    /**
     * @param text Texto de la pastilla.
     * @param background Color de fondo.
     * @param foreground Color del texto.
     */
    public Pill(String text, Color background, Color foreground) {
        super(text, SwingConstants.CENTER);
        setFont(Theme.font(Font.BOLD, 11));
        setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        setColors(background, foreground);
    }

    /**
     * Cambia los colores.
     * @param background Fondo.
     * @param foreground Texto.
     */
    public final void setColors(Color background, Color foreground) {
        this.background = background;
        setForeground(foreground);
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
