package com.invpeajes.ui.traveler;

import com.invpeajes.model.Assistance;
import com.invpeajes.model.Toll;
import com.invpeajes.model.Traveler;
import com.invpeajes.service.AuthService;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.service.TravelerService;
import com.invpeajes.service.TravelerService.Catalog;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.login.LoginFrame;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Ventana del viajero (con cuenta o invitado): barra lateral con las pantallas y
 * {@link CardLayout} para cambiar entre ellas, con el mismo estilo que la ventana del
 * jefe de estación.
 *
 * <p>Pantallas: Inicio, Mapa de peajes, Planificar viaje y Pedir asistencia.
 * Con cuenta, además: Mis solicitudes y Mi cuenta. El invitado ve "Pedir asistencia" con
 * candado: ahí puede crear su cuenta en un paso y queda dentro, listo para pedirla.</p>
 *
 * <p>Se abre desde {@link LoginFrame} con {@code new TravelerFrame(traveler).setVisible(true)};
 * {@code traveler} es null cuando entra como invitado.</p>
 *
 * @author Salma Jiménez Vega
 * @version 1.0
 */
public class TravelerFrame extends JFrame {

    /** Nombre de la pantalla de inicio. */
    public static final String HOME = "home";
    /** Nombre de la pantalla del mapa. */
    public static final String MAP = "map";
    /** Nombre de la pantalla para planificar un viaje. */
    public static final String TRIP = "trip";
    /** Nombre de la pantalla para pedir asistencia. */
    public static final String ASSISTANCE = "assistance";
    /** Nombre de la pantalla con las solicitudes del viajero. */
    public static final String REQUESTS = "requests";
    /** Nombre de la pantalla de la cuenta. */
    public static final String ACCOUNT = "account";

    /** Cada cuántos segundos se recargan las solicitudes cuando están a la vista. */
    private static final int REFRESH_SECONDS = 30;

    private final TravelerService service;
    private final Traveler traveler;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Inicio");
    private final JLabel pageSubtitle = Theme.muted(" ");
    private final JLabel loading = new JLabel(" ", SwingConstants.RIGHT);
    private final JLabel plateInfo = new JLabel();
    private final Map<String, NavItem> nav = new LinkedHashMap<>();
    private final Map<String, String[]> titles = new LinkedHashMap<>();
    private final HomePanel homePanel;
    private final MapPanel mapPanel;
    private final TripPanel tripPanel;
    /** Formulario de asistencia (con cuenta) o {@link GuestAssistancePanel} (invitado). */
    private final AssistancePanel assistancePanel;
    private final GuestAssistancePanel guestAssistancePanel;
    private final RequestsPanel requestsPanel;
    private final AccountPanel accountPanel;
    private final Timer refreshTimer;
    private String current = HOME;

    /**
     * Abre la ventana con los servicios de la base de datos.
     *
     * @param traveler viajero con cuenta, o null si entra como invitado
     */
    public TravelerFrame(Traveler traveler) {
        this(new TravelerService(), traveler);
    }

    /**
     * @param service  servicio del viajero
     * @param traveler viajero con cuenta, o null si entra como invitado
     */
    public TravelerFrame(TravelerService service, Traveler traveler) {
        super("RutaPeaje Colombia — " + (traveler == null ? "Invitado" : "Viajero · " + traveler.getFullName()));
        this.service = service;
        this.traveler = traveler;

        homePanel = new HomePanel(service, traveler, this::show);
        mapPanel = new MapPanel(service);
        tripPanel = new TripPanel(service, this::showOnMap);
        requestsPanel = traveler == null ? null : new RequestsPanel(service, traveler);
        assistancePanel = traveler == null ? null : new AssistancePanel(service, traveler, this::afterRequest);
        guestAssistancePanel = traveler != null ? null
                : new GuestAssistancePanel(new AuthService(), this::enterWithNewAccount, this::goToLogin);
        accountPanel = traveler == null ? null : new AccountPanel(service, traveler, this::afterAccountSaved);

        addPage(HOME, "Inicio", "Inicio", "Resumen y accesos rápidos", homePanel);
        addPage(MAP, "Mapa de peajes", "Mapa de peajes", "Filtra por departamento, ruta o concesionario",
                mapPanel);
        addPage(TRIP, "Planificar viaje", "Planificar viaje", "Peajes del camino, total a pagar y km aproximados",
                tripPanel);
        if (traveler == null) {
            addPage(ASSISTANCE, "Pedir asistencia", "Pedir asistencia",
                    "Necesita una cuenta: créela aquí mismo en un minuto", guestAssistancePanel);
            nav.get(ASSISTANCE).setLocked(true);
        } else {
            addPage(ASSISTANCE, "Pedir asistencia", "Pedir asistencia",
                    "La solicitud llega al jefe de estación del peaje más cercano", assistancePanel);
        }
        if (traveler != null) {
            addPage(REQUESTS, "Mis solicitudes", "Mis solicitudes", "Cómo va cada asistencia que ha pedido",
                    requestsPanel);
            requestsPanel.setOnLoaded(this::updateBadge);
            homePanel.setOnLoaded(this::updateBadge);
            homePanel.setOnOpenRequest(id -> {
                requestsPanel.select(id);
                show(REQUESTS);
            });
            addPage(ACCOUNT, "Mi cuenta", "Mi cuenta", "Sus datos y su placa", accountPanel);
        }

        refreshTimer = new Timer(REFRESH_SECONDS * 1000, e -> {
            if (REQUESTS.equals(current)) {
                requestsPanel.refresh();
            } else if (HOME.equals(current)) {
                homePanel.refresh();
            }
        });
        refreshTimer.start();

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(buildTopBar(), BorderLayout.NORTH);
        content.add(cardPanel, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildSidebar(), BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        show(HOME);
        loadCatalog();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });
        setMinimumSize(new Dimension(1200, 720));
        setSize(1366, 768);
        setLocationRelativeTo(null);
    }

    private void addPage(String id, String navText, String title, String subtitle, JComponent panel) {
        NavItem item = new NavItem(navText);
        item.onClick(() -> show(id));
        nav.put(id, item);
        titles.put(id, new String[]{title, subtitle});
        cardPanel.add(panel, id);
    }

    private void loadCatalog() {
        loading.setText("● Cargando peajes...");
        loading.setForeground(Theme.MUTED);
        Async.run(() -> {
            Catalog catalog = service.loadCatalog();
            Integer category = traveler == null ? null : service.categoryOfPlate(traveler.getPlate());
            return new Object[]{catalog, category};
        }, data -> {
            Catalog catalog = (Catalog) data[0];
            homePanel.setCatalog(catalog);
            mapPanel.setCatalog(catalog);
            tripPanel.setCatalog(catalog);
            if (assistancePanel != null) {
                assistancePanel.setCatalog(catalog);
            }
            if (guestAssistancePanel != null) {
                guestAssistancePanel.setCatalog(catalog);
            }
            if (requestsPanel != null) {
                requestsPanel.setCatalog(catalog);
            }
            if (data[1] != null) {
                tripPanel.selectCategory((Integer) data[1]);
            }
            loading.setText("● " + catalog.getTolls().size() + " peajes cargados");
            loading.setForeground(Theme.SUCCESS);
        }, error -> {
            loading.setText("● Sin conexión");
            loading.setForeground(Theme.DANGER);
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los peajes.\n" + error.getMessage(),
                    "RutaPeaje", JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Cambia de pantalla.
     *
     * @param page nombre de la pantalla (constantes de esta clase)
     */
    public void show(String page) {
        if (!nav.containsKey(page)) {
            return;
        }
        current = page;
        cards.show(cardPanel, page);
        for (Map.Entry<String, NavItem> e : nav.entrySet()) {
            e.getValue().setActive(e.getKey().equals(page));
        }
        pageTitle.setText(titles.get(page)[0]);
        pageSubtitle.setText(titles.get(page)[1]);
        if (HOME.equals(page)) {
            homePanel.refresh();
        } else if (REQUESTS.equals(page)) {
            requestsPanel.refresh();
        }
    }

    private void showOnMap(Toll toll) {
        show(MAP);
        mapPanel.focus(toll);
    }

    /** Pone en el menú cuántas solicitudes siguen en curso. */
    private void updateBadge(List<Assistance> requests) {
        NavItem item = nav.get(REQUESTS);
        if (item != null) {
            item.setBadge(StationManagerService.active(requests).size());
        }
    }

    /** El invitado acaba de crear su cuenta: entra con ella directo a "Pedir asistencia". */
    private void enterWithNewAccount(Traveler newTraveler) {
        refreshTimer.stop();
        dispose();
        TravelerFrame frame = new TravelerFrame(newTraveler);
        frame.show(ASSISTANCE);
        frame.setVisible(true);
        JOptionPane.showMessageDialog(frame, "¡Cuenta creada, " + newTraveler.getFirstName() + "!\n"
                + "Ya puede pedir la asistencia y seguirla en \"Mis solicitudes\".",
                "Cuenta creada", JOptionPane.INFORMATION_MESSAGE);
    }

    /** El invitado ya tiene cuenta: va al inicio de sesión del viajero. */
    private void goToLogin() {
        refreshTimer.stop();
        dispose();
        LoginFrame.openTravelerLogin();
    }

    private void afterRequest() {
        if (traveler != null) {
            show(REQUESTS);
        }
    }

    private void afterAccountSaved() {
        plateInfo.setText(traveler.getPlate() == null ? "Sin placa" : traveler.getPlate());
        if (assistancePanel != null) {
            assistancePanel.refreshPersonalData();
        }
    }

    private void logout() {
        String question = traveler == null ? "¿Desea salir?" : "¿Desea cerrar la sesión?";
        int answer = JOptionPane.showOptionDialog(this, question, "Salir",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[]{traveler == null ? "Sí, salir" : "Sí, cerrar sesión", "Cancelar"}, "Cancelar");
        if (answer == JOptionPane.YES_OPTION) {
            refreshTimer.stop();
            dispose();
            LoginFrame.open();
        }
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(26, 14, 18, 14));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(indent(label("RutaPeaje", Font.BOLD, 26, Color.WHITE)));
        top.add(indent(label("Colombia", Font.PLAIN, 14, Theme.SIDEBAR_MUTED)));
        top.add(Box.createVerticalStrut(30));
        for (NavItem item : nav.values()) {
            top.add(item);
            top.add(Box.createVerticalStrut(6));
        }
        top.add(Box.createVerticalStrut(20));
        if (traveler == null) {
            top.add(info("VIAJERO", new JLabel("Invitado")));
        } else {
            top.add(info("VIAJERO", new JLabel(traveler.getFullName())));
            plateInfo.setText(traveler.getPlate() == null ? "Sin placa" : traveler.getPlate());
            top.add(info("PLACA", plateInfo));
        }

        JPanel bottom = new JPanel(new BorderLayout(0, 14));
        bottom.setOpaque(false);
        FlatButton close = new FlatButton(traveler == null ? "Salir" : "Cerrar sesión");
        close.setColors(Theme.SIDEBAR, new Color(0x1B3A78), Color.WHITE, Theme.SIDEBAR_MUTED);
        close.addActionListener(e -> logout());
        JLabel footer = new JLabel("<html>Conectando caminos,<br>impulsando el país</html>");
        footer.setFont(Theme.font(Font.PLAIN, 12));
        footer.setForeground(Theme.SIDEBAR_MUTED);
        bottom.add(close, BorderLayout.NORTH);
        bottom.add(footer, BorderLayout.SOUTH);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 22, 14, 22)));
        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        pageTitle.setFont(Theme.font(Font.BOLD, 20));
        pageTitle.setForeground(Theme.TEXT);
        left.add(pageTitle);
        left.add(pageSubtitle);
        loading.setFont(Theme.font(Font.PLAIN, 13));
        bar.add(left, BorderLayout.WEST);
        bar.add(loading, BorderLayout.EAST);
        return bar;
    }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        return label;
    }

    private static JComponent indent(JComponent c) {
        c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private static JPanel info(String caption, JLabel value) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        panel.add(label(caption, Font.BOLD, 11, Theme.SIDEBAR_MUTED));
        value.setFont(Theme.font(Font.PLAIN, 14));
        value.setForeground(Theme.SIDEBAR_TEXT);
        panel.add(value);
        return panel;
    }

    /** Opción del menú lateral (mismo diseño que en la ventana del jefe de estación). */
    private static final class NavItem extends JPanel {
        private final JLabel text;
        private final JPanel right = new JPanel(new BorderLayout());
        private final Badge badge = new Badge();
        private boolean active;
        private boolean hover;
        private boolean locked;

        NavItem(String label) {
            super(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(232, 40));
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            text = new JLabel(label);
            add(text, BorderLayout.CENTER);
            right.setOpaque(false);
            add(right, BorderLayout.EAST);
            setActive(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void onClick(Runnable action) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        /** Muestra un candado: la pantalla necesita una cuenta. */
        void setLocked(boolean value) {
            locked = value;
            right.removeAll();
            if (value) {
                JPanel holder = new JPanel(new java.awt.GridBagLayout());
                holder.setOpaque(false);
                holder.add(new GuestAssistancePanel.LockIcon(14, Theme.SIDEBAR_MUTED, null));
                right.add(holder, BorderLayout.CENTER);
                setToolTipText("Necesita una cuenta");
            }
            revalidate();
            repaint();
        }

        /** Número de solicitudes en curso (0 lo oculta). */
        void setBadge(int count) {
            if (locked) {
                return;
            }
            badge.count = count;
            right.removeAll();
            if (count > 0) {
                JPanel holder = new JPanel(new java.awt.GridBagLayout());
                holder.setOpaque(false);
                holder.add(badge);
                right.add(holder, BorderLayout.CENTER);
            }
            badge.active = active;
            revalidate();
            repaint();
        }

        void setActive(boolean value) {
            active = value;
            badge.active = value;
            text.setFont(Theme.font(value ? Font.BOLD : Font.PLAIN, 15));
            text.setForeground(value ? Color.WHITE : Theme.SIDEBAR_TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? Theme.PRIMARY : new Color(0x1B3A78));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Circulito con un número, para el menú lateral. */
    private static final class Badge extends JComponent {
        private int count;
        private boolean active;

        Badge() {
            Dimension d = new Dimension(22, 22);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(active ? Color.WHITE : Theme.PRIMARY);
            g2.fillOval(0, 0, 22, 22);
            g2.setColor(active ? Theme.PRIMARY_DARK : Color.WHITE);
            g2.setFont(Theme.font(Font.BOLD, 12));
            String t = count > 9 ? "9+" : String.valueOf(count);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(t, 11 - fm.stringWidth(t) / 2, 11 + fm.getAscent() / 2 - 2);
            g2.dispose();
        }
    }
}
