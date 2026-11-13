package com.invpeajes.ui.station;

import com.invpeajes.model.BoothActivity;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.TollStatistics.Total;
import com.invpeajes.model.TollUser;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Charts;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.Pill;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * "Casetas del peaje" del jefe de estación: una fila por caseta con su sentido, forma de pago,
 * quién está cobrando, los cobros y el recaudo de hoy, y un botón para cambiar su estado
 * (Activa, Mantenimiento o Inactiva). Abajo, los últimos cobros del peaje.
 *
 * Agregar o quitar casetas y cambiar su forma de pago lo hace el administrador de la entidad.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class BoothsPanel extends JPanel {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final int CHARGES = 50;

    private final StationManagerService service;
    private final CashierSession session;
    private final Runnable onRefreshed;
    private final StatTile workingTile = new StatTile("Casetas cobrando", Theme.SUCCESS, 26);
    private final StatTile operatorsTile = new StatTile("Operadores en turno", Theme.PRIMARY, 26);
    private final StatTile ticketsTile = new StatTile("Tiquetes hoy", Theme.TEXT, 26);
    private final StatTile revenueTile = new StatTile("Recaudado hoy", Theme.WARNING, 26);
    private final JPanel rows = new JPanel();
    private final JPanel chargeChips = new JPanel(new GridLayout(1, 4, 10, 0));
    private List<BoothActivity.Charge> charges = List.of();

    /**
     * @param service Servicio del jefe de estación.
     * @param session Sesión del jefe (con su peaje).
     * @param onRefreshed Se llama cada vez que terminan de cargar los datos.
     */
    public BoothsPanel(StationManagerService service, CashierSession session, Runnable onRefreshed) {
        super(new BorderLayout(0, 14));
        this.service = service;
        this.session = session;
        this.onRefreshed = onRefreshed;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(workingTile);
        tiles.add(operatorsTile);
        tiles.add(ticketsTile);
        tiles.add(revenueTile);

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(Color.WHITE);
        JPanel boothsCard = new JPanel(new BorderLayout());
        boothsCard.setBackground(Color.WHITE);
        boothsCard.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        boothsCard.add(rows, BorderLayout.NORTH);

        JPanel chargesCard = Theme.card();
        chargesCard.setLayout(new BorderLayout(0, 10));
        JPanel chargesHead = new JPanel(new BorderLayout());
        chargesHead.setOpaque(false);
        chargesHead.add(Theme.title("Últimos cobros"), BorderLayout.WEST);
        JLabel all = new JLabel("Ver todos los de hoy →");
        all.setFont(Theme.font(Font.BOLD, 13));
        all.setForeground(Theme.PRIMARY);
        all.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        all.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showAllCharges();
            }
        });
        chargesHead.add(all, BorderLayout.EAST);
        chargeChips.setOpaque(false);
        chargesCard.add(chargesHead, BorderLayout.NORTH);
        chargesCard.add(chargeChips, BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        boothsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        chargesCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(boothsCard);
        center.add(Box.createVerticalStrut(14));
        center.add(chargesCard);
        JScrollPane scroll = new JScrollPane(Forms.fitWidth(center));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(tiles, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /** Recarga las casetas, los últimos cobros y la comparación con ayer. */
    public void refresh() {
        int tollId = session.getTollId();
        Async.run(() -> new Object[]{
            service.booths(tollId),
            service.recentCharges(tollId, CHARGES),
            service.yesterdayUntilNow(tollId),
            service.staff().operators(tollId),
        }, this::fill, error -> {
            rows.removeAll();
            rows.add(message("No se pudieron cargar las casetas: " + error.getMessage()));
            rows.revalidate();
        });
    }

    @SuppressWarnings("unchecked")
    private void fill(Object[] data) {
        List<BoothActivity> booths = (List<BoothActivity>) data[0];
        charges = (List<BoothActivity.Charge>) data[1];
        Total yesterday = (Total) data[2];
        List<TollUser> operators = (List<TollUser>) data[3];
        LocalDateTime now = LocalDateTime.now();

        long active = booths.stream().filter(b -> "ACTIVA".equals(b.getBooth().getStatus())).count();
        long maintenance = booths.stream().filter(b -> "MANTENIMIENTO".equals(b.getBooth().getStatus())).count();
        long attended = booths.stream().filter(b -> b.isAttended(now)).count();
        long tickets = booths.stream().mapToLong(BoothActivity::getTickets).sum();
        java.math.BigDecimal revenue = booths.stream().map(BoothActivity::getRevenue)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long activeOperators = operators.stream().filter(TollUser::isActive).count();

        workingTile.setValue(active + " de " + booths.size(), maintenance == 0 ? "ninguna en mantenimiento"
                : maintenance + (maintenance == 1 ? " en mantenimiento" : " en mantenimiento"));
        operatorsTile.setValue(String.valueOf(attended), "de " + activeOperators + " operadores del peaje");
        ticketsTile.setValue(String.format("%,d", tickets).replace(',', '.'),
                Labels.variation(java.math.BigDecimal.valueOf(tickets),
                        java.math.BigDecimal.valueOf(yesterday.getVehicles()), "ayer a esta hora"));
        revenueTile.setValue(Charts.shortMoney(revenue.doubleValue()),
                Labels.variation(revenue, yesterday.getRevenue(), "ayer a esta hora"));

        rows.removeAll();
        if (booths.isEmpty()) {
            rows.add(message("El peaje no tiene casetas. Las agrega el administrador de la entidad."));
        }
        for (int i = 0; i < booths.size(); i++) {
            rows.add(row(booths.get(i), now, i == booths.size() - 1));
        }
        rows.revalidate();
        rows.repaint();

        chargeChips.removeAll();
        for (int i = 0; i < 4; i++) {
            chargeChips.add(i < charges.size() ? chip(charges.get(i)) : Box.createGlue());
        }
        if (charges.isEmpty()) {
            chargeChips.removeAll();
            chargeChips.add(Theme.muted("Todavía no hay cobros en el peaje."));
        }
        chargeChips.revalidate();
        chargeChips.repaint();
        onRefreshed.run();
    }

    private JComponent row(BoothActivity a, LocalDateTime now, boolean last) {
        String status = a.getBooth().getStatus();
        JPanel row = new JPanel(new GridLayout(1, 6, 10, 0));
        row.setBackground("MANTENIMIENTO".equals(status) ? new Color(0xFFFBEB) : Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, last ? 0 : 1, 0, new Color(0xF0F2F6)),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JLabel name = new JLabel("Caseta " + a.getBooth().getBoothNumber());
        name.setFont(Theme.font(Font.BOLD, 17));
        name.setForeground(Theme.TEXT);
        name.setIcon(new Dot(Labels.boothColor(status)));
        name.setIconTextGap(8);
        row.add(name);
        row.add(twoLines(Labels.direction(a.getBooth().getDirection()),
                Labels.paymentShort(a.getBooth().getPaymentType()), Theme.MUTED, Theme.MUTED, false));

        JPanel who = new JPanel(new BorderLayout(10, 0));
        who.setOpaque(false);
        if (a.isAttended(now)) {
            who.add(new Avatar(a.getLastOperator()), BorderLayout.WEST);
            who.add(twoLines(a.getLastOperator(), "en turno · " + a.getLastCharge().format(HOUR),
                    Theme.TEXT, Theme.MUTED, true), BorderLayout.CENTER);
        } else {
            who.add(twoLines("Sin operador", a.getLastCharge() == null ? "sin cobros hoy"
                    : "último cobro " + a.getLastCharge().format(HOUR), Theme.MUTED, Theme.MUTED, false),
                    BorderLayout.CENTER);
        }
        row.add(who);
        row.add(number(String.format("%,d", a.getTickets()).replace(',', '.'), "tiquetes hoy"));
        row.add(number(Charts.shortMoney(a.getRevenue().doubleValue()), "recaudado"));

        JPanel statusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        statusBox.setOpaque(false);
        statusBox.add(statusButton(a));
        row.add(statusBox);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /** Pastilla del estado con una flecha: al hacer clic se escoge el estado nuevo. */
    private JComponent statusButton(BoothActivity a) {
        String status = a.getBooth().getStatus();
        Color[] colors = Labels.boothPillColors(status);
        Pill pill = new Pill(Labels.boothStatus(status) + "  ▾", colors[0], colors[1]);
        pill.setFont(Theme.font(Font.BOLD, 13));
        pill.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.setToolTipText("Cambiar el estado de la caseta " + a.getBooth().getBoothNumber());
        pill.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                for (String option : Labels.BOOTH_STATUSES) {
                    JMenuItem item = new JMenuItem(Labels.boothStatus(option) + (option.equals(status) ? "  ✓" : ""),
                            new Dot(Labels.boothColor(option)));
                    item.setFont(Theme.font(option.equals(status) ? Font.BOLD : Font.PLAIN, 14));
                    item.setEnabled(!option.equals(status));
                    item.addActionListener(ev -> changeStatus(a, option));
                    menu.add(item);
                }
                menu.show(pill, 0, pill.getHeight() + 4);
            }
        });
        return pill;
    }

    private void changeStatus(BoothActivity a, String status) {
        Async.run(() -> {
            service.changeBoothStatus(session.getTollId(), a.getBooth(), status);
            return Boolean.TRUE;
        }, ok -> refresh(), error -> Forms.info(this, "Casetas del peaje", error.getMessage()));
    }

    private JComponent chip(BoothActivity.Charge c) {
        JPanel chip = new JPanel(new BorderLayout(0, 2));
        chip.setBackground(new Color(0xF8FAFC));
        chip.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JLabel top = new JLabel("<html><b>" + c.getPlate() + "</b> <span style='color:#6B7280'>· caseta "
                + c.getBoothNumber() + "</span></html>");
        top.setFont(Theme.font(Font.PLAIN, 13));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel when = new JLabel(c.getPaidAt().format(HOUR) + " · " + Labels.method(c.getMethod()));
        when.setFont(Theme.font(Font.PLAIN, 12));
        when.setForeground(Theme.MUTED);
        JLabel value = new JLabel(Theme.money(c.getAmount()));
        value.setFont(Theme.font(Font.BOLD, 14));
        value.setForeground(Theme.TEXT);
        bottom.add(when, BorderLayout.WEST);
        bottom.add(value, BorderLayout.EAST);
        chip.add(top, BorderLayout.NORTH);
        chip.add(bottom, BorderLayout.SOUTH);
        return chip;
    }

    private void showAllCharges() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Hora", "Caseta", "Tiquete", "Placa", "Operador", "Pago", "Valor"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        for (BoothActivity.Charge c : charges) {
            if (c.getPaidAt().isBefore(today)) {
                continue;
            }
            model.addRow(new Object[]{c.getPaidAt().format(HOUR), c.getBoothNumber(), c.getTicket(), c.getPlate(),
                c.getOperator() == null ? "—" : c.getOperator(), Labels.method(c.getMethod()),
                Theme.money(c.getAmount())});
        }
        JTable table = new JTable(model);
        Forms.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 380));
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        Forms.dialog(this, "Cobros de hoy", "Los últimos " + CHARGES + " cobros del peaje "
                + session.getTollName() + ".", scroll, null, false, () -> Boolean.TRUE, ok -> { });
    }

    private static JComponent twoLines(String top, String bottom, Color topColor, Color bottomColor, boolean bold) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);
        JLabel a = new JLabel(top);
        a.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, 14));
        a.setForeground(topColor);
        JLabel b = new JLabel(bottom);
        b.setFont(Theme.font(Font.PLAIN, 12));
        b.setForeground(bottomColor);
        panel.add(a);
        panel.add(b);
        return panel;
    }

    private static JComponent number(String value, String caption) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);
        JLabel a = new JLabel(value, JLabel.RIGHT);
        a.setFont(Theme.font(Font.BOLD, 16));
        a.setForeground(Theme.TEXT);
        JLabel b = new JLabel(caption, JLabel.RIGHT);
        b.setFont(Theme.font(Font.PLAIN, 11));
        b.setForeground(Theme.MUTED);
        panel.add(a);
        panel.add(b);
        return panel;
    }

    private static JComponent message(String text) {
        JLabel label = Theme.muted(text);
        label.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return label;
    }

    /** Punto de color para el estado de la caseta. */
    private static final class Dot implements javax.swing.Icon {
        private final Color color;

        Dot(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, 10, 10);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }

    /** Círculo azul claro con la inicial del operador. */
    private static final class Avatar extends JComponent {
        private final String initial;

        Avatar(String name) {
            initial = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - 34) / 2;
            g2.setColor(new Color(0xDBEAFE));
            g2.fillOval(0, y, 34, 34);
            g2.setColor(new Color(0x1D4ED8));
            g2.setFont(Theme.font(Font.BOLD, 14));
            int w = g2.getFontMetrics().stringWidth(initial);
            g2.drawString(initial, 17 - w / 2, y + 22);
            g2.dispose();
        }
    }
}
