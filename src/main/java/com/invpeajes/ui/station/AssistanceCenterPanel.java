package com.invpeajes.ui.station;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.ResourceAvailability;
import com.invpeajes.model.Toll;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Centro de asistencia del jefe de estación.
 *
 * <ul>
 *   <li>Arriba, cuatro contadores: sin atender, en curso, atendidas hoy y recursos disponibles.</li>
 *   <li>En el centro, el tablero con una columna por estado (Registradas, Asignadas, En camino,
 *       En proceso) y la columna "Atendidas hoy".</li>
 *   <li>A la derecha, el detalle de la solicitud elegida con su historial y el botón que corresponde
 *       a su estado: asignar recurso, marcar en camino, iniciar atención, cerrar o cancelar.</li>
 * </ul>
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AssistanceCenterPanel extends JPanel {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_HOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color COLUMN = new Color(0xE9EEF5);
    /** Minutos sin atender a partir de los cuales la espera se marca en rojo. */
    private static final long LATE_MINUTES = 15;
    private static final AssistanceStatus[] BOARD = {
        AssistanceStatus.REGISTERED, AssistanceStatus.ASSIGNED, AssistanceStatus.EN_ROUTE, AssistanceStatus.IN_PROGRESS};
    private static final String[] BOARD_TITLES = {"Registradas", "Asignadas", "En camino", "En proceso"};

    private final StationManagerService service;
    private final CashierSession session;
    private final IntConsumer onUnattended;
    private final Runnable onRefreshed;
    private final StatTile unattendedTile = new StatTile("Sin atender", Theme.DANGER, 24);
    private final StatTile ongoingTile = new StatTile("En curso", Theme.PRIMARY, 24);
    private final StatTile attendedTile = new StatTile("Atendidas hoy", Theme.SUCCESS, 24);
    private final StatTile resourcesTile = new StatTile("Recursos disponibles", Theme.WARNING, 24);
    private final JTextField search = Theme.textField();
    private final BoardColumn[] columns = new BoardColumn[BOARD.length + 1];
    private final JPanel detail = new JPanel(new BorderLayout());
    private Toll toll;
    private List<Assistance> all = Collections.emptyList();
    private Map<Integer, LocalDateTime> closedToday = Collections.emptyMap();
    private Map<Integer, AssistanceResource> resources = Collections.emptyMap();
    private Integer selectedId;

    /**
     * @param service Servicio del jefe de estación.
     * @param session Sesión del jefe.
     * @param onUnattended Recibe la cantidad de solicitudes sin atender (para el contador del menú).
     * @param onRefreshed Se llama cada vez que terminan de cargar los datos.
     */
    public AssistanceCenterPanel(StationManagerService service, CashierSession session,
                                 IntConsumer onUnattended, Runnable onRefreshed) {
        super(new BorderLayout(0, 14));
        this.service = service;
        this.session = session;
        this.onUnattended = onUnattended;
        this.onRefreshed = onRefreshed;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(unattendedTile);
        tiles.add(ongoingTile);
        tiles.add(attendedTile);
        tiles.add(resourcesTile);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);
        search.setPreferredSize(new Dimension(210, 36));
        search.setToolTipText("Buscar por placa o número de solicitud");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fillBoard();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fillBoard();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fillBoard();
            }
        });
        tools.add(Theme.muted("Buscar placa o N.º"));
        tools.add(search);
        FlatButton history = Theme.secondaryButton("Ver historial y canceladas");
        history.addActionListener(e -> showHistoryDialog());
        tools.add(history);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(tiles, BorderLayout.NORTH);
        north.add(tools, BorderLayout.SOUTH);

        JPanel board = new JPanel(new GridLayout(1, columns.length, 10, 0));
        board.setOpaque(false);
        for (int i = 0; i < BOARD.length; i++) {
            columns[i] = new BoardColumn(BOARD_TITLES[i], StationTexts.statusPill(BOARD[i]).getBackground());
            board.add(columns[i]);
        }
        columns[BOARD.length] = new BoardColumn("Atendidas hoy", null);
        board.add(columns[BOARD.length]);

        detail.setPreferredSize(new Dimension(330, 0));
        detail.setBackground(Color.WHITE);
        detail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        showEmptyDetail();

        JPanel middle = new JPanel(new BorderLayout(14, 0));
        middle.setOpaque(false);
        middle.add(board, BorderLayout.CENTER);
        middle.add(detail, BorderLayout.EAST);

        add(north, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
    }

    /**
     * Guarda el peaje del jefe, para calcular la distancia de cada solicitud.
     * @param toll Peaje, o null si no se pudo cargar.
     */
    public void setToll(Toll toll) {
        this.toll = toll;
    }

    /** Recarga solicitudes, cierres de hoy y recursos, y vuelve a dibujar el tablero. */
    public void refresh() {
        int tollId = session.getTollId();
        Async.run(() -> new Object[]{
            service.assistances().listByToll(tollId),
            service.closedToday(tollId),
            service.resources().listByToll(tollId)
        }, data -> {
            @SuppressWarnings("unchecked")
            List<Assistance> list = (List<Assistance>) data[0];
            @SuppressWarnings("unchecked")
            Map<Integer, LocalDateTime> closed = (Map<Integer, LocalDateTime>) data[1];
            @SuppressWarnings("unchecked")
            List<AssistanceResource> res = (List<AssistanceResource>) data[2];
            all = list;
            closedToday = closed;
            resources = res.stream().collect(Collectors.toMap(AssistanceResource::getResourceId, r -> r));
            fillTiles();
            fillBoard();
            if (selectedId != null) {
                select(selectedId);
            }
            onRefreshed.run();
        }, this::showError);
    }

    /** Recarga solo el contador de solicitudes sin atender (cuando el jefe está en otro apartado). */
    public void refreshCountsOnly() {
        Async.run(() -> service.assistances().listByToll(session.getTollId()), list -> {
            all = list;
            onUnattended.accept((int) list.stream()
                    .filter(a -> a.getCurrentStatus() == AssistanceStatus.REGISTERED).count());
        }, error -> { });
    }

    /**
     * Muestra el detalle de una solicitud.
     * @param assistanceId ID de la solicitud.
     */
    public void select(int assistanceId) {
        selectedId = assistanceId;
        for (BoardColumn c : columns) {
            c.markSelected(assistanceId);
        }
        all.stream().filter(a -> a.getAssistanceId() == assistanceId).findFirst().ifPresent(this::showDetail);
    }

    private void fillTiles() {
        long unattended = all.stream().filter(a -> a.getCurrentStatus() == AssistanceStatus.REGISTERED).count();
        long ongoing = all.stream().filter(a -> a.getCurrentStatus() == AssistanceStatus.ASSIGNED
                || a.getCurrentStatus() == AssistanceStatus.EN_ROUTE
                || a.getCurrentStatus() == AssistanceStatus.IN_PROGRESS).count();
        long attended = attendedToday().size();
        unattendedTile.setValue(String.valueOf(unattended), "sin recurso asignado");
        ongoingTile.setValue(String.valueOf(ongoing), "asignadas, en camino o en proceso");
        attendedTile.setValue(String.valueOf(attended), "cerradas hoy");

        Map<AssistanceType, Integer> free = new EnumMap<>(AssistanceType.class);
        int available = 0;
        for (AssistanceResource r : resources.values()) {
            if (r.getAvailability() == ResourceAvailability.AVAILABLE) {
                free.merge(r.getType(), 1, Integer::sum);
                available++;
            }
        }
        String breakdown = free.entrySet().stream()
                .map(e -> e.getValue() + " " + (e.getValue() == 1 ? StationTexts.type(e.getKey())
                        : StationTexts.typePlural(e.getKey())).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" · "));
        resourcesTile.setValue(available + " de " + resources.size(), breakdown);
        onUnattended.accept((int) unattended);
    }

    private List<Assistance> attendedToday() {
        return all.stream()
                .filter(a -> a.getCurrentStatus() == AssistanceStatus.ATTENDED && closedToday.containsKey(a.getAssistanceId()))
                .sorted(Comparator.comparing((Assistance a) -> closedToday.get(a.getAssistanceId())).reversed())
                .collect(Collectors.toList());
    }

    private void fillBoard() {
        String filter = search.getText().trim().toUpperCase(Locale.ROOT).replace("#", "");
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < BOARD.length; i++) {
            AssistanceStatus status = BOARD[i];
            List<Assistance> items = all.stream()
                    .filter(a -> a.getCurrentStatus() == status && matches(a, filter))
                    .sorted(Comparator.comparing(Assistance::getRequestDate))
                    .collect(Collectors.toList());
            List<JComponent> cards = new ArrayList<>();
            for (Assistance a : items) {
                long wait = Duration.between(a.getRequestDate(), now).toMinutes();
                boolean late = status == AssistanceStatus.REGISTERED && wait >= LATE_MINUTES;
                String line = status == AssistanceStatus.REGISTERED
                        ? a.getRequestDate().format(HOUR) + distanceText(a, " · ")
                        : a.getRequestDate().format(HOUR) + " · " + resourceName(a.getResourceId());
                cards.add(new AssistanceCard(a, StationTexts.duration(wait), line,
                        status == AssistanceStatus.REGISTERED ? Theme.DANGER : null,
                        late ? Theme.DANGER : Theme.MUTED, false));
            }
            columns[i].setCards(items.size(), cards);
        }
        List<Assistance> done = attendedToday().stream().filter(a -> matches(a, filter)).collect(Collectors.toList());
        List<JComponent> cards = new ArrayList<>();
        for (Assistance a : done) {
            LocalDateTime closedAt = closedToday.get(a.getAssistanceId());
            long took = Duration.between(a.getRequestDate(), closedAt).toMinutes();
            cards.add(new AssistanceCard(a, "✓ " + closedAt.format(HOUR),
                    resourceName(a.getResourceId()) + " · " + StationTexts.duration(took), null, Theme.SUCCESS, true));
        }
        columns[BOARD.length].setCards(done.size(), cards);
        if (selectedId != null) {
            for (BoardColumn c : columns) {
                c.markSelected(selectedId);
            }
        }
    }

    private static boolean matches(Assistance a, String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        return String.valueOf(a.getAssistanceId()).equals(filter)
                || (a.getPlate() != null && a.getPlate().toUpperCase(Locale.ROOT).contains(filter));
    }

    private String resourceName(Integer resourceId) {
        if (resourceId == null) {
            return "sin recurso";
        }
        AssistanceResource r = resources.get(resourceId);
        return r == null ? "Recurso " + resourceId : r.getIdentification();
    }

    private String distanceText(Assistance a, String prefix) {
        if (toll == null || a.getIncidentLatitude() == null || a.getIncidentLongitude() == null) {
            return "";
        }
        double km = StationManagerService.distanceKm(toll.getLatitude(), toll.getLongitude(),
                a.getIncidentLatitude(), a.getIncidentLongitude());
        return prefix + "a " + String.format(Locale.forLanguageTag("es-CO"), "%.1f", km) + " km";
    }

    // ------------------------------------------------------------------ Detalle

    private void showEmptyDetail() {
        detail.removeAll();
        JLabel empty = new JLabel("<html><div style='text-align:center;width:240px'>Selecciona una solicitud del "
                + "tablero para ver su detalle, asignar un recurso o cambiar su estado.</div></html>",
                SwingConstants.CENTER);
        empty.setFont(Theme.font(Font.PLAIN, 14));
        empty.setForeground(Theme.MUTED);
        detail.add(empty, BorderLayout.CENTER);
        detail.revalidate();
        detail.repaint();
    }

    private void showDetail(Assistance a) {
        detail.removeAll();
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JPanel head = new JPanel(new BorderLayout(8, 0));
        head.setOpaque(false);
        JLabel title = new JLabel("<html>Solicitud #" + a.getAssistanceId() + " · " + StationTexts.type(a.getType())
                + "</html>");
        title.setFont(Theme.font(Font.BOLD, 16));
        title.setForeground(Theme.TEXT);
        head.add(title, BorderLayout.CENTER);
        JPanel pillBox = new JPanel(new GridBagLayout());
        pillBox.setOpaque(false);
        pillBox.add(StationTexts.statusPill(a.getCurrentStatus()));
        head.add(pillBox, BorderLayout.EAST);
        body.add(left(head));
        body.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        long wait = Duration.between(a.getRequestDate(), LocalDateTime.now()).toMinutes();
        boolean open = a.getCurrentStatus() != AssistanceStatus.ATTENDED && a.getCurrentStatus() != AssistanceStatus.CANCELLED;
        addRow(grid, 0, "Hora", a.getRequestDate().format(HOUR)
                + (open ? " · espera <b style='color:" + (wait >= LATE_MINUTES && a.getCurrentStatus() == AssistanceStatus.REGISTERED
                        ? "#DC2626" : "#1F2937") + "'>" + StationTexts.duration(wait) + "</b>" : ""));
        addRow(grid, 1, "Placa", "<b>" + (a.getPlate() == null ? "—" : a.getPlate()) + "</b>");
        addRow(grid, 2, "Teléfono", a.getContactPhone() == null ? "—" : a.getContactPhone());
        String location;
        if (a.getIncidentLatitude() != null && a.getIncidentLongitude() != null) {
            location = String.format(Locale.ROOT, "%.4f, %.4f", a.getIncidentLatitude(), a.getIncidentLongitude())
                    + distanceText(a, "<br>");
            location += distanceText(a, "").isEmpty() ? "" : " del peaje";
        } else {
            location = "Sin coordenadas · cerca del peaje " + session.getTollName();
        }
        addRow(grid, 3, "Ubicación", location);
        addRow(grid, 4, "Descripción", a.getNotes() == null || a.getNotes().isEmpty() ? "—" : a.getNotes());
        if (a.getResourceId() != null) {
            addRow(grid, 5, "Recurso", "<b>" + resourceName(a.getResourceId()) + "</b>");
        }
        body.add(left(grid));
        if (a.getIncidentLatitude() != null && a.getIncidentLongitude() != null) {
            body.add(Box.createVerticalStrut(6));
            JLabel maps = new JLabel("Ver en Google Maps ↗");
            maps.setFont(Theme.font(Font.BOLD, 13));
            maps.setForeground(Theme.PRIMARY);
            maps.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            maps.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openMaps(a);
                }
            });
            body.add(left(maps));
        }

        body.add(Box.createVerticalStrut(14));
        JLabel histTitle = new JLabel("HISTORIAL");
        histTitle.setFont(Theme.font(Font.BOLD, 11));
        histTitle.setForeground(Theme.MUTED);
        body.add(left(histTitle));
        body.add(Box.createVerticalStrut(6));
        Timeline timeline = new Timeline();
        body.add(left(timeline));
        Async.run(() -> service.assistances().getHistory(a.getAssistanceId()),
                h -> timeline.setHistory(h, a.getCurrentStatus()), error -> { });

        JPanel actions = buildActions(a);
        actions.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JPanel holder = new WidthTracking();
        holder.add(body, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        detail.add(scroll, BorderLayout.CENTER);
        detail.add(actions, BorderLayout.SOUTH);
        detail.revalidate();
        detail.repaint();
    }

    private JPanel buildActions(Assistance a) {
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setOpaque(false);
        int id = a.getAssistanceId();
        switch (a.getCurrentStatus()) {
            case REGISTERED: {
                JLabel label = Theme.muted("Asignar " + StationTexts.type(a.getType()).toLowerCase(Locale.ROOT)
                        + " disponible");
                actions.add(left(label));
                actions.add(Box.createVerticalStrut(6));
                JComboBox<AssistanceResource> combo = new JComboBox<>();
                combo.setFont(Theme.font(Font.PLAIN, 14));
                combo.setBackground(Color.WHITE);
                combo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                  boolean selected, boolean focus) {
                        super.getListCellRendererComponent(list, value, index, selected, focus);
                        if (value instanceof AssistanceResource) {
                            setText(((AssistanceResource) value).getIdentification() + " · Disponible");
                        } else if (value == null) {
                            setText("Cargando recursos...");
                        }
                        return this;
                    }
                });
                FlatButton assign = Theme.primaryButton("Asignar");
                assign.setEnabled(false);
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.add(combo, BorderLayout.CENTER);
                row.add(assign, BorderLayout.EAST);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                actions.add(left(row));
                Async.run(() -> service.assistances().listCandidateResources(id), list -> {
                    list.forEach(combo::addItem);
                    assign.setEnabled(!list.isEmpty());
                    if (list.isEmpty()) {
                        label.setText("No hay " + StationTexts.typePlural(a.getType()).toLowerCase(Locale.ROOT)
                                + " disponibles");
                        label.setForeground(Theme.DANGER);
                    }
                }, this::showError);
                assign.addActionListener(e -> {
                    AssistanceResource r = (AssistanceResource) combo.getSelectedItem();
                    if (r != null) {
                        run(() -> service.assistances().assignResource(id, r.getResourceId()));
                    }
                });
                actions.add(Box.createVerticalStrut(8));
                actions.add(left(cancelButton(id)));
                break;
            }
            case ASSIGNED:
                actions.add(left(wide(Theme.primaryButton("Marcar en camino →"),
                        () -> service.assistances().markEnRoute(id))));
                actions.add(Box.createVerticalStrut(8));
                actions.add(left(cancelButton(id)));
                break;
            case EN_ROUTE:
                actions.add(left(wide(Theme.primaryButton("Llegó: iniciar atención →"),
                        () -> service.assistances().startService(id))));
                actions.add(Box.createVerticalStrut(8));
                actions.add(left(cancelButton(id)));
                break;
            case IN_PROGRESS: {
                FlatButton close = Theme.primaryButton("Cerrar como atendida ✓");
                close.setColors(Theme.SUCCESS, new Color(0x15803D), Color.WHITE, null);
                close.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                close.addActionListener(e -> completeDialog(id));
                actions.add(left(close));
                break;
            }
            default: {
                LocalDateTime when = closedToday.get(id);
                actions.add(left(Theme.muted(StationTexts.status(a.getCurrentStatus())
                        + (when == null ? "" : " a las " + when.format(HOUR))
                        + (a.getCost() != null ? " · costo " + Theme.money(a.getCost()) : ""))));
                break;
            }
        }
        return actions;
    }

    private FlatButton cancelButton(int id) {
        FlatButton cancel = Theme.secondaryButton("Cancelar solicitud");
        cancel.setColors(Color.WHITE, new Color(0xFEF2F2), Theme.DANGER, new Color(0xFCA5A5));
        cancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cancel.addActionListener(e -> {
            String reason = JOptionPane.showInputDialog(this, "Motivo de la cancelación:", "Cancelar solicitud #" + id,
                    JOptionPane.QUESTION_MESSAGE);
            if (reason != null) {
                run(() -> service.assistances().cancel(id, reason));
            }
        });
        return cancel;
    }

    private FlatButton wide(FlatButton button, Runnable action) {
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.addActionListener(e -> run(action));
        return button;
    }

    private void completeDialog(int id) {
        JTextField cost = Theme.textField();
        cost.setText("0");
        JTextArea notes = new JTextArea(3, 24);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setFont(Theme.font(Font.PLAIN, 14));
        JPanel form = new JPanel(new BorderLayout(0, 8));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(new JLabel("Costo del servicio (0 si no tiene costo):"), BorderLayout.NORTH);
        top.add(cost, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.add(new JLabel("Observaciones:"), BorderLayout.NORTH);
        bottom.add(new JScrollPane(notes), BorderLayout.CENTER);
        form.add(top, BorderLayout.NORTH);
        form.add(bottom, BorderLayout.CENTER);
        int answer = JOptionPane.showOptionDialog(this, form, "Cerrar solicitud #" + id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Cerrar como atendida", "Volver"}, "Cerrar como atendida");
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(cost.getText().replace(".", "").replace("$", "").trim());
        } catch (NumberFormatException e) {
            showError(new IllegalArgumentException("El costo debe ser un número"));
            return;
        }
        run(() -> service.assistances().complete(id, value, notes.getText().trim()));
    }

    /** Ejecuta una acción sobre la asistencia y recarga el tablero. */
    private void run(Runnable action) {
        Async.run(() -> {
            action.run();
            return Boolean.TRUE;
        }, ok -> refresh(), this::showError);
    }

    private void openMaps(Assistance a) {
        try {
            Desktop.getDesktop().browse(new URI(StationManagerService.mapsUrl(
                    a.getIncidentLatitude(), a.getIncidentLongitude())));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el navegador.\nCoordenadas: "
                    + a.getIncidentLatitude() + ", " + a.getIncidentLongitude(), "Google Maps",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showError(RuntimeException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Centro de asistencia", JOptionPane.WARNING_MESSAGE);
    }

    private void showHistoryDialog() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"N.º", "Fecha", "Tipo", "Placa", "Estado", "Recurso", "Costo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        all.stream()
                .filter(a -> a.getCurrentStatus() == AssistanceStatus.ATTENDED || a.getCurrentStatus() == AssistanceStatus.CANCELLED)
                .sorted(Comparator.comparing(Assistance::getRequestDate).reversed())
                .forEach(a -> model.addRow(new Object[]{"#" + a.getAssistanceId(), a.getRequestDate().format(DATE_HOUR),
                    StationTexts.type(a.getType()), a.getPlate() == null ? "—" : a.getPlate(),
                    StationTexts.status(a.getCurrentStatus()), resourceName(a.getResourceId()),
                    a.getCost() == null ? "—" : Theme.money(a.getCost())}));
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 13));
        table.setAutoCreateRowSorter(true);
        JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "Historial de solicitudes cerradas · Peaje " + session.getTollName(), JDialog.ModalityType.APPLICATION_MODAL);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.getContentPane().add(scroll);
        dialog.setSize(820, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static void addRow(JPanel grid, int row, String label, String html) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 7, 10);
        c.gridx = 0;
        JLabel l = Theme.muted(label);
        l.setPreferredSize(new Dimension(84, l.getPreferredSize().height));
        grid.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 7, 0);
        JLabel v = new JLabel("<html><div style='width:150px'>" + html + "</div></html>");
        v.setFont(Theme.font(Font.PLAIN, 13));
        v.setForeground(Theme.TEXT);
        grid.add(v, c);
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Panel que se ajusta al ancho del área visible (solo baja con barra vertical). */
    private static final class WidthTracking extends JPanel implements javax.swing.Scrollable {
        WidthTracking() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 80;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Columna del tablero: título, contador y lista de tarjetas con barra para bajar. */
    private final class BoardColumn extends JPanel {
        private final JLabel count = new JLabel("0");
        private final JPanel list = new JPanel();
        private final List<AssistanceCard> cards = new ArrayList<>();

        BoardColumn(String title, Color pillColor) {
            super(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 9, 9, 9));
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            JLabel name = new JLabel(title);
            name.setFont(Theme.font(Font.BOLD, 13));
            name.setForeground(Theme.TEXT);
            head.add(name, BorderLayout.WEST);
            count.setFont(Theme.font(Font.BOLD, 11));
            count.setForeground(pillColor == null ? new Color(0x15803D) : Theme.TEXT);
            head.add(count, BorderLayout.EAST);
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setOpaque(false);
            JPanel holder = new WidthTracking();
            holder.add(list, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(holder,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(head, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
        }

        void setCards(int total, List<JComponent> newCards) {
            count.setText(String.valueOf(total));
            list.removeAll();
            cards.clear();
            for (JComponent card : newCards) {
                cards.add((AssistanceCard) card);
                list.add(card);
                list.add(Box.createVerticalStrut(8));
            }
            if (newCards.isEmpty()) {
                JLabel none = Theme.muted("Sin solicitudes");
                none.setFont(Theme.font(Font.PLAIN, 12));
                none.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 0));
                list.add(none);
            }
            list.revalidate();
            list.repaint();
        }

        void markSelected(int id) {
            for (AssistanceCard card : cards) {
                card.setSelected(card.assistance.getAssistanceId() == id);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLUMN);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
        }
    }

    /** Tarjeta de una solicitud en el tablero. */
    private final class AssistanceCard extends JPanel {
        private final Assistance assistance;
        private final Color accent;
        private final boolean faded;
        private boolean selected;

        AssistanceCard(Assistance a, String corner, String line, Color accent, Color cornerColor, boolean faded) {
            super(new BorderLayout(0, 2));
            this.assistance = a;
            this.accent = accent;
            this.faded = faded;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, accent == null ? 10 : 13, 8, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            JLabel number = new JLabel("#" + a.getAssistanceId());
            number.setFont(Theme.font(Font.BOLD, 13));
            number.setForeground(Theme.TEXT);
            JLabel time = new JLabel(corner);
            time.setFont(Theme.font(Font.BOLD, 11));
            time.setForeground(cornerColor);
            top.add(number, BorderLayout.WEST);
            top.add(time, BorderLayout.EAST);
            JLabel what = new JLabel(StationTexts.type(a.getType()) + (a.getPlate() == null ? "" : " · " + a.getPlate()));
            what.setFont(Theme.font(Font.BOLD, 12));
            what.setForeground(Theme.TEXT);
            JLabel detailLine = new JLabel(line);
            detailLine.setFont(Theme.font(Font.PLAIN, 11));
            detailLine.setForeground(Theme.MUTED);
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(what);
            text.add(detailLine);
            add(top, BorderLayout.NORTH);
            add(text, BorderLayout.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    select(assistance.getAssistanceId());
                }
            });
        }

        void setSelected(boolean value) {
            selected = value;
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(faded ? new Color(0xFAFBFC) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (accent != null) {
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight() - 1, 6, 6);
                g2.fillRect(3, 0, 3, getHeight() - 1);
            }
            g2.setColor(selected ? Theme.PRIMARY : Theme.BORDER);
            g2.setStroke(new BasicStroke(selected ? 2f : 1f));
            g2.drawRoundRect(selected ? 1 : 0, selected ? 1 : 0, getWidth() - (selected ? 3 : 1),
                    getHeight() - (selected ? 3 : 1), 10, 10);
            g2.dispose();
        }
    }

    /** Línea de tiempo del historial: pasos hechos con su hora y pasos pendientes en gris. */
    private static final class Timeline extends JPanel {
        private static final AssistanceStatus[] PATH = {AssistanceStatus.REGISTERED, AssistanceStatus.ASSIGNED,
            AssistanceStatus.EN_ROUTE, AssistanceStatus.IN_PROGRESS, AssistanceStatus.ATTENDED};

        Timeline() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            add(Theme.muted("Cargando..."));
        }

        void setHistory(List<AssistanceHistory> history, AssistanceStatus current) {
            removeAll();
            List<AssistanceStatus> done = new ArrayList<>();
            for (AssistanceHistory h : history) {
                String note = h.getNotes() == null || h.getNotes().isEmpty() ? "" : " · " + h.getNotes();
                add(step(true, h.getChangeDate().format(HOUR), StationTexts.status(h.getNewStatus()) + note));
                done.add(h.getNewStatus());
            }
            if (history.isEmpty()) {
                add(step(true, "", StationTexts.status(AssistanceStatus.REGISTERED) + " por el viajero"));
                done.add(AssistanceStatus.REGISTERED);
            }
            if (current != AssistanceStatus.CANCELLED) {
                for (AssistanceStatus s : PATH) {
                    if (!done.contains(s)) {
                        add(step(false, "", StationTexts.status(s)));
                    }
                }
            }
            revalidate();
            repaint();
        }

        private static JComponent step(boolean doneStep, String time, String text) {
            JLabel label = new JLabel("<html><div style='width:230px'>"
                    + (time.isEmpty() ? "" : "<b>" + time + "</b>&nbsp; ") + text + "</div></html>");
            label.setFont(Theme.font(Font.PLAIN, 12));
            label.setForeground(doneStep ? Theme.TEXT : new Color(0x9CA3AF));
            label.setIcon(new javax.swing.Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (doneStep) {
                        g2.setColor(Theme.PRIMARY);
                        g2.fillOval(x, y + 2, 10, 10);
                    } else {
                        g2.setColor(new Color(0xC9D1DD));
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawOval(x + 1, y + 3, 8, 8);
                    }
                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return 12;
                }

                @Override
                public int getIconHeight() {
                    return 14;
                }
            });
            label.setIconTextGap(10);
            label.setVerticalTextPosition(SwingConstants.TOP);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }
    }
}
