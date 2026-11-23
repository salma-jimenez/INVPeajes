package com.invpeajes.ui.traveler;

import com.invpeajes.model.Concessionaire;
import com.invpeajes.model.Department;
import com.invpeajes.model.HealthCenter;
import com.invpeajes.model.Route;
import com.invpeajes.model.Toll;
import com.invpeajes.service.HealthCenterService;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Mapa de peajes con filtros por departamento, ruta y concesionario, buscador por
 * nombre y la ficha del peaje que se escoja. Una casilla permite mostrar también los
 * centros de salud públicos (se cargan de la base de datos la primera vez que se activa).
 *
 * <p>Todos los datos salen de funciones de la base que devuelven un cursor:</p>
 * <ul>
 *   <li>Opciones de los filtros: {@code fun_departamentos_con_peajes},
 *       {@code fun_rutas_con_peajes} y {@code fun_concesionarios_con_peajes}.</li>
 *   <li>Cada vez que cambia un filtro o se escribe en el buscador:
 *       {@code fun_filtrar_peajes(departamento, ruta, concesionario, texto)}.</li>
 *   <li>Centros de salud: {@code fun_listar_centros_salud}.</li>
 * </ul>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class MapPanel extends JPanel {

    private static final String ALL_DEPARTMENTS = "Todos los departamentos";
    private static final String ALL_ROUTES = "Todas las rutas";
    private static final String ALL_CONCESSIONAIRES = "Todos los concesionarios";
    private static final int SEARCH_DELAY_MS = 350;

    private final TollMap map = new TollMap();
    private final TollDetailPanel detail;
    private final JTextField search = Theme.textField();
    private final JComboBox<Option> department = new JComboBox<>();
    private final JComboBox<Option> route = new JComboBox<>();
    private final JComboBox<Option> concessionaire = new JComboBox<>();
    private final JCheckBox healthCenters = new JCheckBox("Centros de salud");
    private final HealthCenterService healthCenterService = new HealthCenterService();
    private final TravelerService service;
    /** Espera a que el viajero deje de escribir antes de consultar la base. */
    private final Timer searchDelay = new Timer(SEARCH_DELAY_MS, e -> applyFilters(false));
    private Catalog catalog;
    private boolean filling;
    /** Número de la última consulta de filtros, para ignorar respuestas viejas. */
    private int filterRequest;

    /**
     * @param service servicio del viajero
     */
    public MapPanel(TravelerService service) {
        super(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        this.service = service;
        detail = new TollDetailPanel(service);
        searchDelay.setRepeats(false);

        search.setToolTipText("Buscar peaje por nombre");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchDelay.restart();
            }
        });
        for (JComboBox<Option> combo : List.of(department, route, concessionaire)) {
            TravelerUi.styleCombo(combo);
            combo.addActionListener(e -> applyFilters(true));
        }
        healthCenters.setOpaque(false);
        healthCenters.setFont(Theme.font(java.awt.Font.PLAIN, 13));
        healthCenters.setForeground(Theme.TEXT);
        healthCenters.setToolTipText("Mostrar los hospitales y centros de salud públicos en el mapa");
        healthCenters.addActionListener(e -> toggleHealthCenters());
        FlatButton clear = Theme.secondaryButton("Limpiar");
        clear.addActionListener(e -> clearFilters(null));

        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBackground(java.awt.Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        addFilter(filters, 0, "Buscar por nombre", search, 1.3);
        addFilter(filters, 1, "Departamento", department, 1);
        addFilter(filters, 2, "Ruta", route, 1);
        addFilter(filters, 3, "Concesionario", concessionaire, 1.2);
        GridBagConstraints h = new GridBagConstraints();
        h.gridx = 4;
        h.anchor = GridBagConstraints.SOUTH;
        h.insets = new Insets(0, 4, 6, 6);
        filters.add(healthCenters, h);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 5;
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
     * Guarda el catálogo (para los nombres de la ficha) y llena los filtros con las opciones que
     * da la base: {@code fun_departamentos_con_peajes}, {@code fun_rutas_con_peajes} y
     * {@code fun_concesionarios_con_peajes}. Después muestra todos los peajes.
     *
     * @param catalog catálogo de peajes
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        detail.setCatalog(catalog);
        Async.run(() -> new Object[]{service.departmentsWithTolls(), service.routesWithTolls(),
                service.concessionairesWithTolls()}, data -> {
            filling = true;
            department.removeAllItems();
            department.addItem(new Option(null, ALL_DEPARTMENTS));
            for (Department d : castList(data[0], Department.class)) {
                department.addItem(new Option(d.getDepartmentId(), d.getName()));
            }
            route.removeAllItems();
            route.addItem(new Option(null, ALL_ROUTES));
            for (Route r : castList(data[1], Route.class)) {
                route.addItem(new Option(r.getRouteId(), r.getName()));
            }
            concessionaire.removeAllItems();
            concessionaire.addItem(new Option(null, ALL_CONCESSIONAIRES));
            for (Concessionaire c : castList(data[2], Concessionaire.class)) {
                concessionaire.addItem(new Option(String.valueOf(c.getConcessionaireId()), c.getName()));
            }
            filling = false;
            applyFilters(true);
        }, error -> JOptionPane.showMessageDialog(this, error.getMessage(),
                "No se pudieron cargar los filtros", JOptionPane.ERROR_MESSAGE));
    }

    private static <T> List<T> castList(Object value, Class<T> type) {
        List<T> list = new ArrayList<>();
        for (Object item : (List<?>) value) {
            list.add(type.cast(item));
        }
        return list;
    }

    /**
     * Muestra un peaje en el mapa con su ficha (lo usan otras pantallas).
     *
     * @param toll peaje
     */
    public void focus(Toll toll) {
        clearFilters(toll);
        detail.showToll(toll);
    }

    /**
     * Muestra u oculta los centros de salud. La primera vez los trae de la base de datos
     * en segundo plano (son unos 3.800).
     */
    private void toggleHealthCenters() {
        boolean show = healthCenters.isSelected();
        if (!show || map.hasHealthCenters()) {
            map.setHealthCentersVisible(show);
            return;
        }
        healthCenters.setEnabled(false);
        healthCenters.setText("Cargando...");
        Async.run(healthCenterService::findAll, (List<HealthCenter> centers) -> {
            map.showHealthCenters(centers);
            map.setHealthCentersVisible(true);
            healthCenters.setText("Centros de salud");
            healthCenters.setEnabled(true);
        }, error -> {
            healthCenters.setSelected(false);
            healthCenters.setText("Centros de salud");
            healthCenters.setEnabled(true);
            JOptionPane.showMessageDialog(this, error.getMessage(),
                    "No se pudieron cargar los centros de salud", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void clearFilters(Toll toSelect) {
        filling = true;
        search.setText("");
        department.setSelectedIndex(department.getItemCount() > 0 ? 0 : -1);
        route.setSelectedIndex(route.getItemCount() > 0 ? 0 : -1);
        concessionaire.setSelectedIndex(concessionaire.getItemCount() > 0 ? 0 : -1);
        filling = false;
        searchDelay.stop(); // el texto se borró aquí mismo, no hace falta otra consulta
        applyFilters(toSelect == null, toSelect);
    }

    /**
     * Pide a la base los peajes que cumplen los filtros escogidos con la función
     * {@code fun_filtrar_peajes} (por medio de {@code TravelerService.filterTolls} →
     * {@code TollService.filterTolls} → {@code TollDAO.filter}) y los dibuja en el mapa.
     */
    private void applyFilters(boolean refit) {
        applyFilters(refit, null);
    }

    /**
     * Igual que {@link #applyFilters(boolean)}; cuando llega la respuesta deja seleccionado
     * {@code toSelect} (si no es nulo).
     */
    private void applyFilters(boolean refit, Toll toSelect) {
        if (catalog == null || filling) {
            return;
        }
        String dep = selectedId(department);
        String rte = selectedId(route);
        String conc = selectedId(concessionaire);
        Integer concId = conc == null ? null : Integer.valueOf(conc);
        String text = search.getText();
        int request = ++filterRequest;
        Async.run(() -> service.filterTolls(dep, rte, concId, text), (List<Toll> shown) -> {
            if (request != filterRequest) {
                return; // llegó tarde: ya se pidió otra combinación de filtros
            }
            map.showTolls(shown);
            if (refit) {
                map.fitAll();
            }
            if (toSelect != null) {
                map.select(toSelect, true);
            }
        }, error -> JOptionPane.showMessageDialog(this, error.getMessage(),
                "No se pudieron filtrar los peajes", JOptionPane.ERROR_MESSAGE));
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
