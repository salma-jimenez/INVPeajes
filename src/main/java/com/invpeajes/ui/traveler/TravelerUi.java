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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
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
 * @version 1.3
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
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
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
     * Convierte un combo de municipios en buscable con vista compacta de dos líneas:
     * sin foco muestra el municipio en negrita arriba y el departamento abajo;
     * al hacer clic permite escribir y borrar libremente para filtrar la lista.
     *
     * @param combo combo de municipios
     */
    public static void makeSearchable(JComboBox<City> combo) {
        CitySearchSupport.install(combo);
    }

    /**
     * Carga la lista completa de municipios en un combo (buscable o estándar)
     * y selecciona el municipio indicado por defecto.
     *
     * @param combo       combo a actualizar
     * @param items       lista completa de municipios desde la base de datos
     * @param defaultItem municipio seleccionado inicialmente
     */
    public static void setComboItems(JComboBox<City> combo, List<City> items, City defaultItem) {
        CitySearchSupport.setItems(combo, items, defaultItem);
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
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        combo.setPreferredSize(new Dimension(200, 38));
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

    /** Municipio de origen o destino cargado desde la base de datos. */
    public static final class City {
        private final String name;
        private final String departmentName;
        private final double latitude;
        private final double longitude;

        public City(String name, String departmentName, double latitude, double longitude) {
            this.name = formatPlace(name);
            this.departmentName = shortDepartment(formatPlace(departmentName));
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() {
            return name;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        private static String formatPlace(String raw) {
            if (raw == null || raw.isBlank()) {
                return raw;
            }
            String trimmed = raw.trim();
            if (!trimmed.equals(trimmed.toUpperCase(Locale.ROOT))) {
                return trimmed;
            }
            String[] words = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                String w = words[i];
                if ("d.c.".equals(w) || "d.c".equals(w)) {
                    sb.append("D.C.");
                } else if (i > 0 && (w.equals("de") || w.equals("del") || w.equals("la")
                        || w.equals("las") || w.equals("los") || w.equals("y") || w.equals("en"))) {
                    sb.append(w);
                } else {
                    sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
                }
            }
            return sb.toString();
        }

        private static String shortDepartment(String dept) {
            if (dept == null || dept.isBlank()) {
                return dept;
            }
            String norm = normalize(dept);
            if (norm.contains("san andres")) {
                return "San Andrés y Providencia";
            }
            if (norm.startsWith("bogota")) {
                return "Bogotá, D.C.";
            }
            return dept;
        }

        @Override
        public String toString() {
            if (departmentName == null || departmentName.isBlank()) {
                return name;
            }
            if (normalize(name).startsWith("bogota") && normalize(departmentName).startsWith("bogota")) {
                return "Bogotá, D.C.";
            }
            return name + ", " + departmentName;
        }
    }
}