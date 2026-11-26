package com.invpeajes.ui.traveler;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceHistory;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.AssistanceStatus;
import com.invpeajes.model.Toll;
import com.invpeajes.model.Traveler;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.service.TravelerService.RequestDetail;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.station.StationTexts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

/**
 * "Mis solicitudes": el viajero ve cómo va cada asistencia que pidió.
 *
 * <ul>
 *   <li>A la izquierda, tarjetas separadas en "En curso" y "Terminadas"; las que siguen en
 *       curso tienen una mini barra con los pasos que llevan.</li>
 *   <li>A la derecha, el seguimiento de la escogida: los cinco pasos con su hora
 *       ({@link AssistanceSteps}), un mensaje que explica qué está pasando, quién lo atiende
 *       (recurso, peaje y sus teléfonos), sus datos y el historial con las notas del jefe de
 *       estación.</li>
 * </ul>
 * La ventana la recarga sola cada 30 segundos mientras está a la vista.
 *
 * @author Salma Jiménez Vega
 * @version 2.0
 */
public class RequestsPanel extends JPanel {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final TravelerService service;
    private final Traveler traveler;
    private final JPanel list = new JPanel();
    private final JPanel detail = new JPanel();
    private final JLabel status = Theme.muted(" ");
    private final JLabel updated = TravelerUi.label(" ", Font.PLAIN, 12, Theme.MUTED);
    private final List<RequestCard> cards = new ArrayList<>();
    private List<Assistance> rows = Collections.emptyList();
    private Catalog catalog;
    private Integer selectedId;
    private int detailRequest;
    private Consumer<List<Assistance>> onLoaded = l -> { };

    /**
     * @param service  servicio del viajero
     * @param traveler viajero con cuenta
     */
    public RequestsPanel(TravelerService service, Traveler traveler) {
        super(new BorderLayout(14, 12));
        this.service = service;
        this.traveler = traveler;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        FlatButton reload = Theme.secondaryButton("Actualizar");
        reload.addActionListener(e -> refresh());
        JPanel rightHead = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHead.setOpaque(false);
        rightHead.add(updated);
        rightHead.add(reload);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(status, BorderLayout.WEST);
        head.add(rightHead, BorderLayout.EAST);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.setOpaque(false);
        listHolder.add(list, BorderLayout.NORTH);
        JScrollPane listScroll = scroll(listHolder);
        listScroll.setPreferredSize(new Dimension(320, 0));

        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setOpaque(false);
        detail.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel detailHolder = new JPanel(new BorderLayout());
        detailHolder.setOpaque(false);
        detailHolder.add(detail, BorderLayout.NORTH);
        RoundBox detailBox = new RoundBox(Color.WHITE, Theme.BORDER);
        detailBox.setLayout(new BorderLayout());
        detailBox.add(scroll(detailHolder), BorderLayout.CENTER);

        add(head, BorderLayout.NORTH);
        add(listScroll, BorderLayout.WEST);
        add(detailBox, BorderLayout.CENTER);
        showDetail(null);
    }

    /**
     * @param catalog catálogo cargado (para el nombre y los teléfonos de los peajes)
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        fillList();
    }

    /**
     * @param action qué hacer cada vez que se cargan las solicitudes (por ejemplo, el contador del menú)
     */
    public void setOnLoaded(Consumer<List<Assistance>> action) {
        this.onLoaded = action == null ? l -> { } : action;
    }

    /**
     * Deja escogida una solicitud (se ve al terminar de cargar).
     *
     * @param assistanceId número de la solicitud
     */
    public void select(int assistanceId) {
        selectedId = assistanceId;
    }

    /** Vuelve a consultar las solicitudes del viajero. */
    public void refresh() {
        status.setText("Cargando...");
        Async.run(() -> service.myRequests(traveler), loaded -> {
            rows = loaded;
            long active = Counts.activeCount(loaded);
            status.setText(loaded.isEmpty() ? "Aún no ha pedido asistencia."
                    : active + " en curso · " + loaded.size()
                    + (loaded.size() == 1 ? " solicitud en total" : " solicitudes en total"));
            updated.setText("Actualizado " + LocalTime.now().format(CLOCK) + " · se actualiza solo cada 30 s");
            if (selectedId == null || find(selectedId) == null) {
                Assistance first = firstActiveOrNewest();
                selectedId = first == null ? null : first.getAssistanceId();
            }
            fillList();
            showDetail(selectedId == null ? null : find(selectedId));
            onLoaded.accept(loaded);
        }, error -> status.setText("No se pudieron cargar las solicitudes."));
    }

    private Assistance find(int id) {
        for (Assistance a : rows) {
            if (a.getAssistanceId() == id) {
                return a;
            }
        }
        return null;
    }

    private Assistance firstActiveOrNewest() {
        for (Assistance a : rows) {
            if (!a.getCurrentStatus().isFinal()) {
                return a;
            }
        }
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------------ Lista de tarjetas

    private void fillList() {
        list.removeAll();
        cards.clear();
        List<Assistance> active = new ArrayList<>();
        List<Assistance> done = new ArrayList<>();
        for (Assistance a : rows) {
            (a.getCurrentStatus().isFinal() ? done : active).add(a);
        }
        if (rows.isEmpty()) {
            JLabel empty = TravelerUi.label("<html>Cuando pida una asistencia, aquí podrá ver cómo avanza.</html>",
                    Font.PLAIN, 13, Theme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));
            list.add(left(empty));
        }
        if (!active.isEmpty()) {
            list.add(left(section("EN CURSO")));
            for (Assistance a : active) {
                addCard(a);
            }
        }
        if (!done.isEmpty()) {
            if (!active.isEmpty()) {
                list.add(Box.createVerticalStrut(8));
            }
            list.add(left(section("TERMINADAS")));
            for (Assistance a : done) {
                addCard(a);
            }
        }
        list.revalidate();
        list.repaint();
    }

    private void addCard(Assistance a) {
        RequestCard card = new RequestCard(a);
        card.setSelected(selectedId != null && selectedId == a.getAssistanceId());
        cards.add(card);
        list.add(card);
        list.add(Box.createVerticalStrut(8));
    }

    private static JLabel section(String text) {
        JLabel label = TravelerUi.label(text, Font.BOLD, 12, Theme.MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 0));
        return label;
    }

    // ------------------------------------------------------------------ Seguimiento

    private void showDetail(Assistance a) {
        detail.removeAll();
        int myRequest = ++detailRequest;
        if (a == null) {
            detail.add(left(Theme.title("Seguimiento")));
            detail.add(Box.createVerticalStrut(8));
            detail.add(left(TravelerUi.label("<html>Escoja una solicitud para ver en qué paso va, quién la atiende"
                    + " y su historial.</html>", Font.PLAIN, 13, Theme.MUTED)));
            refreshDetail();
            return;
        }
        Toll toll = catalog == null ? null : catalog.toll(a.getTollId());
        String tollName = toll == null ? "Peaje " + a.getTollId() : toll.getName();

        JPanel title = new JPanel(new BorderLayout(10, 0));
        title.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(TravelerUi.label("Solicitud #" + a.getAssistanceId() + " · " + StationTexts.type(a.getType()),
                Font.BOLD, 22, Theme.TEXT));
        titles.add(TravelerUi.label("Pedida " + AssistanceSteps.when(a.getRequestDate()) + " · Peaje " + tollName
                + (a.getPlate() == null ? "" : " · Placa " + a.getPlate()), Font.PLAIN, 13, Theme.MUTED));
        title.add(titles, BorderLayout.CENTER);
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        pill.setOpaque(false);
        pill.add(StationTexts.statusPill(a.getCurrentStatus()));
        title.add(pill, BorderLayout.EAST);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        detail.add(left(title));
        detail.add(Box.createVerticalStrut(18));

        AssistanceSteps steps = new AssistanceSteps(false);
        steps.setData(a.getCurrentStatus(), null);
        steps.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        detail.add(left(steps));
        detail.add(Box.createVerticalStrut(14));
        detail.add(left(message(a, tollName)));
        detail.add(Box.createVerticalStrut(14));

        JLabel resource = value("Cargando...");
        JPanel history = new JPanel();
        history.setOpaque(false);
        history.setLayout(new BoxLayout(history, BoxLayout.Y_AXIS));
        history.add(left(TravelerUi.label("Cargando...", Font.PLAIN, 13, Theme.MUTED)));

        RoundBox who = new RoundBox(Color.WHITE, Theme.BORDER);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        who.add(left(Theme.title("Quién lo atiende")));
        who.add(Box.createVerticalStrut(8));
        who.add(row("Recurso", resource));
        who.add(row("Sale desde", value("Peaje " + tollName)));
        who.add(row("Línea de grúas del peaje", value(toll == null ? "—"
                : GuestAssistancePanel.phones(toll.getTowPhone()))));
        who.add(row("Teléfono del peaje", value(toll == null ? "—" : GuestAssistancePanel.phones(toll.getPhone()))));
        who.add(Box.createVerticalStrut(10));
        who.add(left(Theme.title("Su solicitud")));
        who.add(Box.createVerticalStrut(8));
        who.add(row("Placa", value(TravelerUi.orDash(a.getPlate()))));
        who.add(row("Teléfono", value(TravelerUi.orDash(a.getContactPhone()))));
        who.add(row("Qué pasó", value(TravelerUi.orDash(a.getNotes()))));

        RoundBox hist = new RoundBox(Color.WHITE, Theme.BORDER);
        hist.setLayout(new BoxLayout(hist, BoxLayout.Y_AXIS));
        hist.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        hist.add(left(Theme.title("Historial")));
        hist.add(Box.createVerticalStrut(8));
        hist.add(left(history));

        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);
        JPanel whoHolder = new JPanel(new BorderLayout());
        whoHolder.setOpaque(false);
        whoHolder.add(who, BorderLayout.NORTH);
        JPanel histHolder = new JPanel(new BorderLayout());
        histHolder.setOpaque(false);
        histHolder.add(hist, BorderLayout.NORTH);
        grid.add(whoHolder);
        grid.add(histHolder);
        detail.add(left(grid));
        refreshDetail();

        Async.run(() -> service.requestDetail(a), d -> {
            if (myRequest != detailRequest) {
                return;
            }
            steps.setData(a.getCurrentStatus(), d.getHistory());
            AssistanceResource r = d.getResource();
            resource.setText(r == null ? "Aún sin asignar"
                    : "<html><div style='text-align:right'>" + StationTexts.type(r.getType()) + " · "
                    + r.getIdentification() + "</div></html>");
            fillHistory(history, d);
            refreshDetail();
        }, error -> {
            if (myRequest == detailRequest) {
                resource.setText("No se pudo cargar");
                history.removeAll();
                history.add(left(TravelerUi.label("No se pudo cargar el historial.", Font.PLAIN, 13, Theme.DANGER)));
                refreshDetail();
            }
        });
    }

    /** Recuadro azul (gris si se canceló) que explica qué está pasando. */
    private JComponent message(Assistance a, String tollName) {
        AssistanceStatus s = a.getCurrentStatus();
        boolean cancelled = s == AssistanceStatus.CANCELLED;
        boolean done = s == AssistanceStatus.ATTENDED;
        Color bg = cancelled ? new Color(0xF3F4F6) : (done ? new Color(0xDCFCE7) : Theme.PRIMARY_SOFT);
        Color fg = cancelled ? new Color(0x4B5563) : (done ? new Color(0x15803D) : Theme.PRIMARY_DARK);
        RoundBox box = new RoundBox(bg, null);
        box.setLayout(new GridLayout(2, 1, 0, 2));
        box.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        box.add(TravelerUi.label(AssistanceSteps.headline(s, a.getType(), tollName), Font.BOLD, 16, fg));
        box.add(TravelerUi.label(AssistanceSteps.explanation(s, tollName, a.getContactPhone()),
                Font.PLAIN, 13, Theme.TEXT));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        return box;
    }

    private void fillHistory(JPanel history, RequestDetail d) {
        history.removeAll();
        List<AssistanceHistory> items = new ArrayList<>(d.getHistory());
        Collections.reverse(items); // lo más reciente arriba
        if (items.isEmpty()) {
            history.add(left(TravelerUi.label("Sin cambios todavía.", Font.PLAIN, 13, Theme.MUTED)));
        }
        boolean first = true;
        for (AssistanceHistory h : items) {
            JPanel item = new JPanel(new BorderLayout(10, 0));
            item.setOpaque(false);
            item.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
            JLabel dot = TravelerUi.label("●", Font.BOLD, 12, first ? Theme.PRIMARY : new Color(0x93B4F5));
            dot.setVerticalAlignment(SwingConstants.TOP);
            item.add(dot, BorderLayout.WEST);
            String notes = h.getNotes() == null || h.getNotes().isBlank() ? ""
                    : "<br><span style='color:#6b7280'>" + h.getNotes() + "</span>";
            item.add(TravelerUi.label("<html><b>" + StationTexts.status(h.getNewStatus()) + "</b> · "
                    + AssistanceSteps.when(h.getChangeDate()) + notes + "</html>", Font.PLAIN, 13, Theme.TEXT),
                    BorderLayout.CENTER);
            history.add(left(item));
            first = false;
        }
    }

    private static JPanel row(String caption, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JLabel c = TravelerUi.label(caption, Font.PLAIN, 13, Theme.MUTED);
        c.setVerticalAlignment(SwingConstants.TOP);
        row.add(c, BorderLayout.WEST);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private static JLabel value(String text) {
        return TravelerUi.label("<html><div style='text-align:right'>" + text + "</div></html>",
                Font.BOLD, 13, Theme.TEXT);
    }

    private void refreshDetail() {
        detail.revalidate();
        detail.repaint();
    }

    private static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    // ------------------------------------------------------------------ Componentes

    /** Panel con esquinas redondeadas. */
    static final class RoundBox extends JPanel {
        private final Color fill;
        private final Color outline;

        RoundBox(Color fill, Color outline) {
            this.fill = fill;
            this.outline = outline;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Tarjeta de una solicitud en la lista. */
    private final class RequestCard extends JPanel {
        private final Assistance assistance;
        private boolean selected;

        RequestCard(Assistance a) {
            super(new BorderLayout(0, 6));
            this.assistance = a;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setOpaque(false);
            top.add(TravelerUi.label("#" + a.getAssistanceId() + " · " + StationTexts.type(a.getType()),
                    Font.BOLD, 14, Theme.TEXT), BorderLayout.CENTER);
            top.add(StationTexts.statusPill(a.getCurrentStatus()), BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            Toll toll = catalog == null ? null : catalog.toll(a.getTollId());
            String where = toll == null ? "Peaje " + a.getTollId() : "Peaje " + toll.getName();
            add(TravelerUi.label(where + " · " + AssistanceSteps.when(a.getRequestDate())
                    + (a.getPlate() == null ? "" : " · " + a.getPlate()), Font.PLAIN, 12, Theme.MUTED),
                    BorderLayout.CENTER);
            if (!a.getCurrentStatus().isFinal()) {
                add(new MiniProgress(AssistanceSteps.progress(a.getCurrentStatus())), BorderLayout.SOUTH);
            }
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedId = assistance.getAssistanceId();
                    for (RequestCard c : cards) {
                        c.setSelected(c == RequestCard.this);
                    }
                    showDetail(assistance);
                }
            });
        }

        void setSelected(boolean value) {
            selected = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(selected ? Theme.PRIMARY : Theme.BORDER);
            g2.setStroke(new java.awt.BasicStroke(selected ? 2f : 1f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Cinco rayitas: cuántos pasos lleva la solicitud. */
    private static final class MiniProgress extends JComponent {
        private final int done;

        MiniProgress(int done) {
            this.done = done;
            setPreferredSize(new Dimension(200, 7));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int n = AssistanceSteps.STEPS.length;
            int gap = 4;
            double w = (getWidth() - gap * (n - 1)) / (double) n;
            for (int i = 0; i < n; i++) {
                g2.setColor(i < done ? Theme.PRIMARY : Theme.BORDER);
                g2.fillRoundRect((int) (i * (w + gap)), 1, (int) w, 5, 5, 5);
            }
            g2.dispose();
        }
    }

    /** Cuenta las solicitudes que no han terminado. */
    private static final class Counts {
        static long activeCount(List<Assistance> list) {
            return list.stream().filter(a -> !a.getCurrentStatus().isFinal()).count();
        }
    }
}
