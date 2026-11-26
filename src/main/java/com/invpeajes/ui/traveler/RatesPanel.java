package com.invpeajes.ui.traveler;

import com.invpeajes.model.Toll;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Tabla de peajes con la tarifa de la categoría que escoja el viajero, y comparación
 * de dos o más peajes con la distancia entre ellos.
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class RatesPanel extends JPanel {

    private static final int MAX_COMPARE = 6;

    private final TravelerService service;
    private final Consumer<Toll> onShowOnMap;
    private final JComboBox<VehicleCategory> category = new JComboBox<>();
    private final JTextField search = Theme.textField();
    private final RatesModel model = new RatesModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<RatesModel> sorter = new TableRowSorter<>(model);
    private final JLabel status = Theme.muted(" ");
    private final JPanel compareBox = new JPanel();
    private final FlatButton compare = Theme.primaryButton("Comparar seleccionados");
    private final FlatButton showOnMap = Theme.secondaryButton("Ver en el mapa");
    private Catalog catalog;
    private int requestId;

    /**
     * @param service     servicio del viajero
     * @param onShowOnMap qué hacer al pedir "Ver en el mapa"
     */
    public RatesPanel(TravelerService service, Consumer<Toll> onShowOnMap) {
        super(new BorderLayout(0, 14));
        this.service = service;
        this.onShowOnMap = onShowOnMap;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        TravelerUi.styleCombo(category);
        TravelerUi.useCategoryText(category);
        category.addActionListener(e -> loadPrices());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });

        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        addField(top, 0, "Categoría de su vehículo", category, 1);
        addField(top, 1, "Buscar peaje o departamento", search, 1.4);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.anchor = GridBagConstraints.SOUTH;
        top.add(status, c);

        table.setRowSorter(sorter);
        TravelerUi.columns(table, 260, 160, 110, 100, 110);
        table.getColumnModel().getColumn(4).setCellRenderer(TravelerUi.moneyRenderer());
        javax.swing.JScrollPane scroll = TravelerUi.styleTable(table);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    onShowOnMap.accept(model.get(table.convertRowIndexToModel(table.getSelectedRow())));
                }
            }
        });
        sorter.setComparator(4, (a, b) -> {
            BigDecimal x = (BigDecimal) a;
            BigDecimal y = (BigDecimal) b;
            if (x == null || y == null) {
                return x == null ? (y == null ? 0 : 1) : -1;
            }
            return x.compareTo(y);
        });

        compare.addActionListener(e -> showComparison());
        showOnMap.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                onShowOnMap.accept(model.get(table.convertRowIndexToModel(row)));
            }
        });
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.add(Theme.muted("<html>Ctrl + clic para escoger varios. Doble clic: ver en el mapa.</html>"),
                BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(showOnMap);
        buttons.add(compare);
        actions.add(buttons, BorderLayout.EAST);

        JPanel tableArea = new JPanel(new BorderLayout(0, 10));
        tableArea.setOpaque(false);
        tableArea.add(scroll, BorderLayout.CENTER);
        tableArea.add(actions, BorderLayout.SOUTH);

        compareBox.setLayout(new BorderLayout());
        compareBox.setOpaque(false);
        compareBox.setPreferredSize(new Dimension(360, 0));
        compareBox.add(hint(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(tableArea, BorderLayout.CENTER);
        center.add(compareBox, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        updateButtons();
    }

    private static void addField(JPanel panel, int column, String caption, JComponent field, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 12);
        field.setPreferredSize(new Dimension(160, 36));
        panel.add(TravelerUi.field(caption, field), c);
    }

    /**
     * @param catalog catálogo cargado
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        category.removeAllItems();
        for (VehicleCategory c : catalog.getCategories()) {
            category.addItem(c);
        }
        model.setTolls(catalog, catalog.getTolls());
        loadPrices();
    }

    /**
     * Escoge la categoría (por ejemplo, la del vehículo del viajero).
     *
     * @param categoryId categoría
     */
    public void selectCategory(int categoryId) {
        for (int i = 0; i < category.getItemCount(); i++) {
            if (category.getItemAt(i).getCategoryId() == categoryId) {
                category.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadPrices() {
        VehicleCategory selected = (VehicleCategory) category.getSelectedItem();
        if (selected == null || catalog == null) {
            return;
        }
        int myRequest = ++requestId;
        status.setText("Cargando tarifas...");
        Async.run(() -> service.pricesFor(selected.getCategoryId()), prices -> {
            if (myRequest != requestId) {
                return;
            }
            model.setPrices(prices);
            status.setText(prices.size() + " peajes con tarifa para esta categoría");
            compareBox.removeAll();
            compareBox.add(hint(), BorderLayout.NORTH);
            compareBox.revalidate();
            compareBox.repaint();
        }, error -> status.setText("No se pudieron cargar las tarifas"));
    }

    private void filter() {
        String text = TravelerUi.normalize(search.getText());
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(new RowFilter<RatesModel, Integer>() {
            @Override
            public boolean include(Entry<? extends RatesModel, ? extends Integer> entry) {
                return TravelerUi.normalize(String.valueOf(entry.getValue(0))).contains(text)
                        || TravelerUi.normalize(String.valueOf(entry.getValue(1))).contains(text);
            }
        });
    }

    private void updateButtons() {
        int count = table.getSelectedRowCount();
        compare.setEnabled(count >= 2);
        showOnMap.setEnabled(count == 1);
    }

    private List<Toll> selectedTolls() {
        List<Toll> list = new ArrayList<>();
        for (int row : table.getSelectedRows()) {
            list.add(model.get(table.convertRowIndexToModel(row)));
        }
        return list;
    }

    private void showComparison() {
        List<Toll> tolls = selectedTolls();
        if (tolls.size() > MAX_COMPARE) {
            tolls = tolls.subList(0, MAX_COMPARE);
        }
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.add(left(Theme.title("Comparación")));
        VehicleCategory cat = (VehicleCategory) category.getSelectedItem();
        card.add(left(Theme.muted(cat == null ? " " : "Tarifas para " + TravelerUi.categoryText(cat))));
        card.add(Box.createVerticalStrut(10));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cheapest = null;
        for (Toll t : tolls) {
            BigDecimal p = model.price(t);
            if (p != null) {
                total = total.add(p);
                cheapest = cheapest == null || p.compareTo(cheapest) < 0 ? p : cheapest;
            }
        }
        for (Toll t : tolls) {
            BigDecimal p = model.price(t);
            JPanel line = new JPanel(new BorderLayout(10, 0));
            line.setOpaque(false);
            line.setAlignmentX(LEFT_ALIGNMENT);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            line.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(6, 0, 6, 0)));
            line.add(TravelerUi.label("<html>" + t.getName() + "<br><span style='color:#6b7280;font-size:10px'>"
                    + catalog.departmentName(t.getDepartmentId()) + "</span></html>", Font.PLAIN, 13, Theme.TEXT),
                    BorderLayout.CENTER);
            JLabel price = TravelerUi.label(p == null ? "Sin tarifa" : Theme.money(p), Font.BOLD, 13,
                    p != null && p.equals(cheapest) ? Theme.SUCCESS : Theme.TEXT);
            line.add(price, BorderLayout.EAST);
            card.add(line);
        }
        card.add(Box.createVerticalStrut(12));
        card.add(left(TravelerUi.label("Pasar por todos: " + Theme.money(total), Font.BOLD, 14, Theme.TEXT)));
        card.add(Box.createVerticalStrut(14));
        card.add(left(Theme.title("Distancia entre ellos")));
        card.add(Box.createVerticalStrut(4));
        card.add(left(Theme.muted("En línea recta, en el orden de la selección.")));
        card.add(Box.createVerticalStrut(6));
        double sum = 0;
        for (int i = 1; i < tolls.size(); i++) {
            Toll a = tolls.get(i - 1);
            Toll b = tolls.get(i);
            double km = TravelerService.distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
            sum += km;
            card.add(left(TravelerUi.label("<html>" + a.getName() + " → " + b.getName() + ": <b>"
                    + TravelerUi.km(km) + "</b></html>", Font.PLAIN, 13, Theme.TEXT)));
            card.add(Box.createVerticalStrut(4));
        }
        if (tolls.size() > 2) {
            card.add(Box.createVerticalStrut(4));
            card.add(left(TravelerUi.label("Total: " + TravelerUi.km(sum), Font.BOLD, 13, Theme.TEXT)));
        }
        compareBox.removeAll();
        compareBox.add(card, BorderLayout.NORTH);
        compareBox.revalidate();
        compareBox.repaint();
    }

    private JComponent hint() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.add(Theme.title("Comparar peajes"), BorderLayout.NORTH);
        card.add(TravelerUi.label("<html>Seleccione dos o más peajes en la tabla y pulse <b>Comparar"
                + " seleccionados</b> para ver sus tarifas y la distancia entre ellos.</html>",
                Font.PLAIN, 13, Theme.MUTED), BorderLayout.CENTER);
        return card;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    /** Modelo de la tabla: un peaje por fila con la tarifa de la categoría escogida. */
    private static final class RatesModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Peaje", "Departamento", "Administra", "Estado", "Tarifa"};
        private List<Toll> tolls = Collections.emptyList();
        private Map<Integer, BigDecimal> prices = Collections.emptyMap();
        private Catalog catalog;

        void setTolls(Catalog catalog, List<Toll> tolls) {
            this.catalog = catalog;
            this.tolls = tolls;
            fireTableDataChanged();
        }

        void setPrices(Map<Integer, BigDecimal> prices) {
            this.prices = prices;
            fireTableDataChanged();
        }

        Toll get(int row) {
            return tolls.get(row);
        }

        BigDecimal price(Toll toll) {
            return prices.get(toll.getTollId());
        }

        @Override
        public int getRowCount() {
            return tolls.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 4 ? BigDecimal.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            Toll t = tolls.get(row);
            switch (column) {
                case 0:
                    return t.getName();
                case 1:
                    return catalog.departmentName(t.getDepartmentId());
                case 2:
                    return TravelerUi.levelText(t.getLevel());
                case 3:
                    return TravelerUi.pretty(t.getOperationalStatus());
                default:
                    return prices.get(t.getTollId());
            }
        }
    }
}
