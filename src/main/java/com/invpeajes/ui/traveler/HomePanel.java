package com.invpeajes.ui.traveler;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.AssistanceResource;
import com.invpeajes.model.Toll;
import com.invpeajes.model.Traveler;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.service.TravelerService.RequestDetail;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.station.StationTexts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Inicio del viajero: saludo, cifras del inventario y accesos rápidos.
 *
 * <ul>
 *   <li>Con cuenta y una asistencia en curso: arriba aparece una franja azul con el paso en que
 *       va y el botón "Ver seguimiento".</li>
 *   <li>Invitado: un recuadro explica que pedir asistencia necesita una cuenta y da el 123.</li>
 * </ul>
 *
 * @author Salma Jiménez Vega
 * @version 2.0
 */
public class HomePanel extends JPanel {

    private final TravelerService service;
    private final Traveler traveler;
    private final Consumer<String> navigate;
    private final StatTile total = new StatTile("Peajes en el inventario", Theme.PRIMARY, 26);
    private final StatTile operating = new StatTile("Operativos", Theme.SUCCESS, 26);
    private final StatTile colpass = new StatTile("Aceptan COLPASS", Theme.PURPLE, 26);
    private final StatTile departments = new StatTile("Departamentos", Theme.WARNING, 26);
    private final JPanel banner = new JPanel(new BorderLayout());
    private Catalog catalog;
    private Consumer<List<Assistance>> onLoaded = l -> { };
    private Consumer<Integer> onOpenRequest = id -> { };
    private int loadRequest;

    /**
     * @param service  servicio del viajero
     * @param traveler viajero con cuenta, o null si es invitado
     * @param navigate abre una pantalla por su nombre (constantes de {@link TravelerFrame})
     */
    public HomePanel(TravelerService service, Traveler traveler, Consumer<String> navigate) {
        super(new BorderLayout(0, 18));
        this.service = service;
        this.traveler = traveler;
        this.navigate = navigate;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        String name = traveler == null ? "invitado" : traveler.getFirstName();
        head.add(left(TravelerUi.label("Hola, " + name, Font.BOLD, 28, Theme.TEXT)));
        head.add(Box.createVerticalStrut(4));
        head.add(left(Theme.muted(traveler == null
                ? "Consulta los peajes de Colombia y cuánto cuesta tu viaje."
                : "Consulta los peajes de Colombia, cuánto cuesta tu viaje y pide ayuda en la vía.")));
        head.add(Box.createVerticalStrut(18));
        banner.setOpaque(false);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        head.add(banner);
        JPanel tiles = new JPanel(new GridLayout(1, 4, 12, 0));
        tiles.setOpaque(false);
        tiles.setAlignmentX(LEFT_ALIGNMENT);
        for (StatTile t : new StatTile[]{total, operating, colpass, departments}) {
            t.setValue("—", "Cargando...");
            tiles.add(t);
        }
        head.add(tiles);

        JPanel shortcuts = new JPanel(new GridLayout(1, 3, 12, 0));
        shortcuts.setOpaque(false);
        shortcuts.add(shortcut("Mapa de peajes", "Ubica un peaje y mira su ficha y tarifas.", TravelerFrame.MAP));
        shortcuts.add(shortcut("Planificar viaje", "Peajes del camino y total a pagar.", TravelerFrame.TRIP));
        shortcuts.add(shortcut("Pedir asistencia", traveler == null
                ? "Necesita una cuenta: créela en un minuto."
                : "Grúa, carro taller, auxilio mecánico o ambulancia.", TravelerFrame.ASSISTANCE));

        JPanel middle = new JPanel(new BorderLayout(0, 18));
        middle.setOpaque(false);
        middle.add(shortcuts, BorderLayout.NORTH);
        if (traveler == null) {
            JPanel guestHolder = new JPanel(new BorderLayout());
            guestHolder.setOpaque(false);
            guestHolder.add(guestBox(), BorderLayout.NORTH);
            middle.add(guestHolder, BorderLayout.CENTER);
        }

        add(head, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        setBanner(null);
    }

    /**
     * @param action qué hacer cada vez que se cargan las solicitudes (contador del menú)
     */
    public void setOnLoaded(Consumer<List<Assistance>> action) {
        this.onLoaded = action == null ? l -> { } : action;
    }

    /**
     * @param action abre el seguimiento de una solicitud por su número
     */
    public void setOnOpenRequest(Consumer<Integer> action) {
        this.onOpenRequest = action == null ? id -> { } : action;
    }

    /**
     * @param catalog catálogo cargado
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        List<Toll> tolls = catalog.getTolls();
        int ops = 0;
        int tag = 0;
        Set<String> deps = new HashSet<>();
        for (Toll t : tolls) {
            if (TravelerUi.isOperating(t)) {
                ops++;
            }
            if ("S".equals(t.getAcceptsColpass())) {
                tag++;
            }
            if (t.getDepartmentId() != null) {
                deps.add(t.getDepartmentId());
            }
        }
        total.setValue(String.valueOf(tolls.size()), "ANI, INVÍAS y territoriales");
        operating.setValue(String.valueOf(ops), percent(ops, tolls.size()) + " del total");
        colpass.setValue(String.valueOf(tag), "pago electrónico");
        departments.setValue(String.valueOf(deps.size()), "con al menos un peaje");
    }

    /** Recarga la asistencia en curso (solo con cuenta). */
    public void refresh() {
        if (traveler == null) {
            return;
        }
        int my = ++loadRequest;
        Async.run(() -> {
            List<Assistance> all = service.myRequests(traveler);
            List<Assistance> active = StationManagerService.active(all);
            RequestDetail detail = active.isEmpty() ? null : service.requestDetail(active.get(0));
            return new Object[]{all, active, detail};
        }, data -> {
            if (my != loadRequest) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Assistance> all = (List<Assistance>) data[0];
            @SuppressWarnings("unchecked")
            List<Assistance> active = (List<Assistance>) data[1];
            setBanner(active.isEmpty() ? null : new Object[]{data[2], active.size()});
            onLoaded.accept(all);
        }, error -> setBanner(null));
    }

    /** Franja azul con la asistencia en curso (data = {detalle, cuántas activas}), o nada. */
    private void setBanner(Object[] data) {
        banner.removeAll();
        if (data == null) {
            banner.setBorder(null);
            banner.revalidate();
            banner.repaint();
            return;
        }
        RequestDetail d = (RequestDetail) data[0];
        int count = (Integer) data[1];
        Assistance a = d.getAssistance();
        Toll toll = catalog == null ? null : catalog.toll(a.getTollId());
        String tollName = toll == null ? "Peaje " + a.getTollId() : toll.getName();
        AssistanceResource r = d.getResource();

        JPanel box = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SIDEBAR);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.add(TravelerUi.label(count > 1 ? "TIENE " + count + " ASISTENCIAS EN CURSO · LA MÁS RECIENTE"
                : "TU ASISTENCIA EN CURSO", Font.BOLD, 11, Theme.SIDEBAR_MUTED));
        text.add(TravelerUi.label(StationTexts.type(a.getType()) + " #" + a.getAssistanceId() + " · "
                + AssistanceSteps.headline(a.getCurrentStatus(), a.getType(), tollName), Font.BOLD, 19, Color.WHITE));
        text.add(TravelerUi.label("Peaje " + tollName
                + (r == null ? " · aún sin recurso asignado" : " · " + StationTexts.type(r.getType()) + " "
                + r.getIdentification()) + " · pedida " + AssistanceSteps.when(a.getRequestDate()),
                Font.PLAIN, 13, Theme.SIDEBAR_TEXT));

        FlatButton open = new FlatButton("Ver seguimiento →");
        open.setColors(Color.WHITE, Theme.PRIMARY_SOFT, Theme.SIDEBAR, null);
        open.addActionListener(e -> onOpenRequest.accept(a.getAssistanceId()));
        JPanel buttonHolder = new JPanel(new java.awt.GridBagLayout());
        buttonHolder.setOpaque(false);
        buttonHolder.add(open);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(text, BorderLayout.CENTER);
        top.add(buttonHolder, BorderLayout.EAST);

        AssistanceSteps steps = new AssistanceSteps(true);
        steps.setData(a.getCurrentStatus(), d.getHistory());

        box.add(top, BorderLayout.NORTH);
        box.add(steps, BorderLayout.CENTER);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onOpenRequest.accept(a.getAssistanceId());
            }
        });
        banner.add(box, BorderLayout.CENTER);
        banner.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, banner.getPreferredSize().height));
        banner.revalidate();
        banner.repaint();
    }

    /** Recuadro del invitado: asistencia con cuenta y el 123 para urgencias. */
    private JComponent guestBox() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JPanel title = new JPanel(new BorderLayout(10, 0));
        title.setOpaque(false);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.add(new GuestAssistancePanel.LockIcon(28, Theme.PRIMARY, Theme.PRIMARY_SOFT), BorderLayout.WEST);
        title.add(Theme.title("¿Necesitas ayuda en la vía?"), BorderLayout.CENTER);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        box.add(title);
        box.add(Box.createVerticalStrut(8));
        JLabel text = TravelerUi.label("<html>Para pedir una grúa, un carro taller, auxilio mecánico o una ambulancia"
                + " necesitas una cuenta; así puedes seguir tu solicitud paso a paso."
                + " <span style='color:#2563eb'><b>Crear cuenta →</b></span><br>"
                + "En una emergencia llama al <b>123</b>.</html>", Font.PLAIN, 13, Theme.MUTED);
        text.setAlignmentX(LEFT_ALIGNMENT);
        text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        text.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate.accept(TravelerFrame.ASSISTANCE);
            }
        });
        box.add(text);
        return box;
    }

    private JPanel shortcut(String title, String text, String page) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                g.setColor(Theme.PRIMARY);
                g.fillRect(0, 0, getWidth(), 4);
            }
        };
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 16, 18)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(TravelerUi.label(title + "  →", Font.BOLD, 16, Theme.TEXT), BorderLayout.NORTH);
        card.add(TravelerUi.label("<html>" + text + "</html>", Font.PLAIN, 13, Theme.MUTED), BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate.accept(page);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(Theme.PRIMARY_SOFT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
            }
        });
        return card;
    }

    private static String percent(int part, int whole) {
        return whole == 0 ? "0 %" : Math.round(part * 100.0 / whole) + " %";
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }
}
