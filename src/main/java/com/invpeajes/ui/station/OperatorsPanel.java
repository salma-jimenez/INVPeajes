package com.invpeajes.ui.station;

import com.invpeajes.model.BoothActivity;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.TollUser;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.EmployeeForm;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.Pill;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/**
 * "Empleados del peaje" del jefe de estación: los operadores de caseta de su peaje. El jefe los
 * registra (su contraseña inicial es la cédula), edita sus datos, les restablece la contraseña y
 * los activa o desactiva. También ve quién está cobrando ahora y en qué caseta.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class OperatorsPanel extends JPanel {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ALL = "Todos";
    private static final String ACTIVE = "Activos";
    private static final String INACTIVE = "Inactivos";

    private final StationManagerService service;
    private final CashierSession session;
    private final StatTile activeTile = new StatTile("Operadores activos", Theme.PRIMARY, 26);
    private final StatTile workingTile = new StatTile("En turno ahora", Theme.SUCCESS, 26);
    private final StatTile defaultTile = new StatTile("Usan la cédula como contraseña", Theme.TEXT, 26);
    private final JTextField search = Forms.text("");
    private final JComboBox<String> filter = Forms.combo(s -> s, List.of(ALL, ACTIVE, INACTIVE));
    private final OperatorModel model = new OperatorModel();
    private final JTable table = new JTable(model);
    private final JPanel editor = new JPanel(new BorderLayout(0, 12));
    private List<TollUser> operators = new ArrayList<>();
    private Map<String, String> workingAt = new HashMap<>();

    /**
     * @param service Servicio del jefe de estación.
     * @param session Sesión del jefe (con su peaje).
     */
    public OperatorsPanel(StationManagerService service, CashierSession session) {
        super(new BorderLayout(0, 14));
        this.service = service;
        this.session = session;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 3, 14, 0));
        tiles.setOpaque(false);
        tiles.add(activeTile);
        tiles.add(workingTile);
        tiles.add(defaultTile);

        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        search.setToolTipText("Buscar por cédula o nombre");
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
        filter.addActionListener(e -> applyFilter());
        filter.setPreferredSize(new Dimension(130, 36));
        FlatButton add = Theme.primaryButton("+ Registrar operador");
        add.addActionListener(e -> registerDialog());
        JPanel right = new JPanel(new BorderLayout(10, 0));
        right.setOpaque(false);
        right.add(filter, BorderLayout.WEST);
        right.add(add, BorderLayout.EAST);
        tools.add(Forms.field("Buscar por cédula o nombre", search), BorderLayout.CENTER);
        JPanel rightBottom = new JPanel(new BorderLayout());
        rightBottom.setOpaque(false);
        rightBottom.add(right, BorderLayout.SOUTH);
        tools.add(rightBottom, BorderLayout.EAST);

        Forms.styleTable(table);
        table.getColumnModel().getColumn(2).setCellRenderer(Forms.pillRenderer(v -> {
            String text = (String) v;
            return text.isEmpty() ? new Pill("Sin turno", Color.WHITE, Theme.MUTED)
                    : new Pill(text, new Color(0xDBEAFE), new Color(0x1D4ED8));
        }));
        table.getColumnModel().getColumn(3).setCellRenderer(Forms.pillRenderer(v -> Labels.activePill((Boolean) v)));
        int[] widths = {100, 150, 190, 80};
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

        JPanel listCard = Theme.card();
        listCard.add(tools, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);

        editor.setBackground(Color.WHITE);
        editor.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        editor.setPreferredSize(new Dimension(360, 0));
        showEmptyEditor();

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(listCard, BorderLayout.CENTER);
        center.add(editor, BorderLayout.EAST);

        add(tiles, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    /** Recarga los operadores y quién está cobrando ahora. */
    public void refresh() {
        int tollId = session.getTollId();
        String selected = selectedId();
        Async.run(() -> new Object[]{service.staff().operators(tollId), service.booths(tollId)}, data -> {
            fill(data);
            select(selected);
        }, error -> Forms.info(this, "Empleados del peaje", "No se pudieron cargar los operadores: "
                + error.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private void fill(Object[] data) {
        operators = (List<TollUser>) data[0];
        LocalDateTime now = LocalDateTime.now();
        workingAt = new HashMap<>();
        List<String> booths = new ArrayList<>();
        for (BoothActivity b : (List<BoothActivity>) data[1]) {
            if (b.isAttended(now)) {
                workingAt.put(b.getLastOperatorId(), "Caseta " + b.getBooth().getBoothNumber() + " · desde "
                        + b.getLastCharge().format(HOUR));
                booths.add(String.valueOf(b.getBooth().getBoothNumber()));
            }
        }
        long active = operators.stream().filter(TollUser::isActive).count();
        long defaults = operators.stream().filter(u -> u.isActive() && u.isDefaultPassword()).count();
        activeTile.setValue(String.valueOf(active), (operators.size() - active) == 0 ? "ninguno inactivo"
                : (operators.size() - active) + (operators.size() - active == 1 ? " inactivo" : " inactivos"));
        workingTile.setValue(String.valueOf(workingAt.size()), booths.isEmpty() ? "nadie está cobrando"
                : (booths.size() == 1 ? "caseta " : "casetas ") + String.join(", ", booths));
        defaultTile.setValue(String.valueOf(defaults), defaults == 0 ? "todos cambiaron su contraseña"
                : "recuérdales cambiarla");
        applyFilter();
    }

    private void applyFilter() {
        String text = search.getText().trim().toLowerCase(Locale.ROOT);
        String which = (String) filter.getSelectedItem();
        List<TollUser> shown = new ArrayList<>();
        for (TollUser u : operators) {
            if ((ACTIVE.equals(which) && !u.isActive()) || (INACTIVE.equals(which) && u.isActive())) {
                continue;
            }
            if (!text.isEmpty() && !u.getIdNumber().contains(text)
                    && !u.getFullName().toLowerCase(Locale.ROOT).contains(text)) {
                continue;
            }
            shown.add(u);
        }
        String selected = selectedId();
        model.setRows(shown);
        select(selected);
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
            showEmptyEditor();
            return;
        }
        TollUser user = model.get(row);
        EmployeeForm form = new EmployeeForm(false, true, null);
        form.load(user);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(form);
        body.add(Box.createVerticalStrut(12));
        JLabel pwLabel = Theme.muted("Contraseña" + (user.isDefaultPassword() ? " · todavía es su cédula" : ""));
        pwLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(pwLabel);
        body.add(Box.createVerticalStrut(4));
        FlatButton reset = Theme.secondaryButton("↺ Restablecer a la cédula");
        reset.setAlignmentX(LEFT_ALIGNMENT);
        reset.addActionListener(e -> Forms.confirm(this, "¿Restablecer la contraseña de " + user.getFullName() + "?",
                "Su nueva contraseña será su cédula: <b>" + user.getIdNumber()
                        + "</b>. Díselo para que pueda entrar y cambiarla.", "Restablecer", false,
                () -> service.staff().resetPassword(user.getIdNumber()), this::refresh));
        body.add(reset);

        FlatButton save = Theme.primaryButton("Guardar cambios");
        save.addActionListener(e -> save(form, user));
        FlatButton toggle = user.isActive() ? Theme.secondaryButton("Desactivar") : Theme.secondaryButton("Activar");
        if (user.isActive()) {
            toggle.setColors(Color.WHITE, new Color(0xFEF2F2), Theme.DANGER, new Color(0xFCA5A5));
        }
        toggle.addActionListener(e -> toggleActive(user));
        JPanel buttons = new JPanel(new BorderLayout(8, 0));
        buttons.setOpaque(false);
        buttons.add(save, BorderLayout.CENTER);
        buttons.add(toggle, BorderLayout.EAST);

        editor.removeAll();
        editor.add(Theme.title("Editar operador · " + user.getFirstName() + " " + user.getFirstSurname()),
                BorderLayout.NORTH);
        JPanel holder = Forms.fitWidth(body);
        editor.add(holder, BorderLayout.CENTER);
        editor.add(buttons, BorderLayout.SOUTH);
        editor.revalidate();
        editor.repaint();
    }

    private void showEmptyEditor() {
        editor.removeAll();
        editor.add(Theme.title("Operadores de caseta"), BorderLayout.NORTH);
        JLabel hint = new JLabel("<html><div style='width:245px'>Escoge un operador de la lista para editar sus "
                + "datos, restablecer su contraseña o desactivarlo.<br><br>Con <b>+ Registrar operador</b> agregas "
                + "a alguien nuevo: su contraseña inicial será su cédula.</div></html>");
        hint.setFont(Theme.font(Font.PLAIN, 13));
        hint.setForeground(Theme.MUTED);
        hint.setVerticalAlignment(JLabel.TOP);
        editor.add(hint, BorderLayout.CENTER);
        editor.revalidate();
        editor.repaint();
    }

    private void save(EmployeeForm form, TollUser user) {
        Async.run(() -> {
            service.staff().update(form.read(user));
            return Boolean.TRUE;
        }, ok -> {
            Forms.info(this, "Empleados del peaje", "Cambios guardados.");
            refresh();
        }, error -> Forms.info(this, "Empleados del peaje", error.getMessage()));
    }

    private void toggleActive(TollUser user) {
        boolean activate = !user.isActive();
        Forms.confirm(this, (activate ? "¿Activar a " : "¿Desactivar a ") + user.getFullName() + "?",
                activate ? "Podrá volver a iniciar sesión y abrir turno en la caja."
                        : "Ya no podrá iniciar sesión. Sus tiquetes y turnos se conservan. Puedes volver a "
                        + "activarlo cuando quieras.", activate ? "Activar" : "Desactivar", !activate,
                () -> service.staff().setActive(user.getIdNumber(), activate), this::refresh);
    }

    private void registerDialog() {
        EmployeeForm form = new EmployeeForm(true, true, null);
        Forms.dialog(this, "Registrar operador de caseta", "Quedará en el peaje " + session.getTollName()
                + ". Su contraseña inicial será su cédula; la puede cambiar después.", form, "Registrar operador",
                false, () -> service.staff().register(form.read(null),
                        com.invpeajes.service.AuthService.ROLE_OPERATOR, session.getTollId()),
                user -> {
                    refresh();
                    Forms.info(this, "Empleados del peaje", user.getFullName() + " quedó registrado. Entra con su "
                            + "cédula " + user.getIdNumber() + " y esa misma es su contraseña.");
                });
    }

    /** Filas de la tabla de operadores. */
    private final class OperatorModel extends AbstractTableModel {
        private final String[] columns = {"Cédula", "Nombre", "Turno", "Estado"};
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
                    return workingAt.getOrDefault(u.getIdNumber(), "");
                default:
                    return u.isActive();
            }
        }
    }
}
