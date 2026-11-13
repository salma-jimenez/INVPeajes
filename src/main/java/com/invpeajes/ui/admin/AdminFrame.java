package com.invpeajes.ui.admin;

import com.invpeajes.model.CashierSession;
import com.invpeajes.service.AdminService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.SideNav;
import com.invpeajes.ui.common.SideNav.NavItem;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.login.LoginFrame;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Ventana del administrador de una entidad (ANI, INVÍAS...): barra lateral con sus datos y cinco
 * apartados, "Resumen", "Mapa", "Peajes", "Empleados" y "Mi cuenta". Solo muestra y deja cambiar
 * lo de los peajes de su entidad.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AdminFrame extends JFrame {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    static final String SUMMARY = "summary";
    static final String MAP = "map";
    static final String TOLLS = "tolls";
    static final String STAFF = "staff";
    static final String ACCOUNT = "account";

    private final AdminService service;
    private final CashierSession session;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Resumen");
    private final JLabel entityPill = new JLabel();
    private final JLabel pageSubtitle = Theme.muted(" ");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final Map<String, NavItem> nav = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final Map<String, AdminPage> pages = new LinkedHashMap<>();
    private final SummaryPanel summaryPanel;
    private final AdminMapPanel mapPanel;
    private final TollsPanel tollsPanel;
    private final StaffPanel staffPanel;
    private final AccountPanel accountPanel;
    private final JLabel entityInfo;
    private final JLabel tollsInfo;
    private final Timer clockTimer;
    private AdminContext context;
    private String current = SUMMARY;

    /**
     * @param service Servicio del administrador.
     * @param session Sesión del administrador (de aquí sale su cédula).
     */
    public AdminFrame(AdminService service, CashierSession session) {
        super("RutaPeaje Colombia — Administración");
        this.service = service;
        this.session = session;

        summaryPanel = new SummaryPanel(service, () -> show(TOLLS));
        mapPanel = new AdminMapPanel(this::openToll);
        tollsPanel = new TollsPanel(service, this::reload, this::showOnMap);
        staffPanel = new StaffPanel(service);
        accountPanel = new AccountPanel(service, admin -> {
            context = new AdminContext(admin, context.getCatalog());
            pages.values().forEach(p -> p.setContext(context));
        });

        SideNav side = new SideNav();
        addPage(side, SUMMARY, "Resumen", summaryPanel);
        addPage(side, MAP, "Mapa", mapPanel);
        addPage(side, TOLLS, "Peajes", tollsPanel);
        addPage(side, STAFF, "Empleados", staffPanel);
        addPage(side, ACCOUNT, "Mi cuenta", accountPanel);
        side.addInfo("ADMINISTRADOR", session.getOperatorName());
        entityInfo = side.addInfo("ENTIDAD", "…");
        tollsInfo = side.addInfo("PEAJES A CARGO", "…");
        side.addInfo("SESIÓN DESDE", LocalDateTime.now().format(HOUR));
        side.addBottomButton("Cerrar sesión", this::logout);

        JLabel clock = new JLabel(" ", SwingConstants.RIGHT);
        clock.setFont(Theme.font(Font.PLAIN, 13));
        clock.setForeground(Theme.MUTED);
        clockTimer = new Timer(1000, e -> clock.setText(LocalDateTime.now().format(CLOCK)));
        clockTimer.setInitialDelay(0);
        clockTimer.start();

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(buildTopBar(clock), BorderLayout.NORTH);
        content.add(cardPanel, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(side, BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        show(SUMMARY);
        load();

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

    private void openToll(com.invpeajes.model.Toll toll) {
        show(TOLLS);
        tollsPanel.select(toll.getTollId());
    }

    private void showOnMap(com.invpeajes.model.Toll toll) {
        show(MAP);
        mapPanel.focus(toll);
    }

    private void addPage(SideNav side, String name, String title, AdminPage page) {
        titles.put(name, title);
        pages.put(name, page);
        cardPanel.add((JComponent) page, name);
        nav.put(name, side.addItem(title, () -> show(name)));
    }

    /** Carga al administrador y el catálogo, y se los pasa a todas las pantallas. */
    private void load() {
        pageSubtitle.setText("Cargando los peajes de tu entidad...");
        Async.run(() -> new AdminContext(service.admin(session.getOperatorId()), service.catalog()), ctx -> {
            setContext(ctx);
            pages.get(current).refresh();
        }, error -> {
            pageSubtitle.setText(" ");
            JOptionPane.showMessageDialog(this, "No se pudo abrir el panel de Administración:\n"
                    + error.getMessage(), "Administración", JOptionPane.ERROR_MESSAGE);
        });
    }

    /** Vuelve a cargar el catálogo (después de registrar, editar o borrar un peaje). */
    private void reload(Integer selectTollId) {
        Async.run(() -> new AdminContext(context.getAdmin(), service.catalog()), ctx -> {
            setContext(ctx);
            if (selectTollId != null) {
                tollsPanel.select(selectTollId);
            }
        }, error -> JOptionPane.showMessageDialog(this, error.getMessage(), "Administración",
                JOptionPane.WARNING_MESSAGE));
    }

    private void setContext(AdminContext ctx) {
        context = ctx;
        entityInfo.setText("<html>" + Labels.entityLong(ctx.getLevel()) + "</html>");
        tollsInfo.setText(String.valueOf(ctx.getMine().size()));
        entityPill.setText(Labels.entity(ctx.getLevel()));
        entityPill.setVisible(true);
        pages.values().forEach(p -> p.setContext(ctx));
        updateHeader();
    }

    void show(String name) {
        current = name;
        cards.show(cardPanel, name);
        nav.forEach((n, item) -> item.setActive(n.equals(name)));
        updateHeader();
        if (context != null) {
            pages.get(name).refresh();
        }
    }

    private void updateHeader() {
        AdminPage page = pages.get(current);
        pageTitle.setText(titles.get(current));
        pageSubtitle.setText(context == null ? "Cargando..." : page.subtitle(context));
        actions.removeAll();
        JComponent action = page.topAction();
        if (action != null && context != null) {
            actions.add(action);
        }
        actions.revalidate();
        actions.repaint();
    }

    private void logout() {
        int answer = JOptionPane.showOptionDialog(this, "¿Desea cerrar la sesión?", "Cerrar sesión",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[]{"Sí, cerrar sesión", "Cancelar"}, "Cancelar");
        if (answer == JOptionPane.YES_OPTION) {
            clockTimer.stop();
            dispose();
            LoginFrame.open();
        }
    }

    private JPanel buildTopBar(JLabel clock) {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 22, 14, 22)));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        pageTitle.setFont(Theme.font(Font.BOLD, 20));
        pageTitle.setForeground(Theme.TEXT);
        entityPill.setFont(Theme.font(Font.BOLD, 11));
        entityPill.setForeground(new Color(0x1D4ED8));
        entityPill.setOpaque(true);
        entityPill.setBackground(new Color(0xDBEAFE));
        entityPill.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        entityPill.setVisible(false);
        titleRow.add(pageTitle);
        titleRow.add(javax.swing.Box.createHorizontalStrut(10));
        titleRow.add(entityPill);
        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(titleRow);
        left.add(pageSubtitle);
        JPanel right = new JPanel(new BorderLayout(14, 0));
        right.setOpaque(false);
        actions.setOpaque(false);
        JPanel clockBox = new JPanel(new GridLayout(2, 1));
        clockBox.setOpaque(false);
        JLabel online = new JLabel("● Conectado", SwingConstants.RIGHT);
        online.setFont(Theme.font(Font.PLAIN, 13));
        online.setForeground(Theme.SUCCESS);
        clockBox.add(online);
        clockBox.add(clock);
        JPanel actionBox = new JPanel(new java.awt.GridBagLayout());
        actionBox.setOpaque(false);
        actionBox.add(actions);
        right.add(actionBox, BorderLayout.CENTER);
        right.add(clockBox, BorderLayout.EAST);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
