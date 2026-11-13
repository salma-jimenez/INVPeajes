package com.invpeajes.ui.station;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.Toll;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.PasswordDialog;
import com.invpeajes.ui.common.SideNav;
import com.invpeajes.ui.common.SideNav.NavItem;
import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.login.LoginFrame;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Ventana de la sesión del jefe de estación: barra lateral con sus datos y cinco apartados,
 * "Centro de asistencia", "Recursos de asistencia", "Estadísticas del peaje", "Casetas del peaje"
 * y "Empleados del peaje". Abajo de la barra puede cambiar su contraseña.
 *
 * El Centro de asistencia, los Recursos y las Casetas se actualizan solos cada
 * {@link #REFRESH_SECONDS} segundos, para que aparezcan las solicitudes y los cobros nuevos.
 *
 * @author Salomejimenez
 * @version 2.0
 */
public class StationFrame extends JFrame {

    /** Cada cuántos segundos se recargan las solicitudes, los recursos y las casetas. */
    public static final int REFRESH_SECONDS = 15;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final String CENTER = "center";
    private static final String RESOURCES = "resources";
    private static final String STATISTICS = "statistics";
    private static final String BOOTHS = "booths";
    private static final String OPERATORS = "operators";

    private final CashierSession session;
    private final LocalDateTime openedAt = LocalDateTime.now();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Centro de asistencia");
    private final JLabel pageSubtitle = Theme.muted(" ");
    private final JLabel updated = new JLabel(" ", SwingConstants.RIGHT);
    private final Map<String, NavItem> nav = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final AssistanceCenterPanel centerPanel;
    private final ResourcesPanel resourcesPanel;
    private final StatisticsPanel statisticsPanel;
    private final BoothsPanel boothsPanel;
    private final OperatorsPanel operatorsPanel;
    private final Timer clockTimer;
    private final Timer refreshTimer;
    private String current = CENTER;
    private LocalDateTime lastRefresh;

    /**
     * @param service Servicio del jefe de estación.
     * @param session Sesión del jefe (con su peaje).
     */
    public StationFrame(StationManagerService service, CashierSession session) {
        super("RutaPeaje Colombia — Jefe de estación · Peaje " + session.getTollName());
        this.session = session;

        centerPanel = new AssistanceCenterPanel(service, session, this::setBadge, this::markRefreshed);
        resourcesPanel = new ResourcesPanel(service, session, id -> {
            show(CENTER);
            centerPanel.select(id);
        }, this::markRefreshed);
        statisticsPanel = new StatisticsPanel(service, session);
        boothsPanel = new BoothsPanel(service, session, this::markRefreshed);
        operatorsPanel = new OperatorsPanel(service, session);
        cardPanel.add(centerPanel, CENTER);
        cardPanel.add(resourcesPanel, RESOURCES);
        cardPanel.add(statisticsPanel, STATISTICS);
        cardPanel.add(boothsPanel, BOOTHS);
        cardPanel.add(operatorsPanel, OPERATORS);

        SideNav side = new SideNav();
        addPage(side, CENTER, "Centro de asistencia");
        addPage(side, RESOURCES, "Recursos de asistencia");
        addPage(side, STATISTICS, "Estadísticas del peaje");
        addPage(side, BOOTHS, "Casetas del peaje");
        addPage(side, OPERATORS, "Empleados del peaje");
        side.addInfo("JEFE DE ESTACIÓN", session.getOperatorName());
        side.addInfo("PEAJE", session.getTollName());
        side.addInfo("SESIÓN DESDE", openedAt.format(HOUR));
        side.addBottomLink("Cambiar contraseña",
                () -> PasswordDialog.open(this, service.staff(), session.getOperatorId()));
        side.addBottomButton("Cerrar sesión", this::logout);

        JLabel clock = new JLabel(" ", SwingConstants.RIGHT);
        clock.setFont(Theme.font(Font.PLAIN, 13));
        clock.setForeground(Theme.MUTED);
        clockTimer = new Timer(1000, e -> {
            clock.setText(LocalDateTime.now().format(CLOCK));
            updateRefreshLabel();
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();
        refreshTimer = new Timer(REFRESH_SECONDS * 1000, e -> refreshCurrent());
        refreshTimer.start();

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(buildTopBar(clock), BorderLayout.NORTH);
        content.add(cardPanel, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(side, BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        show(CENTER);

        Async.run(() -> service.toll(session.getTollId()), this::setToll, error -> setToll(null));

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

    private void addPage(SideNav side, String page, String title) {
        titles.put(page, title);
        nav.put(page, side.addItem(title, () -> show(page)));
    }

    private void setToll(Toll toll) {
        centerPanel.setToll(toll);
        pageSubtitle.setText("Peaje " + session.getTollName()
                + (toll != null && toll.getLocationDescription() != null ? " · " + toll.getLocationDescription() : ""));
    }

    private void show(String page) {
        current = page;
        cards.show(cardPanel, page);
        nav.forEach((name, item) -> item.setActive(name.equals(page)));
        pageTitle.setText(titles.get(page));
        updated.setVisible(CENTER.equals(page) || RESOURCES.equals(page) || BOOTHS.equals(page));
        refreshCurrent();
    }

    private void refreshCurrent() {
        switch (current) {
            case CENTER:
                centerPanel.refresh();
                break;
            case RESOURCES:
                resourcesPanel.refresh();
                centerPanel.refreshCountsOnly();
                break;
            case BOOTHS:
                boothsPanel.refresh();
                centerPanel.refreshCountsOnly();
                break;
            case OPERATORS:
                operatorsPanel.refresh();
                centerPanel.refreshCountsOnly();
                break;
            default:
                statisticsPanel.refresh();
                centerPanel.refreshCountsOnly();
                break;
        }
    }

    private void markRefreshed() {
        lastRefresh = LocalDateTime.now();
        updateRefreshLabel();
    }

    private void updateRefreshLabel() {
        if (lastRefresh == null) {
            updated.setText("● Cargando...");
            updated.setForeground(Theme.MUTED);
            return;
        }
        long seconds = Duration.between(lastRefresh, LocalDateTime.now()).getSeconds();
        updated.setText("● Actualizado hace " + seconds + " s");
        updated.setForeground(Theme.SUCCESS);
    }

    private void setBadge(int unattended) {
        nav.get(CENTER).setBadge(unattended);
    }

    private void logout() {
        int answer = JOptionPane.showOptionDialog(this, "¿Desea cerrar la sesión?", "Cerrar sesión",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[]{"Sí, cerrar sesión", "Cancelar"}, "Cancelar");
        if (answer == JOptionPane.YES_OPTION) {
            clockTimer.stop();
            refreshTimer.stop();
            dispose();
            LoginFrame.open();
        }
    }

    private JPanel buildTopBar(JLabel clock) {
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
        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        updated.setFont(Theme.font(Font.PLAIN, 13));
        right.add(updated);
        right.add(clock);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
