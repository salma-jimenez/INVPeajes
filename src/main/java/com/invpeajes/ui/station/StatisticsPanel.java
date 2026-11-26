package com.invpeajes.ui.station;

import com.invpeajes.model.AssistanceType;
import com.invpeajes.model.CashierSession;
import com.invpeajes.model.StatPeriod;
import com.invpeajes.model.TollStatistics;
import com.invpeajes.service.CashierService;
import com.invpeajes.service.StationManagerService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.Charts;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.StatTile;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Estadísticas del peaje para el jefe de estación, por día, semana, mes, semestre o año,
 * comparadas con el periodo anterior: recaudo, vehículos, promedio diario, mejor día (u hora pico),
 * gráfica del periodo, vehículos por categoría, tabla de todos los periodos, medios de pago y
 * asistencias.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class StatisticsPanel extends JPanel {

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII"};
    private static final Color[] CATEGORY_COLORS = {new Color(0x2563EB), new Color(0x3B82F6), new Color(0x60A5FA),
        new Color(0x93C5FD), new Color(0xF59E0B), new Color(0xFB923C), new Color(0xDC2626)};
    private static final String[] METHODS = {CashierService.CASH, CashierService.TAG, CashierService.CARD};
    private static final String[] METHOD_NAMES = {"Efectivo", "TAG Colpass", "Tarjeta"};
    private static final Color[] METHOD_COLORS = {Theme.WARNING, Theme.PURPLE, Theme.PRIMARY};

    private final StationManagerService service;
    private final CashierSession session;
    private final Map<StatPeriod, FlatButton> periodButtons = new EnumMap<>(StatPeriod.class);
    private final JComboBox<LocalDate> startCombo = new JComboBox<>();
    private final JLabel comparing = Theme.muted(" ");
    private StatPeriod period = StatPeriod.MONTH;
    private boolean filling;

    private final StatTile revenueTile = new StatTile("Recaudado", Theme.SUCCESS, 30);
    private final StatTile vehiclesTile = new StatTile("Vehículos", Theme.PRIMARY, 24);
    private final StatTile averageTile = new StatTile("Promedio diario", Theme.TEXT, 24);
    private final StatTile bestTile = new StatTile("Mejor día", Theme.PURPLE, 24);
    private final JLabel chartTitle = Theme.title("Recaudo");
    private final Charts.LineChart lineChart = new Charts.LineChart();
    private final Charts.BarChart categoryChart = new Charts.BarChart();
    private final JLabel categoryNote = Theme.muted(" ");
    private final JPanel periodsTable = new JPanel(new GridBagLayout());
    private final Charts.StackedBar methodsBar = new Charts.StackedBar();
    private final JLabel[] methodValues = new JLabel[METHODS.length];
    private final JLabel methodsNote = Theme.muted(" ");
    private final JLabel attendedValue = big(Theme.SUCCESS);
    private final JLabel cancelledValue = big(Theme.TEXT);
    private final JLabel arrivalValue = big(Theme.PRIMARY);
    private final Charts.StackedBar typesBar = new Charts.StackedBar();
    private final JPanel typesLegend = new JPanel(new GridLayout(2, 2, 6, 2));
    private final JLabel arrivalNote = new JLabel(" ");

    /**
     * @param service Servicio del jefe de estación.
     * @param session Sesión del jefe.
     */
    public StatisticsPanel(StationManagerService service, CashierSession session) {
        super(new GridBagLayout());
        this.service = service;
        this.session = session;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));

        add(buildToolbar(), gbc(0, 0, 1, 0, 0));
        add(buildTiles(), gbc(0, 1, 1, 0, 0));
        add(buildCharts(), gbc(0, 2, 1, 1, 0));
        add(buildBottom(), gbc(0, 3, 1, 0, 0));
        selectPeriod(StatPeriod.MONTH);
    }

    /** Vuelve a cargar las estadísticas del periodo elegido. */
    public void refresh() {
        LocalDate start = (LocalDate) startCombo.getSelectedItem();
        if (start == null) {
            return;
        }
        StatPeriod selected = period;
        Async.run(() -> service.statistics(session.getTollId(), selected, start), this::fill,
                e -> JOptionPane.showMessageDialog(this, e.getMessage(), "Estadísticas", JOptionPane.WARNING_MESSAGE));
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        for (StatPeriod p : StatPeriod.values()) {
            FlatButton b = new FlatButton(p.getLabel());
            b.setFont(Theme.font(Font.BOLD, 13));
            b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
            b.addActionListener(e -> selectPeriod(p));
            periodButtons.put(p, b);
            left.add(b);
        }
        startCombo.setFont(Theme.font(Font.PLAIN, 13));
        startCombo.setBackground(Color.WHITE);
        startCombo.setPreferredSize(new Dimension(190, 34));
        startCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof LocalDate) {
                    setText(period.describe((LocalDate) value));
                }
                return this;
            }
        });
        startCombo.addActionListener(e -> {
            if (!filling) {
                refresh();
            }
        });
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(12, 1));
        left.add(spacer);
        left.add(startCombo);
        bar.add(left, BorderLayout.WEST);
        bar.add(comparing, BorderLayout.EAST);
        return bar;
    }

    private void selectPeriod(StatPeriod p) {
        period = p;
        for (Map.Entry<StatPeriod, FlatButton> e : periodButtons.entrySet()) {
            if (e.getKey() == p) {
                e.getValue().setColors(Theme.PRIMARY, Theme.PRIMARY_DARK, Color.WHITE, null);
            } else {
                e.getValue().setColors(Color.WHITE, Theme.BACKGROUND, Theme.TEXT, Theme.INPUT_BORDER);
            }
        }
        filling = true;
        startCombo.removeAllItems();
        p.recentStarts(LocalDate.now()).forEach(startCombo::addItem);
        filling = false;
        comparing.setText("Comparado con " + p.getPreviousLabel());
        refresh();
    }

    private JComponent buildTiles() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.add(revenueTile, gbc(0, 0, 1.35, 1, 12));
        row.add(vehiclesTile, gbc(1, 0, 1, 1, 12));
        row.add(averageTile, gbc(2, 0, 1, 1, 12));
        row.add(bestTile, gbc(3, 0, 1, 1, 0));
        return row;
    }

    private JComponent buildCharts() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        JPanel line = card();
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(chartTitle, BorderLayout.WEST);
        JLabel legend = new JLabel("<html><span style='color:#2563EB'>━</span> Este periodo &nbsp; "
                + "<span style='color:#9CA3AF'>┅</span> Periodo anterior</html>");
        legend.setFont(Theme.font(Font.PLAIN, 12));
        legend.setForeground(Theme.MUTED);
        head.add(legend, BorderLayout.EAST);
        line.add(head, BorderLayout.NORTH);
        line.add(lineChart, BorderLayout.CENTER);
        JPanel bars = card();
        bars.add(Theme.title("Vehículos por categoría"), BorderLayout.NORTH);
        bars.add(categoryChart, BorderLayout.CENTER);
        categoryNote.setFont(Theme.font(Font.PLAIN, 12));
        bars.add(categoryNote, BorderLayout.SOUTH);
        row.add(line, gbc(0, 0, 1.3, 1, 12));
        row.add(bars, gbc(1, 0, 1, 1, 0));
        return row;
    }

    private JComponent buildBottom() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        JPanel periods = card();
        periods.add(Theme.title("Todos los periodos"), BorderLayout.NORTH);
        periodsTable.setOpaque(false);
        periods.add(periodsTable, BorderLayout.CENTER);

        JPanel methods = card();
        methods.add(Theme.title("Medios de pago"), BorderLayout.NORTH);
        JPanel methodBody = new JPanel(new GridLayout(0, 1, 0, 6));
        methodBody.setOpaque(false);
        methodsBar.setPreferredSize(new Dimension(200, 14));
        methodBody.add(methodsBar);
        for (int i = 0; i < METHODS.length; i++) {
            JPanel r = new JPanel(new BorderLayout());
            r.setOpaque(false);
            JLabel name = new JLabel("<html><span style='color:" + hex(METHOD_COLORS[i]) + "'>●</span> "
                    + METHOD_NAMES[i] + "</html>");
            name.setFont(Theme.font(Font.PLAIN, 13));
            methodValues[i] = new JLabel("$0", SwingConstants.RIGHT);
            methodValues[i].setFont(Theme.font(Font.BOLD, 13));
            r.add(name, BorderLayout.WEST);
            r.add(methodValues[i], BorderLayout.EAST);
            methodBody.add(r);
        }
        methodsNote.setFont(Theme.font(Font.PLAIN, 12));
        methodBody.add(methodsNote);
        methods.add(methodBody, BorderLayout.CENTER);

        JPanel assist = card();
        assist.add(Theme.title("Asistencias del periodo"), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        JPanel numbers = new JPanel(new GridLayout(2, 3));
        numbers.setOpaque(false);
        numbers.add(attendedValue);
        numbers.add(cancelledValue);
        numbers.add(arrivalValue);
        numbers.add(small("atendidas"));
        numbers.add(small("canceladas"));
        numbers.add(small("llegada prom."));
        body.add(numbers, BorderLayout.NORTH);
        JPanel types = new JPanel(new BorderLayout(0, 6));
        types.setOpaque(false);
        typesBar.setPreferredSize(new Dimension(200, 12));
        types.add(typesBar, BorderLayout.NORTH);
        typesLegend.setOpaque(false);
        types.add(typesLegend, BorderLayout.CENTER);
        body.add(types, BorderLayout.CENTER);
        arrivalNote.setFont(Theme.font(Font.BOLD, 12));
        body.add(arrivalNote, BorderLayout.SOUTH);
        assist.add(body, BorderLayout.CENTER);

        row.add(periods, gbc(0, 0, 1.3, 1, 12));
        row.add(methods, gbc(1, 0, 0.8, 1, 12));
        row.add(assist, gbc(2, 0, 1, 1, 0));
        return row;
    }

    private void fill(TollStatistics s) {
        String name = s.getPeriod().describe(s.getStart());
        revenueTile.setValue(Theme.money(s.getTotal().getRevenue()), "");
        Double rv = s.getRevenueVariation();
        BigDecimal diff = s.getTotal().getRevenue().subtract(s.getPreviousTotal().getRevenue());
        revenueTile.setNote(variationText(rv) + "  " + (diff.signum() >= 0 ? Charts.shortMoney(diff.doubleValue())
                + " más que " : Charts.shortMoney(diff.negate().doubleValue()) + " menos que ")
                + s.getPeriod().getPreviousLabel(), color(rv));
        revenueTileTitle("Recaudado · " + name);
        comparing.setText("Comparado con " + s.getPeriod().getPreviousLabel()
                + (!s.isPartial() ? "" : s.getPeriod() == StatPeriod.DAY ? " (hasta la misma hora)" : " (mismos días)"));

        vehiclesTile.setValue(String.format(Locale.ROOT, "%,d", s.getTotal().getVehicles()).replace(',', '.'), "");
        vehiclesTile.setNote(variationText(s.getVehiclesVariation()), color(s.getVehiclesVariation()));
        boolean hourly = s.getPeriod() == StatPeriod.DAY;
        if (hourly) {
            int hours = s.isPartial() ? java.time.LocalTime.now().getHour() + 1 : 24;
            setTileTitle(averageTile, "Promedio por hora");
            averageTile.setValue(Charts.shortMoney(s.getTotal().getRevenue().doubleValue() / hours),
                    Math.round(s.getTotal().getVehicles() / (double) hours) + " vehículos/hora");
        } else {
            setTileTitle(averageTile, "Promedio diario");
            averageTile.setValue(Charts.shortMoney(s.getAverageDailyRevenue().doubleValue()),
                    s.getAverageDailyVehicles() + " vehículos/día");
        }
        bestTileTitle(hourly ? "Hora pico" : "Mejor día");
        if (s.getBestLabel() == null) {
            bestTile.setValue("—", "sin cobros en el periodo");
        } else {
            bestTile.setValue(s.getBestLabel(), Charts.shortMoney(s.getBestTotal().getRevenue().doubleValue())
                    + " · " + s.getBestTotal().getVehicles() + " veh.");
        }

        chartTitle.setText((hourly ? "Recaudo por hora" : s.getPeriod().grouping() == StatPeriod.Grouping.DAY
                ? "Recaudo diario" : "Recaudo mensual") + " vs. " + s.getPeriod().getPreviousLabel());
        List<String> labels = new ArrayList<>();
        List<BigDecimal> cur = new ArrayList<>();
        List<BigDecimal> prev = new ArrayList<>();
        s.getSeries().forEach(p -> {
            labels.add(p.getLabel());
            cur.add(p.getCurrent());
            prev.add(p.getPrevious());
        });
        lineChart.setData(labels, cur, prev);

        List<String> cats = new ArrayList<>();
        List<Double> counts = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < ROMAN.length; i++) {
            cats.add(ROMAN[i]);
            counts.add(s.getVehiclesByCategory().getOrDefault(i + 1, 0L).doubleValue());
            colors.add(CATEGORY_COLORS[i]);
        }
        categoryChart.setData(cats, counts, colors);
        long top = s.getVehiclesByCategory().values().stream().mapToLong(Long::longValue).max().orElse(0);
        categoryNote.setText(top == 0 ? "Sin cobros en el periodo" : "I: automóviles · II: buses · III–VII: camiones");

        fillPeriods(s);

        List<Double> methodAmounts = new ArrayList<>();
        BigDecimal methodTotal = BigDecimal.ZERO;
        for (int i = 0; i < METHODS.length; i++) {
            BigDecimal v = s.getRevenueByMethod().getOrDefault(METHODS[i], BigDecimal.ZERO);
            methodValues[i].setText(Charts.shortMoney(v.doubleValue()));
            methodAmounts.add(v.doubleValue());
            methodTotal = methodTotal.add(v);
        }
        methodsBar.setData(methodAmounts, List.of(METHOD_COLORS));
        methodsNote.setText(methodTotal.signum() == 0 ? "Sin cobros en el periodo"
                : "Efectivo: " + Math.round(methodAmounts.get(0) * 100 / methodTotal.doubleValue()) + " % del recaudo");

        attendedValue.setText(String.valueOf(s.getAttended()));
        cancelledValue.setText(String.valueOf(s.getCancelled()));
        arrivalValue.setText(s.getAverageArrivalMinutes() == null ? "—"
                : Math.round(s.getAverageArrivalMinutes()) + " min");
        List<Double> typeValues = new ArrayList<>();
        List<Color> typeColors = new ArrayList<>();
        typesLegend.removeAll();
        for (AssistanceType t : new AssistanceType[]{AssistanceType.TOW_TRUCK, AssistanceType.MECHANICAL_AID,
            AssistanceType.AMBULANCE, AssistanceType.MOBILE_WORKSHOP}) {
            int n = s.getAttendedByType().getOrDefault(t, 0);
            typeValues.add((double) n);
            typeColors.add(StationTexts.typeColor(t));
            JLabel l = new JLabel("<html><span style='color:" + hex(StationTexts.typeColor(t)) + "'>●</span> "
                    + StationTexts.type(t) + " · " + n + "</html>");
            l.setFont(Theme.font(Font.PLAIN, 12));
            l.setForeground(Theme.TEXT);
            typesLegend.add(l);
        }
        typesBar.setData(typeValues, typeColors);
        Double now = s.getAverageArrivalMinutes();
        Double before = s.getPreviousAverageArrivalMinutes();
        if (now != null && before != null) {
            long delta = Math.round(before - now);
            arrivalNote.setText(delta >= 0 ? "▼ " + delta + " min más rápido que " + s.getPeriod().getPreviousLabel()
                    : "▲ " + (-delta) + " min más lento que " + s.getPeriod().getPreviousLabel());
            arrivalNote.setForeground(delta >= 0 ? Theme.SUCCESS : Theme.DANGER);
        } else {
            arrivalNote.setText(" ");
        }
        revalidate();
        repaint();
    }

    private void fillPeriods(TollStatistics s) {
        periodsTable.removeAll();
        String[] heads = {"Periodo", "Vehículos", "Recaudado", "vs. anterior"};
        for (int c = 0; c < heads.length; c++) {
            JLabel h = new JLabel(heads[c], c == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
            h.setFont(Theme.font(Font.BOLD, 12));
            h.setForeground(Theme.MUTED);
            periodsTable.add(h, cell(c, 0, false));
        }
        int r = 1;
        for (TollStatistics.PeriodRow row : s.getPeriods()) {
            boolean sel = row.isSelected();
            periodsTable.add(text(row.getLabel(), sel, SwingConstants.LEFT, Theme.TEXT), cell(0, r, sel));
            periodsTable.add(text(String.format(Locale.ROOT, "%,d", row.getTotal().getVehicles()).replace(',', '.'),
                    sel, SwingConstants.RIGHT, Theme.TEXT), cell(1, r, sel));
            periodsTable.add(text(Theme.money(row.getTotal().getRevenue()), true, SwingConstants.RIGHT, Theme.TEXT),
                    cell(2, r, sel));
            periodsTable.add(text(variationText(row.getVariation()), true, SwingConstants.RIGHT,
                    color(row.getVariation())), cell(3, r, sel));
            r++;
        }
    }

    private static JLabel text(String value, boolean bold, int align, Color color) {
        JLabel l = new JLabel(value, align);
        l.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, 13));
        l.setForeground(color);
        return l;
    }

    private static GridBagConstraints cell(int x, int y, boolean selected) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = x == 0 ? 1.3 : 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(y == 0 ? 0 : 5, 0, 0, x == 3 ? 0 : 10);
        return c;
    }

    private static String variationText(Double v) {
        if (v == null) {
            return "—";
        }
        String n = String.format(Locale.forLanguageTag("es-CO"), "%.1f %%", Math.abs(v));
        return (v >= 0 ? "▲ " : "▼ ") + n;
    }

    private static Color color(Double v) {
        return v == null ? Theme.MUTED : v >= 0 ? Theme.SUCCESS : Theme.DANGER;
    }

    private void revenueTileTitle(String title) {
        setTileTitle(revenueTile, title);
    }

    private void bestTileTitle(String title) {
        setTileTitle(bestTile, title);
    }

    /** Cambia el título de una tarjeta (el primer texto de la tarjeta). */
    private static void setTileTitle(StatTile tile, String title) {
        JPanel text = (JPanel) tile.getComponent(0);
        ((JLabel) text.getComponent(0)).setText(title);
    }

    private static JPanel card() {
        JPanel panel = Theme.card();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        return panel;
    }

    private static JLabel big(Color color) {
        JLabel l = new JLabel("—", SwingConstants.CENTER);
        l.setFont(Theme.font(Font.BOLD, 22));
        l.setForeground(color);
        return l;
    }

    private static JLabel small(String text) {
        JLabel l = Theme.muted(text);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(Theme.font(Font.PLAIN, 12));
        return l;
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static GridBagConstraints gbc(int x, int y, double wx, double wy, int rightGap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = wx;
        c.weighty = wy;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, x == 0 && y >= 0 && rightGap == 0 && wx == 1 ? 12 : 0, rightGap);
        return c;
    }
}
