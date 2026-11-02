package com.invpeajes.ui.cashier;

import com.invpeajes.model.CashierSession;
import com.invpeajes.service.CashierService;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Ventana de la sesión del cajero: barra lateral con los datos del turno y dos apartados,
 * "Cobro de peaje" y "Mis cobros del turno". Al cerrar el turno muestra el resumen
 * (cobros, recaudado, efectivo en caja) y vuelve a la apertura de turno.
 *
 * @author Luis Guerrero
 * @version 1.1
 */
public class CashierFrame extends JFrame {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final String CHARGE = "cobro";
    private static final String HISTORY = "historial";

    private final CashierService service;
    private final CashierSession session;
    private final ShiftLog log = new ShiftLog();
    private final LocalDateTime openedAt = LocalDateTime.now();
    private final Timer clockTimer;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Cobro de peaje");
    private final FlatButton chargeNav = navButton("Cobro de peaje");
    private final FlatButton historyNav = navButton("Mis cobros del turno");

    /**
     * @param service Servicio de la caja.
     * @param session Turno abierto (con caseta).
     */
    public CashierFrame(CashierService service, CashierSession session) {
        super("RutaPeaje Colombia — Caja · Peaje " + session.getTollName());
        this.service = service;
        this.session = session;

        JLabel clock = new JLabel();
        clock.setFont(Theme.font(Font.PLAIN, 13));
        clock.setForeground(Theme.MUTED);
        clockTimer = new Timer(1000, e -> clock.setText(LocalDateTime.now().format(CLOCK)));
        clockTimer.setInitialDelay(0);
        clockTimer.start();

        cardPanel.add(new CashierPanel(service, session, log), CHARGE);
        cardPanel.add(new HistoryPanel(log), HISTORY);
        chargeNav.addActionListener(e -> show(CHARGE));
        historyNav.addActionListener(e -> show(HISTORY));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(buildTopBar(clock), BorderLayout.NORTH);
        content.add(cardPanel, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildSidebar(), BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        show(CHARGE);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeShift();
            }
        });
        setMinimumSize(new Dimension(1200, 720));
        setSize(1366, 768);
        setLocationRelativeTo(null);
    }

    /**
     * Abre la caja: pide la cédula y la caseta, y muestra la pantalla de cobro.
     * Si el operador cancela, la aplicación termina.
     * @param service Servicio de la caja.
     */
    public static void start(CashierService service) {
        CashierSession session = new ShiftStartDialog(null, service).showDialog();
        if (session == null) {
            System.exit(0);
        }
        new CashierFrame(service, session).setVisible(true);
    }

    /**
     * Punto de entrada de la caja (sesión del cajero).
     * @param args No se usan.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Si no se puede usar el estilo del sistema, Swing usa el suyo por defecto.
        }
        SwingUtilities.invokeLater(() ->
                start(new CashierService()));
    }

    private void show(String page) {
        cards.show(cardPanel, page);
        boolean charge = CHARGE.equals(page);
        pageTitle.setText(charge ? "Cobro de peaje" : "Mis cobros del turno");
        styleNav(chargeNav, charge);
        styleNav(historyNav, !charge);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(26, 14, 18, 14));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(indent(sidebarLabel("RutaPeaje", Font.BOLD, 26, Color.WHITE)));
        top.add(indent(sidebarLabel("Colombia", Font.PLAIN, 14, Theme.SIDEBAR_MUTED)));
        top.add(Box.createVerticalStrut(30));
        top.add(chargeNav);
        top.add(Box.createVerticalStrut(6));
        top.add(historyNav);
        top.add(Box.createVerticalStrut(26));

        top.add(info("OPERADOR", session.getOperatorName()));
        top.add(info("PEAJE", session.getTollName()));
        top.add(info("CASETA", session.getBooth().getNumeroCaseta() + " · " + session.getBooth().getTipoPago()));
        top.add(info("TURNO DESDE", openedAt.format(HOUR)));

        JPanel bottom = new JPanel(new BorderLayout(0, 14));
        bottom.setOpaque(false);
        FlatButton close = new FlatButton("Cerrar turno");
        close.setColors(Theme.SIDEBAR, new Color(0x1B3A78), Color.WHITE, Theme.SIDEBAR_MUTED);
        close.addActionListener(e -> closeShift());
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
        left.add(Theme.muted("Peaje " + session.getTollName() + " · Caseta "
                + session.getBooth().getNumeroCaseta() + " (" + session.getBooth().getTipoPago() + ")"));

        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        JLabel status = new JLabel("● Turno abierto desde las " + openedAt.format(HOUR), SwingConstants.RIGHT);
        status.setFont(Theme.font(Font.PLAIN, 13));
        status.setForeground(Theme.SUCCESS);
        clock.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(status);
        right.add(clock);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void closeShift() {
        String summary = "<html><b>Resumen del turno</b><br><br>"
                + "Operador: " + session.getOperatorName() + "<br>"
                + "Peaje " + session.getTollName() + " · Caseta " + session.getBooth().getNumeroCaseta() + "<br>"
                + "Desde " + openedAt.format(HOUR) + " hasta " + LocalDateTime.now().format(HOUR) + "<br><br>"
                + "Cobros: <b>" + log.count() + "</b><br>"
                + "Recaudado: <b>" + Theme.money(log.total()) + "</b><br>"
                + "Efectivo en caja: <b>" + Theme.money(log.totalBy(CashierService.CASH)) + "</b><br><br>"
                + "¿Desea cerrar el turno?</html>";
        Object[] options = {"Sí, cerrar turno", "Seguir cobrando"};
        int answer = JOptionPane.showOptionDialog(this, summary, "Cerrar turno",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (answer == JOptionPane.YES_OPTION) {
            clockTimer.stop();
            dispose();
            start(service);
        }
    }

    private static FlatButton navButton(String text) {
        FlatButton b = new FlatButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(Theme.font(Font.PLAIN, 15));
        b.setFocusable(false);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }

    private static void styleNav(FlatButton b, boolean active) {
        if (active) {
            b.setColors(Theme.PRIMARY, Theme.PRIMARY, Color.WHITE, null);
            b.setFont(Theme.font(Font.BOLD, 15));
        } else {
            b.setColors(Theme.SIDEBAR, new Color(0x1B3A78), Theme.SIDEBAR_TEXT, null);
            b.setFont(Theme.font(Font.PLAIN, 15));
        }
    }

    private static JLabel sidebarLabel(String text, int style, int size, Color color) {
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
        panel.add(sidebarLabel(caption, Font.BOLD, 11, Theme.SIDEBAR_MUTED));
        panel.add(sidebarLabel(value, Font.PLAIN, 14, Theme.SIDEBAR_TEXT));
        return panel;
    }
}
