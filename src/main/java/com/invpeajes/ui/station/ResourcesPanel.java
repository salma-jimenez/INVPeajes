package com.invpeajes.ui.station;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.ResourceAvailability;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Pill;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Recursos de asistencia del peaje (grúas, ambulancias, carros taller y auxilio mecánico),
 * en una columna por tipo. Cada recurso muestra su estado, qué solicitud atiende o cuántas
 * atendió hoy, y el botón que corresponde: sacar de servicio, reactivar o ver la solicitud.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class ResourcesPanel extends JPanel {

    private static final AssistanceType[] TYPES = {
        AssistanceType.TOW_TRUCK, AssistanceType.AMBULANCE, AssistanceType.MOBILE_WORKSHOP, AssistanceType.MECHANICAL_AID};

    private final StationManagerService service;
    private final CashierSession session;
    private final IntConsumer openAssistance;
    private final Runnable onRefreshed;
    private final StatTile availableTile = new StatTile("Disponibles", Theme.SUCCESS, 24);
    private final StatTile busyTile = new StatTile("En servicio", Theme.PRIMARY, 24);
    private final StatTile outTile = new StatTile("Fuera de servicio", Theme.MUTED, 24);
    private final StatTile usageTile = new StatTile("Uso hoy", Theme.WARNING, 24);
    private final JTextField search = Theme.textField();
    private final JPanel board = new JPanel();
    private List<AssistanceResource> resources = Collections.emptyList();
    private Map<Integer, Integer> attendedToday = Collections.emptyMap();
    private Map<Integer, Assistance> serving = Collections.emptyMap();

    /**
     * @param service Servicio del jefe de estación.
     * @param session Sesión del jefe.
     * @param openAssistance Abre una solicitud en el Centro de asistencia ("Ver solicitud").
     * @param onRefreshed Se llama cada vez que terminan de cargar los datos.
     */
    public ResourcesPanel(StationManagerService service, CashierSession session, IntConsumer openAssistance,
                          Runnable onRefreshed) {
        super(new BorderLayout(0, 14));
        this.service = service;
        this.session = session;
        this.openAssistance = openAssistance;
        this.onRefreshed = onRefreshed;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(availableTile);
        tiles.add(busyTile);
        tiles.add(outTile);
        tiles.add(usageTile);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);
        search.setPreferredSize(new Dimension(210, 36));
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
        tools.add(Theme.muted("Buscar identificación"));
        tools.add(search);
        FlatButton add = Theme.primaryButton("+ Agregar recurso");
        add.addActionListener(e -> addDialog());
        tools.add(add);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(tiles, BorderLayout.NORTH);
        north.add(tools, BorderLayout.SOUTH);

        board.setOpaque(false);
        add(north, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
    }

    /** Recarga los recursos, lo que atiende cada uno y cuántas asistencias atendió hoy. */
    public void refresh() {
        int tollId = session.getTollId();
        Async.run(() -> new Object[]{
            service.resources().listByToll(tollId),
            service.attendedTodayByResource(tollId),
            service.assistances().listByToll(tollId)
        }, data -> {
            @SuppressWarnings("unchecked")
            List<AssistanceResource> list = (List<AssistanceResource>) data[0];
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> attended = (Map<Integer, Integer>) data[1];
            @SuppressWarnings("unchecked")
            List<Assistance> assistances = (List<Assistance>) data[2];
            resources = list;
            attendedToday = attended;
            Map<Integer, Assistance> busy = new HashMap<>();
            for (Assistance a : StationManagerService.active(assistances)) {
                if (a.getResourceId() != null) {
                    busy.put(a.getResourceId(), a);
                }
            }
            serving = busy;
            fillTiles();
            fillBoard();
            onRefreshed.run();
        }, e -> JOptionPane.showMessageDialog(this, e.getMessage(), "Recursos", JOptionPane.WARNING_MESSAGE));
    }

    private void fillTiles() {
        long available = count(ResourceAvailability.AVAILABLE);
        long busy = count(ResourceAvailability.IN_SERVICE);
        long out = count(ResourceAvailability.OUT_OF_SERVICE);
        int used = attendedToday.values().stream().mapToInt(Integer::intValue).sum();
        availableTile.setValue(String.valueOf(available), "listos para asignar");
        busyTile.setValue(String.valueOf(busy), "atendiendo una solicitud");
        outTile.setValue(String.valueOf(out), "en taller o revisión");
        usageTile.setValue(String.valueOf(used), "asistencias atendidas con recursos");
    }

    private long count(ResourceAvailability availability) {
        return resources.stream().filter(r -> r.getAvailability() == availability).count();
    }

    private void fillBoard() {
        String filter = search.getText().trim().toUpperCase(Locale.ROOT);
        List<AssistanceType> types = new ArrayList<>(List.of(TYPES));
        if (resources.stream().anyMatch(r -> r.getType() == AssistanceType.OTHER)) {
            types.add(AssistanceType.OTHER);
        }
        board.removeAll();
        board.setLayout(new GridLayout(1, types.size(), 12, 0));
        for (AssistanceType type : types) {
            List<AssistanceResource> ofType = resources.stream().filter(r -> r.getType() == type)
                    .sorted(Comparator.comparing(AssistanceResource::getIdentification))
                    .collect(Collectors.toList());
            List<AssistanceResource> shown = ofType.stream()
                    .filter(r -> filter.isEmpty() || r.getIdentification().toUpperCase(Locale.ROOT).contains(filter))
                    .collect(Collectors.toList());
            long free = ofType.stream().filter(r -> r.getAvailability() == ResourceAvailability.AVAILABLE).count();
            board.add(column(type, free, ofType.size(), shown));
        }
        board.revalidate();
        board.repaint();
    }

    private JComponent column(AssistanceType type, long free, int total, List<AssistanceResource> shown) {
        JPanel col = new RoundedPanel(new Color(0xE9EEF5));
        col.setLayout(new BorderLayout(0, 10));
        col.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel name = new JLabel(StationTexts.typePlural(type));
        name.setFont(Theme.font(Font.BOLD, 14));
        name.setForeground(Theme.TEXT);
        head.add(name, BorderLayout.WEST);
        Pill pill = free == 0 && total > 0
                ? new Pill("0 de " + total + " libres", new Color(0xFEE2E2), new Color(0xB91C1C))
                : new Pill(free + " de " + total + " libres", new Color(0xDCFCE7), new Color(0x15803D));
        JPanel pillBox = new JPanel(new GridBagLayout());
        pillBox.setOpaque(false);
        pillBox.add(pill);
        head.add(pillBox, BorderLayout.EAST);
        col.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        for (AssistanceResource r : shown) {
            list.add(card(r));
            list.add(Box.createVerticalStrut(8));
        }
        if (shown.isEmpty()) {
            JLabel none = Theme.muted(total == 0 ? "Este peaje no tiene recursos de este tipo" : "Sin resultados");
            none.setFont(Theme.font(Font.PLAIN, 12));
            list.add(none);
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        col.add(scroll, BorderLayout.CENTER);
        return col;
    }

    private JComponent card(AssistanceResource r) {
        Color accent;
        String text;
        FlatButton button;
        switch (r.getAvailability()) {
            case AVAILABLE:
                accent = Theme.SUCCESS;
                text = "Atendidas hoy: " + attendedToday.getOrDefault(r.getResourceId(), 0);
                button = Theme.secondaryButton("Sacar de servicio");
                button.addActionListener(e -> confirmAndRun("¿Sacar de servicio " + r.getIdentification()
                        + "?\nNo se podrá asignar hasta que se reactive.",
                        () -> service.resources().markOutOfService(r.getResourceId())));
                break;
            case IN_SERVICE: {
                accent = Theme.PRIMARY;
                Assistance a = serving.get(r.getResourceId());
                text = a == null ? "Atendiendo una solicitud"
                        : "Atendiendo #" + a.getAssistanceId() + (a.getPlate() == null ? "" : " · " + a.getPlate());
                button = Theme.secondaryButton("Ver solicitud");
                button.setEnabled(a != null);
                if (a != null) {
                    button.addActionListener(e -> openAssistance.accept(a.getAssistanceId()));
                }
                break;
            }
            default:
                accent = new Color(0x9CA3AF);
                text = "No se puede asignar";
                button = Theme.primaryButton("Reactivar");
                button.addActionListener(e -> run(() -> service.resources().reactivate(r.getResourceId())));
                break;
        }
        JPanel card = new ResourceCard(accent, r.getAvailability() == ResourceAvailability.OUT_OF_SERVICE);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        JLabel id = new JLabel(r.getIdentification());
        id.setFont(Theme.font(Font.BOLD, 14));
        id.setForeground(Theme.TEXT);
        top.add(id, BorderLayout.WEST);
        JPanel pillBox = new JPanel(new GridBagLayout());
        pillBox.setOpaque(false);
        pillBox.add(StationTexts.availabilityPill(r.getAvailability()));
        top.add(pillBox, BorderLayout.EAST);
        JLabel detail = Theme.muted(text);
        detail.setFont(Theme.font(Font.PLAIN, 12));
        button.setFont(Theme.font(Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        card.add(top, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        card.add(button, BorderLayout.SOUTH);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private void addDialog() {
        JComboBox<AssistanceType> type = new JComboBox<>(AssistanceType.values());
        type.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof AssistanceType) {
                    setText(StationTexts.type((AssistanceType) value));
                }
                return this;
            }
        });
        JTextField identification = Theme.textField();
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 6));
        form.add(new JLabel("Tipo de recurso:"));
        form.add(type);
        form.add(new JLabel("Identificación (ej. GRU-005 o la placa):"));
        form.add(identification);
        int answer = JOptionPane.showOptionDialog(this, form, "Agregar recurso al peaje " + session.getTollName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Agregar", "Cancelar"}, "Agregar");
        if (answer == JOptionPane.OK_OPTION) {
            AssistanceResource resource = new AssistanceResource(0, (AssistanceType) type.getSelectedItem(),
                    identification.getText().trim().toUpperCase(Locale.ROOT), session.getTollId(),
                    ResourceAvailability.AVAILABLE);
            run(() -> service.resources().register(resource));
        }
    }

    private void confirmAndRun(String question, Runnable action) {
        int answer = JOptionPane.showOptionDialog(this, question, "Recursos de asistencia",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[]{"Sí", "No"}, "No");
        if (answer == JOptionPane.YES_OPTION) {
            run(action);
        }
    }

    private void run(Runnable action) {
        Async.run(() -> {
            action.run();
            return Boolean.TRUE;
        }, ok -> refresh(), e -> JOptionPane.showMessageDialog(this, e.getMessage(), "Recursos",
                JOptionPane.WARNING_MESSAGE));
    }

    /** Fondo redondeado de una columna. */
    private static class RoundedPanel extends JPanel {
        private final Color color;

        RoundedPanel(Color color) {
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
        }
    }

    /** Tarjeta blanca de un recurso con su color a la izquierda. */
    private static class ResourceCard extends JPanel {
        private final Color accent;
        private final boolean faded;

        ResourceCard(Color accent, boolean faded) {
            this.accent = accent;
            this.faded = faded;
            setOpaque(false);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(faded ? new Color(0xF8FAFC) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(accent);
            g2.fillRoundRect(0, 0, 6, getHeight() - 1, 6, 6);
            g2.fillRect(3, 0, 3, getHeight() - 1);
            g2.dispose();
        }
    }
}
