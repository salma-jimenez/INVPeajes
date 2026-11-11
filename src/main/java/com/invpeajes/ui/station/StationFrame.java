package com.invpeajes.ui.station;

import com.invpeajes.model.CashierSession;
import com.invpeajes.model.Toll;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Pill;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * Ventana de la sesión del jefe de estación: barra lateral con sus datos y tres apartados,
 * "Centro de asistencia", "Recursos de asistencia" y "Estadísticas del peaje".
 *
 * El Centro de asistencia y los Recursos se actualizan solos cada {@link #REFRESH_SECONDS}
 * segundos, para que aparezcan las solicitudes nuevas de los viajeros.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StationFrame extends JFrame {

    /** Cada cuántos segundos se recargan las solicitudes y los recursos. */
    public static final int REFRESH_SECONDS = 15;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final String CENTER = "center";
    private static final String RESOURCES = "resources";
    private static final String STATISTICS = "statistics";

    private final CashierSession session;
    private final LocalDateTime openedAt = LocalDateTime.now();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Centro de asistencia");
    private final JLabel pageSubtitle = Theme.muted(" ");
    private final JLabel updated = new JLabel(" ", SwingConstants.RIGHT);
    private final NavItem centerNav = new NavItem("Centro de asistencia");
    private final NavItem resourcesNav = new NavItem("Recursos de asistencia");
    private final NavItem statisticsNav = new NavItem("Estadísticas del peaje");
    private final AssistanceCenterPanel centerPanel;
    private final ResourcesPanel resourcesPanel;
    private final StatisticsPanel statisticsPanel;
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
        cardPanel.add(centerPanel, CENTER);
        cardPanel.add(resourcesPanel, RESOURCES);
        cardPanel.add(statisticsPanel, STATISTICS);
        centerNav.onClick(() -> show(CENTER));
        resourcesNav.onClick(() -> show(RESOURCES));
        statisticsNav.onClick(() -> show(STATISTICS));

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
        getContentPane().add(buildSidebar(), BorderLayout.WEST);
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

    private void setToll(Toll toll) {
        centerPanel.setToll(toll);
        pageSubtitle.setText("Peaje " + session.getTollName()
                + (toll != null && toll.getLocationDescription() != null ? " · " + toll.getLocationDescription() : ""));
    }

    private void show(String page) {
        current = page;
        cards.show(cardPanel, page);
        centerNav.setActive(CENTER.equals(page));
        resourcesNav.setActive(RESOURCES.equals(page));
        statisticsNav.setActive(STATISTICS.equals(page));
        pageTitle.setText(CENTER.equals(page) ? "Centro de asistencia"
                : RESOURCES.equals(page) ? "Recursos de asistencia" : "Estadísticas del peaje");
        updated.setVisible(!STATISTICS.equals(page));
        refreshCurrent();
    }

    private void refreshCurrent() {
        if (CENTER.equals(current)) {
            centerPanel.refresh();
        } else if (RESOURCES.equals(current)) {
            resourcesPanel.refresh();
            centerPanel.refreshCountsOnly();
        } else {
            statisticsPanel.refresh();
            centerPanel.refreshCountsOnly();
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
        centerNav.setBadge(unattended);
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
        top.add(centerNav);
        top.add(Box.createVerticalStrut(6));
        top.add(resourcesNav);
        top.add(Box.createVerticalStrut(6));
        top.add(statisticsNav);
        top.add(Box.createVerticalStrut(26));
        top.add(info("JEFE DE ESTACIÓN", session.getOperatorName()));
        top.add(info("PEAJE", session.getTollName()));
        top.add(info("SESIÓN DESDE", openedAt.format(HOUR)));

        JPanel bottom = new JPanel(new BorderLayout(0, 14));
        bottom.setOpaque(false);
        FlatButton close = new FlatButton("Cerrar sesión");
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

    private static JPanel info(String caption, String value) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        panel.add(label(caption, Font.BOLD, 11, Theme.SIDEBAR_MUTED));
        panel.add(label(value, Font.PLAIN, 14, Theme.SIDEBAR_TEXT));
        return panel;
    }

    /** Opción del menú lateral, con un contador rojo opcional a la derecha. */
    private static final class NavItem extends JPanel {
        private final JLabel text;
        private final Pill badge = new Pill("0", Theme.DANGER, Color.WHITE);
        private boolean active;
        private boolean hover;

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
            JPanel badgeBox = new JPanel(new java.awt.GridBagLayout());
            badgeBox.setOpaque(false);
            badgeBox.add(badge);
            badge.setVisible(false);
            add(badgeBox, BorderLayout.EAST);
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

        void setActive(boolean value) {
            active = value;
            text.setFont(Theme.font(value ? Font.BOLD : Font.PLAIN, 15));
            text.setForeground(value ? Color.WHITE : Theme.SIDEBAR_TEXT);
            repaint();
        }

        void setBadge(int count) {
            badge.setText(String.valueOf(count));
            badge.setVisible(count > 0);
            revalidate();
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
}
