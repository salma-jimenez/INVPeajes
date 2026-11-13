package com.invpeajes.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Tarjeta blanca de un número con borde de color a la izquierda (contadores y estadísticas).
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StatTile extends JPanel {

    private final Color accent;
    private final JLabel value = new JLabel(" ");
    private final JLabel detail = new JLabel(" ");
    private final JLabel titleLabel;

    /**
     * @param title Título (ej. "Sin atender").
     * @param accent Color del borde y del número.
     * @param valueSize Tamaño de letra del número.
     */
    public StatTile(String title, Color accent, int valueSize) {
        super(new BorderLayout());
        this.accent = accent;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 18, 10, 14)));
        value.setFont(Theme.font(Font.BOLD, valueSize));
        value.setForeground(accent);
        detail.setFont(Theme.font(Font.PLAIN, 12));
        detail.setForeground(Theme.MUTED);
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
        text.setOpaque(false);
        titleLabel = Theme.muted(title);
        text.add(titleLabel);
        text.add(value);
        text.add(detail);
        add(text, BorderLayout.CENTER);
    }

    /**
     * Cambia el número y el texto de abajo.
     * @param text Número a mostrar.
     * @param note Texto pequeño (puede ser vacío).
     */
    public void setValue(String text, String note) {
        value.setText(text);
        detail.setText(note == null || note.isEmpty() ? " " : note);
    }

    /**
     * Cambia el título (ej. "Recaudado en noviembre").
     * @param title Título nuevo.
     */
    public void setTitleText(String title) {
        titleLabel.setText(title);
    }

    /**
     * Cambia el texto de abajo y su color.
     * @param note Texto.
     * @param color Color (ej. verde si subió, rojo si bajó).
     */
    public void setNote(String note, Color color) {
        detail.setText(note);
        detail.setForeground(color);
        detail.setFont(Theme.font(Font.BOLD, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(accent);
        g.fillRect(0, 0, 4, getHeight());
    }
}
