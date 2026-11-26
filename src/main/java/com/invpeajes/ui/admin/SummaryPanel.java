package com.invpeajes.ui.admin;

import com.invpeajes.model.AdminSummary;
import com.invpeajes.model.AdminSummary.Pending;
import com.invpeajes.model.StatPeriod;
import com.invpeajes.service.AdminService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Charts;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * "Resumen" del administrador: recaudo de hoy y del mes de los peajes de su entidad, vehículos,
 * asistencias activas, recaudo por mes del año, los peajes con más recaudo y "Pendientes de tus
 * peajes" (avisos que el programa calcula solo: peajes sin tarifa, siniestrados, sin jefe de
 * estación, muy cercanos y tarifas viejas).
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class SummaryPanel extends JPanel implements AdminPage {

    private final AdminService service;
    private final StatTile todayTile = new StatTile("Recaudado hoy", Theme.SUCCESS, 26);
    private final StatTile monthTile = new StatTile("Recaudado en el mes", Theme.PRIMARY, 26);
    private final StatTile vehiclesTile = new StatTile("Vehículos hoy", Theme.TEXT, 26);
    private final StatTile assistanceTile = new StatTile("Asistencias activas", Theme.WARNING, 26);
    private final MonthBars months = new MonthBars();
    private final JLabel monthsTitle = Theme.title("Recaudo por mes");
    private final JPanel topTolls = new JPanel();
    private final JPanel pendings = new JPanel();
    private AdminContext context;

    /**
     * @param service Servicio del administrador.
     * @param showTolls Abre la pantalla de Peajes.
     */
    public SummaryPanel(AdminService service, Runnable showTolls) {
        super(new BorderLayout(0, 14));
        this.service = service;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(todayTile);
        tiles.add(monthTile);
        tiles.add(vehiclesTile);
        tiles.add(assistanceTile);

        JPanel monthsCard = Theme.card();
        JPanel monthsHead = new JPanel(new BorderLayout());
        monthsHead.setOpaque(false);
        monthsHead.add(monthsTitle, BorderLayout.WEST);
        monthsHead.add(Theme.muted("el mes actual va hasta hoy"), BorderLayout.EAST);
        monthsCard.add(monthsHead, BorderLayout.NORTH);
        monthsCard.add(months, BorderLayout.CENTER);

        JPanel topCard = Theme.card();
        topCard.add(Theme.title("Peajes con más recaudo este mes"), BorderLayout.NORTH);
        topTolls.setLayout(new BoxLayout(topTolls, BoxLayout.Y_AXIS));
        topTolls.setOpaque(false);
        topCard.add(topTolls, BorderLayout.CENTER);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 14));
        left.setOpaque(false);
        left.add(monthsCard);
        left.add(topCard);

        JPanel pendingCard = Theme.card();
        pendingCard.setPreferredSize(new Dimension(370, 0));
        pendingCard.add(Theme.title("Pendientes de tus peajes"), BorderLayout.NORTH);
        pendings.setLayout(new BoxLayout(pendings, BoxLayout.Y_AXIS));
        pendings.setOpaque(false);
        JPanel pendingHolder = new JPanel(new BorderLayout());
        pendingHolder.setOpaque(false);
        pendingHolder.add(pendings, BorderLayout.NORTH);
        pendingCard.add(pendingHolder, BorderLayout.CENTER);
        FlatButton go = Theme.secondaryButton("Ver peajes");
        go.addActionListener(e -> showTolls.run());
        pendingCard.add(go, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(left, BorderLayout.CENTER);
        center.add(pendingCard, BorderLayout.EAST);

        add(tiles, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void setContext(AdminContext context) {
        this.context = context;
    }

    @Override
    public String subtitle(AdminContext context) {
        return "Cómo van los " + context.getMine().size() + " peajes de " + Labels.entity(context.getLevel());
    }

    @Override
    public void refresh() {
        if (context == null) {
            return;
        }
        AdminContext ctx = context;
        Async.run(() -> service.summary(ctx.getLevel(), ctx.getMine()), this::fill,
                error -> {
                    pendings.removeAll();
                    pendings.add(Theme.muted("<html>No se pudo cargar el resumen: " + error.getMessage() + "</html>"));
                    pendings.revalidate();
                });
    }

    private void fill(AdminSummary s) {
        LocalDate today = LocalDate.now();
        String monthName = StatPeriod.monthName(today.getMonthValue()).toLowerCase();
        todayTile.setValue(Charts.shortMoney(s.getToday().getRevenue().doubleValue()),
                Labels.variation(s.getToday().getRevenue(), s.getYesterdaySameTime().getRevenue(),
                        "ayer a esta hora"));
        monthTile.setValue(Charts.shortMoney(s.getMonth().getRevenue().doubleValue()),
                Labels.variation(s.getMonth().getRevenue(), s.getPreviousMonthSameDays().getRevenue(),
                        "el mes pasado (mismos días)"));
        monthTile.setTitleText("Recaudado en " + monthName);
        vehiclesTile.setValue(String.format("%,d", s.getToday().getVehicles()).replace(',', '.'),
                "en " + s.getOperatingTolls() + " peajes operativos");
        assistanceTile.setValue(String.valueOf(s.getActiveAssistances()),
                s.getUnattendedAssistances() == 0 ? "todas tienen recurso asignado"
                        : s.getUnattendedAssistances() + " sin atender");

        monthsTitle.setText("Recaudo por mes · " + today.getYear());
        months.setData(s.getRevenueByMonth());

        topTolls.removeAll();
        BigDecimal max = s.getTopTolls().values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        for (Map.Entry<String, BigDecimal> e : s.getTopTolls().entrySet()) {
            topTolls.add(new HBar(e.getKey(), e.getValue(), max));
            topTolls.add(Box.createVerticalStrut(6));
        }
        if (s.getTopTolls().isEmpty()) {
            topTolls.add(Theme.muted("Todavía no hay cobros este mes."));
        }
        topTolls.revalidate();
        topTolls.repaint();

        pendings.removeAll();
        if (s.getPendings().isEmpty()) {
            pendings.add(item(new Color(0xDCFCE7), new Color(0x15803D), "✓", "Todo al día",
                    "Tus peajes tienen tarifas, jefe de estación y ninguno está siniestrado."));
        }
        for (Pending p : s.getPendings()) {
            Color[] c = colors(p.getKind());
            pendings.add(item(c[0], c[1], icon(p.getKind()), p.getTitle(), p.getDetail()));
        }
        pendings.revalidate();
        pendings.repaint();
    }

    private static JComponent item(Color bg, Color fg, String icon, String title, String detail) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF0F2F6)),
                BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        JLabel box = new JLabel(icon, JLabel.CENTER);
        box.setOpaque(true);
        box.setBackground(bg);
        box.setForeground(fg);
        box.setFont(Theme.font(Font.BOLD, 12));
        box.setPreferredSize(new Dimension(24, 24));
        JPanel boxHolder = new JPanel(new BorderLayout());
        boxHolder.setOpaque(false);
        boxHolder.add(box, BorderLayout.NORTH);
        JLabel text = new JLabel("<html><div style='width:205px'><b>" + title + "</b><br><span style='color:#6B7280'>"
                + detail + "</span></div></html>");
        text.setFont(Theme.font(Font.PLAIN, 13));
        text.setForeground(Theme.TEXT);
        row.add(boxHolder, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private static Color[] colors(Pending.Kind kind) {
        switch (kind) {
            case NO_RATES:
                return new Color[]{new Color(0xFEF3C7), new Color(0xB45309)};
            case NO_MANAGER:
                return new Color[]{new Color(0xEDE9FE), new Color(0x6D28D9)};
            case OLD_RATES:
                return new Color[]{new Color(0xDBEAFE), new Color(0x1D4ED8)};
            default:
                return new Color[]{new Color(0xFEE2E2), new Color(0xB91C1C)};
        }
    }

    private static String icon(Pending.Kind kind) {
        switch (kind) {
            case NO_MANAGER:
                return "J";
            case CLOSE_TOLLS:
                return "↔";
            case OLD_RATES:
                return "$";
            default:
                return "!";
        }
    }

    /** Barras del recaudo de cada mes del año; el mes actual en azul claro. */
    private static final class MonthBars extends JComponent {
        private static final String[] NAMES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep",
            "Oct", "Nov", "Dic"};
        private final List<BigDecimal> values = new ArrayList<>();

        MonthBars() {
            setPreferredSize(new Dimension(500, 170));
        }

        void setData(Map<Integer, BigDecimal> byMonth) {
            values.clear();
            values.addAll(byMonth.values());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int n = 12;
            int left = 54;
            int bottom = 20;
            int top = 16;
            int h = getHeight() - top - bottom;
            double max = values.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);
            double step = niceStep(max / 4);
            g2.setFont(Theme.font(Font.PLAIN, 10));
            for (int k = 0; k <= 4; k++) {
                int y = top + h - (int) (k * step / (step * 4) * h);
                g2.setColor(new Color(0xEEF1F5));
                g2.drawLine(left, y, getWidth(), y);
                g2.setColor(new Color(0x9CA3AF));
                g2.drawString(Charts.shortMoney(k * step), 0, y + 4);
            }
            double slot = (getWidth() - left) / (double) n;
            int barW = (int) Math.min(36, slot * 0.6);
            for (int i = 0; i < n; i++) {
                int x = (int) (left + i * slot + (slot - barW) / 2);
                if (i < values.size() && step > 0) {
                    int bh = (int) (values.get(i).doubleValue() / (step * 4) * h);
                    g2.setColor(i == values.size() - 1 ? new Color(0x93C5FD) : Theme.PRIMARY);
                    g2.fillRoundRect(x, top + h - bh, barW, bh, 6, 6);
                }
                g2.setColor(i < values.size() ? Theme.MUTED : new Color(0xC4C9D2));
                g2.setFont(Theme.font(Font.PLAIN, 11));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(NAMES[i], x + (barW - fm.stringWidth(NAMES[i])) / 2, getHeight() - 4);
            }
            g2.dispose();
        }

        private static double niceStep(double raw) {
            if (raw <= 0) {
                return 1;
            }
            double p = Math.pow(10, Math.floor(Math.log10(raw)));
            for (double m : new double[]{1, 2, 2.5, 5, 10}) {
                if (raw <= m * p) {
                    return m * p;
                }
            }
            return 10 * p;
        }
    }

    /** Barra horizontal con el nombre del peaje y su recaudo. */
    private static final class HBar extends JComponent {
        private final String name;
        private final BigDecimal value;
        private final BigDecimal max;

        HBar(String name, BigDecimal value, BigDecimal max) {
            this.name = name;
            this.value = value;
            this.max = max;
            setPreferredSize(new Dimension(400, 22));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(Theme.font(Font.PLAIN, 13));
            g2.setColor(Theme.TEXT);
            g2.drawString(name, 0, 15);
            String money = Charts.shortMoney(value.doubleValue());
            g2.setFont(Theme.font(Font.BOLD, 13));
            int moneyW = g2.getFontMetrics().stringWidth(money);
            int x0 = 140;
            int width = getWidth() - x0 - moneyW - 14;
            int w = max.signum() == 0 ? 0 : (int) (value.doubleValue() / max.doubleValue() * width);
            g2.setColor(Theme.PRIMARY);
            g2.fillRoundRect(x0, 6, Math.max(w, 4), 10, 10, 10);
            g2.setColor(Theme.TEXT);
            g2.drawString(money, x0 + Math.max(w, 4) + 8, 15);
            g2.dispose();
        }
    }
}
