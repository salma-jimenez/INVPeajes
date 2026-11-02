package com.invpeajes.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * Colores, fuentes y componentes base de RutaPeaje, para que todas las pantallas
 * tengan el mismo estilo (barra lateral azul marino, tarjetas blancas, botón azul).
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public final class Theme {

    public static final Color SIDEBAR = new Color(0x0F2A5F);
    public static final Color SIDEBAR_TEXT = new Color(0xDCE4F2);
    public static final Color SIDEBAR_MUTED = new Color(0x9FB0D0);
    public static final Color BACKGROUND = new Color(0xF1F4F9);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E7EF);
    public static final Color INPUT_BORDER = new Color(0xC9D1DD);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color DISABLED = new Color(0xEEF0F3);
    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color PRIMARY_DARK = new Color(0x1D4ED8);
    public static final Color PRIMARY_SOFT = new Color(0xEAF1FE);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color WARNING = new Color(0xF59E0B);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color PURPLE = new Color(0x7C3AED);

    private static final String FAMILY = pickFamily();
    private static final DecimalFormat MONEY = createMoneyFormat();

    private Theme() {
    }

    /**
     * Fuente de la aplicación (Segoe UI en Windows; si no existe, la fuente del sistema).
     * @param style Font.PLAIN o Font.BOLD.
     * @param size Tamaño en puntos.
     * @return La fuente.
     */
    public static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    /**
     * Formatea un valor en pesos colombianos sin decimales (ej. "$27.300").
     * @param value Valor a formatear.
     * @return Texto con el valor.
     */
    public static String money(BigDecimal value) {
        return value == null ? "$0" : MONEY.format(value);
    }

    /**
     * Panel blanco con borde suave y margen interno, como las tarjetas del inicio.
     * @return El panel (con BorderLayout).
     */
    public static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    /**
     * Tarjeta de indicador: un título pequeño y un valor grande de color.
     * @param title Título (ej. "Cobros del turno").
     * @param value Etiqueta que muestra el valor; se puede actualizar después.
     * @param color Color del valor.
     * @return La tarjeta.
     */
    public static JPanel statCard(String title, JLabel value, Color color) {
        JPanel panel = card();
        panel.setLayout(new BorderLayout(0, 6));
        panel.add(muted(title), BorderLayout.NORTH);
        value.setFont(font(Font.BOLD, 26));
        value.setForeground(color);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    /**
     * @param text Texto del título.
     * @return Título de tarjeta en negrita.
     */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 16));
        label.setForeground(TEXT);
        return label;
    }

    /**
     * @param text Texto.
     * @return Etiqueta gris pequeña.
     */
    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.PLAIN, 13));
        label.setForeground(MUTED);
        return label;
    }

    /**
     * Campo de texto con el borde y la fuente de la aplicación.
     * @return El campo.
     */
    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setFont(font(Font.PLAIN, 15));
        field.setForeground(TEXT);
        field.setBorder(inputBorder());
        return field;
    }

    /**
     * @return Borde redondeado para campos de texto.
     */
    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10));
    }

    /**
     * @param text Texto del botón.
     * @return Botón azul principal.
     */
    public static FlatButton primaryButton(String text) {
        return new FlatButton(text);
    }

    /**
     * @param text Texto del botón.
     * @return Botón blanco con borde (como "Limpiar").
     */
    public static FlatButton secondaryButton(String text) {
        FlatButton button = new FlatButton(text);
        button.setColors(Color.WHITE, BACKGROUND, TEXT, INPUT_BORDER);
        return button;
    }

    private static String pickFamily() {
        try {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            return Arrays.asList(names).contains("Segoe UI") ? "Segoe UI" : Font.SANS_SERIF;
        } catch (RuntimeException e) {
            return Font.SANS_SERIF;
        }
    }

    private static DecimalFormat createMoneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("$#,##0", symbols);
    }
}
