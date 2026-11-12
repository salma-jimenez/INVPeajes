package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Textos, colores y componentes que comparten las pantallas del viajero.
 * Los colores base vienen de {@link Theme}, igual que en caja y estación.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public final class TravelerUi {

    /** Peajes de la ANI (concesiones nacionales). */
    public static final Color COLOR_ANI = Theme.PRIMARY;
    /** Peajes del INVÍAS. */
    public static final Color COLOR_INVIAS = Theme.WARNING;
    /** Peajes departamentales, distritales o municipales. */
    public static final Color COLOR_TERRITORIAL = Theme.SUCCESS;
    /** Peajes sin nivel conocido. */
    public static final Color COLOR_UNKNOWN = new Color(0x9CA3AF);

    private TravelerUi() {
    }

    // ------------------------------------------------------------------ Textos

    /**
     * @param level valor de la columna NIVEL
     * @return color del marcador según quién administra el peaje
     */
    public static Color levelColor(String level) {
        if ("ANI".equals(level)) {
            return COLOR_ANI;
        }
        if ("INVIAS".equals(level)) {
            return COLOR_INVIAS;
        }
        if (level == null || level.isBlank() || "DESCONOCIDO".equals(level)) {
            return COLOR_UNKNOWN;
        }
        return COLOR_TERRITORIAL;
    }

    /**
     * @param level valor de la columna NIVEL
     * @return texto para mostrar ("ANI", "INVÍAS", "Departamental"...)
     */
    public static String levelText(String level) {
        if ("ANI".equals(level)) {
            return "ANI";
        }
        if ("INVIAS".equals(level)) {
            return "INVÍAS";
        }
        return pretty(level);
    }

    /**
     * Convierte un valor de la base en texto legible: "NO_OPERATIVO" → "No operativo".
     *
     * @param value valor guardado
     * @return texto legible, o "Sin dato"
     */
    public static String pretty(String value) {
        if (value == null || value.isBlank()) {
            return "Sin dato";
        }
        String t = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /**
     * @param toll peaje
     * @return true si el peaje está operando
     */
    public static boolean isOperating(Toll toll) {
        return "OPERATIVO".equals(toll.getOperationalStatus());
    }

    /**
     * @param text texto o null
     * @return el texto, o "—" si está vacío
     */
    public static String orDash(String text) {
        return text == null || text.isBlank() ? "—" : text.trim();
    }

    /**
     * Pasa a minúsculas y quita tildes, para buscar sin importar cómo se escriba.
     *
     * @param text texto
     * @return texto normalizado ("Bogotá" → "bogota")
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    /**
     * @param km distancia
     * @return texto como "123 km" o "8,4 km"
     */
    public static String km(double km) {
        return km >= 100 ? String.format(Locale.ROOT, "%,.0f km", km).replace(',', '.')
                : String.format(Locale.ROOT, "%.1f km", km).replace('.', ',');
    }

    // ------------------------------------------------------------------ Componentes

    /**
     * @param text  texto
     * @param style estilo de la fuente
     * @param size  tamaño
     * @param color color
     * @return etiqueta con la fuente del proyecto
     */
    public static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        return label;
    }

    /**
     * @param category categoría vehicular
     * @return texto como "Cat. I · Automóviles, camperos y camionetas"
     */
    public static String categoryText(com.invpeajes.model.VehicleCategory category) {
        if (category == null) {
            return "—";
        }
        String desc = category.getDescription();
        return "Cat. " + category.getCategoryCode() + (desc == null || desc.isBlank() ? "" : " · " + desc);
    }

    /**
     * Muestra las categorías del combo con {@link #categoryText}.
     *
     * @param combo combo de categorías
     */
    public static void useCategoryText(JComboBox<com.invpeajes.model.VehicleCategory> combo) {
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof com.invpeajes.model.VehicleCategory) {
                    setText(categoryText((com.invpeajes.model.VehicleCategory) value));
                }
                return this;
            }
        });
    }

    /**
     * Combo con la fuente y el borde de los campos de texto.
     *
     * @param items opciones
     * @param <T>   tipo de las opciones
     * @return combo listo para usar
     */
    public static <T> JComboBox<T> combo(List<T> items) {
        JComboBox<T> combo = new JComboBox<>();
        for (T item : items) {
            combo.addItem(item);
        }
        styleCombo(combo);
        return combo;
    }

    /**
     * @param combo combo a estilizar
     */
    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(Theme.font(Font.PLAIN, 14));
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Color.WHITE);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        combo.setPreferredSize(new Dimension(200, 36));
    }

    /**
     * Campo con su etiqueta encima.
     *
     * @param caption texto de la etiqueta
     * @param field   campo
     * @return panel con los dos
     */
    public static JPanel field(String caption, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label(caption, Font.BOLD, 12, Theme.MUTED), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 24));
        return panel;
    }

    /**
     * Da a la tabla el estilo del proyecto y la mete en un scroll con borde suave.
     *
     * @param table tabla
     * @return scroll con la tabla
     */
    public static JScrollPane styleTable(JTable table) {
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setForeground(Theme.TEXT);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.PRIMARY_SOFT);
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.font(Font.BOLD, 12));
        header.setForeground(Theme.MUTED);
        header.setBackground(Theme.BACKGROUND);
        header.setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, padded(SwingConstants.LEFT));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    /**
     * @param alignment alineación horizontal (SwingConstants)
     * @return renderizador con margen a los lados
     */
    public static DefaultTableCellRenderer padded(int alignment) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
        renderer.setHorizontalAlignment(alignment);
        return renderer;
    }

    /**
     * Fija el ancho preferido de cada columna.
     *
     * @param table  tabla
     * @param widths anchos en píxeles, en el orden de las columnas
     */
    public static void columns(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /**
     * @return renderizador que muestra un BigDecimal como pesos ("$13.700"), alineado a la derecha
     */
    public static DefaultTableCellRenderer moneyRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setText(v instanceof java.math.BigDecimal ? Theme.money((java.math.BigDecimal) v) : "Sin tarifa");
                setForeground(v == null ? Theme.MUTED : Theme.TEXT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        return renderer;
    }

    /**
     * Página con fondo gris y margen, como las de estación.
     *
     * @param content contenido
     * @return panel de la página
     */
    public static JPanel page(JComponent content) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    /**
     * Mensaje centrado para listas vacías o errores.
     *
     * @param text texto (puede llevar HTML)
     * @return etiqueta centrada
     */
    public static JLabel emptyMessage(String text) {
        JLabel label = label("<html><div style='text-align:center'>" + text + "</div></html>",
                Font.PLAIN, 14, Theme.MUTED);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    /** Ciudad de origen o destino para planificar un viaje. */
    public static final class City {
        /** Capitales y ciudades principales (coordenadas aproximadas del centro). */
        public static final List<City> MAIN = List.of(
                new City("Armenia", 4.533, -75.681), new City("Barranquilla", 10.964, -74.796),
                new City("Bogotá", 4.711, -74.072), new City("Bucaramanga", 7.119, -73.122),
                new City("Cali", 3.451, -76.532), new City("Cartagena", 10.391, -75.479),
                new City("Cúcuta", 7.893, -72.508), new City("Ibagué", 4.438, -75.232),
                new City("Manizales", 5.070, -75.513), new City("Medellín", 6.244, -75.581),
                new City("Montería", 8.750, -75.881), new City("Neiva", 2.927, -75.282),
                new City("Pasto", 1.214, -77.281), new City("Pereira", 4.813, -75.696),
                new City("Popayán", 2.444, -76.614), new City("Riohacha", 11.544, -72.907),
                new City("Santa Marta", 11.241, -74.199), new City("Sincelejo", 9.304, -75.397),
                new City("Tunja", 5.535, -73.367), new City("Valledupar", 10.463, -73.253),
                new City("Villavicencio", 4.142, -73.627), new City("Yopal", 5.337, -72.395));

        private final String name;
        private final double latitude;
        private final double longitude;

        public City(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() {
            return name;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
