package com.invpeajes.ui.admin;

import com.invpeajes.model.Toll;
import com.invpeajes.model.TollUser;
import com.invpeajes.service.AdminService;
import com.invpeajes.service.AuthService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.EmployeeForm;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
 * "Empleados" del administrador: los jefes de estación y los operadores de los peajes de su
 * entidad. El administrador registra y edita a los <b>jefes de estación</b> (también les
 * restablece la contraseña y los activa o desactiva). A los operadores solo los ve: los maneja el
 * jefe de cada peaje.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StaffPanel extends JPanel implements AdminPage {

    private static final String BOTH = "Jefes y operadores";
    private static final String MANAGERS = "Solo jefes de estación";
    private static final String OPERATORS = "Solo operadores";
    private static final int ALL_TOLLS = 0;

    private final AdminService service;
    private final StatTile managersTile = new StatTile("Jefes de estación", Theme.PURPLE, 26);
    private final StatTile noManagerTile = new StatTile("Peajes sin jefe", Theme.DANGER, 26);
    private final StatTile operatorsTile = new StatTile("Operadores de caseta", Theme.PRIMARY, 26);
    private final StatTile inactiveTile = new StatTile("Inactivos", Theme.MUTED, 26);
    private final JTextField search = Forms.text("");
    private final JComboBox<String> roleFilter = Forms.combo(s -> s, List.of(BOTH, MANAGERS, OPERATORS));
    private JComboBox<Integer> tollFilter = new JComboBox<>();
    private final StaffModel model = new StaffModel();
    private final JTable table = new JTable(model);
    private final JLabel countLabel = Theme.muted(" ");
    private final JPanel editor = new JPanel(new BorderLayout(0, 12));
    private final FlatButton registerButton = Theme.primaryButton("+ Registrar jefe de estación");
    private AdminContext context;
    private List<TollUser> staff = new ArrayList<>();
    private List<Toll> withoutManager = new ArrayList<>();

    /**
     * @param service Servicio del administrador.
     */
    public StaffPanel(AdminService service) {
        super(new BorderLayout(0, 14));
        this.service = service;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(managersTile);
        tiles.add(noManagerTile);
        tiles.add(operatorsTile);
        tiles.add(inactiveTile);

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
        roleFilter.addActionListener(e -> applyFilter());
        roleFilter.setPreferredSize(new Dimension(190, 36));

        Forms.styleTable(table);
        table.getColumnModel().getColumn(2).setCellRenderer(Forms.pillRenderer(v -> Labels.rolePill((String) v)));
        table.getColumnModel().getColumn(4).setCellRenderer(Forms.pillRenderer(v -> Labels.activePill((Boolean) v)));
        int[] widths = {100, 180, 150, 120, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        tools.add(Forms.field("Buscar por cédula o nombre", search), BorderLayout.CENTER);
        JPanel filters = new JPanel(new GridLayout(1, 2, 10, 0));
        filters.setOpaque(false);
        filters.add(Forms.field("Cargo", roleFilter));
        filters.add(Forms.field("Peaje", tollFilter));
        tools.add(filters, BorderLayout.EAST);

        JPanel listCard = Theme.card();
        listCard.add(tools, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        listCard.add(countLabel, BorderLayout.SOUTH);

        editor.setBackground(Color.WHITE);
        editor.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        editor.setPreferredSize(new Dimension(370, 0));

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(listCard, BorderLayout.CENTER);
        center.add(editor, BorderLayout.EAST);

        registerButton.addActionListener(e -> registerDialog(null));

        add(tiles, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        showEmpty();
    }

    @Override
    public void setContext(AdminContext context) {
        this.context = context;
        List<Integer> tolls = new ArrayList<>();
        tolls.add(ALL_TOLLS);
        context.getMine().forEach(t -> tolls.add(t.getTollId()));
        JComboBox<Integer> newFilter = Forms.combo(id -> id == ALL_TOLLS ? "Todos los peajes" : tollName(id), tolls);
        newFilter.setPreferredSize(new Dimension(180, 36));
        newFilter.addActionListener(e -> applyFilter());
        java.awt.Container parent = tollFilter.getParent();
        if (parent != null) {
            parent.remove(tollFilter);
            parent.add(newFilter, BorderLayout.CENTER);
            parent.revalidate();
        }
        tollFilter = newFilter;
    }

    @Override
    public String subtitle(AdminContext context) {
        return "Tú registras a los jefes de estación; cada jefe registra a los operadores de su peaje";
    }

    @Override
    public JComponent topAction() {
        return registerButton;
    }

    @Override
    public void refresh() {
        if (context == null) {
            return;
        }
        String level = context.getLevel();
        String keep = selectedId();
        Async.run(() -> service.staffOf(level), list -> {
            staff = list;
            fillTiles();
            applyFilter();
            select(keep);
        }, error -> Forms.info(this, "Empleados", "No se pudieron cargar los empleados: " + error.getMessage()));
    }

    private void fillTiles() {
        long managers = staff.stream().filter(u -> isManager(u) && u.isActive()).count();
        long operators = staff.stream().filter(u -> !isManager(u) && u.isActive()).count();
        long inactive = staff.stream().filter(u -> !u.isActive()).count();
        Set<Integer> withManager = new HashSet<>();
        staff.stream().filter(u -> isManager(u) && u.isActive() && u.getTollId() != null)
                .forEach(u -> withManager.add(u.getTollId()));
        withoutManager = new ArrayList<>();
        for (Toll t : context.getMine()) {
            if (!withManager.contains(t.getTollId()) && !"NO_OPERATIVO".equals(t.getOperationalStatus())) {
                withoutManager.add(t);
            }
        }
        managersTile.setValue(String.valueOf(managers), "en " + context.getMine().size() + " peajes");
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < Math.min(3, withoutManager.size()); i++) {
            names.append(i == 0 ? "" : ", ").append(withoutManager.get(i).getName());
        }
        if (withoutManager.size() > 3) {
            names.append(" y ").append(withoutManager.size() - 3).append(" más");
        }
        noManagerTile.setValue(String.valueOf(withoutManager.size()),
                withoutManager.isEmpty() ? "todos tienen jefe" : names.toString());
        operatorsTile.setValue(String.valueOf(operators), "los registra el jefe de cada peaje");
        inactiveTile.setValue(String.valueOf(inactive), "no pueden iniciar sesión");
    }

    private void applyFilter() {
        if (context == null) {
            return;
        }
        String text = search.getText().trim().toLowerCase(Locale.ROOT);
        String role = (String) roleFilter.getSelectedItem();
        Integer toll = (Integer) tollFilter.getSelectedItem();
        List<TollUser> rows = new ArrayList<>();
        for (TollUser u : staff) {
            if ((MANAGERS.equals(role) && !isManager(u)) || (OPERATORS.equals(role) && isManager(u))) {
                continue;
            }
            if (toll != null && toll != ALL_TOLLS && !toll.equals(u.getTollId())) {
                continue;
            }
            if (!text.isEmpty() && !u.getIdNumber().contains(text)
                    && !u.getFullName().toLowerCase(Locale.ROOT).contains(text)) {
                continue;
            }
            rows.add(u);
        }
        String keep = selectedId();
        model.setRows(rows);
        countLabel.setText("Mostrando " + rows.size() + " de " + staff.size() + " empleados");
        select(keep);
    }

    private String selectedId() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.get(row).getIdNumber();
    }

    private void select(String idNumber) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.get(i).getIdNumber().equals(idNumber)) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
        showSelected();
    }

    private void showSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showEmpty();
            return;
        }
        TollUser user = model.get(row);
        boolean manager = isManager(user);
        JComboBox<Integer> toll = tollCombo();
        toll.setSelectedItem(user.getTollId());
        toll.setEnabled(manager);
        JComponent extra = Forms.row(Forms.field("Cargo", Forms.readOnly(Labels.role(user.getPosition()))),
                Forms.field("Peaje", toll));
        EmployeeForm form = new EmployeeForm(false, true, extra);
        form.load(user);
        form.setEditable(manager);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(form);
        body.add(Box.createVerticalStrut(10));
        if (manager) {
            JLabel pw = Theme.muted("Contraseña" + (user.isDefaultPassword() ? " · todavía es su cédula" : ""));
            pw.setAlignmentX(LEFT_ALIGNMENT);
            body.add(pw);
            body.add(Box.createVerticalStrut(4));
            FlatButton reset = Theme.secondaryButton("↺ Restablecer a la cédula");
            reset.setAlignmentX(LEFT_ALIGNMENT);
            reset.addActionListener(e -> Forms.confirm(this, "¿Restablecer la contraseña de " + user.getFullName()
                    + "?", "Su nueva contraseña será su cédula: <b>" + user.getIdNumber() + "</b>. Díselo para que "
                    + "pueda entrar y cambiarla.", "Restablecer", false,
                    () -> service.resetManagerPassword(context.getLevel(), user.getIdNumber()), this::refresh));
            body.add(reset);
        } else {
            JLabel note = new JLabel("<html><div style='width:230px'>Solo puedes ver a los operadores: sus datos, su "
                    + "contraseña y su estado los maneja el jefe de estación de " + tollName(user.getTollId())
                    + ".</div></html>");
            note.setFont(Theme.font(Font.PLAIN, 12));
            note.setForeground(Theme.MUTED);
            note.setAlignmentX(LEFT_ALIGNMENT);
            body.add(note);
        }

        editor.removeAll();
        editor.add(Theme.title((manager ? "Editar jefe de estación · " : "Operador · ") + user.getFirstName() + " "
                + user.getFirstSurname()), BorderLayout.NORTH);
        JPanel holder = Forms.fitWidth(body);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        editor.add(scroll, BorderLayout.CENTER);
        if (manager) {
            FlatButton save = Theme.primaryButton("Guardar cambios");
            save.addActionListener(e -> save(form, user, toll));
            FlatButton toggle = Theme.secondaryButton(user.isActive() ? "Desactivar" : "Activar");
            if (user.isActive()) {
                toggle.setColors(Color.WHITE, new Color(0xFEF2F2), Theme.DANGER, new Color(0xFCA5A5));
            }
            toggle.addActionListener(e -> toggleActive(user));
            JPanel buttons = new JPanel(new BorderLayout(8, 0));
            buttons.setOpaque(false);
            buttons.add(save, BorderLayout.CENTER);
            buttons.add(toggle, BorderLayout.EAST);
            editor.add(buttons, BorderLayout.SOUTH);
        }
        editor.revalidate();
        editor.repaint();
    }

    private void showEmpty() {
        editor.removeAll();
        editor.add(Theme.title("Jefes de estación"), BorderLayout.NORTH);
        JLabel hint = new JLabel("<html><div style='width:245px'>Escoge un jefe de estación para editar sus datos, "
                + "cambiarlo de peaje, restablecer su contraseña o desactivarlo.<br><br>Los <b>operadores</b> aparecen "
                + "para que sepas quién trabaja en cada peaje, pero los maneja el jefe de su peaje.</div></html>");
        hint.setFont(Theme.font(Font.PLAIN, 13));
        hint.setForeground(Theme.MUTED);
        hint.setVerticalAlignment(JLabel.TOP);
        editor.add(hint, BorderLayout.CENTER);
        editor.revalidate();
        editor.repaint();
    }

    private void save(EmployeeForm form, TollUser user, JComboBox<Integer> toll) {
        String level = context.getLevel();
        Async.run(() -> {
            TollUser changed = form.read(user);
            changed.setTollId((Integer) toll.getSelectedItem());
            service.updateManager(level, changed);
            return Boolean.TRUE;
        }, ok -> {
            Forms.info(this, "Empleados", "Cambios guardados.");
            refresh();
        }, error -> Forms.info(this, "Empleados", error.getMessage()));
    }

    private void toggleActive(TollUser user) {
        boolean activate = !user.isActive();
        String level = context.getLevel();
        Forms.confirm(this, (activate ? "¿Activar a " : "¿Desactivar a ") + user.getFullName() + "?",
                activate ? "Podrá volver a iniciar sesión como jefe de estación de " + tollName(user.getTollId()) + "."
                        : "Ya no podrá iniciar sesión. Su historial se conserva. " + tollName(user.getTollId())
                        + " quedará sin jefe hasta que registres o actives otro.",
                activate ? "Activar" : "Desactivar", !activate,
                () -> service.setManagerActive(level, user.getIdNumber(), activate), this::refresh);
    }

    private void registerDialog(Integer preselected) {
        if (context == null) {
            return;
        }
        JComboBox<Integer> toll = tollCombo();
        Integer first = preselected != null ? preselected
                : withoutManager.isEmpty() ? null : withoutManager.get(0).getTollId();
        if (first != null) {
            toll.setSelectedItem(first);
        }
        JLabel note = Forms.note(" ");
        Runnable check = () -> {
            Integer id = (Integer) toll.getSelectedItem();
            boolean empty = withoutManager.stream().anyMatch(t -> id != null && t.getTollId() == id);
            note.setText("<html><div style='width:440px'>" + (empty ? tollName(id) + " no tiene jefe de estación: "
                    + "quedará a cargo de sus solicitudes de asistencia, sus casetas y sus operadores."
                    : tollName(id) + " ya tiene jefe de estación; quedarán los dos.") + "</div></html>");
        };
        toll.addActionListener(e -> check.run());
        check.run();
        JComponent extra = Forms.row(Forms.field("Cargo", Forms.readOnly("Jefe de estación")),
                Forms.field("Peaje *", toll));
        EmployeeForm form = new EmployeeForm(true, true, extra);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(form);
        body.add(Box.createVerticalStrut(12));
        body.add(note);
        String level = context.getLevel();
        Forms.dialog(this, "Registrar jefe de estación", "Su contraseña inicial será su cédula; la puede cambiar "
                + "después.", body, "Registrar jefe", false,
                () -> service.registerManager(level, form.read(null), (Integer) toll.getSelectedItem()), user -> {
                    refresh();
                    Forms.info(this, "Empleados", user.getFullName() + " quedó registrado como jefe de estación de "
                            + tollName(user.getTollId()) + ".\nEntra con su cédula " + user.getIdNumber()
                            + " y esa misma es su contraseña.");
                });
    }

    private JComboBox<Integer> tollCombo() {
        List<Integer> ids = new ArrayList<>();
        context.getMine().forEach(t -> ids.add(t.getTollId()));
        JComboBox<Integer> combo = Forms.combo(this::tollName, ids);
        combo.setPreferredSize(new Dimension(160, 36));
        return combo;
    }

    private String tollName(Integer tollId) {
        if (tollId == null || context == null) {
            return "—";
        }
        Toll t = context.getCatalog().toll(tollId);
        return t == null ? "Peaje " + tollId : t.getName();
    }

    private static boolean isManager(TollUser u) {
        return AuthService.ROLE_MANAGER.equals(u.getPosition());
    }

    /** Filas de la tabla de empleados. */
    private final class StaffModel extends AbstractTableModel {
        private final String[] columns = {"Cédula", "Nombre", "Cargo", "Peaje", "Estado"};
        private List<TollUser> rows = new ArrayList<>();

        void setRows(List<TollUser> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        TollUser get(int row) {
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
            TollUser u = rows.get(row);
            switch (column) {
                case 0:
                    return u.getIdNumber();
                case 1:
                    return u.getFullName();
                case 2:
                    return u.getPosition();
                case 3:
                    return tollName(u.getTollId());
                default:
                    return u.isActive();
            }
        }
    }
}
