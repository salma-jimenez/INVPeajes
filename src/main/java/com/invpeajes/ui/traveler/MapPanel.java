package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Mapa de peajes con filtros por departamento, ruta y concesionario, buscador por
 * nombre y la ficha del peaje que se escoja.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class MapPanel extends JPanel {

    private static final String ALL_DEPARTMENTS = "Todos los departamentos";
    private static final String ALL_ROUTES = "Todas las rutas";
    private static final String ALL_CONCESSIONAIRES = "Todos los concesionarios";

    private final TollMap map = new TollMap();
    private final TollDetailPanel detail;
    private final JTextField search = Theme.textField();
    private final JComboBox<Option> department = new JComboBox<>();
    private final JComboBox<Option> route = new JComboBox<>();
    private final JComboBox<Option> concessionaire = new JComboBox<>();
    private Catalog catalog;
    private boolean filling;

    /**
     * @param service servicio del viajero
     */
    public MapPanel(TravelerService service) {
        super(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        detail = new TollDetailPanel(service);

        search.setToolTipText("Buscar peaje por nombre");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters(false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters(false);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters(false);
            }
        });
        for (JComboBox<Option> combo : List.of(department, route, concessionaire)) {
            TravelerUi.styleCombo(combo);
            combo.addActionListener(e -> applyFilters(true));
        }
        FlatButton clear = Theme.secondaryButton("Limpiar");
        clear.addActionListener(e -> clearFilters());

        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBackground(java.awt.Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        addFilter(filters, 0, "Buscar por nombre", search, 1.3);
        addFilter(filters, 1, "Departamento", department, 1);
        addFilter(filters, 2, "Ruta", route, 1);
        addFilter(filters, 3, "Concesionario", concessionaire, 1.2);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 4;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 4, 0, 0);
        filters.add(clear, c);

        map.setOnSelect(detail::showToll);
        JScrollPane side = new JScrollPane(detail, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        side.setBorder(null);
        side.getVerticalScrollBar().setUnitIncrement(16);
        side.setPreferredSize(new Dimension(340, 0));

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(map, BorderLayout.CENTER);
        center.add(side, BorderLayout.EAST);

        add(filters, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    private static void addFilter(JPanel panel, int column, String caption, JComponent field, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 10);
        field.setPreferredSize(new Dimension(120, 36));
        panel.add(TravelerUi.field(caption, field), c);
    }

    /**
     * Llena los filtros y el mapa con el catálogo cargado.
     *
     * @param catalog catálogo de peajes
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        detail.setCatalog(catalog);
        filling = true;
        fill(department, ALL_DEPARTMENTS, catalog.getDepartments(), usedDepartments());
        fill(route, ALL_ROUTES, catalog.getRoutes(), usedRoutes());
        concessionaire.removeAllItems();
        concessionaire.addItem(new Option(null, ALL_CONCESSIONAIRES));
        TreeSet<Integer> used = new TreeSet<>();
        for (Toll t : catalog.getTolls()) {
            if (t.getConcessionaireId() != null) {
                used.add(t.getConcessionaireId());
            }
        }
        List<Option> options = new ArrayList<>();
        for (Integer id : used) {
            options.add(new Option(String.valueOf(id), catalog.concessionaireName(id)));
        }
        options.sort(Comparator.comparing(o -> TravelerUi.normalize(o.text)));
        options.forEach(concessionaire::addItem);
        filling = false;
        applyFilters(true);
    }

    /**
     * Muestra un peaje en el mapa con su ficha (lo usan otras pantallas).
     *
     * @param toll peaje
     */
    public void focus(Toll toll) {
        clearFilters();
        map.select(toll, true);
        detail.showToll(toll);
    }

    private TreeSet<String> usedDepartments() {
        TreeSet<String> used = new TreeSet<>();
        for (Toll t : catalog.getTolls()) {
            if (t.getDepartmentId() != null) {
                used.add(t.getDepartmentId());
            }
        }
        return used;
    }

    private TreeSet<String> usedRoutes() {
        TreeSet<String> used = new TreeSet<>();
        for (Toll t : catalog.getTolls()) {
            if (t.getRouteId() != null) {
                used.add(t.getRouteId());
            }
        }
        return used;
    }

    private static void fill(JComboBox<Option> combo, String allText, Map<String, String> names,
                             TreeSet<String> used) {
        combo.removeAllItems();
        combo.addItem(new Option(null, allText));
        List<Option> options = new ArrayList<>();
        for (String id : used) {
            options.add(new Option(id, names.getOrDefault(id, id)));
        }
        options.sort(Comparator.comparing(o -> TravelerUi.normalize(o.text)));
        options.forEach(combo::addItem);
    }

    private void clearFilters() {
        filling = true;
        search.setText("");
        department.setSelectedIndex(department.getItemCount() > 0 ? 0 : -1);
        route.setSelectedIndex(route.getItemCount() > 0 ? 0 : -1);
        concessionaire.setSelectedIndex(concessionaire.getItemCount() > 0 ? 0 : -1);
        filling = false;
        applyFilters(true);
    }

    private void applyFilters(boolean refit) {
        if (catalog == null || filling) {
            return;
        }
        String text = TravelerUi.normalize(search.getText());
        String dep = selectedId(department);
        String rte = selectedId(route);
        String conc = selectedId(concessionaire);
        List<Toll> shown = new ArrayList<>();
        for (Toll t : catalog.getTolls()) {
            if (!text.isEmpty() && !TravelerUi.normalize(t.getName()).contains(text)) {
                continue;
            }
            if (dep != null && !dep.equals(t.getDepartmentId())) {
                continue;
            }
            if (rte != null && !rte.equals(t.getRouteId())) {
                continue;
            }
            if (conc != null && (t.getConcessionaireId() == null
                    || !conc.equals(String.valueOf(t.getConcessionaireId())))) {
                continue;
            }
            shown.add(t);
        }
        map.showTolls(shown);
        if (refit) {
            map.fitAll();
        }
    }

    private static String selectedId(JComboBox<Option> combo) {
        Option o = (Option) combo.getSelectedItem();
        return o == null ? null : o.id;
    }

    /** Opción de un filtro: código y nombre. */
    private static final class Option {
        private final String id;
        private final String text;

        Option(String id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
