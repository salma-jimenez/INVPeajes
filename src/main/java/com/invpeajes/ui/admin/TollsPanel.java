package com.invpeajes.ui.admin;

import com.invpeajes.model.Toll;
import com.invpeajes.model.TollBooth;
import com.invpeajes.model.TollRate;
import com.invpeajes.model.VehicleCategory;
import com.invpeajes.service.AdminService;
import com.invpeajes.service.AdminService.ClosePair;
import com.invpeajes.service.AdminService.TollUsage;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.Pill;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.traveler.TollMap;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/**
 * "Peajes" del administrador: tabla con los peajes de su entidad (buscador y filtros) y, a la
 * derecha, el peaje escogido en tres pestañas:
 *
 * <ul>
 *   <li><b>Datos</b>: editar sus datos, o eliminarlo (si tiene cobros o empleados se ofrece pasarlo
 *       a "No operativo").</li>
 *   <li><b>Tarifas</b>: el precio de cada categoría y desde cuándo rige.</li>
 *   <li><b>Casetas</b>: agregar, eliminar y cambiar sentido o forma de pago (el estado lo cambia el
 *       jefe de estación).</li>
 * </ul>
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class TollsPanel extends JPanel implements AdminPage {

    private static final String ALL_DEPARTMENTS = "*";
    private static final String DATA = "data";
    private static final String RATES = "rates";
    private static final String BOOTHS = "booths";

    private final AdminService service;
    private final Consumer<Integer> onChanged;
    private final Consumer<Toll> showOnMap;
    private final StatTile operatingTile = new StatTile("Operativos", Theme.SUCCESS, 26);
    private final StatTile noRatesTile = new StatTile("Sin tarifa", Theme.WARNING, 26);
    private final StatTile closedTile = new StatTile("No operativos", Theme.MUTED, 26);
    private final StatTile damagedTile = new StatTile("Siniestrados", Theme.DANGER, 26);
    private final JTextField search = Forms.text("");
    private JComboBox<String> departmentFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter;
    private final TollModel model = new TollModel();
    private final JTable table = new JTable(model);
    private final JLabel countLabel = Theme.muted(" ");
    private final JPanel editor = new JPanel(new BorderLayout(0, 10));
    private final FlatButton registerButton = Theme.primaryButton("+ Registrar peaje");
    private AdminContext context;
    private Toll selected;
    private String tab = DATA;

    /**
     * @param service Servicio del administrador.
     * @param onChanged Se llama después de registrar, editar o borrar un peaje, con el ID del peaje
     *                  a dejar escogido (o null); debe recargar el catálogo.
     * @param showOnMap Abre el mapa en un peaje.
     */
    public TollsPanel(AdminService service, Consumer<Integer> onChanged, Consumer<Toll> showOnMap) {
        super(new BorderLayout(0, 14));
        this.service = service;
        this.onChanged = onChanged;
        this.showOnMap = showOnMap;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(operatingTile);
        tiles.add(noRatesTile);
        tiles.add(closedTile);
        tiles.add(damagedTile);

        List<String> statuses = new ArrayList<>();
        statuses.add(null);
        statuses.addAll(List.of(Labels.TOLL_STATUSES));
        statusFilter = Forms.combo(st -> st == null ? "Todos los estados" : Labels.tollStatus(st), statuses);
        statusFilter.setPreferredSize(new Dimension(170, 36));
        statusFilter.addActionListener(e -> applyFilter());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        Forms.styleTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(Forms.pillRenderer(v -> Labels.tollStatusPill((String) v)));
        int[] widths = {44, 150, 80, 120, 210, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                showToll(row < 0 ? null : model.get(row));
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel listCard = Theme.card();
        listCard.add(toolbar(), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        listCard.add(countLabel, BorderLayout.SOUTH);

        editor.setBackground(Color.WHITE);
        editor.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        editor.setPreferredSize(new Dimension(420, 0));

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(listCard, BorderLayout.CENTER);
        center.add(editor, BorderLayout.EAST);

        registerButton.addActionListener(e -> registerDialog());

        add(tiles, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        showToll(null);
    }

    private JComponent toolbar() {
        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        search.setToolTipText("Buscar por nombre o código");
        tools.add(Forms.field("Buscar por nombre o código", search), BorderLayout.CENTER);
        JPanel filters = new JPanel(new GridLayout(1, 2, 10, 0));
        filters.setOpaque(false);
        filters.add(Forms.field("Departamento", departmentFilter));
        filters.add(Forms.field("Estado", statusFilter));
        tools.add(filters, BorderLayout.EAST);
        return tools;
    }

    @Override
    public void setContext(AdminContext context) {
        this.context = context;
        List<String> departments = new ArrayList<>();
        departments.add(ALL_DEPARTMENTS);
        TreeSet<String> used = new TreeSet<>(Comparator.comparing(id -> context.getCatalog().departmentName(id)));
        context.getMine().forEach(t -> used.add(t.getDepartmentId()));
        departments.addAll(used);
        JComboBox<String> newFilter = Forms.combo(id -> ALL_DEPARTMENTS.equals(id) ? "Todos los departamentos"
                : context.getCatalog().departmentName(id), departments);
        newFilter.setPreferredSize(new Dimension(190, 36));
        newFilter.addActionListener(e -> applyFilter());
        Object previous = departmentFilter.getSelectedItem();
        java.awt.Container parent = departmentFilter.getParent();
        if (parent != null) {
            parent.remove(departmentFilter);
            parent.add(newFilter, BorderLayout.CENTER);
            parent.revalidate();
        }
        departmentFilter = newFilter;
        if (previous != null) {
            departmentFilter.setSelectedItem(previous);
        }

        long operating = count("OPERATIVO");
        operatingTile.setValue(String.valueOf(operating), "cobrando hoy");
        noRatesTile.setValue(String.valueOf(count("SIN_TARIFA")), "falta cargar sus precios");
        closedTile.setValue(String.valueOf(count("NO_OPERATIVO")), "suspendidos o cerrados");
        damagedTile.setValue(String.valueOf(count("SINIESTRADO")), "con daño en la estructura");
        Integer keep = selected == null ? null : selected.getTollId();
        applyFilter();
        if (keep != null) {
            select(keep);
        }
    }

    private long count(String status) {
        return context.getMine().stream().filter(t -> status.equals(t.getOperationalStatus())).count();
    }

    @Override
    public String subtitle(AdminContext context) {
        return "Peajes a cargo de " + Labels.entity(context.getLevel()) + " · " + context.getMine().size() + " de "
                + context.getCatalog().getTolls().size() + " en el inventario nacional";
    }

    @Override
    public JComponent topAction() {
        return registerButton;
    }

    /**
     * Escoge un peaje en la tabla (si es de la entidad).
     * @param tollId ID del peaje.
     */
    public void select(int tollId) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.get(i).getTollId() == tollId) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
        if (context != null && context.mineById(tollId) != null) {
            search.setText("");
            departmentFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.get(i).getTollId() == tollId) {
                    table.setRowSelectionInterval(i, i);
                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                    return;
                }
            }
        }
    }

    private void applyFilter() {
        if (context == null) {
            return;
        }
        String text = normalize(search.getText());
        String department = (String) departmentFilter.getSelectedItem();
        String status = (String) statusFilter.getSelectedItem();
        List<Toll> rows = new ArrayList<>();
        for (Toll t : context.getMine()) {
            if (department != null && !ALL_DEPARTMENTS.equals(department) && !department.equals(t.getDepartmentId())) {
                continue;
            }
            if (status != null && !status.equals(t.getOperationalStatus())) {
                continue;
            }
            if (!text.isEmpty() && !normalize(t.getName()).contains(text)
                    && (t.getCode() == null || !t.getCode().contains(text))) {
                continue;
            }
            rows.add(t);
        }
        Integer keep = selected == null ? null : selected.getTollId();
        model.setRows(rows);
        countLabel.setText("Mostrando " + rows.size() + " de " + context.getMine().size() + " peajes");
        boolean found = false;
        if (keep != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).getTollId() == keep) {
                    table.setRowSelectionInterval(i, i);
                    found = true;
                }
            }
        }
        if (!found) {
            showToll(null);
        }
    }

    // ------------------------------------------------------------------ Editor del peaje

    private void showToll(Toll toll) {
        selected = toll == null ? null : context.mineById(toll.getTollId());
        editor.removeAll();
        if (selected == null) {
            editor.add(Theme.title("Peajes de tu entidad"), BorderLayout.NORTH);
            JLabel hint = new JLabel("<html><div style='width:280px'>Escoge un peaje de la tabla para editar sus "
                    + "<b>datos</b>, sus <b>tarifas</b> por categoría y sus <b>casetas</b>.<br><br>Con <b>+ Registrar "
                    + "peaje</b> agregas uno nuevo; queda \"Sin tarifa\" hasta que le cargues sus precios.</div></html>");
            hint.setFont(Theme.font(Font.PLAIN, 13));
            hint.setForeground(Theme.MUTED);
            hint.setVerticalAlignment(JLabel.TOP);
            editor.add(hint, BorderLayout.CENTER);
        } else {
            JPanel head = new JPanel(new BorderLayout(0, 2));
            head.setOpaque(false);
            JPanel nameRow = new JPanel(new BorderLayout());
            nameRow.setOpaque(false);
            JLabel name = new JLabel(selected.getName());
            name.setFont(Theme.font(Font.BOLD, 19));
            name.setForeground(Theme.TEXT);
            nameRow.add(name, BorderLayout.WEST);
            JPanel pillBox = new JPanel(new java.awt.GridBagLayout());
            pillBox.setOpaque(false);
            pillBox.add(Labels.tollStatusPill(selected.getOperationalStatus()));
            nameRow.add(pillBox, BorderLayout.EAST);
            head.add(nameRow, BorderLayout.NORTH);
            head.add(Theme.muted(context.getCatalog().departmentName(selected.getDepartmentId()) + " · "
                    + context.getCatalog().concessionaireName(selected.getConcessionaireId())), BorderLayout.CENTER);
            head.add(tabs(), BorderLayout.SOUTH);
            editor.add(head, BorderLayout.NORTH);
            CardLayout cards = new CardLayout();
            JPanel body = new JPanel(cards);
            body.setOpaque(false);
            body.add(dataTab(selected), DATA);
            body.add(ratesTab(selected), RATES);
            body.add(boothsTab(selected), BOOTHS);
            cards.show(body, tab);
            editor.add(body, BorderLayout.CENTER);
        }
        editor.revalidate();
        editor.repaint();
    }

    private JComponent tabs() {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                g.setColor(Theme.BORDER);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        String[][] items = {{DATA, "Datos"}, {RATES, "Tarifas"}, {BOOTHS, "Casetas"}};
        for (String[] item : items) {
            JLabel label = new JLabel(item[1]);
            boolean on = item[0].equals(tab);
            label.setFont(Theme.font(Font.BOLD, 14));
            label.setForeground(on ? Theme.TEXT : Theme.MUTED);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, on ? Theme.PRIMARY : new Color(0, 0, 0, 0)),
                    BorderFactory.createEmptyBorder(0, 2, 8, 2)));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    tab = item[0];
                    showToll(selected);
                }
            });
            bar.add(label);
            bar.add(Box.createHorizontalStrut(22));
        }
        return bar;
    }

    // ------------------------------------------------------------------ Pestaña Datos

    private JComponent dataTab(Toll toll) {
        TollForm form = new TollForm(context.getCatalog(), false);
        form.load(toll);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JScrollPane scroll = new JScrollPane(wrapTop(form));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);

        FlatButton delete = dangerButton("Eliminar");
        delete.addActionListener(e -> deleteToll(toll));
        FlatButton map = Theme.secondaryButton("Ver en el mapa");
        map.addActionListener(e -> showOnMap.accept(toll));
        FlatButton save = Theme.primaryButton("Guardar cambios");
        save.addActionListener(e -> {
            String level = context.getLevel();
            Async.run(() -> service.updateToll(level, form.read(toll.getTollId()), form.acceptsColpass()),
                    saved -> {
                        Forms.info(this, "Peajes", "Cambios guardados en " + saved.getName() + ".");
                        onChanged.accept(saved.getTollId());
                    }, error -> Forms.info(this, "Peajes", error.getMessage()));
        });
        JPanel buttons = new JPanel(new BorderLayout(8, 0));
        buttons.setOpaque(false);
        JPanel leftButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(delete);
        leftButtons.add(map);
        buttons.add(leftButtons, BorderLayout.WEST);
        buttons.add(save, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void deleteToll(Toll toll) {
        String level = context.getLevel();
        Async.run(() -> service.usage(toll.getTollId()), usage -> {
            if (usage.canDelete()) {
                Forms.confirm(this, "¿Eliminar el peaje " + toll.getName() + "?", "No tiene cobros ni empleados. "
                        + "Se borran también sus " + usage.getBooths() + " casetas y sus tarifas. <b>No se puede "
                        + "deshacer.</b>", "Eliminar peaje", true,
                        () -> service.deleteToll(level, toll.getTollId()), () -> onChanged.accept(null));
            } else if ("NO_OPERATIVO".equals(toll.getOperationalStatus())) {
                Forms.info(this, "Eliminar peaje", toll.getName() + " tiene " + history(usage)
                        + " y no se puede borrar. Ya está No operativo, así que no aparece como activo.");
            } else {
                JPanel body = new JPanel();
                body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                JLabel text = new JLabel("<html><div style='width:420px'>Este peaje tiene <b>" + history(usage)
                        + "</b> registrados. Si lo eliminas se perdería su historial de recaudo, por eso no se "
                        + "puede borrar.</div></html>");
                text.setFont(Theme.font(Font.PLAIN, 14));
                text.setAlignmentX(LEFT_ALIGNMENT);
                body.add(text);
                body.add(Box.createVerticalStrut(12));
                body.add(Forms.note("<div style='width:400px'><b>Te recomendamos pasarlo a \"No operativo\".</b> Deja "
                        + "de aparecer como activo para los viajeros, sus casetas se desactivan y los operadores ya "
                        + "no pueden abrir turno en él, pero se conserva todo su historial.</div>"));
                Forms.dialog(this, "¿Eliminar el peaje " + toll.getName() + "?", null, body, "Pasar a No operativo",
                        false, () -> {
                            service.changeTollStatus(level, toll.getTollId(), "NO_OPERATIVO");
                            return Boolean.TRUE;
                        }, ok -> onChanged.accept(toll.getTollId()));
            }
        }, error -> Forms.info(this, "Eliminar peaje", error.getMessage()));
    }

    private static String history(TollUsage usage) {
        List<String> parts = new ArrayList<>();
        if (usage.getTickets() > 0) {
            parts.add(String.format("%,d", usage.getTickets()).replace(',', '.')
                    + (usage.getTickets() == 1 ? " tiquete" : " tiquetes"));
        }
        if (usage.getBooths() > 0) {
            parts.add(usage.getBooths() + (usage.getBooths() == 1 ? " caseta" : " casetas"));
        }
        if (usage.getUsers() > 0) {
            parts.add(usage.getUsers() + (usage.getUsers() == 1 ? " empleado" : " empleados"));
        }
        return parts.size() <= 1 ? String.join("", parts)
                : String.join(", ", parts.subList(0, parts.size() - 1)) + " y " + parts.get(parts.size() - 1);
    }

    // ------------------------------------------------------------------ Pestaña Tarifas

    private JComponent ratesTab(Toll toll) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel loading = Theme.muted("Cargando tarifas...");
        panel.add(loading, BorderLayout.NORTH);
        Async.run(() -> service.rates(toll.getTollId()), rates -> {
            panel.removeAll();
            fillRates(panel, toll, rates);
            panel.revalidate();
            panel.repaint();
        }, error -> loading.setText("No se pudieron cargar las tarifas: " + error.getMessage()));
        return panel;
    }

    private void fillRates(JPanel panel, Toll toll, List<TollRate> rates) {
        Map<Integer, TollRate> current = new HashMap<>();
        LocalDate lastDate = null;
        for (TollRate r : rates) {
            current.put(r.getCategoryId(), r);
            if (r.getEffectiveDate() != null && (lastDate == null || r.getEffectiveDate().isAfter(lastDate))) {
                lastDate = r.getEffectiveDate();
            }
        }
        JPanel grid = new JPanel();
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.setOpaque(false);
        grid.add(rateRow(label("Cat.", true), label("Vehículo", true), label("Actual", true), label("Nueva", true)));
        if (lastDate != null) {
            JLabel since0 = Theme.muted("Tarifas vigentes desde " + Forms.formatDate(lastDate));
            since0.setFont(Theme.font(Font.PLAIN, 12));
            since0.setAlignmentX(LEFT_ALIGNMENT);
            grid.add(since0, 0);
        }
        Map<Integer, JTextField> fields = new LinkedHashMap<>();
        List<VehicleCategory> categories = new ArrayList<>(context.getCatalog().getCategories());
        categories.sort(Comparator.comparingInt(VehicleCategory::getCategoryId));
        for (VehicleCategory c : categories) {
            TollRate rate = current.get(c.getCategoryId());
            JTextField price = Forms.text(rate == null ? "" : plain(rate.getPrice()));
            price.setHorizontalAlignment(JTextField.RIGHT);
            price.setFont(Theme.font(Font.BOLD, 14));
            price.setPreferredSize(new Dimension(100, 28));
            String original = price.getText();
            price.getDocument().addDocumentListener(new DocumentListener() {
                private void mark() {
                    boolean changed = !price.getText().trim().equals(original);
                    price.setBackground(changed ? new Color(0xFFFBEB) : Color.WHITE);
                    price.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(changed ? Theme.WARNING : Theme.INPUT_BORDER, 1, true),
                            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    mark();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    mark();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    mark();
                }
            });
            fields.put(c.getCategoryId(), price);
            JLabel vehicle = new JLabel(c.getDescription());
            vehicle.setToolTipText(c.getDescription());
            vehicle.setFont(Theme.font(Font.PLAIN, 12));
            vehicle.setForeground(Theme.TEXT);
            grid.add(rateRow(label(Labels.roman(c.getCategoryId()), true), vehicle,
                    label(rate == null ? "—" : Theme.money(rate.getPrice()), false), price));
        }
        panel.add(grid, BorderLayout.NORTH);

        JTextField since = Forms.text(Forms.formatDate(LocalDate.now()));
        String level = context.getLevel();
        String source = "ANI".equals(level) || "INVIAS".equals(level) ? Labels.entity(level) : "—";
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(Forms.row(Forms.field("Vigente desde", since), Forms.field("Fuente", Forms.readOnly(source))),
                BorderLayout.NORTH);
        if (rates.isEmpty()) {
            bottom.add(Theme.muted("Este peaje todavía no tiene tarifas."), BorderLayout.CENTER);
        }
        FlatButton discard = Theme.secondaryButton("Descartar");
        discard.addActionListener(e -> showToll(selected));
        FlatButton save = Theme.primaryButton("Guardar tarifas");
        save.addActionListener(e -> {
            Map<Integer, BigDecimal> changes = new LinkedHashMap<>();
            for (Map.Entry<Integer, JTextField> f : fields.entrySet()) {
                String text = f.getValue().getText().trim().replace(".", "").replace("$", "");
                TollRate old = current.get(f.getKey());
                if (text.isEmpty()) {
                    continue;
                }
                BigDecimal value;
                try {
                    value = new BigDecimal(text.replace(',', '.'));
                } catch (NumberFormatException ex) {
                    Forms.info(this, "Tarifas", "Revisa el precio de la categoría " + Labels.roman(f.getKey()));
                    return;
                }
                if (old == null || old.getPrice().compareTo(value) != 0) {
                    changes.put(f.getKey(), value);
                }
            }
            LocalDate date = Forms.parseDate(since.getText());
            if (changes.isEmpty()) {
                Forms.info(this, "Tarifas", "No cambiaste ninguna tarifa.");
                return;
            }
            Async.run(() -> service.saveRates(level, toll.getTollId(), changes, date), operating -> {
                Forms.info(this, "Tarifas", changes.size() + (changes.size() == 1 ? " tarifa guardada." : " tarifas guardadas.")
                        + (operating ? "\n" + toll.getName() + " ya tiene todas sus tarifas y pasó a Operativo." : ""));
                tab = RATES;
                onChanged.accept(toll.getTollId());
            }, error -> Forms.info(this, "Tarifas", error.getMessage()));
        });
        JPanel buttons = new JPanel(new BorderLayout(8, 0));
        buttons.setOpaque(false);
        buttons.add(discard, BorderLayout.WEST);
        buttons.add(save, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);
    }

    private static JComponent rateRow(JComponent cat, JComponent vehicle, JComponent current, JComponent price) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF0F2F6)),
                BorderFactory.createEmptyBorder(1, 0, 1, 0)));
        cat.setPreferredSize(new Dimension(30, 24));
        JPanel right = new JPanel(new BorderLayout(8, 0));
        right.setOpaque(false);
        current.setPreferredSize(new Dimension(70, 24));
        if (!(price instanceof JTextField)) {
            price.setPreferredSize(new Dimension(100, 20));
        }
        right.add(current, BorderLayout.WEST);
        right.add(price, BorderLayout.EAST);
        row.add(cat, BorderLayout.WEST);
        row.add(vehicle, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private static JLabel label(String text, boolean bold) {
        JLabel label = new JLabel(text, bold && !"Vehículo".equals(text) && !"Cat.".equals(text)
                && !text.matches("[IVX]+") ? JLabel.RIGHT : JLabel.LEFT);
        label.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, bold && text.length() > 3 ? 12 : 13));
        label.setForeground(bold && text.length() > 3 && !text.matches("[IVX]+") ? Theme.MUTED : Theme.TEXT);
        if (!bold) {
            label.setHorizontalAlignment(JLabel.RIGHT);
        }
        return label;
    }

    private static String plain(BigDecimal value) {
        return String.format(Locale.ROOT, "%,d", value.longValue()).replace(',', '.');
    }

    // ------------------------------------------------------------------ Pestaña Casetas

    private JComponent boothsTab(Toll toll) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel loading = Theme.muted("Cargando casetas...");
        panel.add(loading, BorderLayout.NORTH);
        Async.run(() -> service.booths(toll.getTollId()), booths -> {
            panel.removeAll();
            fillBooths(panel, toll, booths, booths.isEmpty() ? null : booths.get(0));
            panel.revalidate();
            panel.repaint();
        }, error -> loading.setText("No se pudieron cargar las casetas: " + error.getMessage()));
        return panel;
    }

    private void fillBooths(JPanel panel, Toll toll, List<TollBooth> booths, TollBooth chosen) {
        panel.removeAll();
        long active = booths.stream().filter(b -> "ACTIVA".equals(b.getStatus())).count();
        long maintenance = booths.stream().filter(b -> "MANTENIMIENTO".equals(b.getStatus())).count();
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.muted(booths.isEmpty() ? "Sin casetas" : active + (active == 1 ? " activa" : " activas")
                + (maintenance > 0 ? " · " + maintenance + " en mantenimiento" : "")), BorderLayout.WEST);
        FlatButton add = Theme.primaryButton("+ Agregar caseta");
        add.addActionListener(e -> addBoothDialog(toll, booths.size() + 1));
        head.add(add, BorderLayout.EAST);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        for (TollBooth b : booths) {
            boolean on = chosen != null && b.getBoothId().equals(chosen.getBoothId());
            JPanel row = new JPanel(new BorderLayout(0, 2));
            row.setBackground(on ? new Color(0xEFF6FF) : Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(on ? new Color(0x93C5FD) : Theme.BORDER),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            JLabel name = new JLabel("Caseta " + b.getBoothNumber());
            name.setFont(Theme.font(Font.BOLD, 14));
            top.add(name, BorderLayout.WEST);
            top.add(Labels.boothStatusPill(b.getStatus()), BorderLayout.EAST);
            row.add(top, BorderLayout.NORTH);
            JLabel detail = Theme.muted(Labels.direction(b.getDirection()) + " · " + Labels.paymentShort(b.getPaymentType()));
            detail.setFont(Theme.font(Font.PLAIN, 12));
            row.add(detail, BorderLayout.CENTER);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    fillBooths(panel, toll, booths, b);
                }
            });
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            list.add(row);
            list.add(Box.createVerticalStrut(6));
        }
        JScrollPane scroll = new JScrollPane(wrapTop(list));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(scroll, BorderLayout.CENTER);
        panel.add(top, BorderLayout.CENTER);
        if (chosen != null) {
            panel.add(boothEditor(panel, toll, booths, chosen), BorderLayout.SOUTH);
        }
        panel.revalidate();
        panel.repaint();
    }

    private JComponent boothEditor(JPanel panel, Toll toll, List<TollBooth> booths, TollBooth booth) {
        JComboBox<String> direction = Forms.combo(Labels::direction, withNull(Labels.DIRECTIONS));
        direction.setSelectedItem(booth.getDirection());
        JComboBox<String> payment = Forms.combo(Labels::paymentShort, List.of(Labels.PAYMENT_TYPES));
        payment.setSelectedItem(booth.getPaymentType());
        JPanel edit = new JPanel();
        edit.setLayout(new BoxLayout(edit, BoxLayout.Y_AXIS));
        edit.setOpaque(false);
        edit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 0, 0, 0)));
        JLabel title = new JLabel("Editar caseta " + booth.getBoothNumber());
        title.setFont(Theme.font(Font.BOLD, 14));
        title.setAlignmentX(LEFT_ALIGNMENT);
        edit.add(title);
        edit.add(Box.createVerticalStrut(6));
        edit.add(Forms.row(Forms.field("Sentido", direction), Forms.field("Forma de pago", payment)));
        edit.add(Box.createVerticalStrut(6));
        JLabel note = new JLabel("<html><div style='width:285px'>El estado (activa, mantenimiento, inactiva) lo cambia "
                + "el jefe de estación del peaje.</div></html>");
        note.setFont(Theme.font(Font.PLAIN, 12));
        note.setForeground(Theme.MUTED);
        note.setAlignmentX(LEFT_ALIGNMENT);
        edit.add(note);
        edit.add(Box.createVerticalStrut(8));
        FlatButton delete = dangerButton("Eliminar caseta");
        String level = context.getLevel();
        delete.addActionListener(e -> Forms.confirm(this, "¿Eliminar la caseta " + booth.getBoothNumber() + "?",
                "Solo se puede eliminar si no tiene cobros. Si ya cobró, pídele al jefe de estación que la desactive.",
                "Eliminar caseta", true, () -> service.deleteBooth(level, booth), () -> refreshBooths(panel, toll, null)));
        FlatButton save = Theme.primaryButton("Guardar");
        save.addActionListener(e -> {
            TollBooth changed = new TollBooth(booth.getBoothId(), booth.getBoothNumber(),
                    (String) payment.getSelectedItem(), (String) direction.getSelectedItem(), booth.getStatus(),
                    booth.getTollId());
            Async.run(() -> {
                service.updateBooth(level, changed);
                return Boolean.TRUE;
            }, ok -> refreshBooths(panel, toll, booth.getBoothId()), error -> Forms.info(this, "Casetas",
                    error.getMessage()));
        });
        JPanel buttons = new JPanel(new BorderLayout(8, 0));
        buttons.setOpaque(false);
        buttons.add(delete, BorderLayout.WEST);
        buttons.add(save, BorderLayout.CENTER);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        edit.add(buttons);
        return edit;
    }

    private void refreshBooths(JPanel panel, Toll toll, Integer keep) {
        Async.run(() -> service.booths(toll.getTollId()), booths -> {
            TollBooth chosen = booths.isEmpty() ? null : booths.get(0);
            for (TollBooth b : booths) {
                if (keep != null && b.getBoothId().equals(keep)) {
                    chosen = b;
                }
            }
            fillBooths(panel, toll, booths, chosen);
        }, error -> Forms.info(this, "Casetas", error.getMessage()));
    }

    private void addBoothDialog(Toll toll, int number) {
        JComboBox<String> direction = Forms.combo(Labels::direction, withNull(Labels.DIRECTIONS));
        JComboBox<String> payment = Forms.combo(Labels::payment, List.of(Labels.PAYMENT_TYPES));
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(Forms.row(Forms.field("Sentido *", direction), Forms.field("Forma de pago *", payment)));
        body.add(Box.createVerticalStrut(12));
        JLabel note = new JLabel("<html><div style='width:420px'>" + ("OPERATIVO".equals(toll.getOperationalStatus())
                ? "Empieza activa; el jefe de estación le cambia el estado cuando haga falta."
                : "El peaje no está operativo: la caseta empieza inactiva.")
                + "<br>Formas de pago: Efectivo · Mixto (efectivo, tarjeta y TAG) · Solo COLPASS (carril exclusivo "
                + "de TAG).</div></html>");
        note.setFont(Theme.font(Font.PLAIN, 12));
        note.setForeground(Theme.MUTED);
        note.setAlignmentX(LEFT_ALIGNMENT);
        body.add(note);
        String level = context.getLevel();
        Forms.dialog(this, "Agregar caseta · " + toll.getName(), "Será la caseta número " + number + " del peaje.",
                body, "Agregar caseta", false, () -> service.addBooth(level, toll.getTollId(),
                        (String) direction.getSelectedItem(), (String) payment.getSelectedItem()),
                booth -> {
                    tab = BOOTHS;
                    showToll(selected);
                });
    }

    // ------------------------------------------------------------------ Registrar peaje

    private void registerDialog() {
        if (context == null) {
            return;
        }
        TollForm form = new TollForm(context.getCatalog(), true);
        TollMap map = new TollMap();
        map.setPreferredSize(new Dimension(430, 300));
        map.showTolls(context.getCatalog().getTolls());
        JLabel coords = Theme.muted("Haz clic en el mapa para marcar dónde queda el peaje.");
        JLabel warning = new JLabel(" ");
        warning.setFont(Theme.font(Font.PLAIN, 12));
        warning.setForeground(new Color(0x92400E));
        double[] point = new double[2];
        boolean[] picked = {false};
        Runnable check = () -> {
            if (!picked[0]) {
                return;
            }
            ClosePair near = AdminService.nearestOnRoute(context.getCatalog().getTolls(), form.selectedRoute(),
                    point[0], point[1], 0);
            warning.setText(near == null ? " " : String.format(Locale.forLanguageTag("es-CO"), "⚠ A %.1f km de %s%s.",
                    near.getKm(), near.getFirst().getName(), form.selectedRoute() == null ? "" : " sobre la misma ruta"));
        };
        map.setOnPick(pos -> {
            point[0] = pos.getLatitude();
            point[1] = pos.getLongitude();
            picked[0] = true;
            form.setLocation(pos.getLatitude(), pos.getLongitude());
            coords.setText(String.format(Locale.ROOT, "Latitud %.6f · Longitud %.6f", pos.getLatitude(),
                    pos.getLongitude()));
            check.run();
        });
        form.onRouteChange(check);
        map.fitAll();

        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.setOpaque(false);
        JLabel mapLabel = new JLabel("<html>Ubicación en el mapa * <span style='color:#2563EB'>· haz clic para "
                + "marcarla</span></html>");
        mapLabel.setFont(Theme.font(Font.PLAIN, 13));
        mapLabel.setForeground(Theme.MUTED);
        right.add(mapLabel, BorderLayout.NORTH);
        map.setBorder(BorderFactory.createLineBorder(Theme.INPUT_BORDER));
        right.add(map, BorderLayout.CENTER);
        JPanel under = new JPanel(new GridLayout(2, 1));
        under.setOpaque(false);
        under.add(coords);
        under.add(warning);
        right.add(under, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        form.setPreferredSize(new Dimension(420, form.getPreferredSize().height));
        body.add(form, BorderLayout.CENTER);
        body.add(right, BorderLayout.EAST);
        String level = context.getLevel();
        Forms.dialog(this, "Registrar peaje", "Queda a cargo de " + Labels.entity(level) + ". Después de guardarlo "
                + "podrás cargar sus tarifas y casetas.", body, "Registrar peaje", false,
                () -> service.registerToll(level, form.read(0), form.acceptsColpass()), toll -> {
                    Forms.info(this, "Registrar peaje", toll.getName() + " quedó registrado como \"Sin tarifa\".\n"
                            + "Ahora carga sus tarifas en la pestaña Tarifas.");
                    tab = RATES;
                    onChanged.accept(toll.getTollId());
                });
    }

    // ------------------------------------------------------------------ Apoyo

    private static FlatButton dangerButton(String text) {
        FlatButton button = Theme.secondaryButton(text);
        button.setColors(Color.WHITE, new Color(0xFEF2F2), Theme.DANGER, new Color(0xFCA5A5));
        return button;
    }

    private static JComponent wrapTop(JComponent c) {
        return Forms.fitWidth(c);
    }

    private static List<String> withNull(String[] values) {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.addAll(List.of(values));
        return list;
    }

    private static String normalize(String text) {
        return java.text.Normalizer.normalize(text == null ? "" : text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** Filas de la tabla de peajes. */
    private final class TollModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Peaje", "Km", "Departamento", "Concesionario", "Estado"};
        private List<Toll> rows = new ArrayList<>();

        void setRows(List<Toll> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Toll get(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Toll t = rows.get(row);
            switch (column) {
                case 0:
                    return t.getTollId();
                case 1:
                    return t.getName();
                case 2:
                    return t.getKilometer() == null ? "—" : t.getKilometer();
                case 3:
                    return context.getCatalog().departmentName(t.getDepartmentId());
                case 4:
                    return context.getCatalog().concessionaireName(t.getConcessionaireId());
                default:
                    return t.getOperationalStatus();
            }
        }
    }
}
