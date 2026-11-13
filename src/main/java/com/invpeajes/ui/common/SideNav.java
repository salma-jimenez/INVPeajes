package com.invpeajes.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Barra lateral azul de las ventanas del jefe de estación y del administrador: nombre de
 * RutaPeaje, opciones del menú, datos de la sesión y abajo los botones de la cuenta.
 *
 * <pre>
 * SideNav nav = new SideNav();
 * NavItem resumen = nav.addItem("Resumen", () -&gt; show("resumen"));
 * nav.addInfo("ADMINISTRADOR", "Ana Morales");
 * nav.addBottomButton("Cerrar sesión", this::logout);
 * </pre>
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class SideNav extends JPanel {

    private final JPanel top = new JPanel();
    private final JPanel items = new JPanel();
    private final JPanel infos = new JPanel();
    private final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));

    /** Crea la barra con el nombre de RutaPeaje arriba y el lema abajo. */
    public SideNav() {
        super(new BorderLayout());
        setBackground(Theme.SIDEBAR);
        setPreferredSize(new Dimension(260, 0));
        setBorder(BorderFactory.createEmptyBorder(26, 14, 18, 14));

        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(indent(label("RutaPeaje", Font.BOLD, 26, Color.WHITE)));
        top.add(indent(label("Colombia", Font.PLAIN, 14, Theme.SIDEBAR_MUTED)));
        top.add(Box.createVerticalStrut(30));
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setOpaque(false);
        items.setAlignmentX(LEFT_ALIGNMENT);
        top.add(items);
        top.add(Box.createVerticalStrut(20));
        infos.setLayout(new BoxLayout(infos, BoxLayout.Y_AXIS));
        infos.setOpaque(false);
        infos.setAlignmentX(LEFT_ALIGNMENT);
        top.add(infos);

        JPanel bottom = new JPanel(new BorderLayout(0, 14));
        bottom.setOpaque(false);
        buttons.setOpaque(false);
        JLabel footer = new JLabel("<html>Conectando caminos,<br>impulsando el país</html>");
        footer.setFont(Theme.font(Font.PLAIN, 12));
        footer.setForeground(Theme.SIDEBAR_MUTED);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(footer, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Agrega una opción al menú.
     * @param text Texto de la opción.
     * @param action Qué hacer al hacer clic.
     * @return La opción, para marcarla activa o ponerle un contador.
     */
    public NavItem addItem(String text, Runnable action) {
        NavItem item = new NavItem(text);
        item.onClick(action);
        if (items.getComponentCount() > 0) {
            items.add(Box.createVerticalStrut(6));
        }
        items.add(item);
        return item;
    }

    /**
     * Agrega un dato de la sesión (ej. "PEAJE" / "Aburra").
     * @param caption Título en mayúsculas.
     * @param value Valor.
     * @return La etiqueta del valor, por si cambia después.
     */
    public JLabel addInfo(String caption, String value) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 0));
        JLabel valueLabel = label("<html>" + value + "</html>", Font.PLAIN, 14, Theme.SIDEBAR_TEXT);
        panel.add(label(caption, Font.BOLD, 11, Theme.SIDEBAR_MUTED), BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 20));
        infos.add(panel);
        return valueLabel;
    }

    /**
     * Agrega un botón con borde abajo de la barra (ej. "Cerrar sesión").
     * @param text Texto.
     * @param action Qué hacer.
     * @return El botón.
     */
    public FlatButton addBottomButton(String text, Runnable action) {
        FlatButton button = new FlatButton(text);
        button.setColors(Theme.SIDEBAR, new Color(0x1B3A78), Color.WHITE, Theme.SIDEBAR_MUTED);
        button.addActionListener(e -> action.run());
        buttons.add(button);
        return button;
    }

    /**
     * Agrega un enlace discreto abajo de la barra (ej. "Cambiar contraseña").
     * @param text Texto.
     * @param action Qué hacer.
     * @return El enlace.
     */
    public JLabel addBottomLink(String text, Runnable action) {
        JLabel link = label(text, Font.BOLD, 13, Theme.SIDEBAR_TEXT);
        link.setHorizontalAlignment(JLabel.CENTER);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        buttons.add(link);
        return link;
    }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        return label;
    }

    private static JComponent indent(JComponent c) {
        c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    /** Opción del menú lateral, con un contador rojo opcional a la derecha. */
    public static final class NavItem extends JPanel {
        private final JLabel text;
        private final Pill badge = new Pill("0", Theme.DANGER, Color.WHITE);
        private boolean active;
        private boolean hover;

        NavItem(String label) {
            super(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(232, 40));
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            text = new JLabel(label);
            add(text, BorderLayout.CENTER);
            JPanel badgeBox = new JPanel(new java.awt.GridBagLayout());
            badgeBox.setOpaque(false);
            badgeBox.add(badge);
            badge.setVisible(false);
            add(badgeBox, BorderLayout.EAST);
            setActive(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void onClick(Runnable action) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        /**
         * Marca la opción como la pantalla actual.
         * @param value true si es la actual.
         */
        public void setActive(boolean value) {
            active = value;
            text.setFont(Theme.font(value ? Font.BOLD : Font.PLAIN, 15));
            text.setForeground(value ? Color.WHITE : Theme.SIDEBAR_TEXT);
            repaint();
        }

        /**
         * Muestra un contador rojo (se oculta si es 0).
         * @param count Número a mostrar.
         */
        public void setBadge(int count) {
            badge.setText(String.valueOf(count));
            badge.setVisible(count > 0);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? Theme.PRIMARY : new Color(0x1B3A78));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
